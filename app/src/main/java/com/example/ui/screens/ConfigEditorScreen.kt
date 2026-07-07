package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import android.content.Context
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Upload
import coil.compose.rememberAsyncImagePainter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WebApkProject
import com.example.ui.WebApkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorScreen(
    projectId: Long = 0,
    viewModel: WebApkViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sharedPref = remember { context.getSharedPreferences("config_autosave_pref", Context.MODE_PRIVATE) }

    val saveDraft = { key: String, value: Any ->
        val editor = sharedPref.edit()
        val fullKey = if (projectId == 0L) "draft_$key" else "edit_${projectId}_$key"
        when (value) {
            is String -> editor.putString(fullKey, value)
            is Boolean -> editor.putBoolean(fullKey, value)
            is Int -> editor.putInt(fullKey, value)
        }
        editor.apply()
    }

    val clearDrafts = {
        val editor = sharedPref.edit()
        if (projectId == 0L) {
            editor.remove("draft_appName")
            editor.remove("draft_webUrl")
            editor.remove("draft_packageName")
            editor.remove("draft_orientation")
            editor.remove("draft_displayMode")
            editor.remove("draft_enableJs")
            editor.remove("draft_enableZoom")
            editor.remove("draft_domStorage")
            editor.remove("draft_themeColor")
            editor.remove("draft_appIcon")
            editor.remove("draft_apkFileName")
        } else {
            editor.remove("edit_${projectId}_appName")
            editor.remove("edit_${projectId}_webUrl")
            editor.remove("edit_${projectId}_packageName")
            editor.remove("edit_${projectId}_orientation")
            editor.remove("edit_${projectId}_displayMode")
            editor.remove("edit_${projectId}_enableJs")
            editor.remove("edit_${projectId}_enableZoom")
            editor.remove("edit_${projectId}_domStorage")
            editor.remove("edit_${projectId}_themeColor")
            editor.remove("edit_${projectId}_appIcon")
            editor.remove("edit_${projectId}_apkFileName")
        }
        editor.apply()
    }

    // Form fields state (initializes from draft if available)
    var appName by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_appName", "") ?: "") else "") }
    var webUrl by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_webUrl", "") ?: "") else "") }
    var packageName by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_packageName", "") ?: "") else "") }
    var apkFileName by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_apkFileName", "") ?: "") else "") }
    var orientation by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_orientation", "UNSPECIFIED") ?: "UNSPECIFIED") else "UNSPECIFIED") }
    var displayMode by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_displayMode", "STANDARD") ?: "STANDARD") else "STANDARD") }
    var enableJs by remember { mutableStateOf(if (projectId == 0L) sharedPref.getBoolean("draft_enableJs", true) else true) }
    var enableZoom by remember { mutableStateOf(if (projectId == 0L) sharedPref.getBoolean("draft_enableZoom", false) else false) }
    var domStorage by remember { mutableStateOf(if (projectId == 0L) sharedPref.getBoolean("draft_domStorage", true) else true) }
    var themeColor by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_themeColor", "OCEAN_BLUE") ?: "OCEAN_BLUE") else "OCEAN_BLUE") }
    var appIcon by remember { mutableStateOf(if (projectId == 0L) (sharedPref.getString("draft_appIcon", "language") ?: "language") else "language") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLivePreview by remember { mutableStateOf(false) }

    var customIconUri by remember { mutableStateOf<Uri?>(null) }
    var uploadStatusMessage by remember { mutableStateOf("") }
    var isUploadingIcon by remember { mutableStateOf(false) }

    val configImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customIconUri = it
            if (projectId != 0L) {
                isUploadingIcon = true
                uploadStatusMessage = "Uploading customized launcher icon... Please wait"
                viewModel.uploadProjectIcon(
                    projectId = projectId,
                    iconUri = it,
                    onSuccess = { previewPath ->
                        isUploadingIcon = false
                        uploadStatusMessage = "Icon successfully uploaded!"
                        Toast.makeText(context, "Launcher icon uploaded successfully!", Toast.LENGTH_LONG).show()
                    },
                    onError = { errMsg ->
                        isUploadingIcon = false
                        uploadStatusMessage = "Upload failed: $errMsg"
                        Toast.makeText(context, "Icon upload unsuccessful: $errMsg", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                val pref = context.getSharedPreferences("apk_metadata_pref", Context.MODE_PRIVATE)
                pref.edit().putString("app_icon_uri", it.toString()).apply()
                uploadStatusMessage = "Icon selected. It will upload automatically during compilation."
                Toast.makeText(context, "Icon selected! It will be applied during build.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // If editing, observe project state and initialize
    val editingProject by if (projectId != 0L) {
        viewModel.getProjectById(projectId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<WebApkProject?>(null) }
    }

    LaunchedEffect(editingProject) {
        editingProject?.let { p ->
            appName = sharedPref.getString("edit_${projectId}_appName", p.name) ?: p.name
            webUrl = sharedPref.getString("edit_${projectId}_webUrl", p.url) ?: p.url
            packageName = sharedPref.getString("edit_${projectId}_packageName", p.packageName) ?: p.packageName
            apkFileName = sharedPref.getString("edit_${projectId}_apkFileName", p.apkFileName) ?: p.apkFileName
            orientation = sharedPref.getString("edit_${projectId}_orientation", p.orientation) ?: p.orientation
            displayMode = sharedPref.getString("edit_${projectId}_displayMode", p.displayMode) ?: p.displayMode
            enableJs = sharedPref.getBoolean("edit_${projectId}_enableJs", p.enableJs)
            enableZoom = sharedPref.getBoolean("edit_${projectId}_enableZoom", p.enableZoom)
            domStorage = sharedPref.getBoolean("edit_${projectId}_domStorage", p.domStorage)
            themeColor = sharedPref.getString("edit_${projectId}_themeColor", p.themeColor) ?: p.themeColor
            appIcon = sharedPref.getString("edit_${projectId}_appIcon", p.appIcon) ?: p.appIcon
        }
    }

    // Auto package name and default apk filename generation matching app name
    LaunchedEffect(appName) {
        if (projectId == 0L && appName.isNotEmpty()) {
            val scrubbed = appName.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
            if (scrubbed.isNotEmpty()) {
                packageName = "com.webapk.$scrubbed"
                saveDraft("packageName", "com.webapk.$scrubbed")
            }
            val safeName = appName.replace("[^a-zA-Z0-9]".toRegex(), "_")
            apkFileName = "${safeName}_LustWebApk.apk"
            saveDraft("apkFileName", apkFileName)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (projectId == 0L) "New Web APK" else "Edit Template",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Cancel")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1E293B),
                        navigationIconContentColor = Color(0xFF1E293B)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF7F9FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // SECTION 1: Identity Cards
            Text(
                text = "APPLICATION IDENTITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // App Name
                    val isAppNameError = appName.isBlank()
                    OutlinedTextField(
                        value = appName,
                        onValueChange = {
                            appName = it
                            saveDraft("appName", it)
                        },
                        label = { Text("App Name (e.g., My Portfolio)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AppShortcut, contentDescription = null, tint = Color(0xFF4F46E5))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            focusedLabelColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        isError = isAppNameError,
                        supportingText = if (isAppNameError) {
                            { Text("App name cannot be empty.", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Website URL
                    val webUrlValidationError = remember(webUrl) {
                        com.example.validation.AndroidNamingValidator.getUrlValidationError(webUrl)
                    }
                    OutlinedTextField(
                        value = webUrl,
                        onValueChange = {
                            webUrl = it
                            saveDraft("webUrl", it)
                        },
                        label = { Text("Website Start URL (e.g., myportfolio.com)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color(0xFF4F46E5))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            focusedLabelColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        isError = webUrlValidationError != null,
                        supportingText = if (webUrlValidationError != null) {
                            { Text(webUrlValidationError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Package Name
                    val packageNameValidationError = remember(packageName) {
                        com.example.validation.AndroidNamingValidator.getPackageNameValidationError(packageName)
                    }
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = {
                            packageName = it
                            saveDraft("packageName", it)
                        },
                        label = { Text("Java Package Name (e.g., com.portfolio.app)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF4F46E5))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            focusedLabelColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        isError = packageNameValidationError != null,
                        supportingText = if (packageNameValidationError != null) {
                            { Text(packageNameValidationError, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom APK File Name
                    OutlinedTextField(
                        value = apkFileName,
                        onValueChange = {
                            apkFileName = it
                            saveDraft("apkFileName", it)
                        },
                        label = { Text("Output APK File Name (e.g., custom_name.apk)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.PhonelinkSetup, contentDescription = null, tint = Color(0xFF4F46E5))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            focusedLabelColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        singleLine = true,
                        supportingText = { Text("Specify your preferred file name for the generated APK.") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: System Toggles & Layout Options
            Text(
                text = "SURFACE & CONFIGURATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Orientation Selection
                    Text(
                        text = "Screen Orientation Lock",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OrientationRadioOption("Unspecified", "UNSPECIFIED", orientation) { orientation = it; saveDraft("orientation", it) }
                        OrientationRadioOption("Portrait", "PORTRAIT", orientation) { orientation = it; saveDraft("orientation", it) }
                        OrientationRadioOption("Landscape", "LANDSCAPE", orientation) { orientation = it; saveDraft("orientation", it) }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Display Mode Selection
                    Text(
                        text = "Window Rendering Layout",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DisplayModeRadioOption("Standard", "STANDARD", displayMode) { displayMode = it; saveDraft("displayMode", it) }
                        DisplayModeRadioOption("Fullscreen", "FULLSCREEN", displayMode) { displayMode = it; saveDraft("displayMode", it) }
                        DisplayModeRadioOption("EdgeToEdge", "EDGE_TO_EDGE", displayMode) { displayMode = it; saveDraft("displayMode", it) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: WebView Settings Card
            Text(
                text = "WEBVIEW BROWSER ENGINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    WebViewSwitchOption(
                        title = "Enable JavaScript support",
                        checked = enableJs,
                        onCheckedChange = { enableJs = it; saveDraft("enableJs", it) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    WebViewSwitchOption(
                        title = "Activate Touch Gestures Zooming",
                        checked = enableZoom,
                        onCheckedChange = { enableZoom = it; saveDraft("enableZoom", it) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    WebViewSwitchOption(
                        title = "Support DOM Storage indexing",
                        checked = domStorage,
                        onCheckedChange = { domStorage = it; saveDraft("domStorage", it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 4: Theme Color selector Card
            Text(
                text = "APP BRAND COLORS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Primary Accent Palette",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorPaletteCell("OCEAN_BLUE", Color(0xFF3B82F6), themeColor) { themeColor = it; saveDraft("themeColor", it) }
                        ColorPaletteCell("EMERALD", Color(0xFF10B981), themeColor) { themeColor = it; saveDraft("themeColor", it) }
                        ColorPaletteCell("ROYAL_PURPLE", Color(0xFF8B5CF6), themeColor) { themeColor = it; saveDraft("themeColor", it) }
                        ColorPaletteCell("CRIMSON", Color(0xFFEF4444), themeColor) { themeColor = it; saveDraft("themeColor", it) }
                        ColorPaletteCell("AMBER", Color(0xFFF59E0B), themeColor) { themeColor = it; saveDraft("themeColor", it) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 5: App Launcher Icon selection Card
            Text(
                text = "APP LAUNCHER ICON",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Launcher Icon Preset",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconSelectorCell("language", Icons.Default.Language, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                        IconSelectorCell("shopping_cart", Icons.Default.ShoppingCart, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                        IconSelectorCell("business", Icons.Default.Work, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                        IconSelectorCell("article", Icons.Default.Article, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconSelectorCell("forum", Icons.Default.Forum, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                        IconSelectorCell("gamepad", Icons.Default.SportsEsports, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                        IconSelectorCell("book", Icons.Default.Book, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                        IconSelectorCell("music_note", Icons.Default.MusicNote, appIcon) { appIcon = it; saveDraft("appIcon", it) }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Text(
                        text = "Or Upload Custom Icon",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Upload custom images directly to the backend compiler service. Resizing to Android mipmap densities (mdpi through xxxhdpi) executes automatically.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (customIconUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEF2FF), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(customIconUri),
                                    contentDescription = "Custom Icon preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Icon Picked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4F46E5)
                                )
                                Text(
                                    text = customIconUri.toString().take(30) + "...",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { configImageLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = Color(0xFF4F46E5)
                        ),
                        enabled = !isUploadingIcon
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Upload icon",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUploadingIcon) "Uploading..." else "Select Icon from Gallery",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uploadStatusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uploadStatusMessage,
                            color = if (uploadStatusMessage.contains("failed", true)) Color(0xFFEF4444) else Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // PREVIEW BROWSER GESTURES BEFORE SAVE
            Button(
                onClick = {
                    if (webUrl.isNotBlank()) {
                        showLivePreview = true
                    } else {
                        Toast.makeText(context, "Please enter a target website URL first.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Preview Live")
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PREVIEW WEBSITE VIEWPORT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DYNAMIC COMPLIANCE & VALIDATION CARD
            val activeTempProj = WebApkProject(
                id = projectId,
                name = appName,
                url = webUrl,
                packageName = packageName,
                orientation = orientation,
                displayMode = displayMode,
                enableJs = enableJs,
                enableZoom = enableZoom,
                domStorage = domStorage,
                themeColor = themeColor,
                appIcon = appIcon,
                apkFileName = apkFileName
            )
            val activeValidationErrors = remember(appName, webUrl, packageName) {
                viewModel.validateProject(activeTempProj)
            }
            val hasAppNameError = appName.isBlank()
            val isFormFullyCompliant = activeValidationErrors.isEmpty() && !hasAppNameError

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isFormFullyCompliant) Color(0xFF10B981) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFormFullyCompliant) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isFormFullyCompliant) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isFormFullyCompliant) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFormFullyCompliant) "ANDROID COMPLIANCE: PASSED" else "ANDROID COMPLIANCE: VERIFYING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFormFullyCompliant) Color(0xFF047857) else Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // App Name check
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (!hasAppNameError) Color(0xFF10B981) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasAppNameError) "App name is required" else "App Name: Compliant",
                            fontSize = 12.sp,
                            color = if (hasAppNameError) Color(0xFFEF4444) else Color(0xFF1E293B),
                            fontWeight = if (hasAppNameError) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Web URL check
                    val isUrlEmpty = webUrl.isBlank()
                    val urlErrorMsg = activeValidationErrors.firstOrNull { it.contains("URL", ignoreCase = true) }
                    val hasUrlError = urlErrorMsg != null
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (!isUrlEmpty && !hasUrlError) Color(0xFF10B981) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isUrlEmpty -> "Website URL is required"
                                hasUrlError -> urlErrorMsg ?: "Invalid website URL structure"
                                else -> "Website URL: Compliant"
                            },
                            fontSize = 12.sp,
                            color = if (isUrlEmpty || hasUrlError) Color(0xFFEF4444) else Color(0xFF1E293B),
                            fontWeight = if (isUrlEmpty || hasUrlError) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Package Name check
                    val isPkgEmpty = packageName.isBlank()
                    val pkgErrorMsg = activeValidationErrors.firstOrNull { it.contains("Package", ignoreCase = true) || it.contains("keyword", ignoreCase = true) }
                    val hasPkgError = pkgErrorMsg != null
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (!isPkgEmpty && !hasPkgError) Color(0xFF10B981) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isPkgEmpty -> "Package Name is required"
                                hasPkgError -> pkgErrorMsg ?: "Package name structure invalid (must contain dot)"
                                else -> "Package Name: Compliant"
                            },
                            fontSize = 12.sp,
                            color = if (isPkgEmpty || hasPkgError) Color(0xFFEF4444) else Color(0xFF1E293B),
                            fontWeight = if (isPkgEmpty || hasPkgError) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    if (activeValidationErrors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                activeValidationErrors.forEach { error ->
                                    Text(
                                        text = "• $error",
                                        color = Color(0xFF991B1B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SAVE BUTTON
            Button(
                onClick = {
                    if (isFormFullyCompliant) {
                        viewModel.saveProject(
                            id = projectId,
                            name = appName,
                            url = webUrl,
                            packageName = packageName,
                            orientation = orientation,
                            displayMode = displayMode,
                            enableJs = enableJs,
                            enableZoom = enableZoom,
                            domStorage = domStorage,
                            themeColor = themeColor,
                            appIcon = appIcon,
                            apkFileName = apkFileName
                        )
                        clearDrafts()
                        Toast.makeText(context, "Configuration profiles saved offline!", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Configuration saved successfully!")
                        }
                        onSaved()
                    }
                },
                enabled = isFormFullyCompliant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (projectId == 0L) "SAVE & START BUILD" else "SAVE PROFILE CHANGES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showLivePreview) {
        Dialog(
            onDismissRequest = { showLivePreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header inside Dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE BROWSER PREVIEW Sandbox",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = webUrl,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { showLivePreview = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = Color.White
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    webViewClient = WebViewClient()
                                    webChromeClient = WebChromeClient()
                                    settings.apply {
                                        javaScriptEnabled = enableJs
                                        domStorageEnabled = domStorage
                                        builtInZoomControls = enableZoom
                                        displayZoomControls = false
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                    }
                                    val formattedUrl = if (webUrl.startsWith("http://") || webUrl.startsWith("https://")) {
                                        webUrl
                                    } else {
                                        "https://$webUrl"
                                    }
                                    loadUrl(formattedUrl)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrientationRadioOption(label: String, value: String, selectedValue: String, onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onSelect(value) }
    ) {
        RadioButton(
            selected = value == selectedValue,
            onClick = { onSelect(value) }
        )
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun DisplayModeRadioOption(label: String, value: String, selectedValue: String, onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onSelect(value) }
    ) {
        RadioButton(
            selected = value == selectedValue,
            onClick = { onSelect(value) }
        )
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun WebViewSwitchOption(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4F46E5)
            )
        )
    }
}

@Composable
fun ColorPaletteCell(
    colorKey: String,
    resolvedColor: Color,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    val isSelected = colorKey == selectedValue
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(resolvedColor)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF4F46E5) else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onSelect(colorKey) },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun IconSelectorCell(
    iconKey: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    val isSelected = iconKey == selectedValue
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect(iconKey) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF4F46E5) else Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
        )
    }
}
