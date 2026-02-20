package io.github.xororz.localdream.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.xororz.localdream.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class ModelDownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ModelDownloadService"
        private const val NOTIFICATION_CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 2001

        private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
        val downloadState: StateFlow<DownloadState> = _downloadState

        const val ACTION_START_DOWNLOAD = "action_start_download"
        const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"

        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_MODEL_NAME = "model_name"
        const val EXTRA_FILE_URL = "file_url"
        const val EXTRA_MANIFEST_URL = "manifest_url"
        const val EXTRA_IS_ZIP = "is_zip"
        const val EXTRA_IS_NPU = "is_npu"
        const val EXTRA_MODEL_TYPE = "model_type" // "sd" or "upscaler"
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(
            val modelId: String,
            val progress: Float,
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : DownloadState()

        data class Extracting(val modelId: String) : DownloadState()
        data class Success(val modelId: String) : DownloadState()
        data class Error(val modelId: String, val message: String) : DownloadState()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: modelId
                val fileUrl = intent.getStringExtra(EXTRA_FILE_URL) ?: ""
                val manifestUrl = intent.getStringExtra(EXTRA_MANIFEST_URL) ?: ""
                Log.i(
                    TAG,
                    "start download request: modelId=$modelId fileUrl=$fileUrl manifestUrl=$manifestUrl"
                )
                if (fileUrl.isEmpty() && manifestUrl.isEmpty()) return START_NOT_STICKY
                val isZip = intent.getBooleanExtra(EXTRA_IS_ZIP, false)
                val isNpu = intent.getBooleanExtra(EXTRA_IS_NPU, false)
                val modelType = intent.getStringExtra(EXTRA_MODEL_TYPE) ?: "sd"

                startForeground(NOTIFICATION_ID, createNotification(modelName, 0f))
                startDownload(modelId, modelName, fileUrl, manifestUrl, isZip, isNpu, modelType)
            }

            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(
        modelId: String,
        modelName: String,
        fileUrl: String,
        manifestUrl: String,
        isZip: Boolean,
        isNpu: Boolean,
        modelType: String
    ) {
        downloadJob?.cancel()
        downloadJob = serviceScope.launch {
            var tempFile: File? = null
            var extractTempDir: File? = null
            try {
                _downloadState.value = DownloadState.Downloading(modelId, 0f, 0, 0)

                val tempDir = File(filesDir, "temp_downloads")

                if (tempDir.exists()) {
                    tempDir.deleteRecursively()
                }
                tempDir.mkdirs()

                if (manifestUrl.isBlank() && fileUrl.isNotEmpty()) {
                    tempFile = File(tempDir, "${modelId}_${System.currentTimeMillis()}.tmp")
                    downloadFile(fileUrl, tempFile, modelId, modelName)
                }

                when (modelType) {
                    "sd" -> {
                        val modelDir = File(getModelsDir(), modelId)
                        if (modelDir.exists()) {
                            modelDir.deleteRecursively()
                        }
                        modelDir.mkdirs()

                        if (manifestUrl.isNotBlank()) {
                            downloadManifestModel(
                                manifestUrl = manifestUrl,
                                modelDir = modelDir,
                                tempDir = tempDir,
                                modelId = modelId,
                                modelName = modelName
                            )
                        } else if (isZip && tempFile != null) {
                            extractTempDir = File(tempDir, "${modelId}_extract")
                            extractTempDir.mkdirs()

                            _downloadState.value = DownloadState.Extracting(modelId)
                            updateNotification(modelName, 0f, isExtracting = true)

                            unzipFile(tempFile, extractTempDir)

                            extractTempDir.listFiles()?.forEach { file ->
                                file.renameTo(File(modelDir, file.name))
                            }
                            extractTempDir.delete()
                            extractTempDir = null
                        } else if (tempFile != null) {
                            val targetName = fileUrl.substringAfterLast('/').ifBlank { "model.bin" }
                            val targetFile = File(modelDir, targetName)
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                            tempFile.renameTo(targetFile)
                        }

                        if (isNpu) {
                            File(modelDir, "v3").createNewFile()
                        }
                        File(modelDir, "finished").createNewFile()
                    }

                    "upscaler" -> {
                        val upscalerDir = File(getModelsDir(), modelId).apply {
                            if (!exists()) mkdirs()
                        }
                        val targetFile = File(upscalerDir, "upscaler.bin")

                        if (targetFile.exists()) {
                            targetFile.delete()
                        }

                        val moved = tempFile?.renameTo(targetFile) ?: false
                        if (!moved) {
                            throw Exception("Failed to store upscaler model file")
                        }
                    }
                }

                tempFile?.delete()
                tempFile = null

                _downloadState.value = DownloadState.Success(modelId)
                updateNotification(modelName, 100f, true)

                withContext(Dispatchers.Main) {
                    kotlinx.coroutines.delay(2000)
                    _downloadState.value = DownloadState.Idle
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)

                tempFile?.delete()
                extractTempDir?.deleteRecursively()

                _downloadState.value = DownloadState.Error(modelId, e.message ?: "Unknown error")
                updateNotification(modelName, 0f, false, e.message)

                withContext(Dispatchers.Main) {
                    kotlinx.coroutines.delay(3000)
                    _downloadState.value = DownloadState.Idle
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private data class ManifestItem(
        val url: String,
        val targetPath: String,
        val isZip: Boolean
    )

    private suspend fun downloadManifestModel(
        manifestUrl: String,
        modelDir: File,
        tempDir: File,
        modelId: String,
        modelName: String
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "download manifest: $manifestUrl")
        val request = Request.Builder().url(manifestUrl).build()
        val manifestItems = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Manifest download failed with code: ${response.code}")
            }
            val body = response.body?.string()
                ?: throw Exception("Manifest response body is null")
            parseManifestItems(manifestUrl, body)
        }

        if (manifestItems.isEmpty()) {
            throw Exception("Manifest has no files")
        }
        Log.i(TAG, "manifest files: ${manifestItems.size}")

        manifestItems.forEachIndexed { index, item ->
            Log.i(
                TAG,
                "manifest item ${index + 1}/${manifestItems.size}: url=${item.url} target=${item.targetPath}"
            )
            val tempFile = File(tempDir, "${modelId}_${index}.tmp")
            val displayName = "$modelName (${index + 1}/${manifestItems.size})"
            downloadFile(item.url, tempFile, modelId, displayName)

            val targetFile = resolveTargetFile(modelDir, item.targetPath)
            targetFile.parentFile?.mkdirs()
            if (item.isZip) {
                _downloadState.value = DownloadState.Extracting(modelId)
                updateNotification(modelName, 0f, isExtracting = true)
                unzipFile(tempFile, targetFile.parentFile ?: modelDir)
                tempFile.delete()
            } else {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
            }
        }
    }

    private fun parseManifestItems(manifestUrl: String, jsonText: String): List<ManifestItem> {
        val root = JSONObject(jsonText)
        val files = root.optJSONArray("files") ?: return emptyList()
        val result = mutableListOf<ManifestItem>()

        for (i in 0 until files.length()) {
            val file = files.optJSONObject(i) ?: continue
            val rawPath = file.optString("path", "")
            val url = file.optString("url", "").ifBlank {
                if (rawPath.isNotBlank()) {
                    resolveRelativeUrl(manifestUrl, rawPath)
                } else {
                    ""
                }
            }
            val target = file.optString("target", rawPath)
            if (url.isBlank() || target.isBlank()) {
                continue
            }
            result += ManifestItem(
                url = url,
                targetPath = target,
                isZip = file.optBoolean("is_zip", false)
            )
        }
        return result
    }

    private fun resolveRelativeUrl(base: String, path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        return URI(base).resolve(path).toString()
    }

    private fun resolveTargetFile(modelDir: File, targetPath: String): File {
        val safeTarget = targetPath
            .replace('\\', '/')
            .trimStart('/')
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString("/")
        if (safeTarget.isEmpty()) {
            throw Exception("Invalid target path in manifest")
        }

        val base = modelDir.canonicalFile
        val out = File(modelDir, safeTarget).canonicalFile
        if (out.path != base.path && !out.path.startsWith(base.path + File.separator)) {
            throw Exception("Invalid target path outside model dir: $targetPath")
        }
        return out
    }

    private suspend fun downloadFile(
        url: String,
        destFile: File,
        modelId: String,
        modelName: String
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "download file: $url")
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download failed with code: ${response.code}")
            }

            val body = response.body ?: throw Exception("Response body is null")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastUpdateTime = 0L

            java.io.BufferedOutputStream(FileOutputStream(destFile)).use { output ->
                body.byteStream().buffered().use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var bytes: Int

                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloadedBytes += bytes

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= 500 || downloadedBytes == totalBytes) {
                            lastUpdateTime = currentTime
                            val progress = if (totalBytes > 0) {
                                downloadedBytes.toFloat() / totalBytes
                            } else 0f

                            _downloadState.value = DownloadState.Downloading(
                                modelId,
                                progress,
                                downloadedBytes,
                                totalBytes
                            )

                            updateNotification(modelName, progress)
                        }
                    }
                }
            }
        }
    }

    private suspend fun unzipFile(
        zipFile: File,
        destDir: File,
    ) = withContext(Dispatchers.IO) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry

            while (entry != null) {
                if (!entry.isDirectory) {
                    val fileName = entry.name.substringAfterLast('/')
                    if (fileName.isNotEmpty() && !fileName.startsWith(".") && !fileName.startsWith("__MACOSX")) {
                        val file = File(destDir, fileName)

                        java.io.BufferedOutputStream(FileOutputStream(file)).use { output ->
                            zis.copyTo(output)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        _downloadState.value = DownloadState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun getModelsDir(): File {
        return File(filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.model_download_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.model_download_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(
        modelName: String,
        progress: Float,
        isExtracting: Boolean = false
    ): android.app.Notification {
        val title = if (isExtracting) {
            getString(R.string.extracting)
        } else {
            getString(R.string.downloading_model, modelName)
        }

        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, (progress * 100).toInt(), isExtracting)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .build()
    }

    private fun updateNotification(
        modelName: String,
        progress: Float,
        success: Boolean = false,
        error: String? = null,
        isExtracting: Boolean = false
    ) {
        val notification = when {
            success -> {
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.download_complete))
                    .setContentText(modelName)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .build()
            }

            error != null -> {
                NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.download_failed))
                    .setContentText(error)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setOngoing(false)
                    .build()
            }

            else -> {
                createNotification(modelName, progress, isExtracting)
            }
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        Log.e(TAG, "Foreground service timeout")
        _downloadState.value = DownloadState.Error("timeout", "Foreground service timeout")
        updateNotification("Timeout", 0f, false, "Service timeout")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
