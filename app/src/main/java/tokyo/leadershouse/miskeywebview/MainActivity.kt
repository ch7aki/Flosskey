package tokyo.leadershouse.miskeywebview
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

const val MISSKEY_URL      = "https://misskey.io"
const val MISSKEY_DOMAIN   = "misskey.io"
const val MISSKEY_API_URL  = "https://misskey.io/api/i/notifications"
class MainActivity : AppCompatActivity(), ApiKeyInputDialog.ApiKeyListener {
    private val contenMenuId = 1001
    private var apiKey: String? = null
    private val LOG_FILE_NAME = "logcat.txt"
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // APIキーチェック
        if (!checkApi()){ showApiKeyInputDialog() }
        else { startBackgroundJob() }
        // 外観変更
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        // 戻るボタン制御
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    if (webView.canGoBack()) {
                        webView.goBack() // WebView内のページを1つ戻す
                    } else {
                        builder.setMessage("アプリを終了しますか？")
                        builder.setPositiveButton("いいえ") { dialog, _ -> dialog.dismiss() }
                        builder.setNegativeButton("はい") { _, _ -> finish() }
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
                else { false }
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // ダイアログからのコールバック
    override fun onApiKeyEntered(apiKey: String) {
        saveApiKey(apiKey)
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
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        apiKey = sharedPreferences.getString("api_key", "") ?: ""
        return apiKey!!.isNotEmpty()
    }

    // バックグラウンドサービス実行
    private fun startBackgroundJob() {
        Log.d("debug", "apiKey:$apiKey で通知取得開始")
        val componentName = ComponentName(this, NotificationJobService::class.java)
        val jobId = 1001

        val jobInfoBuilder = JobInfo.Builder(jobId, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(0) // おそらくAndroid13は最小間隔が10分

        val extras = PersistableBundle()
        extras.putString("apiKey", apiKey)

        jobInfoBuilder.setExtras(extras)

        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobInfo = jobInfoBuilder.build()
        jobScheduler.schedule(jobInfo)
    }

    private fun getLogFilePath() {
        // Android 10以降では外部ストレージの代わりにアプリ専用ディレクトリを使用する
        val logDir = getExternalFilesDir(null)
        logDir?.let {
            if (!it.exists()) {
                it.mkdir()
            }
            val logFile = File(logDir, LOG_FILE_NAME)
            logFile.absolutePath
        } ?: ""
    }

    override fun onDestroy() {
        Log.d("debug", "onDestroy")
        super.onDestroy()
        webView.destroy()
    }
}