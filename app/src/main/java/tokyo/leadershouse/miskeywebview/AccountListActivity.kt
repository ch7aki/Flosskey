package tokyo.leadershouse.miskeywebview

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

data class AccountInfo(var accountName: String, var apiKey: String)

class AccountListActivity : AppCompatActivity() {

    private lateinit var accountListView: ListView
    private val accountList = mutableListOf<AccountInfo>()
    private lateinit var accountAdapter: AccountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        Log.d("debug","AccountListActivity Oncreate")
        // accountListViewを初期化
        accountListView = findViewById(R.id.accountListView)

        accountList.add(AccountInfo(
            "APIキー追加",
            "※APIキーは通知取得に使います！"))
        // SharedPreferencesからアカウント情報を取得してリストに追加
        val sharedPreferences = getSharedPreferences("AccountInfo", Context.MODE_PRIVATE)
        val accountCount = sharedPreferences.getInt("accountCount", 0)
        Log.d("debug","accountCount = $accountCount")
        if (accountCount > 1) {
            // 1番目には追加用の要素があるから1から数え上げる
            for (i in 1 until accountCount) {
                val accountName = sharedPreferences.getString("accountName_$i", "") ?: ""
                Log.d("debug",accountName)
                val apiKey = sharedPreferences.getString("apiKey_$i", "") ?: ""
                Log.d("debug",apiKey)
                accountList.add(AccountInfo(accountName, apiKey))
            }
        }
        // アダプターをセット
        accountAdapter = AccountAdapter(this, accountList)
        accountListView.adapter = accountAdapter

        accountListView.setOnItemClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]

            if (accountInfo.accountName == "APIキー追加") {
                showAddAccountDialog()
            } else {
                // 通常の処理（アカウント情報を表示）を行う
            }
        }

        // ロングタップで情報を更新
        accountListView.setOnItemLongClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]

            if (accountInfo.accountName != "APIキー追加") {
                showUpdateAccountDialog(accountInfo)
            }

            true
        }
    }

    private fun showUpdateAccountDialog(accountInfo: AccountInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null)
        val accountEditText = dialogView.findViewById<EditText>(R.id.accountEditText)
        val apiKeyEditText = dialogView.findViewById<EditText>(R.id.apiKeyEditText)

        // ダイアログに既存のアカウント情報をセット
        accountEditText.setText(accountInfo.accountName)
        apiKeyEditText.setText(accountInfo.apiKey)

        AlertDialog.Builder(this)
            .setTitle("アカウントとAPIキーを更新")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                // 更新ボタンが押された場合の処理
                val updatedAccount = accountEditText.text.toString()
                val updatedApiKey = apiKeyEditText.text.toString()

                // リストビューのアイテムを更新
                accountInfo.accountName = updatedAccount
                accountInfo.apiKey = updatedApiKey
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // 「アカウントとAPIキーを登録」のダイアログを表示する関数
    private fun showAddAccountDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_account, null)
        dialogBuilder.setView(dialogView)

        val accountEditText = dialogView.findViewById<EditText>(R.id.accountEditText)
        val apiKeyEditText = dialogView.findViewById<EditText>(R.id.apiKeyEditText)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            // OKボタンが押されたときの処理
            val account = accountEditText.text.toString()
            val apiKey = apiKeyEditText.text.toString()
            // 入力されたアカウントとAPIキーをリストに追加
            accountList.add(AccountInfo(account, apiKey))
            // リストビューを更新
            (accountListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()

            // 新しいアカウント情報を保存
            saveAccountInfo()
        }
        dialogBuilder.setNegativeButton("キャンセル", null)
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun saveAccountInfo() {
        val sharedPreferences = getSharedPreferences("AccountInfo", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.putInt("accountCount", accountList.size)
        Log.d("debug", "accountList.size = " + accountList.size.toString())

        for (i in accountList.indices) {
            editor.putString("accountName_$i", accountList[i].accountName)
            Log.d("debug",accountList[i].accountName)
            editor.putString("apiKey_$i", accountList[i].apiKey)
            Log.d("debug",accountList[i].apiKey)
        }
        editor.apply()
    }
    override fun onDestroy() {
        saveAccountInfo()
        super.onDestroy()
    }
}