package com.veloris.app

import android.app.Application
import android.content.Context
import androidx.work.*
import com.veloris.app.data.AuthManager
import com.veloris.app.data.LibraryRepository
import java.util.concurrent.TimeUnit

class VelorisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
        LibraryRepository.init(this)
        // Auto-sync library di latar belakang tiap 15 menit (batas minimum WorkManager)
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("veloris_sync", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

class SyncWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        LibraryRepository.sync()
        return Result.success()
    }
}
