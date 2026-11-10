package com.jamsell.gethics.analytics.presentation.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.jamsell.gethics.analytics.data.ExportedReport
import java.io.File

/**
 * US39. Guarda el CSV en la carpeta de cache de la app (cache/reports) y abre el selector de Android
 * para compartirlo o guardarlo. Usa FileProvider, declarado en el AndroidManifest.
 */
fun shareReport(context: Context, report: ExportedReport, chooserTitle: String) {
    val dir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(dir, report.fileName)
    file.writeBytes(report.bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
