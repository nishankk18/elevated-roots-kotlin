package com.knv.elevatedroots

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        configureWebView()
        registerBackHandler()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
            if (webView.url.isNullOrBlank()) {
                webView.loadUrl(HOME_URL)
            }
        } else {
            webView.loadUrl(HOME_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.apply {
            setBackgroundColor(android.graphics.Color.parseColor("#0B1D14"))
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString + " ElevatedRootsAndroid/1.0"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url ?: return true
                return when (url.scheme?.lowercase()) {
                    "http", "https" -> false
                    else -> openExternalIntent(url)
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    showErrorPage(view)
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    showErrorPage(view)
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: android.net.http.SslError
            ) {
                handler.cancel()
                showErrorPage(view)
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                recreateWebView()
                return true
            }
        }
    }

    private fun registerBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun openExternalIntent(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun showErrorPage(target: WebView) {
        target.loadDataWithBaseURL(null, ERROR_HTML, "text/html", "utf-8", null)
    }

    private fun recreateWebView() {
        if (::webView.isInitialized) {
            cleanupWebView(webView)
        }
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        configureWebView()
        webView.loadUrl(HOME_URL)
    }

    private fun cleanupWebView(currentWebView: WebView?) {
        currentWebView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            webChromeClient = null
            clearHistory()
            clearCache(false)
            loadUrl("about:blank")
            onPause()
            removeAllViews()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) {
            webView.saveState(outState)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::webView.isInitialized && isFinishing) {
            cleanupWebView(webView)
        }
        super.onDestroy()
    }

    companion object {
        private const val HOME_URL = "https://www.elevatedrootsma.com/"
        private const val ERROR_HTML = """
            <!doctype html>
            <html>
              <body style=\"background:#0B1D14;color:#FFFFFF;font-family:sans-serif;text-align:center;padding:24px;\">
                <h2>Unable to load Elevated Roots MA</h2>
                <p>Please check your connection and try again.</p>
              </body>
            </html>
        """
    }
}
