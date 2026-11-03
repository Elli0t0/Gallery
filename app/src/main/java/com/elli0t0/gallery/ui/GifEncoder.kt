package com.elli0t0.gallery.ui

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream

// Minimal GIF89a encoder: uniform 216-color cube palette, LZW compression,
// looping forever. Frames must all share the same width and height.
object GifEncoder {
    fun encode(frames: List<Bitmap>, delayHundredths: Int, out: OutputStream) {
        require(frames.isNotEmpty())
        val width = frames[0].width
        val height = frames[0].height
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(out, width)
        writeShort(out, height)
        out.write(0xF7) // GCT present, 8-bit color, 256 entries
        out.write(0)
        out.write(0)
        // Global palette: 216 cube colors, padded to 256 with black.
        for (i in 0 until 256) {
            if (i < 216) {
                out.write((i / 36) * 51)
                out.write(((i / 6) % 6) * 51)
                out.write((i % 6) * 51)
            } else {
                out.write(0); out.write(0); out.write(0)
            }
        }
        // NETSCAPE2.0 looping extension: loop forever.
        out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00))
        for (frame in frames) {
            out.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, 0x00))
            out.write(delayHundredths and 0xFF)
            out.write((delayHundredths shr 8) and 0xFF)
            out.write(0)
            out.write(0)
            out.write(0x2C)
            writeShort(out, 0)
            writeShort(out, 0)
            writeShort(out, width)
            writeShort(out, height)
            out.write(0) // no local palette, not interlaced
            val indices = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = frame.getPixel(x, y)
                    val r = ((pixel shr 16) and 0xFF) * 5 / 255
                    val g = ((pixel shr 8) and 0xFF) * 5 / 255
                    val b = (pixel and 0xFF) * 5 / 255
                    indices[y * width + x] = r * 36 + g * 6 + b
                }
            }
            out.write(8) // LZW minimum code size
            writeLzw(out, indices)
            out.write(0)
        }
        out.write(0x3B)
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun writeLzw(out: OutputStream, indices: IntArray) {
        val clearCode = 256
        val eoiCode = 257
        var codeSize = 9
        var dict = HashMap<Long, Int>()
        var nextCode = 258
        val buffer = ByteArrayOutputStream()
        var bitBuffer = 0
        var bitCount = 0
        fun emit(code: Int) {
            bitBuffer = bitBuffer or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                buffer.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer ushr 8
                bitCount -= 8
            }
        }
        fun flushSubBlocks() {
            val bytes = buffer.toByteArray()
            var offset = 0
            while (offset < bytes.size) {
                val chunk = minOf(255, bytes.size - offset)
                out.write(chunk)
                out.write(bytes, offset, chunk)
                offset += chunk
            }
            buffer.reset()
        }
        emit(clearCode)
        var prefix = indices[0]
        for (i in 1 until indices.size) {
            val k = indices[i]
            val key = (prefix.toLong() shl 32) or k.toLong()
            val found = dict[key]
            if (found != null) {
                prefix = found
            } else {
                emit(prefix)
                dict[key] = nextCode++
                if (nextCode - 1 >= (1 shl codeSize) && codeSize < 12) codeSize++
                if (nextCode > 4095) {
                    emit(clearCode)
                    dict = HashMap()
                    nextCode = 258
                    codeSize = 9
                }
                prefix = k
            }
        }
        emit(prefix)
        emit(eoiCode)
        if (bitCount > 0) buffer.write(bitBuffer and 0xFF)
        flushSubBlocks()
    }
}
