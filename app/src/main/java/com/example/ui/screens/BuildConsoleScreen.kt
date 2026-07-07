package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WebApkProject
import com.example.generator.GeneratedFile
import com.example.ui.BuildStatus
import com.example.ui.WebApkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildConsoleScreen(
    viewModel: WebApkViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.selectedProject.collectAsState()
    val buildState by viewModel.buildState.collectAsState()
    
    val lazyListState = rememberLazyListState()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedFileIndex by remember { mutableStateOf(0) }
    
    var isBuildInitiated by remember { mutableStateOf(buildState.status != BuildStatus.Idle) }

    // Start compile when initiated
    LaunchedEffect(isBuildInitiated, project) {
        if (isBuildInitiated && buildState.status == BuildStatus.Idle) {
            project?.let {
                viewModel.startBuildSimulation(it)
            }
        }
    }

    // Auto-scroll build logger console to bottom
    LaunchedEffect(buildState.logs.size) {
        if (buildState.logs.isNotEmpty()) {
            lazyListState.scrollToItem(buildState.logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(project?.name?.let { "$it - Build Console" } ?: "APK Studio Console", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        project?.let { p ->
                            IconButton(
                                onClick = {
                                    val uri = viewModel.exportConfigAndLogsJson(p, buildState.logs)
                                    if (uri != null) {
                                        Toast.makeText(context, "Exported build setup and logs to JSON!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export Configuration JSON",
                                    tint = Color(0xFF4F46E5)
                                )
                            }
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
        containerColor = Color(0xFFF7F9FB)
    ) { padding ->
        if (isBuildInitiated) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            // SECTION 1: Build progress status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (buildState.status) {
                                        BuildStatus.Building -> Color(0xFFEEF2FF)
                                        BuildStatus.Success -> Color(0xFFDCFCE7)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (buildState.status) {
                                    BuildStatus.Building -> Icons.Default.Loop
                                    BuildStatus.Success -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Build
                                },
                                contentDescription = null,
                                tint = when (buildState.status) {
                                    BuildStatus.Building -> Color(0xFF4F46E5)
                                    BuildStatus.Success -> Color(0xFF10B981)
                                    else -> Color(0xFF94A3B8)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = when (buildState.status) {
                                    BuildStatus.Building -> "Generating Wrap Target Packaging..."
                                    BuildStatus.Success -> "Compilation & Packaging Ready!"
                                    else -> "Idle"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Build assembly status: ${(buildState.progress * 100).toInt()}% complete",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { buildState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (buildState.status == BuildStatus.Success) Color(0xFF10B981) else Color(0xFF4F46E5),
                        trackColor = Color(0xFFE2E8F0),
                    )

                    if (buildState.status == BuildStatus.Building) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.cancelActiveBuild() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancel Build",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel Build", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else if (buildState.status == BuildStatus.Failed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Build halted due to errors.",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = {
                                    project?.let { viewModel.startBuildSimulation(it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Loop,
                                    contentDescription = "Retry Build",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Build", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (buildState.status == BuildStatus.Success) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Genuine Assets Exported Successfully!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your genuine Android Project ZIP and Launcher APK have been assembled & saved offline under your device's Downloads directory:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val safeProjectName = project?.name?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "Project"
                                val apkFileName = project?.resolveApkFileName() ?: "${safeProjectName}_LustWebApk.apk"
                                Text(
                                    text = "📁 ZIP: ${safeProjectName}_AndroidProject.zip\n📱 APK: $apkFileName",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            BuildStepper(
                currentProgress = buildState.progress,
                status = buildState.status,
                activePhase = buildState.activePhase
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab rows: Console logs vs Code generator workspace
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF4F46E5)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Build Logs", fontWeight = FontWeight.SemiBold, color = if (selectedTab == 0) Color(0xFF4F46E5) else Color.Gray) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    enabled = buildState.status == BuildStatus.Success,
                    text = {
                        Text(
                            text = "Code Workspace",
                            fontWeight = FontWeight.SemiBold,
                            color = if (buildState.status != BuildStatus.Success) Color.LightGray else if (selectedTab == 1) Color(0xFF4F46E5) else Color.Gray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Contents
            if (selectedTab == 0) {
                // LOG PANEL
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)), // Dark terminal
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(buildState.logs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    color = if (log.contains("SUCCESS") || log.contains("COMPLETED")) Color(0xFF81C784) else if (log.contains("Target site") || log.contains("settings")) Color(0xFFFFD54F) else Color(0xFFECEFF1),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            } else if (selectedTab == 1 && buildState.status == BuildStatus.Success) {
                // CODE GENERATED STUDIO
                Column(modifier = Modifier.weight(1f)) {
                    // File Picker scrollable list row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 10.dp)
                    ) {
                        buildState.generatedFiles.forEachIndexed { idx, file ->
                            FileChoiceCell(
                                name = file.name,
                                isSelected = idx == selectedFileIndex,
                                onClick = { selectedFileIndex = idx }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    // Active File Contents Explorer
                    val activeFile = buildState.generatedFiles.getOrNull(selectedFileIndex)
                    if (activeFile != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Subheader with location path and copy button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeveloperMode,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeFile.path,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // COPY BUTTON
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("LustWebApk_GeneratedCode", activeFile.content)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "${activeFile.name} copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Code",
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Interactive source text pane
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = activeFile.content,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (activeFile.language == "markdown") Color.Black else Color(0xFF263238),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // EXPORT CONFIG & LOGS BUTTON (visible if there is a project)
                project?.let { proj ->
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            val uri = viewModel.exportConfigAndLogsJson(proj, buildState.logs)
                            if (uri != null) {
                                Toast.makeText(context, "Assembled JSON exported to Downloads folder!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to export configuration JSON.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4F46E5)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXPORT JSON SETUP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (buildState.status == BuildStatus.Success && selectedTab == 0) {
                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeveloperMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXPLORE NATIVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("BACK TO BENCH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    } else {
            // Show State-Managed Verification Preview Panel
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeveloperMode,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "METADATA VERIFICATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Verify Package Details",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF1F5F9))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Detail rows
                        MetadataPreviewRow("App Icon", if (project?.appIcon == "language") "Preset: Language Globe" else "Custom Image Selected", isIcon = true, iconName = project?.appIcon)
                        MetadataPreviewRow("App Name", project?.name ?: "N/A")
                        MetadataPreviewRow("Website URL", project?.url ?: "N/A", isUrl = true)
                        MetadataPreviewRow("Java Package", project?.packageName ?: "N/A", isMonospace = true)
                        MetadataPreviewRow("Output APK File", project?.resolveApkFileName() ?: "N/A", isMonospace = true)
                        MetadataPreviewRow("Screen Orientation", project?.orientation ?: "UNSPECIFIED")
                        MetadataPreviewRow("Display Lock", project?.displayMode ?: "STANDARD")
                        MetadataPreviewRow("Theme Color Accent", project?.themeColor ?: "OCEAN_BLUE", isColorAccent = true)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        // Info alert
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Packaging includes custom asset directories, asset statements, and offline fallback configurations.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Trigger Build Button
                        Button(
                            onClick = { isBuildInitiated = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "🚀 INITIATE PRODUCTION BUILD FLOW",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataPreviewRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    isUrl: Boolean = false,
    isColorAccent: Boolean = false,
    isIcon: Boolean = false,
    iconName: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isColorAccent) {
                val hexColor = when (value) {
                    "EMERALD" -> Color(0xFF10B981)
                    "ROYAL_PURPLE" -> Color(0xFF8B5CF6)
                    "CRIMSON" -> Color(0xFFEF4444)
                    "AMBER" -> Color(0xFFF59E0B)
                    else -> Color(0xFF3B82F6) // OCEAN_BLUE
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(hexColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            
            if (isIcon && iconName != null) {
                Icon(
                    imageVector = when (iconName) {
                        "shopping_cart" -> Icons.Default.ShoppingCart
                        "business" -> Icons.Default.Work
                        "article" -> Icons.Default.Article
                        "forum" -> Icons.Default.Forum
                        "gamepad" -> Icons.Default.SportsEsports
                        "book" -> Icons.Default.Book
                        "music_note" -> Icons.Default.MusicNote
                        else -> Icons.Default.Language
                    },
                    contentDescription = null,
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUrl) Color(0xFF4F46E5) else Color(0xFF1E293B),
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
    }
}

private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun FileChoiceCell(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFE2E8F0))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.FilePresent,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.DarkGray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.DarkGray
            )
        }
    }
}

@Composable
fun BuildStepper(
    currentProgress: Float,
    status: BuildStatus,
    activePhase: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf("QUEUED", "BUILDING", "SIGNING", "COMPLETED")
    val stepDescriptions = listOf(
        Triple("Project initialized and queued successfully", "Waiting in remote queue...", "Awaiting queue"),
        Triple("Generated code structure, compiled sources and assets", "Compiling package specifications...", "Pending compile stage"),
        Triple("Signed package binaries with development keys", "Signing generated APK...", "Pending digital signature"),
        Triple("Web wrapper package compiled and exported!", "Completing final packaging...", "Pending completion")
    )
    val activeStep = when {
        status == BuildStatus.Success -> 3
        activePhase == "READY" || activePhase == "COMPLETED" -> 3
        activePhase == "SIGNING" -> 2
        activePhase == "COMPILING" || activePhase == "GENERATING" || activePhase == "BUILDING" -> 1
        activePhase == "QUEUED" -> 0
        else -> when {
            currentProgress >= 1.0f -> 3
            currentProgress >= 0.70f -> 2
            currentProgress >= 0.15f -> 1
            else -> 0
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "BUILD PIPELINE LIFECYCLE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            steps.forEachIndexed { index, title ->
                val isCompleted = index < activeStep || (status == BuildStatus.Success && index <= 3)
                val isActive = index == activeStep && status == BuildStatus.Building
                val isFailed = index == activeStep && status == BuildStatus.Failed
                val isPending = !isCompleted && !isActive && !isFailed

                val desc = when {
                    isCompleted -> stepDescriptions[index].first
                    isActive -> stepDescriptions[index].second
                    isFailed -> "Build failed at this stage. Check terminal console logs below."
                    else -> stepDescriptions[index].third
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Circle & Line
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight()
                    ) {
                        // Circle Indicator
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> Color(0xFF10B981) // Green for complete
                                        isActive -> Color(0xFF4F46E5) // Indigo for active
                                        isFailed -> Color(0xFFEF4444) // Red for failure
                                        else -> Color(0xFFF1F5F9) // Gray for pending
                                    }
                                )
                                .border(
                                    width = if (isActive) 2.dp else 0.dp,
                                    color = if (isActive) Color(0xFFC7D2FE) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCompleted -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                isFailed -> {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Failed",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                else -> {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // Vertical connecting line
                        if (index < steps.size - 1) {
                            val isLineActive = index < activeStep
                            val isLineFailed = status == BuildStatus.Failed && index >= activeStep
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(
                                        when {
                                            isLineActive || (status == BuildStatus.Success) -> Color(0xFF10B981)
                                            isLineFailed -> Color(0xFFFEE2E2)
                                            isActive -> Color(0xFFC7D2FE)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right Column: Texts & vertical spacing
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isActive || isFailed) FontWeight.Bold else FontWeight.SemiBold,
                            color = when {
                                isCompleted -> Color(0xFF047857)
                                isActive -> Color(0xFF4F46E5)
                                isFailed -> Color(0xFFEF4444)
                                else -> Color(0xFF1E293B)
                            }
                        )
                        Text(
                            text = desc,
                            fontSize = 10.sp,
                            color = when {
                                isActive -> Color(0xFF4F46E5)
                                isFailed -> Color(0xFFEF4444)
                                else -> Color(0xFF64748B)
                            }
                        )
                        if (index < steps.size - 1) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}
