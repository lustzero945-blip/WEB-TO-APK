package com.example.generator

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.WebApkProject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileExporter {

    fun exportProjectZip(context: Context, project: WebApkProject, generatedFiles: List<GeneratedFile>): Uri? {
        val safeName = project.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val zipFileName = "${safeName}_AndroidProject.zip"
        
        val outputStream: OutputStream?
        val fileUri: Uri?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = fileUri?.let { resolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, zipFileName)
            fileUri = Uri.fromFile(file)
            outputStream = FileOutputStream(file)
        }

        outputStream?.use { os ->
            ZipOutputStream(os).use { zos ->
                for (genFile in generatedFiles) {
                    val entry = ZipEntry(genFile.path)
                    zos.putNextEntry(entry)
                    zos.write(genFile.content.toByteArray())
                    zos.closeEntry()
                }
            }
        }
        return fileUri
    }

    fun exportApk(context: Context, project: WebApkProject): Uri? {
        val apkFileName = project.resolveApkFileName()
        
        val outputStream: OutputStream?
        val fileUri: Uri?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, apkFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = fileUri?.let { resolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, apkFileName)
            fileUri = Uri.fromFile(file)
            outputStream = FileOutputStream(file)
        }

        outputStream?.use { os ->
            LocalizedBuildWrapper.buildCustomApk(context, project, os)
        }
        return fileUri
    }

    fun exportProjectSetupAsJson(
        context: Context,
        project: WebApkProject,
        logs: List<String>,
        buildStatus: String,
        progress: Float
    ): Uri? {
        val safeName = project.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val jsonFileName = "${safeName}_BuildSetup.json"
        
        val outputStream: OutputStream?
        val fileUri: Uri?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, jsonFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = fileUri?.let { resolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, jsonFileName)
            fileUri = Uri.fromFile(file)
            outputStream = FileOutputStream(file)
        }

        try {
            val rootJson = org.json.JSONObject().apply {
                put("exporter", "Lust Web APK Builder")
                put("exported_at", java.text.DateFormat.getDateTimeInstance().format(java.util.Date()))
                
                val configJson = org.json.JSONObject().apply {
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
                }
                put("project_configuration", configJson)
                
                val logsArray = org.json.JSONArray()
                for (log in logs) {
                    logsArray.put(log)
                }
                put("build_logs", logsArray)
                put("build_status", buildStatus)
                put("build_progress_percent", (progress * 100).toInt())
            }

            outputStream?.use { os ->
                os.write(rootJson.toString(4).toByteArray())
            }
            return fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
