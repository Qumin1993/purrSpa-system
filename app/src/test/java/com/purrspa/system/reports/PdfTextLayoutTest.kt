package com.purrspa.system.reports

import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfTextLayoutTest {
    private val paint = Paint().apply { textSize = 12f }

    @Test fun preservesParagraphBreaks() {
        assertEquals(listOf("First", "", "Second"), PdfTextLayout.wrap("First\n\nSecond", paint, 500f))
    }

    @Test fun wrapsAtWordBoundaries() {
        val width = paint.measureText("Hello")
        assertEquals(listOf("Hello", "world"), PdfTextLayout.wrap("Hello world", paint, width))
    }

    @Test fun splitsOversizedWords() {
        val width = paint.measureText("abcd")
        val lines = PdfTextLayout.wrap("abcdefghijkl", paint, width)
        assertTrue(lines.size > 1)
        assertEquals("abcdefghijkl", lines.joinToString(""))
        assertTrue(lines.all { paint.measureText(it) <= width })
    }
}
