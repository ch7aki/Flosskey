package tokyo.leadershouse.miskeywebview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/*
### Mainには最低限の初期化と関数呼び出しだけさせたい ###
TODO webview構築やcookie管理や通知のポーリングは別クラスにさせたい
TODO misskeyのAPI見て通知を自力で取りにいく実装をする
TODO PWA出来ないブラウザ愛好者向けとかOSSだからとかいう以外のメリットも考える
*/

const val MISSKEY_URL = "https://misskey.io"
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var cookieManager: CookieManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        onBackPressedDispatcher.addCallback(this, backCallback)
        // webview初期化
        initializeWebView()
        // Cookie復元
        loadCookies()
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
                    startActivity(intent)
                }
                return true
            }
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            // ページ読み込み完了後にCookieを保存する
            saveCookies()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView() {
        // WebViewの初期化
        webView = findViewById(R.id.webView)

        // WebViewの設定
        webView.webViewClient = MyWebViewClient()
/*        webView.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, " +
                    "like Gecko) Chrome/92.0.4515.107 Safari/537.36"*/
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        limitAccesToOuterDomain(webView)
        webView.loadUrl(MISSKEY_URL)
        // これつけないとmisskeyアクセスできない
        webView.settings.domStorageEnabled = true

        //Disable some WebView features
        webView.settings.allowContentAccess = false
        webView.settings.allowFileAccess = false
        webView.settings.builtInZoomControls = false
        webView.settings.databaseEnabled = false
        webView.settings.displayZoomControls = false
        webView.settings.setGeolocationEnabled(false)

        // CookieManagerの設定
        cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
    }

    private fun loadCookies() {
        val sharedPreferences = getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val savedCookies = sharedPreferences.getString("cookies", null)

        savedCookies?.let {
            // 保存したいドメインを指定する
            cookieManager.setCookie(MISSKEY_URL, it)
        }
    }

    private fun saveCookies() {
        val cookies = cookieManager.getCookie(MISSKEY_URL)
        val sharedPreferences = getSharedPreferences("Cookies", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("cookies", cookies)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
        }
    }
}