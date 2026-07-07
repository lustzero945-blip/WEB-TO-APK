package com.example

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.WebApkViewModel
import com.example.ui.WebApkViewModelFactory
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BuildConsoleScreen
import com.example.ui.screens.ConfigEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WebApkPreviewScreen
import com.example.ui.screens.ApkMetadataScreen
import com.example.ui.theme.MyApplicationTheme
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if there is a target runtime packaged config file in assets
        val runtimeConfig = loadRuntimeConfig()

        if (runtimeConfig != null) {
            // Dual-Mode compilation run: We are launching as a compiled packaged webapp!
            setContent {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PackagedWebContainer(runtimeConfig)
                    }
                }
            }
        } else {
            // Regular Mode: We are launching as Lust URL Studio compiler PWA
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
            
            val pwaConfig = JSONObject().apply {
                put("url", "file:///android_asset/web/index.html")
                put("enableJs", true)
                put("enableZoom", false)
                put("domStorage", true)
                put("orientation", "UNSPECIFIED")
            }

            setContent {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PackagedWebContainer(pwaConfig)
                    }
                }
            }
        }
    }

    private fun loadRuntimeConfig(): JSONObject? {
        return try {
            assets.open("runtime_config.json").use { inputStream ->
                val jsonStr = inputStream.bufferedReader().use { it.readText() }
                JSONObject(jsonStr)
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun PackagedWebContainer(config: JSONObject) {
    val url = config.optString("url", "https://google.com")
    val enableJs = config.optBoolean("enableJs", true)
    val enableZoom = config.optBoolean("enableZoom", false)
    val domStorage = config.optBoolean("domStorage", true)
    val orient = config.optString("orientation", "UNSPECIFIED")
    
    val context = LocalContext.current
    LaunchedEffect(orient) {
        val activity = context as? ComponentActivity
        when (orient) {
            "PORTRAIT" -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "LANDSCAPE" -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                        allowFileAccess = true
                        allowContentAccess = true
                        builtInZoomControls = enableZoom
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    loadUrl(url)
                }
            }
        )
    }
}
