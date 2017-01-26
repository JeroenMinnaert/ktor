package org.jetbrains.ktor.tests.nio

import org.jetbrains.ktor.cio.*
import org.junit.*
import java.io.*
import kotlin.test.*

class FileChannelTest {
    private val sandbox = File("target/files")
    private lateinit var temp: File

    @Before
    fun setUp() {
        if (!sandbox.mkdirs() && !sandbox.isDirectory) {
            fail()
        }

        temp = File.createTempFile("file", "", sandbox)
    }

    @Test
    fun testEmptyFileDefaults() {
        assertEquals(0, temp.readChannel().toInputStream().use { it.readBytes().size })
    }

    @Test
    fun testSingleByteFile() {
        temp.writeBytes(byteArrayOf(7))

        assertEquals(listOf(7.toByte()), temp.readChannel().toInputStream().use { it.readBytes().toList() })
    }

    @Test
    fun testSingleByteFileOffsetEnd() {
        temp.writeBytes(byteArrayOf(7))

        assertEquals(0, temp.readChannel(start = 1L).toInputStream().use { it.readBytes().size })
    }

    @Test
    fun testSingleByteDrop1Take1() {
        temp.writeBytes(byteArrayOf(7, 8, 9))

        assertEquals(listOf(8.toByte()), temp.readChannel(start = 1L, endInclusive = 1L).toInputStream().use { it.readBytes().toList() })
    }

    @Test
    fun test3Bytes() {
        temp.writeBytes(byteArrayOf(7, 8, 9))

        assertEquals(byteArrayOf(7, 8, 9).toList(), temp.readChannel().toInputStream().use { it.readBytes().toList() })
    }
}