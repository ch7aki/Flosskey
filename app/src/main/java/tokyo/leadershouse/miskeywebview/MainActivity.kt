package tokyo.leadershouse.miskeywebview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale

/*
### Mainには最低限の初期化と関数呼び出しだけさせたい ###
TODO webview構築やcookie管理や通知のポーリングや画像保存周りは別クラスにさせたい
TODO ↑ガチで闇鍋状態なのでちゃんと整備してほしい
TODO misskeyのAPI見て通知を自力で取りにいく実装をする
TODO PWA出来ないブラウザ愛好者向けとかOSSだからとかいう以外のメリットも考える
*/

const val MISSKEY_URL = "https://misskey.io"
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var cookieManager: CookieManager

    companion object {
        private const val CONTEXT_MENU_ID = 1001
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        onBackPressedDispatcher.addCallback(this, backCallback)
        // TODO:権限チェック

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
            view?.loadUrl(
                "javascript:(function(){" +
                        "var imgs = document.querySelectorAll('img.pswp__img');" +
                        "for(var i=0;i<imgs.length;i++){" +
                        "imgs[i].addEventListener('contextmenu', function(e){" +
                        "e.preventDefault();" +
                        "var title = this.getAttribute('alt');" +
                        "window.android.saveImage(this.src, title);" +
                        "});" +
                        "}" +
                        "})()"
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView() {
        val javascriptInterface = JavaScriptInterface(this)
        // WebViewの初期化
        webView = findViewById(R.id.webView)

        // WebViewの設定
        registerForContextMenu(webView)
        webView.webViewClient = MyWebViewClient()
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
        webView.addJavascriptInterface(javascriptInterface, "AndroidInterface")

        // 余計な機能は無効にしておく
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

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu?.add(0, CONTEXT_MENU_ID, 0, "Download") // コンテキストメニューの項目を追加
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            CONTEXT_MENU_ID -> {
                val webViewHitTestResult = webView.hitTestResult
                if (webViewHitTestResult.type == WebView.HitTestResult.IMAGE_TYPE) {
                    val imageUrl = webViewHitTestResult.extra
                    if (imageUrl != null) {
                        saveImage(imageUrl)
                    }
                    true
                } else {
                    false
                }
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private inner class JavaScriptInterface(private val context: Context) {
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage(imageUrl: String) {
        Thread {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)

                // 画像の拡張子を取得
                val fileExtension = getFileExtensionFromUrl(imageUrl)

                // 画像を保存するためのファイルパスを生成
                val currentTime = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
                val filename = "$currentTime.$fileExtension"
                val directory = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),
                    "misskeywebview")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, filename)

                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                // 画像の保存が完了したことをユーザーに通知する
                runOnUiThread {
                    JavaScriptInterface(this).showToast("画像が保存されました")
                }

            } catch (e: IOException) {
                e.printStackTrace()
                // 画像の保存が失敗したことをユーザーに通知する
                runOnUiThread {
                    JavaScriptInterface(this).showToast("画像の保存に失敗しました")
                }
            }
        }.start()
    }

    private fun getFileExtensionFromUrl(url: String): String {
        val fileUrl = URL(url)
        val path = fileUrl.path
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return if (extension.isNotEmpty()) extension else "png"
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