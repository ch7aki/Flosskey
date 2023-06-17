package tokyo.leadershouse.miskeywebview
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MisskeyWebViewClient(private val context: AppCompatActivity) : WebViewClient() {
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

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
            @SuppressLint("QueryPermissionsNeeded")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (!url.contains(MISSKEY_DOMAIN)) {
                    // それ以外のドメインの場合、外部ブラウザを起動
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
                return true
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initializeWebView(webView: WebView) {
        launcher = context.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                onActivityResult(data)
            }
        }
        // WebViewの設定
        webView.webViewClient = MisskeyWebViewClient(context)
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webView.settings.javaScriptEnabled   = true
        webView.settings.domStorageEnabled   = true
        limitAccesToOuterDomain(webView)
        webView.loadUrl(MISSKEY_URL)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
                fileUploadCallback = filePathCallback
                openFilePicker()
                return true
            }
        }

        script.trimIndent()
        webView.evaluateJavascript(script,null)

        // 余計な機能は無効にしておく
        webView.settings.allowContentAccess  = false
        webView.settings.allowFileAccess     = false
        webView.settings.builtInZoomControls = false
        webView.settings.databaseEnabled     = false
        webView.settings.displayZoomControls = false
        webView.settings.setGeolocationEnabled(false)

        // CookieManagerの設定
        CookieHandler(context).manageCookie()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        launcher.launch(intent)
    }

    private fun onActivityResult(data: Intent?) {
            data?.let { intent ->
                val result = if (intent.data != null) {
                    arrayOf(intent.data!!)
                } else {
                    null
                }
                fileUploadCallback?.onReceiveValue(result)
                fileUploadCallback = null
            }
    }
}