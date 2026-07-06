package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.WebApkViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebApkPreviewScreen(
    projectId: Long,
    viewModel: WebApkViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val projectFlow = viewModel.getProjectById(projectId).collectAsState(initial = null)
    val project = projectFlow.value

    var progressState by remember { mutableStateOf(0) }
    var isPageLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Backup current activity orientation and lock to configured orientation
    val activity = context as? Activity
    DisposableEffect(project?.orientation) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        project?.let { p ->
            val targetOrientation = when (p.orientation) {
                "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            activity?.requestedOrientation = targetOrientation
        }
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Capture device back buttons inside the WebView
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4F46E5))
        }
        return
    }

    val themeHexColor = when (project.themeColor) {
        "EMERALD" -> Color(0xFF10B981)
        "ROYAL_PURPLE" -> Color(0xFF8B5CF6)
        "CRIMSON" -> Color(0xFFEF4444)
        "AMBER" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6) // OCEAN_BLUE
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mini Header explaining the live test preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = "LIVE APK SANDBOX TEST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeHexColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Running: ${project.name} (${project.url})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // WebView Loading progress indicator matching project's accent theme color choice
            AnimatedVisibility(visible = isPageLoading) {
                LinearProgressIndicator(
                    progress = { progressState / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = themeHexColor,
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            // Real WebView implementation container
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isPageLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoading = false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progressState = newProgress
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = project.enableJs
                            domStorageEnabled = project.domStorage
                            builtInZoomControls = project.enableZoom
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        
                        webViewInstance = this
                        loadUrl(project.url)
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )
        }

        // Clean Floating Close floating button to easily back out of sandbox preview
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp),
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Preview Sandbox",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
