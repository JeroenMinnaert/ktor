package io.ktor.content

import io.ktor.cio.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.experimental.*
import java.io.*
import kotlin.coroutines.experimental.*

/**
 * Information about the content to be sent to the peer, recognized by an [ApplicationEngine]
 */
sealed class OutgoingContent {
    /**
     * Status code to set when sending this content
     */
    open val status: HttpStatusCode?
        get() = null

    /**
     * Headers to set when sending this content
     */
    open val headers: ValuesMap
        get() = ValuesMap.Empty

    /**
     * Variant of a [OutgoingContent] without payload
     */
    abstract class NoContent : OutgoingContent()

    /**
     * Variant of a [OutgoingContent] with payload read from [ReadChannel]
     *
     */
    abstract class ReadChannelContent : OutgoingContent() {
        /**
         * Provides [ReadChannel] from which engine will read the data and send it to peer
         */
        abstract fun readFrom(): ReadChannel
    }

    /**
     * Variant of a [OutgoingContent] with payload written to [WriteChannel]
     */
    abstract class WriteChannelContent : OutgoingContent() {
        /**
         * Receives [channel] provided by the engine and writes all data to it
         */
        abstract suspend fun writeTo(channel: WriteChannel)
    }

    /**
     * Variant of a [OutgoingContent] with payload represented as [ByteArray]
     */
    abstract class ByteArrayContent : OutgoingContent() {
        /**
         * Provides [ByteArray] which engine will send to peer
         */
        abstract fun bytes(): ByteArray
    }

    /**
     * Variant of a [OutgoingContent] for upgrading an HTTP connection
     */
    abstract class ProtocolUpgrade : OutgoingContent() {
        final override val status: HttpStatusCode?
            get() = HttpStatusCode.SwitchingProtocols

        /**
         * Upgrades an HTTP connection
         * @param input is a [ReadChannel] for an upgraded connection
         * @param output is a [WriteChannel] for an upgraded connection
         * @param closeable is a [Closeable] instance to call when upgraded connection terminates.
         * [closeable] is provided by particular engine implementation and it is up to host what should be done
         *  when this [closeable] is closed. For example for HTTP/1.x it may close socket connection.
         * @param engineContext is a [CoroutineContext] to execute non-blocking code, such as parsing or processing
         * @param userContext is a [CoroutineContext] to execute user-provided callbacks or code potentially blocking
         */
        abstract suspend fun upgrade(input: ReadChannel,
                                     output: WriteChannel,
                                     engineContext: CoroutineContext,
                                     userContext: CoroutineContext): Job
    }
}

fun OutgoingContent.contentLength(): Long? {
    if (this is Resource) {
        return contentLength
    }

    return headers[HttpHeaders.ContentLength]?.let(String::toLong)
}

fun OutgoingContent.contentType(): ContentType? {
    if (this is Resource) {
        return contentType
    }

    return headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
}
