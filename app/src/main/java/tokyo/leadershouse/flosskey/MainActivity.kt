package tokyo.leadershouse.flosskey

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import tokyo.leadershouse.flosskey.handler.AppUpdate
import tokyo.leadershouse.flosskey.handler.CookieHandler
import tokyo.leadershouse.flosskey.handler.ImageDownloader
import tokyo.leadershouse.flosskey.handler.KeyStoreHelper
import tokyo.leadershouse.flosskey.handler.PermissionHandler
import tokyo.leadershouse.flosskey.service.NotificationJobService
import tokyo.leadershouse.flosskey.ui.AccountListActivity
import tokyo.leadershouse.flosskey.util.DEVELOPER_MISSKEY_URL
import tokyo.leadershouse.flosskey.util.GITHUB_URL
import tokyo.leadershouse.flosskey.util.LICENSE_URL
import tokyo.leadershouse.flosskey.util.MISSKEY_DOMAIN
import tokyo.leadershouse.flosskey.util.changeInstance
import tokyo.leadershouse.flosskey.util.getMisskeyUrlData
import tokyo.leadershouse.flosskey.webview.MisskeyWebViewClient

class MainActivity : AppCompatActivity() {
    private val contentViewId = 1001
    private var sidebarOpen   = false
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeApp()
    }

    private fun getAccessToken(): String? {
        return BuildConfig.token
    }

    private fun getLatestRelease() {
        val client      = OkHttpClient()
        val accessToken = getAccessToken()
        val owner       = "ch1ak1STR"
        val repo        = "Flosskey"

        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/latest")
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("debug", "Error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val latestRelease = JSONObject(responseBody!!)
                    val latestVersionName = latestRelease.getString("tag_name")

                    if (BuildConfig.VERSION_NAME < latestVersionName) {
                        // 新しいバージョンが利用可能な場合、ダウンロード処理などを行う
                        runOnUiThread {
                            Log.d("debug", "New version available: $latestVersionName")
                            AppUpdate(this@MainActivity,latestVersionName).downloadAndInstallNewVersion()
                        }
                    } else { runOnUiThread { Log.d("debug", "App is up to date") } }
                }
            }
        })
    }

    private fun initializeApp() {
        getLatestRelease()
        setDefaultInstance()
        startBackgroundJob()
        setSideBar()
        configureAppUI()
        loadCookies()
        requestPermissions()
        initWebView()
    }

    private fun setDefaultInstance() {
        val sharedPreferences = getSharedPreferences("instance", Context.MODE_PRIVATE)
        MISSKEY_DOMAIN = sharedPreferences.getString("misskeyDomain", "misskey.io") ?: ""
        Log.d("debug", "MISSKEY_DOMAIN = $MISSKEY_DOMAIN")
    }

    private fun startBackgroundJob() {
        val accountList  = KeyStoreHelper.loadAccountInfo(this)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        // 既存のjobは全て止めてから設定する
        jobScheduler.cancelAll()
        if (accountList.isNotEmpty()) {
            for (element in accountList) {
                val instanceName = element.instanceName
                val apiKey       = element.apiKey
                val jobId        = element.jobId
                if (apiKey.isNotEmpty() && instanceName.isNotEmpty()) {
                    Log.d("debug", "apiKey:${apiKey}で通知取得開始")
                    val componentName = ComponentName(this, NotificationJobService::class.java)
                    val jobInfoBuilder = JobInfo.Builder(jobId, componentName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(15 * 60 * 1000)
                    val extras = PersistableBundle()
                    extras.putString("instanceName", instanceName)
                    extras.putString("apiKey", apiKey)
                    extras.putInt("jobId", jobId)
                    jobInfoBuilder.setExtras(extras)
                    val jobInfo = jobInfoBuilder.build()
                    jobScheduler.schedule(jobInfo)
                }
            }
        }
    }

    private fun setSideBar() {
        val versionTextView    = findViewById<TextView>(R.id.versionTextView)
        val appVersion         = "Flosskey Version : ${BuildConfig.VERSION_NAME}"
        versionTextView.text   = appVersion
        val drawerLayout       = findViewById<DrawerLayout>(R.id.drawerLayout)
        val sidebarListView    = findViewById<ListView>(R.id.sidebar)
        val sidebarDevListView = findViewById<ListView>(R.id.devListView)
        val accountList        = KeyStoreHelper.loadAccountInfo(this)
        val instanceNames      = accountList.map { it.instanceName }.toTypedArray()
        val sidebarItems       = arrayOf(
            "APIキーの管理",
            "ブラウザを更新"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sidebarItems)
        sidebarListView.adapter = adapter
        sidebarListView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    val intent = Intent(this, AccountListActivity::class.java)
                    startAccountListActivity.launch(intent)
                }
                1 -> webView.loadUrl(getMisskeyUrlData("URL", ""))
            }
        }
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerOpened(drawerView: View) { sidebarOpen = true }
            override fun onDrawerClosed(drawerView: View) { sidebarOpen = false }
        })

        val touchInterceptor = findViewById<View>(R.id.touchInterceptor)
        touchInterceptor.setOnTouchListener { _, event ->
            if (sidebarOpen) { true }
            else {
                webView.dispatchTouchEvent(event)
                touchInterceptor.performClick()
            }
        }
        val accountListView       = findViewById<ListView>(R.id.accountListView)
        val adapter2              = ArrayAdapter(this, android.R.layout.simple_list_item_1, instanceNames)
        accountListView.adapter   = adapter2
        accountListView.setOnItemClickListener { _, _, position, _ ->
            val instanceName      = instanceNames[position]
            val sharedPreferences = getSharedPreferences("instance", Context.MODE_PRIVATE)
            changeInstance(sharedPreferences, instanceName)
            webView.loadUrl(getMisskeyUrlData("URL", getMisskeyUrlData("URL",instanceName)))
        }
        val sidebarDevItems = arrayOf(
            "ライセンス",
            "開発者",
            "ソースコード",
            "寄付をする(準備中)"
        )
        val adapter3 = ArrayAdapter(this, android.R.layout.simple_list_item_1, sidebarDevItems )
        sidebarDevListView.adapter = adapter3
        sidebarDevListView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LICENSE_URL)))
                1 -> webView.loadUrl(DEVELOPER_MISSKEY_URL)
                2 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
            }
        }
    }

    private val startAccountListActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                setSideBar()
                startBackgroundJob()
            }
        }

    private fun configureAppUI() {
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(this@MainActivity)
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    builder.setMessage("アプリを終了しますか？")
                    builder.setPositiveButton("いいえ") { dialog, _ -> dialog.dismiss() }
                    builder.setNegativeButton("はい") { _, _ -> finish() }
                    builder.show()
                }
            }
        }
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun loadCookies() { CookieHandler(this).loadCookies() }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionHandler(this).requestPermission()
        }
        else { PermissionHandler(this).requestPermissionsLegacy(this) }
    }

    private fun initWebView() {
        webView = findViewById(R.id.webView)
        registerForContextMenu(webView)
        MisskeyWebViewClient(this).initializeWebView(webView)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webView = v as WebView
        val hitTestResult = webView.hitTestResult
        if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE) {
            menu?.add(0, contentViewId, 0, "Download")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            contentViewId -> {
                val webViewHitTestResult = webView.hitTestResult
                val imageUrl = webViewHitTestResult.extra
                if (imageUrl != null) {
                    ImageDownloader(this).saveImage(imageUrl)
                    true
                } else { false }
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
