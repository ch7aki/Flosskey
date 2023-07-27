package tokyo.leadershouse.miskeywebview

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

data class AccountInfo(var accountName: String, var apiKey: String)
class AccountListActivity : AppCompatActivity() {

    private val accountList = mutableListOf<AccountInfo>()
    private lateinit var accountListView: ListView
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
                showHandleAccountDialog(true, accountInfo)
            }
        }

        // ロングタップで情報を更新
        accountListView.setOnItemLongClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]
            if (accountInfo.accountName != "APIキー追加") {
                showDeleteOrEditDialog(accountInfo)
            }
            true
        }
    }

    // 「アカウントとAPIキーを登録」のダイアログを表示する関数
    private fun showHandleAccountDialog(isAdd: Boolean, accountInfo: AccountInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null)
        val accountEditText = dialogView.findViewById<EditText>(R.id.accountEditText)
        val apiKeyEditText = dialogView.findViewById<EditText>(R.id.apiKeyEditText)
        val dialogBuilder = AlertDialog.Builder(this)
        if (isAdd) {
            // アカウント追加用のダイアログ
            dialogBuilder.setTitle("アカウントとAPIキーを追加")
            dialogBuilder.setView(dialogView)
            dialogBuilder.setPositiveButton("OK") { _, _ ->
                // OKボタンが押されたときの処理
                val account = accountEditText.text.toString()
                val apiKey  = apiKeyEditText.text.toString()
                // 入力されたアカウントとAPIキーをリストに追加
                accountList.add(AccountInfo(account, apiKey))
                accountAdapter.notifyDataSetChanged()
                // 新しいアカウント情報を保存
                saveAccountInfo()
            }
        } else {
            // 既存のアカウント情報を編集するダイアログ
            dialogBuilder.setTitle("アカウントとAPIキーを更新")
            dialogBuilder.setView(dialogView)
            // ダイアログに既存のアカウント情報をセット
            accountEditText.setText(accountInfo.accountName)
            apiKeyEditText.setText(accountInfo.apiKey)
            dialogBuilder.setPositiveButton("OK") { _, _ ->
                // 更新ボタンが押された場合の処理
                val updatedAccount = accountEditText.text.toString()
                val updatedApiKey = apiKeyEditText.text.toString()
                // リストビューのアイテムを更新
                accountInfo.accountName = updatedAccount
                accountInfo.apiKey = updatedApiKey
            }
        }
        dialogBuilder.setNegativeButton("キャンセル", null)
        dialogBuilder.show()
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
    private fun showDeleteOrEditDialog(accountInfo: AccountInfo) {
        val options = arrayOf("編集", "削除")
        val builder = AlertDialog.Builder(this)
        builder.setItems(options) { _, which ->
                when (which) {
                    0 -> showHandleAccountDialog(false, accountInfo) // 編集の選択
                    1 -> deleteAccount(accountInfo) // 削除の選択
                }
            }
            .setNegativeButton("キャンセル") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteAccount(accountInfo: AccountInfo) {
        // アカウント情報の削除処理を実装
        accountList.remove(accountInfo)
        accountAdapter.notifyDataSetChanged()
        // SharedPreferencesからアカウント情報を取得し、該当のアカウント情報を削除する
        val sharedPreferences = getSharedPreferences("AccountInfo", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val accountCount = sharedPreferences.getInt("accountCount", 0)
        for (i in 1 until accountCount) {
            val accountName = sharedPreferences.getString("accountName_$i", "")
            if (accountName == accountInfo.accountName) {
                // 該当のアカウント情報を削除
                editor.remove("accountName_$i")
                editor.remove("apiKey_$i")
                // 他のアカウント情報をシフトする
                for (j in i + 1 until accountCount) {
                    val name = sharedPreferences.getString("accountName_$j", "")
                    val key = sharedPreferences.getString("apiKey_$j", "")
                    editor.putString("accountName_${j - 1}", name)
                    editor.putString("apiKey_${j - 1}", key)
                }
                // accountCountを1つ減らす
                editor.putInt("accountCount", accountCount - 1)
                editor.apply()
                break
            }
        }
    }

    override fun onDestroy() {
        saveAccountInfo()
        super.onDestroy()
    }
}