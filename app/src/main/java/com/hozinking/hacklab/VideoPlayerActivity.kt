package com.hozinking.hacklab

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * One video, played inside the app.
 *
 * This screen holds a single-purpose YouTube embed frame: it loads only
 * the youtube-nocookie embed for the video id passed in, and it blocks
 * navigation to anything else. Every other screen in the app — home,
 * lessons, terminal — is native Kotlin; nothing else is rendered in a
 * web view. Back returns to the lesson screen, and watching never
 * touches learning progress.
 *
 * If the video cannot play (no internet, or the owner restricts embeds),
 * a plain error line is shown instead of a dead player.
 */
class VideoPlayerActivity : Activity() {

    companion object {
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_VIDEO_TITLE = "video_title"
    }

    private var webView: WebView? = null
    private var errorText: TextView? = null
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: ""
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: ""
        if (videoId.isEmpty()) {
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16))
        }
        setContentView(root)

        root.addView(Ui.label(this, "VIDEO PEMBELAJARAN").apply {
            setPadding(0, 0, 0, Ui.dp(this, 4))
        })
        root.addView(Ui.body(this, title).apply {
            setPadding(0, 0, 0, Ui.dp(this, 12))
        })
        errorText = Ui.small(this, "Video tidak bisa diputar. Periksa internet, atau pemilik video membatasi pemutaran di luar YouTube.").apply {
            visibility = View.GONE
            setPadding(0, 0, 0, Ui.dp(this, 12))
        }
        root.addView(errorText)

        val view = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url.host ?: return true
                    val allowed = host == "www.youtube.com" ||
                        host.endsWith(".youtube.com") ||
                        host.endsWith(".youtube-nocookie.com") ||
                        host.endsWith(".googlevideo.com") ||
                        host.endsWith(".ytimg.com")
                    return !allowed
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        errorText?.visibility = View.VISIBLE
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(
                    view: View,
                    callback: CustomViewCallback
                ) {
                    if (customView != null) {
                        callback.onCustomViewHidden()
                        return
                    }
                    customView = view
                    customCallback = callback
                    val content = findViewById<FrameLayout>(android.R.id.content)
                    content.addView(
                        view,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    this@apply.visibility = View.GONE
                }

                override fun onHideCustomView() {
                    hideCustomView()
                }
            }
        }
        webView = view
        root.addView(view)
        view.loadDataWithBaseURL(
            "https://www.youtube.com",
            embedHtml(videoId),
            "text/html",
            "utf-8",
            null
        )

        val backButton = Ui.secondaryButton(this, "Kembali ke langkah").apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = Ui.dp(this, 12)
            layoutParams = params
            setOnClickListener { finish() }
        }
        root.addView(backButton)
    }

    private fun hideCustomView() {
        val view = customView ?: return
        val content = findViewById<FrameLayout>(android.R.id.content)
        content.removeView(view)
        customView = null
        webView?.visibility = View.VISIBLE
        customCallback?.onCustomViewHidden()
        customCallback = null
    }

    override fun onBackPressed() {
        if (customView != null) {
            hideCustomView()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private fun embedHtml(videoId: String): String {
        return "<html><head>" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<style>html,body{height:100%;margin:0;background:#000}</style>" +
            "</head><body>" +
            "<iframe width=\"100%\" height=\"100%\" " +
            "src=\"https://www.youtube-nocookie.com/embed/" + videoId + "?rel=0&modestbranding=1\" " +
            "frameborder=\"0\" " +
            "allow=\"accelerometer; autoplay; encrypted-media; picture-in-picture\" " +
            "allowfullscreen></iframe>" +
            "</body></html>"
    }
}
