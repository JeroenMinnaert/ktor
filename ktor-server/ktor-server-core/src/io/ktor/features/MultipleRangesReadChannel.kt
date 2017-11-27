package io.ktor.features

import io.ktor.cio.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*

object MultipleRangesReadChannel {
    fun create(source: RandomAccessReadChannel, ranges: List<LongRange>, fullLength: Long?, boundary: String, contentType: String): ReadChannel =
            CompositeReadChannel(ranges.build(fullLength, boundary, contentType) { range ->
                RangeReadChannel(source, range.start, range.length, closeSource = false)
            })

    fun create(source: ReadChannel, ranges: List<LongRange>, fullLength: Long?, boundary: String, contentType: String): ReadChannel {
        if (source is RandomAccessReadChannel) {
            return create(source, ranges, fullLength, boundary, contentType)
        }

        var position = 0L

        return CompositeReadChannel(ranges.build(fullLength, boundary, contentType) { range ->
            val skip = range.start - position
            position = range.endInclusive + 1

            RangeReadChannel(source, skip, range.length, closeSource = false)
        })
    }

    private fun List<LongRange>.build(fullLength: Long?, boundary: String, contentType: String, builder: (LongRange) -> ReadChannel): Sequence<() -> ReadChannel> {
        require(size > 1) { "There should be at least 2 file ranges" }

        return asSequence().flatMap { range ->
            sequenceOf({
                buildString {
                    append(boundary)
                    append("\r\n")

                    append(HttpHeaders.ContentType)
                    append(": ")
                    append(contentType)
                    append("\r\n")

                    append(HttpHeaders.ContentRange)
                    append(": ")
                    append(contentRangeHeaderValue(range, fullLength, RangeUnits.Bytes))
                    append("\r\n")

                    append("\r\n")
                }.toByteArray(Charsets.ISO_8859_1).toReadChannel()
            }, {
                builder(range)
            }, {
                buildString {
                    append("\r\n")
                }.toByteArray(Charsets.ISO_8859_1).toReadChannel()
            })
        } + sequenceOf({
            boundary.toByteArray(Charsets.ISO_8859_1).toReadChannel()
        })
    }
}

val NEWLINE = "\r\n".toByteArray(Charsets.ISO_8859_1)

fun writeMultipleRanges(
        channelProducer: (LongRange) -> ByteReadChannel,
        ranges: List<LongRange>,
        fullLength: Long?,
        boundary: String,
        contentType: String
): ByteReadChannel = writer(Unconfined, autoFlush = true) {
    for (range in ranges) {
        val current = channelProducer(range)
        channel.writeHeaders(range, boundary, contentType, fullLength)
        current.joinTo(channel, closeOnEnd = false)
        channel.writeFully(NEWLINE)
    }

    channel.writeFully(boundary.toByteArray(Charsets.ISO_8859_1))
}.channel


private suspend fun ByteWriteChannel.writeHeaders(range: LongRange, boundary: String, contentType: String, fullLength: Long?) {
    val headers = buildString {
        append(boundary)
        append("\r\n")

        append(HttpHeaders.ContentType)
        append(": ")
        append(contentType)
        append("\r\n")

        append(HttpHeaders.ContentRange)
        append(": ")
        append(contentRangeHeaderValue(range, fullLength, RangeUnits.Bytes))
        append("\r\n")

        append("\r\n")
    }.toByteArray(Charsets.ISO_8859_1)

    writeFully(headers)
}

