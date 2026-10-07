package com.purrspa.system.reports

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.purrspa.system.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
        }
        context.startActivity(Intent.createChooser(intent, "Share grooming report"))
    }

    fun create(context: Context, visit: Visit, cat: Cat, client: Client, assessment: GroomingAssessment?): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val output = File(dir, "purrspa-${visit.id}.pdf")
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 28, 26); textSize = 25f; isFakeBoldText = true }
            val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 12f }
            val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(176, 130, 68); textSize = 12f }
            var y = 55f
            canvas.drawText("PURR SPA", 44f, y, title)
            y += 25f
            canvas.drawText("CAT GROOMING  |  VISIT REPORT", 44f, y, accent)
            y += 38f
            val whenText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK).format(Date(visit.startMillis))
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
            for (line in lines) {
                val words = line.split(" ")
                var current = ""
                for (word in words) {
                    val next = if (current.isEmpty()) word else "$current $word"
                    if (body.measureText(next) > 500f && current.isNotEmpty()) {
                        if (y > 795f) break
                        canvas.drawText(current, 44f, y, body)
                        y += 19f
                        current = word
                    } else current = next
                }
                if (y > 795f) break
                canvas.drawText(current, 44f, y, body)
                y += if (line.isEmpty()) 10f else 22f
            }
            canvas.drawText("Purr Spa  |  Coleraine  |  Report generated from saved visit data", 44f, 815f, accent)
            document.finishPage(page)
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
