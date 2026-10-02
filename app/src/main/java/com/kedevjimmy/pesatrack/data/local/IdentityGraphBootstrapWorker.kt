package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class IdentityGraphBootstrapWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(
    appContext,
    workerParams
) {
    override fun doWork(): Result =
        runCatching {
            IdentityGraphStore
                .get(applicationContext)
                .backfillExistingData()
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )

    companion object {
        private const val UNIQUE_WORK =
            "identity-graph-bootstrap-v1"

        fun schedule(
            context: Context
        ) {
            val request =
                OneTimeWorkRequest.Builder(
                    IdentityGraphBootstrapWorker::
                        class.java
                ).build()

            WorkManager.getInstance(
                context.applicationContext
            ).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
