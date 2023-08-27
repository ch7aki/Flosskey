package tokyo.leadershouse.flosskey.activity
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import tokyo.leadershouse.flosskey.R
import tokyo.leadershouse.flosskey.handler.KeyStoreHelper
import tokyo.leadershouse.flosskey.util.MISSKEY_DOMAIN
import tokyo.leadershouse.flosskey.util.changeInstance
import tokyo.leadershouse.flosskey.util.doActivity
class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        controlOnBackPress(this)
        val instance                 = getSharedPreferences("instance", Context.MODE_PRIVATE)
        MISSKEY_DOMAIN               = instance.getString("misskeyDomain", "") ?: ""
        val firstTutorial            = getSharedPreferences("firstTutorial", Context.MODE_PRIVATE)
        val isFirstTutorial: Boolean = firstTutorial.getBoolean("isFirstTutorial", true)
        val accountList              = KeyStoreHelper.loadAccountInfo(this)
        if (MISSKEY_DOMAIN.isEmpty()) {
            setContentView(R.layout.activity_startup)
            val instanceNameEditText: EditText = findViewById(R.id.instance_name_edittext)
            val proceedButton: Button = findViewById(R.id.proceed_button)
            proceedButton.isEnabled   = false
            instanceNameEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    proceedButton.isEnabled = !s.isNullOrEmpty()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            proceedButton.setOnClickListener {
                val instanceName = instanceNameEditText.text.toString()
                changeInstance(instance,instanceName)
                doActivity(this, Intent(this@StartupActivity, SidebarTutorialActivity::class.java))
            }
        }
        // アカウントリストを持っていない(≒サイドバーを開いことがないかもしれない)+チュートリアルしたことがなければ
        else if (accountList.isEmpty() && isFirstTutorial) {
            doActivity(this, Intent(this@StartupActivity, SidebarTutorialActivity::class.java))
            firstTutorial.edit().putBoolean("firstTutorial", false).apply()
        }
        // それ以外はmisskeyを開く
        else { doActivity(this, Intent(this@StartupActivity, MainActivity::class.java)) }
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
