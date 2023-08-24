package tokyo.leadershouse.flosskey.activity
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import tokyo.leadershouse.flosskey.BuildConfig
import tokyo.leadershouse.flosskey.R
import tokyo.leadershouse.flosskey.util.doActivity
class SidebarTutorialActivity : AppCompatActivity() {
    private var sidebarOpen = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sidebar_tutorial)
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        controlOnBackPress(this)
        setSideBarForTutorial()
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setSideBarForTutorial() {
        val discription: TextView = findViewById(R.id.done)
        val proceedButton: Button = findViewById(R.id.proceed_button)
        proceedButton.setOnClickListener{
            val firstTutorial = getSharedPreferences("firstTutorial", Context.MODE_PRIVATE)
            val editor = firstTutorial.edit()
            editor.putBoolean("isFirstTutorial", false)
            editor.apply()
            doActivity(
                this,
                Intent(this@SidebarTutorialActivity, MainActivity::class.java)
            )
        }
        val versionTextView       = findViewById<TextView>(R.id.versionTextView)
        val appVersion            = "Flosskey Version : ${BuildConfig.VERSION_NAME}"
        versionTextView.text      = appVersion
        val drawerLayout          = findViewById<DrawerLayout>(R.id.drawerLayout)
        val sidebarListView       = findViewById<ListView>(R.id.sidebar)
        val sidebarItems          = arrayOf("APIキーの管理", "ブラウザを更新")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sidebarItems)
        sidebarListView.adapter = adapter
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerOpened(drawerView: View) {
                discription.visibility    = View.VISIBLE
                proceedButton.visibility  = View.VISIBLE
                sidebarOpen = true }
            override fun onDrawerClosed(drawerView: View) { sidebarOpen = false }
        })
    }
    private fun controlOnBackPress(activity: Activity) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val builder = AlertDialog.Builder(activity)
                builder.setMessage("アプリを終了しますか？")
                builder.setPositiveButton("いいえ") { dialog, _ -> dialog.dismiss() }
                builder.setNegativeButton("はい") { _, _ -> finish() }
                builder.show()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}
