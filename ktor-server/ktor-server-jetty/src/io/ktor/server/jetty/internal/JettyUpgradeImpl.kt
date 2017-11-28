package io.ktor.server.jetty.internal

import io.ktor.cio.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.server.jetty.*
import io.ktor.server.servlet.*
import kotlinx.coroutines.experimental.*
import org.eclipse.jetty.io.*
import org.eclipse.jetty.server.*
import java.util.concurrent.Executor
import javax.servlet.http.*
import kotlin.coroutines.experimental.*

object JettyUpgradeImpl : ServletUpgrade {
    suspend override fun performUpgrade(upgrade: OutgoingContent.ProtocolUpgrade, servletRequest: HttpServletRequest, servletResponse: HttpServletResponse, engineContext: CoroutineContext, userContext: CoroutineContext) {
        // Jetty doesn't support Servlet API's upgrade so we have to implement our own

        val connection = servletRequest.getAttribute(HttpConnection::class.qualifiedName) as Connection
        val inputChannel = EndPointReadChannel(connection.endPoint, Executor {
            launch(engineContext) {
                it.run()
            }
        }).toByteReadChannel()

        val outputChannel = EndPointWriteChannel(connection.endPoint).toByteWriteChannel()

        servletRequest.setAttribute(HttpConnection.UPGRADE_CONNECTION_ATTRIBUTE, inputChannel)
        val job = upgrade.upgrade(inputChannel, outputChannel, engineContext, userContext)
        job.invokeOnCompletion {
            connection.close()
        }
    }
}