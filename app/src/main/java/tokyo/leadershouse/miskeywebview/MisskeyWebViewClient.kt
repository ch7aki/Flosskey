package tokyo.leadershouse.miskeywebview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MisskeyWebViewClient(private val context: Context) : WebViewClient(){

    private val script = """
            (function() {
                var imgs = document.querySelectorAll('.pswp__img');
                for(var i=0; i<imgs.length; i++) {
                    imgs[i].addEventListener('contextmenu', function(e) {
                        e.preventDefault();
                        var title = this.getAttribute('alt');
                        window.android.saveImage(this.src, title);
                    });
                }
            })();
        """

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // ページ読み込み完了後にCookieを保存する
        CookieHandler(context).saveCookies()
    }

    private fun limitAccesToOuterDomain(webView: WebView) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (!url.contains("misskey.io")) {
                    // それ以外のドメインの場合、外部ブラウザを起動してURLを開く処理を記述します
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
                return true
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initializeWebView(webView: WebView) {
        // WebViewの設定
        webView.webViewClient = MisskeyWebViewClient(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        limitAccesToOuterDomain(webView)
        webView.loadUrl(MISSKEY_URL)
        // これつけないとmisskeyアクセスできない
        webView.settings.domStorageEnabled = true

        // 画像保存対応
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.webChromeClient = WebChromeClient()
        script.trimIndent()
        webView.evaluateJavascript(script,null)

        // 余計な機能は無効にしておく
        webView.settings.allowContentAccess = false
        webView.settings.allowFileAccess = false
        webView.settings.builtInZoomControls = false
        webView.settings.databaseEnabled = false
        webView.settings.displayZoomControls = false
        webView.settings.setGeolocationEnabled(false)

        // CookieManagerの設定
        CookieHandler(context).manageCookie()
    }

}