package com.example.htmlappstudio

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import org.json.JSONArray

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var exportContent = ""

    private val prefs by lazy {
        getSharedPreferences("html_apps", MODE_PRIVATE)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.setBackgroundColor(0xFF080D1B.toInt())
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            loadsImagesAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        web.isVerticalScrollBarEnabled = false
        web.isHorizontalScrollBarEnabled = false

        web.webChromeClient = object : WebChromeClient() {

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("باشه") { _, _ ->
                        result?.confirm()
                    }
                    .setOnCancelListener {
                        result?.cancel()
                    }
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("بله") { _, _ ->
                        result?.confirm()
                    }
                    .setNegativeButton("خیر") { _, _ ->
                        result?.cancel()
                    }
                    .show()
                return true
            }
        }

        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(AppBridge(), "AndroidStore")

        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class AppBridge {

        @JavascriptInterface
        fun load(): String {
            return prefs.getString("apps", "[]") ?: "[]"
        }

        @JavascriptInterface
        fun save(json: String) {
            try {
                val parsed = JSONArray(json)
                prefs.edit()
                    .putString("apps", parsed.toString())
                    .apply()
            } catch (e: Exception) {
                message("خطا در ذخیره‌سازی")
            }
        }

        @JavascriptInterface
        fun message(text: String) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    text.take(100),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        @JavascriptInterface
        fun exportHtml(name: String, html: String) {
            runOnUiThread {
                exportContent = html

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/html"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        name.take(50) + ".html"
                    )
                }

                try {
                    startActivityForResult(intent, 1001)
                } catch (e: Exception) {
                    message("امکان ذخیره فایل وجود ندارد")
                }
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data

            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(exportContent.toByteArray(Charsets.UTF_8))
                    }

                    Toast.makeText(
                        this,
                        "فایل ذخیره شد ✓",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "خطا در ذخیره فایل",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        web.evaluateJavascript(
            """
            (function(){
              if(typeof closePreviewFullscreen==='function'){
                return closePreviewFullscreen();
              }
              return false;
            })();
            """.trimIndent()
        ) { result ->
            if (result != "true") {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.stopLoading()
            web.loadUrl("about:blank")
            web.removeJavascriptInterface("AndroidStore")
            web.destroy()
        }
        super.onDestroy()
    }
}
