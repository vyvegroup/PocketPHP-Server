package com.pocketphp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(port: Int, tunnelUrl: String?) {
    val context = LocalContext.current
    var url by remember(port, tunnelUrl) {
        mutableStateOf(tunnelUrl ?: "http://localhost:$port")
    }
    var urlInput by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("") }
    var webView: WebView? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // URL Bar
        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { webView?.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                IconButton(
                    onClick = { webView?.goForward() },
                    enabled = canGoForward
                ) {
                    Icon(Icons.Default.ArrowForward, "Forward")
                }
                IconButton(
                    onClick = { webView?.reload() }
                ) {
                    Icon(Icons.Default.Refresh, "Reload")
                }
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    singleLine = true,
                    placeholder = { Text("Enter URL") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        val newUrl = if (urlInput.startsWith("http://") || urlInput.startsWith("https://"))
                            urlInput else "http://$urlInput"
                        url = newUrl
                        webView?.loadUrl(newUrl)
                    }),
                    shape = MaterialTheme.shapes.large,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, urlStr, favicon)
                            isLoading = true
                            urlInput = urlStr ?: ""
                        }

                        override fun onPageFinished(view: WebView?, urlStr: String?) {
                            super.onPageFinished(view, urlStr)
                            isLoading = false
                            pageTitle = view?.title ?: ""
                            canGoBack = view?.canGoBack() ?: false
                            canGoForward = view?.canGoForward() ?: false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            // Keep all navigation in-app
                            return false
                        }
                    }
                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
