package tokyo.leadershouse.miskeywebview
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout

// TODO: 入力されたAPIキーが有効かを軽く確認したい
// TODO: sinceIdに対応したい、今はAPI引き出してアプリ内で判定してごまかしてる
// TODO: 自分用のlogcat保存ライブラリを作る（優先度低）

const val MISSKEY_URL      = "https://misskey.io"
const val MISSKEY_DOMAIN   = "misskey.io"
const val MISSKEY_API_URL  = "https://misskey.io/api/i/notifications"
class MainActivity : AppCompatActivity() {
    private var contenMenuId = 1001
    private var sidebarOpen = false
    private lateinit var webView: WebView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 登録済みのAPIキーを使って通知ジョブ実行
        checkApi()
        // サイドバー設定
        setSideBar()
        // 外観と細かい設定
        configureAppUI()
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

    // 登録済みのAPIキーを使って通知ジョブ実行
    private fun checkApi() {
        val sharedPreferences = getSharedPreferences("AccountInfo", Context.MODE_PRIVATE)
        val accountCount = sharedPreferences.getInt("accountCount", 0)
        if (accountCount > 1) {
            // 1番目には追加用の要素があるから1から数え上げる
            for (i in 1 until accountCount) {
                val apiKey = sharedPreferences.getString("apiKey_$i", "") ?: ""
                startBackgroundJob(apiKey)
            }
        }
    }

    // サイドバー設定
    private fun setSideBar(){
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        // サイドバーの開閉状態を監視し、WebViewに対するタッチイベントを遮断する
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerOpened(drawerView: View) { sidebarOpen = true }
            override fun onDrawerClosed(drawerView: View) { sidebarOpen = false }
        })

        // WebViewの下に重なる透明なViewのクリックイベントリスナーを設定
        val touchInterceptor = findViewById<View>(R.id.touchInterceptor)
        touchInterceptor.setOnTouchListener { _, event ->
            // サイドバーの開閉状態に合わせてwebviewへのタップを遮断
            if (sidebarOpen) { true }
            else {
                webView.dispatchTouchEvent(event)
                touchInterceptor.performClick() // クリックイベントも発生させる
            }
        }

        // サイドバーの項目をクリックしたときの処理
        val apiKeyItem = findViewById<TextView>(R.id.apiKeyItem)
        apiKeyItem.setOnClickListener {
            val intent = Intent(this, AccountListActivity::class.java)
            startActivity(intent)
        }
    }

    // 外観と細かい設定
    private fun configureAppUI() {
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        // 戻るボタン制御
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@MainActivity)
                if (webView.canGoBack()) { webView.goBack() } // WebView内のページを1つ戻す
                else {
                    builder.setMessage("アプリを終了しますか？")
                    builder.setPositiveButton("いいえ") { dialog, _ -> dialog.dismiss() }
                    builder.setNegativeButton("はい") { _, _ -> finish() }
                    builder.show()
                }
            }
        }
        this.onBackPressedDispatcher.addCallback(this, callback)
    }
    // バックグラウンドサービス実行
    private fun startBackgroundJob(apiKey: String) {
        Log.d("debug", "apiKey:$apiKey で通知取得開始")
        val componentName = ComponentName(this, NotificationJobService::class.java)
        val jobId = contenMenuId++
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

    override fun onDestroy() {
        Log.d("debug", "onDestroy")
        super.onDestroy()
        webView.destroy()
    }
}