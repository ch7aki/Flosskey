package tokyo.leadershouse.miskeywebview

import android.graphics.Color
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/*
TODO misskeyのAPI見て通知を自力で取りにいく実装をする
TODO PWA出来ないブラウザ愛好者向けとかOSSだからとかいう以外のメリットも考える
*/

const val MISSKEY_URL = "https://misskey.io"
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val contenMenuId = 1001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 外観変更
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        // 戻るボタン無効化
        onBackPressedDispatcher.addCallback(this, backCallback)
        // Cookie復元
        CookieHandler(this).loadCookies()
        // 権限処理
        val permissionHandler = PermissionHandler(this)
        permissionHandler.requestPermission()
        // webview初期化
        webView = findViewById(R.id.webView)
        registerForContextMenu(webView)
        MisskeyWebViewClient(this).initializeWebView(webView)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?) {
            super.onCreateContextMenu(menu, v, menuInfo)
            val webView = v as WebView
            val hitTestResult = webView.hitTestResult
            // 画像だけ長押し時の挙動を変える
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE) {
                menu?.add(0, contenMenuId, 0, "Download") // コンテキストメニューの項目を追加
            }
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            contenMenuId -> {
                val webViewHitTestResult = webView.hitTestResult
                val imageUrl = webViewHitTestResult.extra
                if (imageUrl != null) {
                    ImageDownloader(this).saveImage(imageUrl)
                    true
                }
                else {
                    false
                }
            }
            else -> super.onContextItemSelected(item)
        }
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