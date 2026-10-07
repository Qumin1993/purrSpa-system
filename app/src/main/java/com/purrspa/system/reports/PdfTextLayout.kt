package com.purrspa.system.reports

import android.graphics.Paint

/**
 * Wraps PDF paragraphs to measured width, preferring word boundaries.
 * Long unbroken words are split by characters to avoid clipping.
 */
internal object PdfTextLayout {
    fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        require(maxWidth > 0f)
        if (text.isEmpty()) return listOf("")
        return text.replace("\r", "").split("\n").flatMap { paragraph ->
            if (paragraph.isBlank()) listOf("") else {
                val result = mutableListOf<String>()
                var line = ""
                for (word in paragraph.trim().split(Regex("\\s+"))) {
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        line = candidate
                    } else {
                        if (line.isNotEmpty()) {
                            result.add(line)
                            line = ""
                        }
                        for (char in word) {
                            val next = line + char
                            if (line.isNotEmpty() && paint.measureText(next) > maxWidth) {
                                result.add(line)
                                line = ""
                            }
                            line += char
                        }
                    }
                }
                if (line.isNotEmpty()) result.add(line)
                result
            }
        }
    }
}
