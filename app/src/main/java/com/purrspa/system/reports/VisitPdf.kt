package com.purrspa.system.reports

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.purrspa.system.data.*
import java.io.File
import java.util.Locale

/** Single visit report, shared only through Android's chooser. */
object VisitPdf {
    fun share(context: Context, visit: Visit, cat: Cat, client: Client, assessment: GroomingAssessment?) {
        val file = create(context, visit, cat, client, assessment)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "Purr Spa report", uri)
        }
        context.startActivity(Intent.createChooser(intent, "Share grooming report"))
    }

    fun create(context: Context, visit: Visit, cat: Cat, client: Client, assessment: GroomingAssessment?): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val output = File(dir, "purrspa-${visit.id}.pdf")
        val document = PdfDocument()
        try {
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 28, 26); textSize = 25f; isFakeBoldText = true }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 12f }
            val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(176, 130, 68); textSize = 12f }
            val whenText = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.UK)
                .withZone(java.time.ZoneId.of("Europe/London"))
                .format(java.time.Instant.ofEpochMilli(visit.startMillis))
            val lines = mutableListOf(
                "Cat: ${cat.name}", "Breed: ${cat.breed.ifBlank { "Not specified" }}",
                "Owner: ${client.name}", "Visit: $whenText", "Service: ${visit.service}",
                "Location: ${visit.location}", "Status: ${visit.status}",
                "Price: GBP ${"%.2f".format(Locale.UK, visit.pricePence / 100.0)}",
                "", "GROOMER OBSERVATIONS", visit.notes.ifBlank { "No visit notes recorded." }
            )
            if (assessment != null) {
                lines += listOf("", "HANDLING ASSESSMENT")
                val ratings = listOf(
                    "Brushing" to assessment.brushing, "Bathing" to assessment.bathing,
                    "Drying" to assessment.drying, "Nail trim" to assessment.nailTrim,
                    "Paws" to assessment.paws, "Belly" to assessment.belly, "Tail" to assessment.tail
                )
                val names = listOf("Not assessed", "Very calm", "Calm", "Okay", "Nervous", "Stressed")
                lines += ratings.map { (label, score) -> "$label: ${names.getOrElse(score + 1) { "Not assessed" }}" }
                lines += listOf("", "COAT / SKIN", assessment.coatCondition.ifBlank { "Not recorded" },
                    "", "HOME CARE", assessment.recommendations.ifBlank { "Not recorded" })
            }
            val wrappedLines = lines.flatMap { PdfTextLayout.wrap(it, body, 500f) }
            var index = 0
            var pageNumber = 0
            do {
                pageNumber++
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                val canvas = page.canvas
                canvas.drawText("PURR SPA", 44f, 55f, title)
                canvas.drawText("CAT GROOMING  |  VISIT REPORT", 44f, 80f, accent)
                var y = 118f
                while (index < wrappedLines.size && y <= 780f) {
                    val line = wrappedLines[index++]
                    if (line.isNotEmpty()) canvas.drawText(line, 44f, y, body)
                    y += if (line.isEmpty()) 10f else 19f
                }
                canvas.drawText("Purr Spa  |  Page $pageNumber", 44f, 815f, accent)
                document.finishPage(page)
            } while (index < wrappedLines.size)
            val temp = File(dir, "${output.name}.tmp")
            temp.outputStream().use { document.writeTo(it) }
            if (!temp.renameTo(output)) {
                temp.copyTo(output, overwrite = true)
                temp.delete()
            }
            return output
        } finally { document.close() }
    }
}
