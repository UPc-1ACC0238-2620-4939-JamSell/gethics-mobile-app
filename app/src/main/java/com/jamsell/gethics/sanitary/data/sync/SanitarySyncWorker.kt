package com.jamsell.gethics.sanitary.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jamsell.gethics.GethicsApplication
import java.util.concurrent.TimeUnit

/** US35: adaptador de WorkManager; la logica (y sus tests) esta en SanitarySyncer. */
class SanitarySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        when ((applicationContext as GethicsApplication).container.sanitarySyncer.syncAll()) {
            SyncResult.SUCCESS -> Result.success()
            SyncResult.RETRY -> Result.retry()
        }
}

private const val SANITARY_SYNC_WORK = "sanitary-sync"

/**
 * Programa la sincronizacion cuando haya red. Trabajo unico "sanitary-sync" con APPEND_OR_REPLACE: si ya hay uno
 * pendiente o corriendo, el nuevo se encadena DETRAS (nunca dos a la vez) y vuelve a leer la cola al terminar el
 * anterior, asi no se pierde una operacion encolada mientras el otro ya estaba enviando. Si el anterior fallo o se
 * cancelo, lo reemplaza. Los eslabones extra no duplican nada: la cola ya se vacio y el backend es idempotente.
 */
fun enqueueSanitarySync(context: Context) {
    val request = OneTimeWorkRequestBuilder<SanitarySyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(SANITARY_SYNC_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
}
