package tkd.apptkd.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import tkd.apptkd.data.room.SessionWithMovements
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_WIDTH_PT = 595
private const val PAGE_HEIGHT_PT = 842
private const val MARGIN_X_PT = 40f
private const val MARGIN_TOP_PT = 50f
private const val LINE_HEIGHT_PT = 16f
private const val HEADER_LINE_HEIGHT_PT = 24f
private const val TITLE_TEXT_SIZE = 16f
private const val SUBTITLE_TEXT_SIZE = 12f
private const val BODY_TEXT_SIZE = 10f
private const val DISCLAIMER_TEXT_SIZE = 8.5f
private const val MAX_MOVEMENTS_PER_PDF_PAGE = 18
private const val SESSION_ID_PREVIEW_LENGTH = 8

/**
 * Servicio de exportación de sesiones a formato de texto plano y documento PDF (RF-11, Sección 14.8).
 *
 * Incluye obligatoriamente los avisos normativos de puntuación orientativa y secuencia fijada documentalmente.
 */
@Singleton
class SessionExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportAsText(sessionWithMovements: SessionWithMovements): Uri {
        val session = sessionWithMovements.session
        val movements = sessionWithMovements.movements
        val dateStr = dateFormat.format(Date(session.startedAtMs))

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                TAEGEUK COACH — REPORTE DE SESIÓN DE PRÁCTICA                   ")
        sb.appendLine("================================================================================")
        sb.appendLine("Poomsae: Taegeuk 1 Jang (태극 1장)")
        sb.appendLine("Fecha de inicio: $dateStr")
        sb.appendLine("ID de sesión: ${session.id}")
        sb.appendLine("Estado: ${session.status} | Modo: ${session.mode} | Modo estricto: ${session.strictMode}")
        sb.appendLine()
        sb.appendLine("AVISOS NORMATIVOS OBLIGATORIOS:")
        sb.appendLine("- La puntuación es orientativa y no es una calificación oficial.")
        sb.appendLine("- Secuencia fijada documentalmente; no certificada oficialmente.")
        sb.appendLine()
        sb.appendLine("RESULTADOS GLOBALES:")
        val scoreStr = session.globalScore?.let { String.format(Locale.US, "%.1f / 100", it) } ?: "Sin puntaje"
        sb.appendLine("Puntaje global: $scoreStr (orientativo)")
        sb.appendLine("Movimientos evaluables: ${session.evaluableCount}")
        sb.appendLine("Movimientos parcialmente evaluables: ${session.partialCount}")
        sb.appendLine("Movimientos no evaluables: ${session.notEvaluableCount}")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("DESGLOSE POR MOVIMIENTO (1-18):")
        sb.appendLine("--------------------------------------------------------------------------------")

        for (m in movements) {
            val movScore = m.score?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val category = m.category ?: "NO_EVALUABLE"
            sb.appendLine("Movimiento ${m.movementIndex}: Outcome=${m.outcome} | Cat=$category | Score=$movScore")
        }
        sb.appendLine("================================================================================")

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val textFile = File(exportDir, "sesion_${session.id}.txt")
        textFile.writeText(sb.toString())

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            textFile,
        )
    }

    fun exportAsPdf(sessionWithMovements: SessionWithMovements): Uri {
        val session = sessionWithMovements.session
        val movements = sessionWithMovements.movements
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = TITLE_TEXT_SIZE
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = SUBTITLE_TEXT_SIZE
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = BODY_TEXT_SIZE
        }
        val disclaimerPaint = Paint().apply {
            color = Color.RED
            textSize = DISCLAIMER_TEXT_SIZE
            isFakeBoldText = true
        }

        var currentY = MARGIN_TOP_PT
        canvas.drawText("TAEGEUK COACH — REPORTE DE PRÁCTICA", MARGIN_X_PT, currentY, titlePaint)
        currentY += HEADER_LINE_HEIGHT_PT

        canvas.drawText("Taegeuk 1 Jang (태극 1장)", MARGIN_X_PT, currentY, subtitlePaint)
        currentY += LINE_HEIGHT_PT

        val dateStr = dateFormat.format(Date(session.startedAtMs))
        val idPreview = session.id.take(SESSION_ID_PREVIEW_LENGTH)
        canvas.drawText("Fecha: $dateStr  |  ID: $idPreview...", MARGIN_X_PT, currentY, bodyPaint)
        currentY += LINE_HEIGHT_PT

        canvas.drawText(
            "AVISO: La puntuación es orientativa y no oficial. Secuencia no validada oficialmente.",
            MARGIN_X_PT,
            currentY,
            disclaimerPaint,
        )
        currentY += HEADER_LINE_HEIGHT_PT

        val scoreStr = session.globalScore?.let { String.format(Locale.US, "%.1f / 100", it) } ?: "Sin puntaje"
        canvas.drawText("Puntaje Global: $scoreStr (orientativo)", MARGIN_X_PT, currentY, titlePaint)
        currentY += LINE_HEIGHT_PT

        canvas.drawText(
            "Evaluados: ${session.evaluableCount}  |  Parciales: ${session.partialCount}  |  " +
                "No evaluables: ${session.notEvaluableCount}",
            MARGIN_X_PT,
            currentY,
            bodyPaint,
        )
        currentY += HEADER_LINE_HEIGHT_PT

        canvas.drawText("Desglose técnico de movimientos:", MARGIN_X_PT, currentY, subtitlePaint)
        currentY += LINE_HEIGHT_PT

        val displayMovements = movements.take(MAX_MOVEMENTS_PER_PDF_PAGE)
        for (m in displayMovements) {
            val scoreText = m.score?.let { String.format(Locale.US, "%.1f", it) } ?: "--"
            val line = "Mov ${m.movementIndex}: ${m.outcome}  |  Cat: ${m.category ?: "NO_EVAL"}  |  Pts: $scoreText"
            canvas.drawText(line, MARGIN_X_PT, currentY, bodyPaint)
            currentY += LINE_HEIGHT_PT
        }

        pdfDocument.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "sesion_${session.id}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
    }

    fun createShareIntent(uri: Uri, mimeType: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}

