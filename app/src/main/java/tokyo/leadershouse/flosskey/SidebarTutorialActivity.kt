package tokyo.leadershouse.flosskey

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout

class SidebarTutorialActivity : AppCompatActivity() {
    private var sidebarOpen = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sidebar_tutorial)
        supportActionBar?.hide()
        setSideBarForTutorial()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setSideBarForTutorial() {
        val discription: TextView = findViewById(R.id.done)
        val proceedButton: Button = findViewById(R.id.proceed_button)
        proceedButton.setOnClickListener{
            val intent = Intent(this@SidebarTutorialActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
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
}