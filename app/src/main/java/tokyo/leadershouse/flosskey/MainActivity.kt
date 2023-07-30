package tokyo.leadershouse.flosskey
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout

// TODO: 入力されたAPIキーが有効かを軽く確認したい
// TODO: sinceIdに対応したい、今はAPI引き出してアプリ内で判定してごまかしてる

class MainActivity : AppCompatActivity() {
    private var contenMenuId = 1001
    private var sidebarOpen = false
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // デフォルトインスタンス設定
        setDefaultInstance()
        // バックグラウンド設定
        startBackgroundJob()
        // サイドバー設定
        setSideBar()
        // 外観と細かい設定
        configureAppUI()
        // Cookie
        CookieHandler(this).loadCookies()
        // 権限処理
        PermissionHandler(this).requestPermission()
        // webview初期化
        webView = findViewById(R.id.webView)
        registerForContextMenu(webView)
        MisskeyWebViewClient(this).initializeWebView(webView)
    }

    private fun setDefaultInstance() {
        // デフォルトインスタンス設定
        val sharedPreferences = getSharedPreferences("instance", Context.MODE_PRIVATE)
        MISSKEY_DOMAIN = sharedPreferences.getString("misskeyDomain", "misskey.io") ?:"misskey.io"
        Log.d("debug","MISSKEY_DOMAIN = $MISSKEY_DOMAIN")
    }

    // バックグラウンドサービス実行
    private fun startBackgroundJob() {
        val accountList = KeyStoreHelper.loadAccountInfo(this)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (accountList.isNotEmpty()) {
            // 1番目には追加用の要素があるから1から数え上げる
            for (element in accountList) {
                val instanceName = element.instanceName
                val apiKey       = element.apiKey
                val jobId        = element.jobId
                // 同じJobId起動を制御
                val existingJob = jobScheduler.getPendingJob(jobId)
                if (existingJob != null) {
                    Log.d("debug", "$jobId:同一JobIdがスケジュールされているのでスキップします")
                    continue
                }
                Log.d("debug", "apiKey:${apiKey}で通知取得開始")
                val componentName = ComponentName(this, NotificationJobService::class.java)
                val jobInfoBuilder = JobInfo.Builder(jobId, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(0)
                val extras = PersistableBundle()
                extras.putString("instanceName", instanceName)
                extras.putString("apiKey", apiKey)
                extras.putInt("jobId", jobId)
                jobInfoBuilder.setExtras(extras)
                val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                val jobInfo = jobInfoBuilder.build()
                jobScheduler.schedule(jobInfo)
            }
        }
    }

    // サイドバー設定
    private fun setSideBar() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val sidebarListView = findViewById<ListView>(R.id.sidebar)
        val sidebarItems = arrayOf(
            "APIキーの管理",
            "WebViewを更新",
            "ch1ak1@misskey.io",
            "ソースコード"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sidebarItems)
        sidebarListView.adapter = adapter
        sidebarListView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> { startActivity(Intent(this, AccountListActivity::class.java)) }
                1 -> { webView.loadUrl(getMisskeyUrlData("URL","")) }
                2 -> { webView.loadUrl(DEVELOPER_MISSKEY_URL ) }
                3 -> { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL ))) }
            }
        }
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
                menu?.add(0, contenMenuId, 0, "Download")
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
        super.onDestroy()
        webView.destroy()
    }
}