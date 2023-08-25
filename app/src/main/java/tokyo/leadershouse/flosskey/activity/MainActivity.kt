package tokyo.leadershouse.flosskey.activity
import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import android.widget.Toast
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
import tokyo.leadershouse.flosskey.BuildConfig
import tokyo.leadershouse.flosskey.R
import tokyo.leadershouse.flosskey.handler.AppUpdate
import tokyo.leadershouse.flosskey.handler.CookieHandler
import tokyo.leadershouse.flosskey.handler.ImageDownloader
import tokyo.leadershouse.flosskey.handler.KeyStoreHelper
import tokyo.leadershouse.flosskey.handler.PermissionHandler
import tokyo.leadershouse.flosskey.service.NotificationJobService
import tokyo.leadershouse.flosskey.util.GITHUB_API_URL
import tokyo.leadershouse.flosskey.util.MISSKEY_DOMAIN
import tokyo.leadershouse.flosskey.util.RESULT_GO_DEVLOPER_URL
import tokyo.leadershouse.flosskey.util.SIDEBAR_TITLE
import tokyo.leadershouse.flosskey.util.changeInstance
import tokyo.leadershouse.flosskey.util.getFlosskeyDeveloperUrl
import tokyo.leadershouse.flosskey.util.getMisskeyInstanceUrl
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
    private fun getLatestRelease() {
        val client      = OkHttpClient()
        val accessToken = BuildConfig.token
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .header("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { Log.d("debug", "Error") }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val latestRelease = JSONObject(responseBody!!)
                    val latestVersionName = latestRelease.getString("tag_name")
                    if (BuildConfig.VERSION_NAME < latestVersionName) {
                        // 新しいバージョンが利用可能な場合、ダウンロード処理などを行う
                        runOnUiThread {
                            Log.d("debug", "New version available: $latestVersionName")
                            AppUpdate(this@MainActivity).downloadNewVersion(latestVersionName)
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
        loadCookies()
        requestPermissions()
        initWebView()
        configureActivity(webView)
    }
    private fun setDefaultInstance() {
        val sharedPreferences = getSharedPreferences("instance", Context.MODE_PRIVATE)
        MISSKEY_DOMAIN = sharedPreferences.getString("misskeyDomain", "") ?: ""
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
        versionTextView.text   = SIDEBAR_TITLE
        val drawerLayout       = findViewById<DrawerLayout>(R.id.drawerLayout)
        val accountList        = KeyStoreHelper.loadAccountInfo(this)
        val instanceNames      = accountList.map { it.instanceName }.toTypedArray()
        val openSetting        = findViewById<TextView>(R.id.openSetting)
        val reloadBrowser      = findViewById<TextView>(R.id.reloadBrowser)
        openSetting.setOnClickListener{
            startSettingsActivity.launch(Intent(this, SettingsActivity::class.java))
        }
        reloadBrowser.setOnClickListener{
            webView.loadUrl(getMisskeyInstanceUrl())
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
            webView.loadUrl(getMisskeyInstanceUrl())
        }
    }
    private fun loadCookies() { CookieHandler(this).loadCookies() }
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionHandler(this).requestPermission()
        } else { PermissionHandler(this).requestPermissionsLegacy(this) }
    }
    private fun initWebView() {
        webView = findViewById(R.id.webView)
        registerForContextMenu(webView)
        MisskeyWebViewClient(this).initializeWebView(webView)
    }
    private fun configureActivity(webView: WebView) {
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
        onBackPressedDispatcher.addCallback(this, callback)
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
    private val startSettingsActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK || result.resultCode == RESULT_GO_DEVLOPER_URL) {
                Log.d("debug","てすと")
                setSideBar()
                startBackgroundJob()
                if (result.resultCode == RESULT_GO_DEVLOPER_URL) {
                    webView.loadUrl(getFlosskeyDeveloperUrl())
                    Toast.makeText(this, "開発者のアカウントページを開きました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
