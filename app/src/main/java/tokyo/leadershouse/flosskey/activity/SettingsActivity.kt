package tokyo.leadershouse.flosskey.activity
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import tokyo.leadershouse.flosskey.R
import tokyo.leadershouse.flosskey.listview.AccountListActivity
import tokyo.leadershouse.flosskey.util.GITHUB_URL
import tokyo.leadershouse.flosskey.util.LICENSE_URL
import tokyo.leadershouse.flosskey.util.QA_URL
import tokyo.leadershouse.flosskey.util.RESULT_GO_DEVLOPER_URL
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        val listView           = findViewById<ListView>(R.id.listView)
        listView.divider       = null
        listView.dividerHeight = 0
        val items = arrayOf(
            "よくあるQ&A",
            "インスタンス/API登録",
            "ライセンス",
            "開発者",
            "ソースコード",
            "既知の不具合",
            "寄付をする(準備中)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(QA_URL)))
                1 -> startAccountListActivity.launch(Intent(this, AccountListActivity::class.java))
                2 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LICENSE_URL))) // Q&A
                3 -> {
                    setResult(RESULT_GO_DEVLOPER_URL, intent)
                    finish()
                }
                4 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                5 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) // issue
            }
        }
    }
    private val startAccountListActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "インスタンス/APIを更新しました。", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK, intent)
            }
        }
}
