package tokyo.leadershouse.flosskey

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import tokyo.leadershouse.flosskey.handler.KeyStoreHelper
import tokyo.leadershouse.flosskey.util.MISSKEY_DOMAIN
import tokyo.leadershouse.flosskey.util.changeInstance

class StartupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val instance = getSharedPreferences("instance", Context.MODE_PRIVATE)
        MISSKEY_DOMAIN = instance.getString("misskeyDomain", "") ?: ""
        val firstTutorial = getSharedPreferences("firstTutorial", Context.MODE_PRIVATE)
        val isFirstTutorial: Boolean = firstTutorial.getBoolean("isFirstTutorial", true)
        val accountList  = KeyStoreHelper.loadAccountInfo(this)

        if (MISSKEY_DOMAIN.isEmpty()) {
            setContentView(R.layout.activity_startup)
            val instanceNameEditText: EditText = findViewById(R.id.instance_name_edittext)
            val proceedButton: Button = findViewById(R.id.proceed_button)
            proceedButton.isEnabled = false

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
                doTutorial()
            }
        }
        // アカウントリストを持っていない(≒サイドバーを開いことがないかもしれない)+チュートリアルしたことがなければ
        else if (accountList.isEmpty() && isFirstTutorial) {
            doTutorial()
            firstTutorial.edit().putBoolean("firstTutorial", false).apply()
        }
        // それ以外はmisskeyを開く
        else { doMainActivity() }

    }
    private fun doMainActivity() {
        val intent = Intent(this@StartupActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun doTutorial() {
        val intent = Intent(this@StartupActivity, SidebarTutorialActivity::class.java)
        startActivity(intent)
        finish()
    }
}
