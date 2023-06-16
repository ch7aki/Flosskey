package tokyo.leadershouse.miskeywebview

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/*
TODO misskeyのAPI見て通知を自力で取りにいく実装をする
TODO PWA出来ないブラウザ愛好者向けとかOSSだからとかいう以外のメリットも考える
TODO ApiKeyInputDialogの実装はOK 後はMAINでAPIKEYをハンドリングする
TODO ビルドしてAPI持ててるか確認する。その後通知とかかな...
*/

const val MISSKEY_URL = "https://misskey.io"
class MainActivity : AppCompatActivity(), ApiKeyInputDialog.ApiKeyListener {
    private val contenMenuId = 1001
    private lateinit var apiKey: String
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // APIキーチェック
        //if (!checkApi()){ showApiKeyInputDialog() }
        // 外観変更
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        // 戻るボタン無効化
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                    // バックボタンが押された時の処理をここに記述します
                    val builder = AlertDialog.Builder(this@MainActivity)
                    if (webView.canGoBack()) {
                        webView.goBack() // WebView内のページを1つ戻す
                    } else {
                        builder.setMessage("アプリを終了しますか？")
                        builder.setNegativeButton("いいえ") { dialog, _ ->
                            dialog.dismiss()
                        }
                        builder.setPositiveButton("はい") { _, _ ->
                            finish()
                        }
                        builder.show()
                    }
                }
            }
        this.onBackPressedDispatcher.addCallback(this, callback)
        // Cookie
        CookieHandler(this).loadCookies()
        // 権限処理
        val permissionHandler = PermissionHandler(this)
        permissionHandler.requestPermission()
        // webview初期化
        webView = findViewById(R.id.webView)
        registerForContextMenu(webView)
        MisskeyWebViewClient(this).initializeWebView(webView)
    }

    // 長押しメニュー作成処理
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

    // 長押しメニュー押下処理
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

    // ダイアログからのコールバック
    override fun onApiKeyEntered(apiKey: String) {
        // APIキーを保存し、必要な処理を実行する
        saveApiKey(apiKey)
        // ...
    }

    // ダイアログ表示
    private fun showApiKeyInputDialog() {
        val apiKeyInputDialog = ApiKeyInputDialog(this)
        apiKeyInputDialog.setApiKeyListener(this) // リスナーをセット
        apiKeyInputDialog.show()
    }

    // API保存
    private fun saveApiKey(apiKey: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("api_key", apiKey)
        editor.apply()
    }

    private fun checkApi() : Boolean{
        apiKey = sharedPreferences.getString("api_key", "") ?: ""
        return apiKey.isNotEmpty()
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