package com.example.generator

import com.example.data.WebApkProject

data class GeneratedFile(
    val name: String,
    val path: String,
    val content: String,
    val language: String
)

object CodeGenerator {

    fun generateFiles(project: WebApkProject): List<GeneratedFile> {
        return listOf(
            generateAndroidManifest(project),
            generateMainActivity(project),
            generateActivityMain(project),
            generateBuildGradle(project),
            generateColors(project),
            generateInstructions(project)
        )
    }

    private fun generateAndroidManifest(project: WebApkProject): GeneratedFile {
        val orientationAttr = when (project.orientation) {
            "PORTRAIT" -> "\n            android:screenOrientation=\"portrait\""
            "LANDSCAPE" -> "\n            android:screenOrientation=\"landscape\""
            else -> ""
        }

        val themeStyle = if (project.displayMode == "FULLSCREEN") {
            "@android:style/Theme.NoTitleBar.Fullscreen"
        } else {
            "@android:style/Theme.Material.Light.NoActionBar"
        }

        val content = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="${project.packageName}">

    <!-- WebView needs internet connection to load websites -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="${project.name}"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="$themeStyle">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout"$orientationAttr>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
        """.trimIndent()

        return GeneratedFile(
            name = "AndroidManifest.xml",
            path = "app/src/main/AndroidManifest.xml",
            content = content,
            language = "xml"
        )
    }

    private fun generateMainActivity(project: WebApkProject): GeneratedFile {
        val jsLine = if (project.enableJs) {
            "settings.javaScriptEnabled = true"
        } else {
            "settings.javaScriptEnabled = false"
        }

        val zoomLine = if (project.enableZoom) {
            """
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            """.trimIndent()
        } else {
            "settings.builtInZoomControls = false"
        }

        val domLine = if (project.domStorage) {
            "settings.domStorageEnabled = true"
        } else {
            "settings.domStorageEnabled = false"
        }

        val edgeToEdgeSetup = if (project.displayMode == "EDGE_TO_EDGE") {
            "enableEdgeToEdge()"
        } else {
            "// Standard layout display"
        }

        val colorValue = when (project.themeColor) {
            "EMERALD" -> "0xFF2E7D32"
            "ROYAL_PURPLE" -> "0xFF651FFF"
            "CRIMSON" -> "0xFFD50000"
            "AMBER" -> "0xFFFFAB00"
            else -> "0xFF0288D1" // OCEAN_BLUE
        }

        val content = """
package ${project.packageName}

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        $edgeToEdgeSetup

        // Dynamic view construction
        setContentView(createContentView())

        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progress_bar)

        setupWebView()
        setupBackNavigation()
        
        webView.loadUrl("${project.url}")
    }

    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // Return false to let WebView load URL directly instead of external browser
                val requestUrl = request?.url?.toString() ?: ""
                if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                    return false
                }
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }

        val settings = webView.settings
        $jsLine
        $zoomLine
        $domLine
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.databaseEnabled = true
        
        // Caching optimization for offline startup speed
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun createContentView(): View {
        // In clean production Android wrappers, you can define a simple RelativeLayout holding WebView and ProgressBar.
        // Or you can inflate an XML layout containing:
        // - <WebView android:id="@+id/webview" android:layout_width="match_parent" android:layout_height="match_parent"/>
        // - <ProgressBar android:id="@+id/progress_bar" style="@android:style/Widget.ProgressBar.Horizontal" .../>
        // This ensures the application starts instantaneously.
        
        val inflater = android.view.LayoutInflater.from(this)
        // Inflate your activity_main resource containing layout views
        return inflater.inflate(R.layout.activity_main, null)
    }
}
        """.trimIndent()

        return GeneratedFile(
            name = "MainActivity.kt",
            path = "app/src/main/java/${project.packageName.replace('.', '/')}/MainActivity.kt",
            content = content,
            language = "kotlin"
        )
    }

    private fun generateActivityMain(project: WebApkProject): GeneratedFile {
        val content = """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <WebView
        android:id="@+id/webview"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <ProgressBar
        android:id="@+id/progress_bar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="6dp"
        android:indeterminate="false"
        android:max="100"
        android:progressDrawable="@android:drawable/progress_horizontal"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_of="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
        """.trimIndent()
        return GeneratedFile(
            name = "activity_main.xml",
            path = "app/src/main/res/layout/activity_main.xml",
            content = content,
            language = "xml"
        )
    }

    private fun generateBuildGradle(project: WebApkProject): GeneratedFile {
        val content = """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "${project.packageName}"
    compileSdk = 34

    defaultConfig {
        applicationId = "${project.packageName}"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
        """.trimIndent()

        return GeneratedFile(
            name = "build.gradle.kts",
            path = "app/build.gradle.kts",
            content = content,
            language = "kotlin"
        )
    }

    private fun generateColors(project: WebApkProject): GeneratedFile {
        val colorHex = when (project.themeColor) {
            "EMERALD" -> "2E7D32"
            "ROYAL_PURPLE" -> "651FFF"
            "CRIMSON" -> "D50000"
            "AMBER" -> "FFAB00"
            else -> "0288D1" // OCEAN_BLUE
        }
        val content = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Brand themes matching Lust Web APK designer settings -->
    <color name="primaryColor">#$colorHex</color>
    <color name="primaryDarkColor">#66$colorHex</color>
    <color name="backgroundColor">#FFFFFF</color>
</resources>
        """.trimIndent()

        return GeneratedFile(
            name = "colors.xml",
            path = "app/src/main/res/values/colors.xml",
            content = content,
            language = "xml"
        )
    }

    private fun generateInstructions(project: WebApkProject): GeneratedFile {
        val colorName = when (project.themeColor) {
            "EMERALD" -> "Emerald Green (#2E7D32)"
            "ROYAL_PURPLE" -> "Royal Purple (#651FFF)"
            "CRIMSON" -> "Crimson Red (#D50000)"
            "AMBER" -> "Amber Gold (#FFAB00)"
            else -> "Ocean Blue (#0288D1)"
        }
        val content = """
# NATIVE WEBSITE-TO-APK WRAPPING INSTRUCTIONS
For configuration: **${project.name}**

This document describes how to use your generated files to build a highly optimized, native Android web wrapper in Android Studio.

## STEP 1: Setup Android Studio Project
1. Open Android Studio and choose **New Project** -> **Empty Views Activity** (or standard views-based activity).
2. Set **Name** to: `${project.name}`
3. Set **Package name** to: `${project.packageName}`
4. Set **Language** to: `Kotlin`
5. Set **Minimum SDK** to: `API 24` (matches your config)

## STEP 2: Copy Generated Files
Replace these file contents inside your Android Studio project with the generated code from Lust Web APK:
- Copy the contents from `AndroidManifest.xml` to your local `app/src/main/AndroidManifest.xml`
- Copy the contents from `MainActivity.kt` into `app/src/main/java/${project.packageName.replace('.', '/')}/MainActivity.kt`
- Copy the contents from `build.gradle.kts` into your local `app/build.gradle.kts`
- Copy the contents from `colors.xml` into `app/src/main/res/values/colors.xml`

## STEP 3: Setup UI layout
Create or modify `app/src/main/res/layout/activity_main.xml` to contain your WebView interface:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <WebView
        android:id="@+id/webview"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <ProgressBar
        android:id="@+id/progress_bar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="6dp"
        android:indeterminate="false"
        android:max="100"
        android:progressDrawable="@android:drawable/progress_horizontal"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

## STEP 4: Build!
1. Sync your project changes with Gradle files.
2. In the top toolbar, go to **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
3. Once completed, a popup will appear. Click **Locate** to retrieve your generated `${project.name}.apk` file!
4. Install it on your phone and load ${project.url} in full native fluid speed.

## Wrap Specifications Saved Offline:
- App Name : ${project.name}
- Package  : ${project.packageName}
- Start URL: ${project.url}
- Color    : $colorName
- App Icon : ${project.appIcon.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}
- JS State : ${if (project.enableJs) "Active (Enabled)" else "Deactivated"}
- Zooming  : ${if (project.enableZoom) "Active (Gesture Zoom Enabled)" else "Disabled"}
- DOM Store: ${if (project.domStorage) "Active" else "Disabled"}
- Display  : ${project.displayMode}
- Lock Dir : ${project.orientation}
        """.trimIndent()

        return GeneratedFile(
            name = "Wrapping_Guide.md",
            path = "Wrapping_Guide.md",
            content = content,
            language = "markdown"
        )
    }
}
