package io.ktor.server.testing

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.pipeline.*
import io.ktor.server.engine.*
import io.ktor.util.*
import kotlinx.coroutines.experimental.*
import java.util.concurrent.*

class TestApplicationEngine(environment: ApplicationEngineEnvironment = createTestEnvironment()) : BaseApplicationEngine(environment, EnginePipeline()) {
    init {
        pipeline.intercept(EnginePipeline.Call) {
            call.application.execute(call)
        }
    }

    override fun start(wait: Boolean): ApplicationEngine {
        environment.start()
        return this
    }

    override fun stop(gracePeriod: Long, timeout: Long, timeUnit: TimeUnit) {
        environment.stop()
    }


    fun handleRequest(setup: TestApplicationRequest.() -> Unit): TestApplicationCall {
        return createCall(setup).apply { execute() }
    }

    fun handleWebSocket(uri: String, setup: TestApplicationRequest.() -> Unit): TestApplicationCall {
        val call = createCall {
            this.uri = uri
            addHeader(HttpHeaders.Connection, "Upgrade")
            addHeader(HttpHeaders.Upgrade, "websocket")
            addHeader(HttpHeaders.SecWebSocketKey, encodeBase64("test".toByteArray()))

            setup()
        }.apply { execute() }

        return call
    }

    fun createCall(setup: TestApplicationRequest.() -> Unit): TestApplicationCall {
        return TestApplicationCall(application).apply {
            setup(request)
        }
    }

    private fun TestApplicationCall.execute() = launch(Unconfined) {
        try {
            pipeline.execute(this@execute)
        } catch (t: Throwable) {
            response.complete(t)
        } finally {
            response.complete()
        }
    }
}