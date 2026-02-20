package io.github.xororz.localdream.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.util.Log
import android.util.JsonReader
import android.util.JsonToken
import androidx.core.app.NotificationCompat
import io.github.xororz.localdream.data.RuntimeBackend
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import io.github.xororz.localdream.R
import java.io.File
import androidx.core.graphics.createBitmap

class BackgroundGenerationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        private const val CHANNEL_ID = "image_generation_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "stop_generation"

        private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
        val generationState: StateFlow<GenerationState> = _generationState

        private val _bitmapConsumed = MutableStateFlow(false)
        val bitmapConsumed: StateFlow<Boolean> = _bitmapConsumed

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        fun resetState() {
            _generationState.value = GenerationState.Idle
            _bitmapConsumed.value = false
        }

        fun clearCompleteState() {
            if (_generationState.value is GenerationState.Complete) {
                _generationState.value = GenerationState.Idle
            }
        }

        fun markBitmapConsumed() {
            _bitmapConsumed.value = true
        }
    }

    sealed class GenerationState {
        object Idle : GenerationState()
        data class Progress(val progress: Float, val intermediateImage: Bitmap? = null) :
            GenerationState()

        data class Complete(
            val bitmap: Bitmap? = null,
            val seed: Long?,
            val imagePath: String? = null
        ) : GenerationState()
        data class Error(val message: String) : GenerationState()
    }

    private fun updateState(newState: GenerationState) {
        _generationState.value = newState
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("GenerationService", "service created")
        _isServiceRunning.value = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("GenerationService", "service execute: ${intent?.extras}")

        startForeground(NOTIFICATION_ID, createNotification(0f))

        when (intent?.action) {
            ACTION_STOP -> {
                Log.d("GenerationService", "service stopped")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val prompt = intent?.getStringExtra("prompt")
        Log.d("GenerationService", "prompt: $prompt")

        if (prompt == null) {
            Log.e("GenerationService", "empty prompt")
            stopSelf()
            return START_NOT_STICKY
        }

        val negativePrompt = intent.getStringExtra("negative_prompt") ?: ""
        val steps = intent.getIntExtra("steps", 28)
        val cfg = intent.getFloatExtra("cfg", 7f)
        val seed = if (intent.hasExtra("seed")) intent.getLongExtra("seed", 0) else null
        val width = intent.getIntExtra("width", 512)
        val height = intent.getIntExtra("height", 512)
        val denoiseStrength = intent.getFloatExtra("denoise_strength", 0.6f)
        val useOpenCL = intent.getBooleanExtra("use_opencl", false)
        val runtimeBackend = RuntimeBackend.fromValue(
            intent.getStringExtra("runtime_backend")
        )
        val scheduler = intent.getStringExtra("scheduler") ?: "dpm"

        val image = if (intent.getBooleanExtra("has_image", false)) {
            try {
                val tmpFile = File(applicationContext.filesDir, "tmp.txt")
                if (tmpFile.exists()) {
                    tmpFile.readText()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("GenerationService", "Failed to read image data", e)
                null
            }
        } else {
            null
        }
        val extraImage = if (intent.getBooleanExtra("has_extra_image", false)) {
            try {
                val tmpFile = File(applicationContext.filesDir, "tmp_ref2.txt")
                if (tmpFile.exists()) {
                    tmpFile.readText()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("GenerationService", "Failed to read extra image data", e)
                null
            }
        } else {
            null
        }
        val mask = if (intent.getBooleanExtra("has_mask", false)) {
            try {
                val maskFile = File(applicationContext.filesDir, "mask.txt")
                if (maskFile.exists()) {
                    maskFile.readText()
                } else {
                    Log.w(
                        "GenerationService",
                        "has_mask is true but mask.txt not found"
                    )
                    null
                }
            } catch (e: Exception) {
                Log.e("GenerationService", "Failed to read mask data", e)
                null
            }
        } else {
            null
        }

        Log.d("GenerationService", "params: steps=$steps, cfg=$cfg, seed=$seed")

        if (_generationState.value is GenerationState.Complete) {
            updateState(GenerationState.Idle)
        }
        _bitmapConsumed.value = false

        serviceScope.launch {
            Log.d("GenerationService", "start generation")
            runGeneration(
                prompt,
                negativePrompt,
                steps,
                cfg,
                seed,
                width,
                height,
                image,
                extraImage,
                mask,
                denoiseStrength,
                useOpenCL,
                runtimeBackend,
                scheduler
            )
        }

        return START_NOT_STICKY
    }

    private suspend fun runGeneration(
        prompt: String,
        negativePrompt: String,
        steps: Int,
        cfg: Float,
        seed: Long?,
        width: Int,
        height: Int,
        image: String?,
        extraImage: String?,
        mask: String?,
        denoiseStrength: Float,
        useOpenCL: Boolean,
        runtimeBackend: RuntimeBackend,
        scheduler: String
    ) = withContext(Dispatchers.IO) {
        try {
            updateState(GenerationState.Progress(0f))

            if (runtimeBackend == RuntimeBackend.ADRENO) {
                runGenerationWithSdApi(
                    prompt = prompt,
                    negativePrompt = negativePrompt,
                    steps = steps,
                    cfg = cfg,
                    seed = seed,
                    width = width,
                    height = height,
                    image = image,
                    extraImage = extraImage,
                    mask = mask,
                    denoiseStrength = denoiseStrength,
                    scheduler = scheduler
                )
                return@withContext
            }

            val preferences =
                applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val showProcess = preferences.getBoolean("show_diffusion_process", false)
            val showStride = preferences.getInt("show_diffusion_stride", 1)

            val jsonObject = JSONObject().apply {
                put("prompt", prompt)
                put("negative_prompt", negativePrompt)
                put("steps", steps)
                put("cfg", cfg)
                put("use_cfg", true)
                put("width", width)
                put("height", height)
                put("denoise_strength", denoiseStrength)
                put("use_opencl", useOpenCL)
                put("scheduler", scheduler)
                put("show_diffusion_process", showProcess)
                put("show_diffusion_stride", showStride)
                seed?.let { put("seed", it) }
                image?.let { put("image", it) }
                mask?.let { put("mask", it) }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(3600, TimeUnit.SECONDS)
                .readTimeout(3600, TimeUnit.SECONDS)
                .writeTimeout(3600, TimeUnit.SECONDS)
                .callTimeout(3600, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val request = Request.Builder()
                .url("http://localhost:8081/generate")
                .post(jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException(
                        this@BackgroundGenerationService.getString(
                            R.string.error_request_failed,
                            response.code.toString()
                        )
                    )
                }

                response.body?.let { responseBody ->
                    Log.d("BgGenService", "Reading streaming response")

                    val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                    var messageCount = 0

                    // Read line by line for efficiency
                    while (isActive) {
                        val readLineStart = System.currentTimeMillis()
                        val line = reader.readLine() ?: break
                        val readLineTime = System.currentTimeMillis() - readLineStart

                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()
                            if (data == "[DONE]") break

                            val jsonParseStart = System.currentTimeMillis()
                            val message = JSONObject(data)
                            val jsonParseTime = System.currentTimeMillis() - jsonParseStart
                            messageCount++

                            when (message.optString("type")) {
                                "progress" -> {
                                    val step = message.optInt("step")
                                    val totalSteps = message.optInt("total_steps")
                                    val progress = step.toFloat() / totalSteps

                                    val b64Img = message.optString("image")
                                    var bitmap: Bitmap? = null
                                    if (b64Img.isNotEmpty()) {
                                        try {
                                            val imageBytes = Base64.getDecoder().decode(b64Img)
                                            val pixels = IntArray(width * height)
                                            for (i in 0 until width * height) {
                                                val index = i * 3
                                                if (index + 2 < imageBytes.size) {
                                                    val r = imageBytes[index].toInt() and 0xFF
                                                    val g = imageBytes[index + 1].toInt() and 0xFF
                                                    val b = imageBytes[index + 2].toInt() and 0xFF
                                                    pixels[i] =
                                                        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                                                }
                                            }
                                            bitmap = createBitmap(width, height)
                                            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                                        } catch (e: Exception) {
                                            Log.e(
                                                "BgGenService",
                                                "Failed to decode intermediate image",
                                                e
                                            )
                                        }
                                    }

                                    updateState(GenerationState.Progress(progress, bitmap))
                                    updateNotification(progress)
                                }

                                "complete" -> {
                                    Log.d(
                                        "BgGenService",
                                        "=== Received complete message, parsing... ==="
                                    )
                                    Log.d(
                                        "BgGenService",
                                        "readLine took: ${readLineTime}ms, line length: ${line.length}"
                                    )
                                    Log.d(
                                        "BgGenService",
                                        "JSONObject parsing took: ${jsonParseTime}ms, data length: ${data.length}"
                                    )
                                    val completeStartTime = System.currentTimeMillis()

                                    // 1. Extract fields from JSON
                                    val extractStart = System.currentTimeMillis()
                                    val base64Image = message.optString("image")
                                    val returnedSeed =
                                        message.optLong("seed", -1).takeIf { it != -1L }
                                    val resultWidth = message.optInt("width", 512)
                                    val resultHeight = message.optInt("height", 512)
                                    Log.d(
                                        "BgGenService",
                                        "JSON extraction took: ${System.currentTimeMillis() - extractStart}ms, Base64 length: ${base64Image.length}"
                                    )

                                    if (base64Image.isNullOrEmpty()) {
                                        throw IOException("no image data")
                                    }

                                    // 2. Base64 decode
                                    val decodeStartTime = System.currentTimeMillis()
                                    val imageBytes = Base64.getDecoder().decode(base64Image)
                                    Log.d(
                                        "BgGenService",
                                        "Base64 decoding took: ${System.currentTimeMillis() - decodeStartTime}ms, decoded size: ${imageBytes.size} bytes"
                                    )

                                    // 3. RGB conversion + Bitmap creation
                                    val bitmapStartTime = System.currentTimeMillis()
                                    val bitmap = createBitmap(resultWidth, resultHeight)
                                    val pixels = IntArray(resultWidth * resultHeight)

                                    for (i in 0 until resultWidth * resultHeight) {
                                        val index = i * 3
                                        val r = imageBytes[index].toInt() and 0xFF
                                        val g = imageBytes[index + 1].toInt() and 0xFF
                                        val b = imageBytes[index + 2].toInt() and 0xFF
                                        pixels[i] =
                                            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                                    }
                                    bitmap.setPixels(
                                        pixels,
                                        0,
                                        resultWidth,
                                        0,
                                        0,
                                        resultWidth,
                                        resultHeight
                                    )
                                    Log.d(
                                        "BgGenService",
                                        "RGB conversion + Bitmap creation took: ${System.currentTimeMillis() - bitmapStartTime}ms"
                                    )

                                    Log.d(
                                        "BgGenService",
                                        "=== Total processing time for complete message: ${System.currentTimeMillis() - completeStartTime}ms, size: ${resultWidth}x${resultHeight} ==="
                                    )

                                    updateState(
                                        GenerationState.Complete(
                                            bitmap = bitmap,
                                            seed = returnedSeed
                                        )
                                    )

                                    Log.d(
                                        "BgGenService",
                                        "Generation completed, waiting for UI to consume bitmap"
                                    )

                                    // Wait for UI to consume the bitmap with timeout
                                    val waitStartTime = System.currentTimeMillis()
                                    val timeoutMs = 30000L // 30 seconds timeout
                                    while (!_bitmapConsumed.value && isActive) {
                                        if (System.currentTimeMillis() - waitStartTime > timeoutMs) {
                                            Log.w(
                                                "BgGenService",
                                                "Timeout waiting for bitmap consumption"
                                            )
                                            break
                                        }
                                        delay(100)
                                    }

                                    Log.d(
                                        "BgGenService",
                                        "Bitmap consumed, stopping service. Wait time: ${System.currentTimeMillis() - waitStartTime}ms"
                                    )
                                    stopSelf()
                                }

                                "error" -> {
                                    val errorMsg =
                                        message.optString("message", "unknown error")
                                    Log.e(
                                        "BgGenService",
                                        "Received error message: $errorMsg"
                                    )
                                    throw IOException(errorMsg)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            if (_generationState.value is GenerationState.Complete) {
                Log.d("GenerationService", "generation canceled after complete; keeping complete state")
            } else {
                Log.e("GenerationService", "generation canceled before completion", e)
                updateState(
                    GenerationState.Error(
                        e.message ?: this@BackgroundGenerationService.getString(R.string.unknown_error)
                    )
                )
            }
            stopSelf()
        } catch (e: Exception) {
            Log.e("GenerationService", "generation error", e)
            updateState(
                GenerationState.Error(
                    e.message ?: this@BackgroundGenerationService.getString(R.string.unknown_error)
                )
            )
            stopSelf()
        }
    }

    private fun estimateAdrenoSdApiDurationMs(
        steps: Int,
        width: Int,
        height: Int,
        hasInitImage: Boolean
    ): Long {
        val areaScale = (width.toDouble() * height.toDouble()) / (512.0 * 512.0)
        val samplePerStepSec = when {
            areaScale >= 3.8 -> 56.0
            areaScale >= 1.8 -> 20.0
            else -> 8.6
        }
        val sampleSec = samplePerStepSec * steps.coerceAtLeast(1)
        val vaeSec = when {
            areaScale >= 3.8 -> 24.0
            areaScale >= 1.8 -> 10.0
            else -> 3.3
        }
        val ioAndPostSec = 1.0
        val editPenalty = if (hasInitImage) 1.25 else 1.0
        val estimateSec = (sampleSec + vaeSec + ioAndPostSec) * editPenalty
        return (estimateSec * 1000.0).toLong().coerceAtLeast(12_000L)
    }

    private suspend fun runGenerationWithSdApi(
        prompt: String,
        negativePrompt: String,
        steps: Int,
        cfg: Float,
        seed: Long?,
        width: Int,
        height: Int,
        image: String?,
        extraImage: String?,
        mask: String?,
        denoiseStrength: Float,
        scheduler: String
    ) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(3600, TimeUnit.SECONDS)
            .readTimeout(3600, TimeUnit.SECONDS)
            .writeTimeout(3600, TimeUnit.SECONDS)
            .callTimeout(3600, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val samplerName = when (scheduler) {
            "euler_a" -> "euler_a"
            // Flux2 Adreno path is tuned/validated with Euler in our perf gates.
            "dpm" -> "euler"
            else -> "dpm++ 2m"
        }
        // For cfg_scale ~= 1, disabling CFG avoids the uncond branch and matches our
        // precomputed-cond Adreno perf/stability gate.
        val useCfg = cfg > 1.0001f
        val effectiveNegativePrompt = if (useCfg) negativePrompt else ""

        val payload = JSONObject().apply {
            put("prompt", prompt)
            put("negative_prompt", effectiveNegativePrompt)
            put("steps", steps)
            put("cfg_scale", cfg)
            put("cfg", cfg)
            put("use_cfg", useCfg)
            put("sampler_name", samplerName)
            put("scheduler", "discrete")
            put("width", width)
            put("height", height)
            put("batch_size", 1)
            seed?.let { put("seed", it) }
        }

        val endpoint = if (image != null) "/sdapi/v1/img2img" else "/sdapi/v1/txt2img"
        if (image != null) {
            payload.put("init_images", org.json.JSONArray().put(image))
            if (!extraImage.isNullOrBlank()) {
                payload.put("extra_images", org.json.JSONArray().put(extraImage))
            }
            payload.put("denoising_strength", denoiseStrength)
            if (mask != null) {
                payload.put("mask", mask)
            }
        }

        Log.d("GenerationService", "sdapi endpoint=$endpoint payload=${payload}")

        val estimatedTotalMs =
            estimateAdrenoSdApiDurationMs(steps, width, height, hasInitImage = image != null)
        Log.d(
            "GenerationService",
            "Adreno progress estimate: ${estimatedTotalMs}ms (steps=$steps, ${width}x${height}, img2img=${image != null})"
        )

        val progressJob = serviceScope.launch {
            val startMs = System.currentTimeMillis()
            while (isActive) {
                val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)
                val linear = (elapsedMs.toFloat() / estimatedTotalMs.toFloat()).coerceIn(0f, 1f)
                val p = (0.01f + linear * 0.94f).coerceAtMost(0.95f)
                updateState(GenerationState.Progress(p))
                updateNotification(p)
                delay(300)
            }
        }

        try {
            val request = Request.Builder()
                .url("http://localhost:8081$endpoint")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val parsedResult = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException(
                        this@BackgroundGenerationService.getString(
                            R.string.error_request_failed,
                            response.code.toString()
                        )
                    )
                }
                val body = response.body ?: throw IOException("empty response body")
                parseSdApiResponse(body)
            }

            val imageBytes = Base64.getDecoder().decode(parsedResult.base64Image)
            val imageFile = persistSdApiImage(imageBytes)
            updateState(
                GenerationState.Complete(
                    bitmap = null,
                    seed = parsedResult.seed ?: seed,
                    imagePath = imageFile.absolutePath
                )
            )
            updateNotification(1.0f)

            // Keep behavior consistent with legacy backend: wait UI to consume bitmap.
            val waitStartTime = System.currentTimeMillis()
            val timeoutMs = 30000L
            while (!_bitmapConsumed.value && isActive) {
                if (System.currentTimeMillis() - waitStartTime > timeoutMs) {
                    Log.w("BgGenService", "Timeout waiting for bitmap consumption (sdapi)")
                    break
                }
                delay(100)
            }
            stopSelf()
        } finally {
            progressJob.cancel()
        }
    }

    private data class SdApiParsedResult(
        val base64Image: String,
        val seed: Long?
    )

    private fun parseSdApiResponse(body: ResponseBody): SdApiParsedResult {
        var imageB64: String? = null
        var seed: Long? = null

        JsonReader(body.charStream()).use { reader ->
            reader.isLenient = true
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "images" -> {
                        reader.beginArray()
                        if (reader.hasNext()) {
                            imageB64 = reader.nextString()
                        }
                        while (reader.hasNext()) {
                            reader.skipValue()
                        }
                        reader.endArray()
                    }

                    "info" -> {
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull()
                        } else {
                            val infoRaw = reader.nextString()
                            if (infoRaw.startsWith("{")) {
                                try {
                                    val infoJson = JSONObject(infoRaw)
                                    if (infoJson.has("seed")) {
                                        seed = infoJson.optLong("seed")
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }

        var b64 = imageB64 ?: throw IOException("sdapi: images is empty")
        val commaPos = b64.indexOf(',')
        if (commaPos >= 0) {
            b64 = b64.substring(commaPos + 1)
        }
        return SdApiParsedResult(base64Image = b64, seed = seed)
    }

    private fun persistSdApiImage(imageBytes: ByteArray): File {
        if (imageBytes.isEmpty()) {
            throw IOException("sdapi: decoded image is empty")
        }
        val tmpDir = File(filesDir, "tmp_results").apply { mkdirs() }
        val outFile = File(tmpDir, "sdapi_${System.currentTimeMillis()}.png")
        outFile.outputStream().use { out ->
            out.write(imageBytes)
        }
        return outFile
    }

    private fun createNotificationChannel() {
        val name = "Image Generation"
        val descriptionText = "Background image generation"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(progress: Float): Notification {

        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(this.getString(R.string.generating_notify))
            .setContentText("Progress: ${(progress * 100).toInt()}%")
            .setProgress(100, (progress * 100).toInt(), false)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(progress: Float) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(progress))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        Log.e("GenerationService", "Foreground service timeout")
        updateState(GenerationState.Error("Service timeout"))
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        if (_generationState.value is GenerationState.Error) {
            resetState()
        }

        _isServiceRunning.value = false
        Log.d("GenerationService", "service destroyed, isServiceRunning set to false")
    }
}
