package com.sergiojosemp.obddashboard.activity

import android.content.Context
import android.util.Log
import androidx.work.*
import com.sergiojosemp.obddashboard.vm.TAG
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
/*
class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    val appContext = appContext
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        print("Hello World!")
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UploadWorker>()
                .setInitialDelay(1,TimeUnit.MINUTES)
                .addTag("Worker de Sergio")
                .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork("", ExistingWorkPolicy.REPLACE, uploadWorkRequest as OneTimeWorkRequest)
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}*/