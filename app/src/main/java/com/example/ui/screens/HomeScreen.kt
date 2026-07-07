package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Download
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WebApkProject
import com.example.data.LocalStorage
import com.example.data.RecentConfig
import com.example.ui.WebApkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WebApkViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToConsole: (WebApkProject) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToMetadataSettings: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    var projectToDelete by remember { mutableStateOf<WebApkProject?>(null) }
    var projectForHistoryClear by remember { mutableStateOf<WebApkProject?>(null) }
    var showClearAllCacheDialog by remember { mutableStateOf(false) }
    val storageInfo = remember(projects, showClearAllCacheDialog) { viewModel.getStorageSizeAndStatus() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF4F46E5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Lust Web APK",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                letterSpacing = (-0.5).sp,
                                fontSize = 20.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToMetadataSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Compiler Settings",
                                tint = Color(0xFF64748B)
                            )
                        }
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help Guide",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New URL Profile",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color(0xFFF7F9FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Premium Geometric Workspace Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WORKSPACE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${projects.size} Active Configs",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                              )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your website configurations are stored locally. Ready to build into native Android packages.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dashboard Storage Indicator & History Panel
            StorageDashboardCard(
                activeCount = projects.size,
                storageSize = storageInfo.first,
                storageStatus = storageInfo.second,
                onClearHistory = { showClearAllCacheDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            ReactUrlTriggerComponent(
                context = LocalContext.current,
                viewModel = viewModel,
                onNavigateToConsole = onNavigateToConsole
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (showClearAllCacheDialog) {
                val context = androidx.compose.ui.platform.LocalContext.current
                AlertDialog(
                    onDismissRequest = { showClearAllCacheDialog = false },
                    title = { Text(text = "Clear Build History & Cached Data?", fontWeight = FontWeight.Bold) },
                    text = { Text(text = "This will release storage by clearing all locally generated APK resources, compilation logs, draft configurations, and internal cache files. Your main URL configuration profiles will remain safe.", fontSize = 14.sp) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllBuildCaches()
                                showClearAllCacheDialog = false
                                android.widget.Toast.makeText(context, "System storage optimized and build logs cleared!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        ) {
                            Text(text = "OPTIMIZE STORAGE", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearAllCacheDialog = false }) {
                            Text(text = "CANCEL", color = Color.DarkGray)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT PROJECTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "View All",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F46E5)
                )
            }

            if (projects.isEmpty()) {
                // Polished Empty State Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8EAF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    tint = Color(0xFF5C6BC0),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "No Web Apps Wrapped Yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A237E)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "LUST WEB APK lets you set up local profiles to translate websites into 100% native Android structures. Tap the floating action button to bundle your first URL.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            // Interactive card tip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF9C4)) // Yellow tint
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "All profiles inside are completely secure, private, and offline.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }
                    }
                }
            } else {
                // List of Wrapped Projects
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(projects) { project ->
                        val context = LocalContext.current
                        ProjectItemCard(
                            project = project,
                            viewModel = viewModel,
                            onBuild = { onNavigateToConsole(project) },
                            onEdit = { onNavigateToEdit(project.id) },
                            onDelete = { projectToDelete = project },
                            onClearHistory = { projectForHistoryClear = project },
                            onPreview = { onNavigateToPreview(project.id) },
                            onExportConfig = {
                                val fileUri = viewModel.exportConfigAndLogsJson(project, emptyList())
                                if (fileUri != null) {
                                    Toast.makeText(context, "Config exported successfully: ${project.name}_build_info.json", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Export failed. Please check storage permission.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                    // Bottom padding spacer
                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }
        }

        // Delete Confirmation dialog box safely offline
        projectToDelete?.let { project ->
            AlertDialog(
                onDismissRequest = { projectToDelete = null },
                title = { Text(text = "Clear Profile & Build History?", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Are you sure you want to remove the website profile for '${project.name}' and clear its cached local configurations and build logs? This action cannot be undone.", fontSize = 14.sp) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteProject(project)
                            projectToDelete = null
                        }
                    ) {
                        Text(text = "DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectToDelete = null }) {
                        Text(text = "CANCEL", color = Color.DarkGray)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Clear history confirmation dialog safely offline
        projectForHistoryClear?.let { project ->
            val context = androidx.compose.ui.platform.LocalContext.current
            AlertDialog(
                onDismissRequest = { projectForHistoryClear = null },
                title = { Text(text = "Clear Build History & Files?", fontWeight = FontWeight.Bold) },
                text = { Text(text = "This will delete the locally cached build configs, draft auto-saves, and generated ZIP/APK output files for '${project.name}' in your Downloads folder.", fontSize = 14.sp) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearBuildHistoryForProject(project.id)
                            projectForHistoryClear = null
                            android.widget.Toast.makeText(context, "Build files and draft configurations cleared for '${project.name}'!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = "CLEAR HISTORY", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectForHistoryClear = null }) {
                        Text(text = "CANCEL", color = Color.DarkGray)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun StorageDashboardCard(
    activeCount: Int,
    storageSize: String,
    storageStatus: String,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage Status",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LOCAL CACHE STORAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = storageSize,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (storageStatus) {
                                        "Optimized" -> Color(0xFFDCFCE7)
                                        "Healthy" -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFFEE2E2)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = storageStatus.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (storageStatus) {
                                    "Optimized" -> Color(0xFF15803D)
                                    "Healthy" -> Color(0xFFB45309)
                                    else -> Color(0xFFB91C1C)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Includes compiled application packages, resource trees, build metadata, and network logs generated dynamically during native packaging runs.",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$activeCount Active Profiles",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Button(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEF2F2),
                        contentColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CLEAR HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardStatsRow(
    activeCount: Int,
    storageSize: String,
    storageStatus: String,
    onClearCache: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PhoneAndroid,
            title = "Wrapped Apps",
            value = activeCount.toString(),
            color = Color(0xFF4F46E5)
        )
        StatCard(
            modifier = Modifier
                .weight(1f)
                .clickable { onClearCache() },
            icon = Icons.Default.Storage,
            title = "Cached Configs",
            value = storageSize,
            color = Color(0xFFF59E0B),
            subtitle = "TAP TO CLEAR"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.OfflinePin,
            title = "Cache Status",
            value = storageStatus,
            color = Color(0xFF10B981)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color,
    subtitle: String? = null
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun ProjectItemCard(
    project: WebApkProject,
    viewModel: WebApkViewModel,
    onBuild: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClearHistory: () -> Unit,
    onPreview: () -> Unit,
    onExportConfig: () -> Unit
) {
    val themeHexColor = when (project.themeColor) {
        "EMERALD" -> Color(0xFF10B981)
        "ROYAL_PURPLE" -> Color(0xFF8B5CF6)
        "CRIMSON" -> Color(0xFFEF4444)
        "AMBER" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6) // OCEAN_BLUE / Blue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Colored project icon avatar badge mapping their styling Choice
                val iconVector = when (project.appIcon) {
                    "shopping_cart" -> Icons.Default.ShoppingCart
                    "business" -> Icons.Default.Work
                    "article" -> Icons.Default.Article
                    "forum" -> Icons.Default.Forum
                    "gamepad" -> Icons.Default.SportsEsports
                    "book" -> Icons.Default.Book
                    "music_note" -> Icons.Default.MusicNote
                    else -> Icons.Default.Language
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeHexColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = "Project Icon",
                        tint = themeHexColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = project.url,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1
                    )
                }

                // Export Project config button
                IconButton(onClick = onExportConfig) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export Configuration JSON",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear History button
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Build History",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Profile",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spec Tag chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecTagChip(text = project.packageName, icon = Icons.Default.Layers)
                SpecTagChip(
                    text = if (project.orientation == "UNSPECIFIED") "Auto Rot" else project.orientation.lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.ScreenLockPortrait
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary control keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Config",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                val isProjectCompliant = remember(project) {
                    viewModel.validateProject(project).isEmpty()
                }

                // Build APK trigger
                Button(
                    onClick = onBuild,
                    enabled = isProjectCompliant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BUILD APK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Live Sandbox preview trigger
                Button(
                    onClick = onPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = themeHexColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TEST RUN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SpecTagChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFECEFF1))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.DarkGray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ReactUrlTriggerComponent(
    context: Context,
    viewModel: WebApkViewModel,
    onNavigateToConsole: (WebApkProject) -> Unit
) {
    // 1. Local state (React style hooks using Compose mutableStateOf)
    var inputUrl by remember { mutableStateOf("") }
    var inputAppName by remember { mutableStateOf("") }
    var inputPackageName by remember { mutableStateOf("") }
    var inputApkFileName by remember { mutableStateOf("") }
    
    // Auto-update metadata in real-time as the user types
    LaunchedEffect(inputUrl) {
        if (inputUrl.isNotBlank()) {
            val cleanHost = inputUrl
                .replace("http://", "")
                .replace("https://", "")
                .replace("www.", "")
                .split("/")[0]
                .split(".")[0]
                .replace("[^a-zA-Z0-9]".toRegex(), "")
                .replaceFirstChar { it.uppercase() }
            
            inputAppName = if (cleanHost.isNotBlank()) "$cleanHost App" else "Web App"
            inputPackageName = "com.webapk.${cleanHost.lowercase()}"
            inputApkFileName = "${cleanHost.ifBlank { "custom" }}_LustWebApk.apk"
        } else {
            inputAppName = ""
            inputPackageName = ""
            inputApkFileName = ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ReactUrlTriggerComponent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Quick Generation Hook",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // URL input field
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("Enter Website URL (React Input State)") },
                placeholder = { Text("e.g., myportfolio.com") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    focusedLabelColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // Dynamic State-Managed Preview Panel
            AnimatedVisibility(visible = inputUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "STATE-MANAGED METADATA PREVIEW (Live)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Title:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = inputAppName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "URL Source:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = inputUrl,
                            fontSize = 11.sp,
                            color = Color(0xFF4F46E5),
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Java Package:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = inputPackageName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Output APK:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = inputApkFileName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger Button
            Button(
                onClick = {
                    if (inputUrl.isNotBlank()) {
                        LocalStorage.saveRecentConfiguration(
                            context = context,
                            name = inputAppName,
                            url = inputUrl,
                            packageName = inputPackageName,
                            apkFileName = inputApkFileName
                        )
                        
                        viewModel.saveProject(
                            id = 0L,
                            name = inputAppName,
                            url = inputUrl,
                            packageName = inputPackageName,
                            orientation = "UNSPECIFIED",
                            displayMode = "STANDARD",
                            enableJs = true,
                            enableZoom = true,
                            domStorage = true,
                            themeColor = "OCEAN_BLUE",
                            appIcon = "language",
                            apkFileName = inputApkFileName,
                            onSaved = { savedProject ->
                                viewModel.selectProject(savedProject)
                                onNavigateToConsole(savedProject)
                            }
                        )
                        
                        Toast.makeText(context, "Initiating packaging flow...", Toast.LENGTH_LONG).show()
                    }
                },
                enabled = inputUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                    disabledContainerColor = Color(0xFFF1F5F9),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "INITIATE APK GENERATION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
