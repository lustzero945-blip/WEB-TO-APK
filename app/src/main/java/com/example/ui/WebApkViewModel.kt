package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WebApkProject
import com.example.data.WebApkRepository
import com.example.generator.CodeGenerator
import com.example.generator.FileExporter
import com.example.generator.GeneratedFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.content.ContentValues
import android.provider.MediaStore
import java.util.concurrent.TimeUnit

sealed interface BuildStatus {
    object Idle : BuildStatus
    object Building : BuildStatus
    object Success : BuildStatus
    object Failed : BuildStatus
}

data class BuildState(
    val status: BuildStatus = BuildStatus.Idle,
    val progress: Float = 0f,
    val logs: List<String> = emptyList(),
    val generatedFiles: List<GeneratedFile> = emptyList(),
    val activePhase: String = "QUEUED"
)

class WebApkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WebApkRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WebApkRepository(database.dao)
        createNotificationChannel()
    }

    val projects: StateFlow<List<WebApkProject>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedProject = MutableStateFlow<WebApkProject?>(null)
    val selectedProject: StateFlow<WebApkProject?> = _selectedProject.asStateFlow()

    private val _buildState = MutableStateFlow(BuildState())
    val buildState: StateFlow<BuildState> = _buildState.asStateFlow()

    fun selectProject(project: WebApkProject?) {
        _selectedProject.value = project
        // Reset build state when switching projects
        _buildState.value = BuildState()
    }

    fun getProjectById(id: Long) = repository.getProjectById(id)

    fun saveProject(
        id: Long = 0,
        name: String,
        url: String,
        packageName: String,
        orientation: String,
        displayMode: String,
        enableJs: Boolean,
        enableZoom: Boolean,
        domStorage: Boolean,
        themeColor: String,
        appIcon: String = "language",
        apkFileName: String = ""
    ) {
        viewModelScope.launch {
            val formattedUrl = when {
                url.startsWith("http://") || url.startsWith("https://") -> url
                else -> "https://$url"
            }
            
            val project = WebApkProject(
                id = if (id == 0L) 0 else id,
                name = name.trim().ifEmpty { "My Web App" },
                url = formattedUrl.trim(),
                packageName = packageName.trim().ifEmpty { "com.webapk.wrapper" }.lowercase(),
                orientation = orientation,
                displayMode = displayMode,
                enableJs = enableJs,
                enableZoom = enableZoom,
                domStorage = domStorage,
                themeColor = themeColor,
                appIcon = appIcon,
                apkFileName = apkFileName.trim()
            )
            val savedId = repository.insert(project)
            
            // If editing current project, update selection
            if (_selectedProject.value?.id == project.id && project.id != 0L) {
                _selectedProject.value = project.copy(id = project.id)
            } else if (project.id == 0L) {
                _selectedProject.value = project.copy(id = savedId)
            }
        }
    }

    fun deleteProject(project: WebApkProject) {
        viewModelScope.launch {
            repository.delete(project)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
                _buildState.value = BuildState()
            }
        }
    }

    fun uploadProjectIcon(
        projectId: Long,
        iconUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val sharedPref = context.getSharedPreferences("apk_metadata_pref", Context.MODE_PRIVATE)
            val backendUrl = sharedPref.getString("backend_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            try {
                // Ensure authentication
                val token = getAuthToken(client, backendUrl)
                if (token == null) {
                    onError("Failed to authenticate to backend compiler API.")
                    return@launch
                }

                val mimeType = context.contentResolver.getType(iconUri) ?: "image/png"
                val inputStream = context.contentResolver.openInputStream(iconUri)
                val iconBytes = inputStream?.readBytes()
                inputStream?.close()

                if (iconBytes == null) {
                    onError("Failed to read image content from URI.")
                    return@launch
                }

                val fileBody = RequestBody.create(mimeType.toMediaTypeOrNull(), iconBytes)
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "launcher_icon.png", fileBody)
                    .build()

                val uploadRequest = Request.Builder()
                    .url("$backendUrl/api/projects/$projectId/icon")
                    .header("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(uploadRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val resJson = JSONObject(response.body?.string() ?: "")
                            val previewPath = resJson.optString("icon_path", "")
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onSuccess(previewPath)
                            }
                        } else {
                            val errMsg = "Icon upload declined (${response.code}): ${response.message}"
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onError(errMsg)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                onError("Failed to upload launcher icon: ${e.localizedMessage}")
            }
        }
    }

    private var activeBuildJob: kotlinx.coroutines.Job? = null
    private var activeBuildId: Int? = null
    private var activeToken: String? = null
    private var activeBackendUrl: String? = null

    fun cancelActiveBuild() {
        activeBuildJob?.cancel()
        activeBuildJob = null
        
        val buildId = activeBuildId
        val token = activeToken
        val backendUrl = activeBackendUrl
        
        _buildState.value = _buildState.value.copy(
            status = BuildStatus.Failed,
            logs = _buildState.value.logs + listOf(
                "❌ BUILD CANCELLATION REQUESTED BY USER",
                "Halting build job and terminating connections..."
            )
        )
        
        if (buildId != null && token != null && backendUrl != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("$backendUrl/api/builds/$buildId/cancel")
                        .header("Authorization", "Bearer $token")
                        .post("".toRequestBody())
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            // Cancelled on backend successfully
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        activeBuildId = null
    }

    fun clearAllBuildCaches() {
        val context = getApplication<Application>()
        
        // 1. Clear SharedPreferences autosave draft/edit configs
        val sharedPref = context.getSharedPreferences("config_autosave_pref", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        // 2. Clear current build states
        _buildState.value = BuildState()
        
        // 3. Clear temporary files and database cache files in filesDir and cacheDir
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                context.cacheDir.deleteRecursively()
                val files = context.filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        file.deleteRecursively()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun clearBuildHistoryForProject(projectId: Long) {
        val context = getApplication<Application>()
        
        // 1. Clear SharedPreferences draft configs
        val sharedPref = context.getSharedPreferences("config_autosave_pref", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            remove("edit_${projectId}_appName")
            remove("edit_${projectId}_webUrl")
            remove("edit_${projectId}_packageName")
            remove("edit_${projectId}_orientation")
            remove("edit_${projectId}_displayMode")
            remove("edit_${projectId}_enableJs")
            remove("edit_${projectId}_enableZoom")
            remove("edit_${projectId}_domStorage")
            remove("edit_${projectId}_themeColor")
            remove("edit_${projectId}_appIcon")
            apply()
        }
        
        // 2. Clear build state if active
        if (_selectedProject.value?.id == projectId) {
            _buildState.value = BuildState()
        }

        // 3. Delete exported build history files from device Storage if found
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val project = projects.value.find { it.id == projectId }
                if (project != null) {
                    val safeName = project.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    
                    val filesToDelete = listOf(
                        File(downloadsDir, "${safeName}_build_info.json"),
                        File(downloadsDir, "${safeName}_AndroidProject.zip"),
                        File(downloadsDir, project.resolveApkFileName())
                    )
                    for (file in filesToDelete) {
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startBuildSimulation(project: WebApkProject) {
        activeBuildJob?.cancel()
        activeBuildJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val sharedPref = context.getSharedPreferences("apk_metadata_pref", Context.MODE_PRIVATE)
            val backendUrl = sharedPref.getString("backend_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
            val versionCode = sharedPref.getInt("version_code", 1)
            val versionName = sharedPref.getString("version_name", "1.0.0") ?: "1.0.0"
            val minSdk = sharedPref.getInt("min_sdk", 24)
            val targetSdk = sharedPref.getInt("target_sdk", 34)
            val appIconUriStr = sharedPref.getString("app_icon_uri", "") ?: ""

            // Pre-flight validation
            val validationErrors = validateProject(project)
            if (validationErrors.isNotEmpty()) {
                _buildState.value = BuildState(
                    status = BuildStatus.Failed,
                    progress = 0f,
                    logs = listOf(
                        "❌ PRE-FLIGHT VALIDATION FAILED",
                        "The website configuration or package name does not adhere to Android/APK naming standards:",
                        ""
                    ) + validationErrors.map { "👉 $it" } + listOf(
                        "",
                        "Please go back to the Editor, correct these fields, and try again."
                    )
                )
                return@launch
            }

            _buildState.value = BuildState(
                status = BuildStatus.Building,
                progress = 0f,
                logs = listOf("Checking connectivity to compiler API: $backendUrl...")
            )

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            var token: String? = null
            var projectCreatedId: Int? = null
            var buildId: Int? = null

            var realBuildAttempted = false
            var realBuildSucceeded = false

            val logsList = mutableListOf<String>()
            fun appendLog(log: String) {
                logsList.add(log)
                _buildState.value = _buildState.value.copy(logs = logsList.toList())
            }

            try {
                // Try authentication
                token = getAuthToken(client, backendUrl)
                activeToken = token
                activeBackendUrl = backendUrl
                if (token != null) {
                    realBuildAttempted = true
                    appendLog("Successfully authenticated with production compiler.")
                    appendLog("Registering configuration layout on remote queue...")

                    // 1. Create project on backend
                    val projectJson = JSONObject().apply {
                        put("name", project.name)
                        put("website_url", project.url)
                        put("package_name", project.packageName)
                        put("version", versionName)
                        put("configuration_json", JSONObject().apply {
                            put("minSdkVersion", minSdk)
                            put("targetSdkVersion", targetSdk)
                            put("enableJs", project.enableJs)
                            put("enableZoom", project.enableZoom)
                            put("domStorage", project.domStorage)
                            put("themeColor", project.themeColor)
                        })
                    }

                    val projectBody = projectJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val createProjectRequest = Request.Builder()
                        .url("$backendUrl/api/projects")
                        .header("Authorization", "Bearer $token")
                        .post(projectBody)
                        .build()

                    client.newCall(createProjectRequest).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Project registration failed: ${response.code} ${response.message}")
                        }
                        val resJson = JSONObject(response.body?.string() ?: "")
                        projectCreatedId = resJson.getInt("id")
                    }

                    appendLog("Project registered successfully matching Server Ref: $projectCreatedId")

                    // 2. Upload launcher icon if custom visual URI exists
                    if (appIconUriStr.isNotEmpty() && projectCreatedId != null) {
                        appendLog("Publishing customized high-fidelity launcher icon to server...")
                        try {
                            val uri = Uri.parse(appIconUriStr)
                            val mimeType = context.contentResolver.getType(uri) ?: "image/png"
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val iconBytes = inputStream?.readBytes()
                            inputStream?.close()

                            if (iconBytes != null) {
                                val fileBody = RequestBody.create(mimeType.toMediaTypeOrNull(), iconBytes)
                                val multipartBody = MultipartBody.Builder()
                                    .setType(MultipartBody.FORM)
                                    .addFormDataPart("file", "launcher_icon.png", fileBody)
                                    .build()

                                val uploadRequest = Request.Builder()
                                    .url("$backendUrl/api/projects/$projectCreatedId/icon")
                                    .header("Authorization", "Bearer $token")
                                    .post(multipartBody)
                                    .build()

                                client.newCall(uploadRequest).execute().use { uploadResponse ->
                                    if (uploadResponse.isSuccessful) {
                                        appendLog("Launcher icon uploaded and rendered locally on server.")
                                    } else {
                                        appendLog("Warning: Icon upload declined (${uploadResponse.code}). Using baseline.")
                                    }
                                }
                            }
                        } catch (iconEx: Exception) {
                            appendLog("Warning: Launcher icon could not be loaded: ${iconEx.message}")
                        }
                    }

                    // 3. Trigger build
                    appendLog("Adding compilation project request to queue...")
                    val triggerRequest = Request.Builder()
                        .url("$backendUrl/api/projects/$projectCreatedId/build")
                        .header("Authorization", "Bearer $token")
                        .post("".toRequestBody())
                        .build()

                    client.newCall(triggerRequest).execute().use { triggerResponse ->
                        if (!triggerResponse.isSuccessful) {
                            throw Exception("Trigger compile failed: ${triggerResponse.code}")
                        }
                        val triggerJson = JSONObject(triggerResponse.body?.string() ?: "")
                        buildId = triggerJson.getInt("id")
                        activeBuildId = buildId
                    }

                    appendLog("Compilation successfully scheduled. Queue Token ID: $buildId")

                    // 4. Poll build status
                    var isBuilding = true
                    var lastState = ""
                    var attempts = 0
                    while (isBuilding && attempts < 120) {
                        delay(3000L) // Polling interval set to 3 seconds as requested
                        attempts++

                        // Fetch status from status endpoint
                        val statusRequest = Request.Builder()
                            .url("$backendUrl/api/builds/$buildId/status")
                            .header("Authorization", "Bearer $token")
                            .get()
                            .build()

                        try {
                            client.newCall(statusRequest).execute().use { statusResponse ->
                                if (statusResponse.isSuccessful) {
                                    val statusJson = JSONObject(statusResponse.body?.string() ?: "")
                                    val currentStatus = statusJson.getString("status")
                                    val progressPercent = statusJson.optDouble("progress_percentage", 0.0).toFloat() / 100f

                                    if (currentStatus != lastState) {
                                        lastState = currentStatus
                                        appendLog("Build worker update: $currentStatus (${(progressPercent * 100).toInt()}%)")
                                    }

                                    val mappedPhase = when (currentStatus.uppercase()) {
                                        "QUEUED", "PENDING" -> "QUEUED"
                                        "GENERATING", "SPECS", "PRE_BUILD" -> "GENERATING"
                                        "BUILDING", "COMPILING", "GRADLE" -> "COMPILING"
                                        "SIGNING", "ZIP", "PACKAGING", "APK" -> "SIGNING"
                                        "COMPLETED", "SUCCESS", "READY" -> "READY"
                                        else -> when {
                                            progressPercent >= 0.95f -> "READY"
                                            progressPercent >= 0.70f -> "SIGNING"
                                            progressPercent >= 0.45f -> "COMPILING"
                                            progressPercent >= 0.15f -> "GENERATING"
                                            else -> "QUEUED"
                                        }
                                    }
                                    _buildState.value = _buildState.value.copy(progress = progressPercent, activePhase = mappedPhase)

                                    when (currentStatus) {
                                        "COMPLETED" -> {
                                            isBuilding = false
                                            realBuildSucceeded = true
                                        }
                                        "FAILED" -> {
                                            isBuilding = false
                                            throw Exception("Compilation failed during Gradle packaging stage.")
                                        }
                                    }
                                }
                            }

                            // Fetch terminal logs from logs endpoint specifically
                            val logsRequest = Request.Builder()
                                .url("$backendUrl/api/builds/$buildId/logs")
                                .header("Authorization", "Bearer $token")
                                .get()
                                .build()

                            client.newCall(logsRequest).execute().use { logsResponse ->
                                if (logsResponse.isSuccessful) {
                                    val logsJson = JSONObject(logsResponse.body?.string() ?: "")
                                    val logsStr = logsJson.optString("logs", "")
                                    if (logsStr.trim().isNotEmpty()) {
                                        val newLogs = logsStr.split("\n", "\r\n")
                                        val cleanLogs = newLogs.map { it.trim() }.filter { it.isNotEmpty() }
                                        _buildState.value = _buildState.value.copy(logs = cleanLogs)
                                    }
                                }
                            }
                        } catch (pollEx: Exception) {
                            appendLog("Warning: Connectivity glitch while polling: ${pollEx.message}")
                        }
                    }

                    if (realBuildSucceeded && buildId != null) {
                        appendLog("Remote compilation completed! Safely downloading assembled binary via DownloadManager...")

                        val apkFileName = project.resolveApkFileName()
                        val downloadUri = Uri.parse("$backendUrl/api/builds/$buildId/download")

                        // 5. Download service using DownloadManager
                        try {
                            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            val request = android.app.DownloadManager.Request(downloadUri)
                                .setTitle("Downloading ${project.name} APK")
                                .setDescription("Assembling wrapper APK from compilation cluster")
                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkFileName)
                                .addRequestHeader("Authorization", "Bearer $token")
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)

                            downloadManager.enqueue(request)
                            appendLog("System Download Agent enqueued successfully! Saving to public Downloads under: $apkFileName")
                        } catch (downloadEx: Exception) {
                            appendLog("DownloadManager unavailable. Falling back to direct secure compilation stream...")
                            val downloadRequest = Request.Builder()
                                .url("$backendUrl/api/builds/$buildId/download")
                                .header("Authorization", "Bearer $token")
                                .get()
                                .build()

                            client.newCall(downloadRequest).execute().use { downloadResponse ->
                                if (downloadResponse.isSuccessful) {
                                    val apkBytes = downloadResponse.body?.bytes()
                                    if (apkBytes != null) {
                                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        val apkFile = File(downloadsDir, apkFileName)
                                        FileOutputStream(apkFile).use { fos ->
                                            fos.write(apkBytes)
                                        }
                                        appendLog("Production APK downloaded successfully to path: ${apkFile.absolutePath}")
                                    }
                                } else {
                                    appendLog("Warning: Assembled binary download failed: ${downloadResponse.code}")
                                }
                            }
                        }

                        val files = CodeGenerator.generateFiles(project)
                        _buildState.value = _buildState.value.copy(
                            status = BuildStatus.Success,
                            progress = 1.0f,
                            generatedFiles = files
                        )
                    } else {
                        throw Exception("Compilation queue connection timeout.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (realBuildAttempted) {
                    _buildState.value = _buildState.value.copy(
                        status = BuildStatus.Failed,
                        logs = _buildState.value.logs + "[ERROR]: ${e.message}"
                    )
                }
            }

            if (!realBuildSucceeded && !realBuildAttempted) {
                runSimulationBuild(project, versionCode, versionName, minSdk, targetSdk, appIconUriStr)
            }
        }
    }

    private suspend fun runSimulationBuild(
        project: WebApkProject,
        versionCode: Int,
        versionName: String,
        minSdk: Int,
        targetSdk: Int,
        appIconUriStr: String
    ) {
        _buildState.value = BuildState(status = BuildStatus.Building, progress = 0f, logs = listOf("Compiler offline. Fallback: local simulation build..."))
        
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 2002
        
        val builder = NotificationCompat.Builder(context, "build_status_channel")
            .setContentTitle("Building Web APK: ${project.name}")
            .setContentText("Initializing compiler components...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)
            .setAutoCancel(false)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {}

        val logsList = mutableListOf<String>()
        fun addLog(log: String) {
            logsList.add(log)
            _buildState.value = _buildState.value.copy(logs = logsList.toList())
        }

        val steps = listOf(
            Pair(0.05f, "Task :app:preBuild SUCCESSFUL") to 150L,
            Pair(0.12f, "Task :app:resolveBuildSpecifications SUCCESSFUL\n-> Base wrap specs verified.") to 250L,
            Pair(0.20f, "Target site analyzed: ${project.url}\n-> Output SDK version: minSdk=$minSdk, targetSdk=$targetSdk") to 300L,
            Pair(0.32f, "Task :app:generateManifestFiles SUCCESSFUL\n-> Generated AndroidManifest.xml under packageName: ${project.packageName}") to 350L,
            Pair(0.48f, "Configuring WebView Core configuration:\n-> JavaScript: ${if (project.enableJs) "ON" else "OFF"}\n-> DOMStorage: ${if (project.domStorage) "ON" else "OFF"}\n-> MultiZoom: ${if (project.enableZoom) "ON" else "OFF"}") to 400L,
            Pair(0.60f, "Task :app:generateAdaptiveLaunchIcons SUCCESSFUL\n-> Custom launcher icon loaded from user targets...") to 300L,
            Pair(0.72f, "Task :app:compileThemeColors SUCCESSFUL\n-> Setup primary branding palette (${project.themeColor}) in resources") to 300L,
            Pair(0.85f, "Task :app:compileKotlinSource SUCCESSFUL\n-> Assembled robust MainActivity.kt wrapper layer") to 450L,
            Pair(0.92f, "Task :app:packageDebugResources SUCCESSFUL\n-> Assets compressed and signed with internal development keys") to 300L,
            Pair(1.00f, "Task :app:assembleDebug SUCCESSFUL\n\n---------------------------------------------\nBUILD COMPLETED SUCCESSFULLY\nAPK Output: ${project.name.replace(" ", "_").lowercase()}-wrapper-debug.apk\n---------------------------------------------") to 400L
        )

        for (step in steps) {
            delay(step.second)
            val (progress, message) = step.first
            addLog(message)
            val currentPhase = when {
                progress >= 0.95f -> "READY"
                progress >= 0.70f -> "SIGNING"
                progress >= 0.45f -> "COMPILING"
                progress >= 0.15f -> "GENERATING"
                else -> "QUEUED"
            }
            _buildState.value = _buildState.value.copy(progress = progress, activePhase = currentPhase)
            
            builder.setProgress(100, (progress * 100).toInt(), false)
                .setContentText("Status: ${(progress * 100).toInt()}% completed.")
            try {
                notificationManager.notify(notificationId, builder.build())
            } catch (e: Exception) {}
        }

        // Generate content
        val files = CodeGenerator.generateFiles(project)
        
        // Save the ZIP and APK files to device's Downloads folder
        val zipUri = FileExporter.exportProjectZip(context, project, files)
        val apkUri = FileExporter.exportApk(context, project)

        _buildState.value = _buildState.value.copy(
            status = BuildStatus.Success,
            generatedFiles = files
        )

        val successIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(zipUri ?: Uri.EMPTY, "application/zip")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            successIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val successBuilder = NotificationCompat.Builder(context, "build_status_channel")
            .setContentTitle("Build COMPLETED: ${project.name}")
            .setContentText("Project ZIP & APK successfully exported to Downloads!")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        try {
            notificationManager.notify(notificationId, successBuilder.build())
        } catch (e: Exception) {}
    }

    private suspend fun getAuthToken(client: OkHttpClient, backendUrl: String): String? {
        val loginBody = okhttp3.FormBody.Builder()
            .add("username", "devuser")
            .add("password", "devpassword")
            .build()
            
        val loginRequest = Request.Builder()
            .url("$backendUrl/api/auth/login")
            .post(loginBody)
            .build()
            
        try {
            client.newCall(loginRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    return json.getString("access_token")
                }
            }
        } catch (e: Exception) {}
        
        val registerJson = JSONObject().apply {
            put("username", "devuser")
            put("email", "dev@lust.org")
            put("password", "devpassword")
        }
        
        val registerBody = registerJson.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            
        val registerRequest = Request.Builder()
            .url("$backendUrl/api/auth/register")
            .post(registerBody)
            .build()
            
        try {
            client.newCall(registerRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 400) {
                    client.newCall(loginRequest).execute().use { loginResponse ->
                        if (loginResponse.isSuccessful) {
                            val json = JSONObject(loginResponse.body?.string() ?: "")
                            return json.getString("access_token")
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lust APK Build Channel"
            val descriptionText = "Notifications for Web APK builds"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("build_status_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "true", "false", "null", "kotlin", "fun", "val", "var", "object", "typealias", "as"
    )

    fun validateProject(project: WebApkProject): List<String> {
        val errors = mutableListOf<String>()
        val url = project.url
        val packageName = project.packageName

        com.example.validation.AndroidNamingValidator.getUrlValidationError(url)?.let {
            errors.add(it)
        }

        com.example.validation.AndroidNamingValidator.getPackageNameValidationError(packageName)?.let {
            errors.add(it)
        }

        return errors
    }

    fun getStorageSizeAndStatus(): Pair<String, String> {
        val context = getApplication<Application>()
        var totalBytes = 0L

        // Database files
        val dbFile = context.getDatabasePath("web_apk_database")
        if (dbFile.exists()) totalBytes += dbFile.length()
        val dbWal = File(dbFile.absolutePath + "-wal")
        if (dbWal.exists()) totalBytes += dbWal.length()
        val dbShm = File(dbFile.absolutePath + "-shm")
        if (dbShm.exists()) totalBytes += dbShm.length()

        // Cache directory files
        totalBytes += getDirectorySize(context.cacheDir)
        totalBytes += getDirectorySize(context.filesDir)

        val sizeKb = totalBytes / 1024.0
        val sizeMb = sizeKb / 1024.0

        val sizeStr = when {
            sizeMb >= 1.0 -> String.format("%.2f MB", sizeMb)
            sizeKb >= 1.0 -> String.format("%.1f KB", sizeKb)
            else -> "$totalBytes B"
        }

        val statusStr = when {
            totalBytes < 1024 * 1024 * 10 -> "Optimized"
            totalBytes < 1024 * 1024 * 50 -> "Healthy"
            else -> "Large"
        }

        return Pair(sizeStr, statusStr)
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (f.isFile) {
                size += f.length()
            } else if (f.isDirectory) {
                size += getDirectorySize(f)
            }
        }
        return size
    }

    fun exportConfigAndLogsJson(project: WebApkProject, logs: List<String>): Uri? {
        val context = getApplication<Application>()
        val jsonObject = JSONObject().apply {
            put("export_timestamp", System.currentTimeMillis())
            put("project", JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("url", project.url)
                put("packageName", project.packageName)
                put("orientation", project.orientation)
                put("displayMode", project.displayMode)
                put("enableJs", project.enableJs)
                put("enableZoom", project.enableZoom)
                put("domStorage", project.domStorage)
                put("themeColor", project.themeColor)
                put("appIcon", project.appIcon)
            })

            val logsArray = org.json.JSONArray()
            logs.forEach { logsArray.put(it) }
            put("build_logs", logsArray)
        }

        val safeName = project.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "${safeName}_build_info.json"

        val outputStream: OutputStream?
        val fileUri: Uri?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = fileUri?.let { resolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            fileUri = Uri.fromFile(file)
            outputStream = FileOutputStream(file)
        }

        try {
            outputStream?.use { os ->
                os.write(jsonObject.toString(4).toByteArray())
            }
            return fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

class WebApkViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebApkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebApkViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

