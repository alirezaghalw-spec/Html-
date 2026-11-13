package com.example.htmlappstudio

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val prefs by lazy { getSharedPreferences("html_apps", MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(0xFF080D1B.toInt())
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = false
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(AppBridge(), "AndroidStore")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class AppBridge {
        @JavascriptInterface
        fun load(): String = prefs.getString("apps", "[]") ?: "[]"

        @JavascriptInterface
        fun save(json: String) {
            try {
                val parsed = JSONArray(json)
                prefs.edit().putString("apps", parsed.toString()).apply()
            } catch (_: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "ذخیره‌سازی ناموفق بود", Toast.LENGTH_SHORT).show() }
            }
        }

        @JavascriptInterface
        fun message(text: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, text.take(100), Toast.LENGTH_SHORT).show() }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
