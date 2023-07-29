package tokyo.leadershouse.flosskey

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

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

        val keyStoreAccounts = KeyStoreHelper.loadAccountInfo(this)
        // アカウント情報をリストに追加
        accountList.add(AccountInfo(
            "ユーザ情報追加",
            "※ユーザ名はあなたがわかれば何でもOK!",
            "※インスタンス名/APIキーは通知取得に使います！"))
        accountList.addAll(keyStoreAccounts)

        // アダプターをセット
        accountAdapter = AccountAdapter(this, accountList)
        accountListView.adapter = accountAdapter
        accountListView.setOnItemClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]
            if (accountInfo.accountName == "ユーザ情報追加") {
                showHandleAccountDialog(true, accountInfo)
            }
        }

        // ロングタップで情報を更新
        accountListView.setOnItemLongClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]
            if (accountInfo.accountName != "ユーザ情報追加") {
                showDeleteOrEditDialog(accountInfo)
            }
            true
        }
    }
    // 「アカウントとAPIキーを登録」のダイアログを表示する関数
// 「アカウントとAPIキーを登録」のダイアログを表示する関数
    private fun showHandleAccountDialog(isAdd: Boolean, accountInfo: AccountInfo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null)
        val accountEditText = dialogView.findViewById<EditText>(R.id.accountEditText)
        val instanceEditText = dialogView.findViewById<EditText>(R.id.instanceEditText)
        val apiKeyEditText = dialogView.findViewById<EditText>(R.id.apiKeyEditText)
        val dialogBuilder = AlertDialog.Builder(this)
        if (isAdd) {
            // アカウント追加用のダイアログ
            dialogBuilder.setTitle("アカウントとAPIキーを追加")
            dialogBuilder.setView(dialogView)
            dialogBuilder.setPositiveButton("OK") { _, _ ->
                // OKボタンが押されたときの処理
                val account  = accountEditText.text.toString()
                val instance = instanceEditText.text.toString()
                val apiKey   = apiKeyEditText.text.toString()
                // 入力されたアカウントとAPIキーをリストに追加
                accountList.add(AccountInfo(account, instance, apiKey))
                accountAdapter.notifyDataSetChanged()
                // アカウント情報を保存
                saveAccountInfo()
            }
        } else {
            // 編集用のダイアログ
            dialogBuilder.setTitle("アカウントとAPIキーを更新")
            dialogBuilder.setView(dialogView)
            // ダイアログに既存のアカウント情報をセット
            accountEditText.setText(accountInfo.accountName)
            instanceEditText.setText(accountInfo.instanceName)
            apiKeyEditText.setText(accountInfo.apiKey)
            dialogBuilder.setPositiveButton("OK") { _, _ ->
                // 更新ボタンが押された場合の処理
                val updatedAccount = accountEditText.text.toString()
                val updatedInstance = instanceEditText.text.toString()
                val updatedApiKey = apiKeyEditText.text.toString()
                // リストビューのアイテムを更新
                accountInfo.accountName = updatedAccount
                accountInfo.instanceName = updatedInstance
                accountInfo.apiKey = updatedApiKey
                // アカウント情報を保存
                saveAccountInfo()
            }
        }
        dialogBuilder.setNegativeButton("キャンセル", null)
        dialogBuilder.show()
    }

    private fun saveAccountInfo() {
        // 「ユーザ情報追加」をリストから除外して保存
        val accountListToSave = accountList.filter { it.accountName != "ユーザ情報追加" }
        KeyStoreHelper.saveAccountInfoList(this, accountListToSave)
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

        // キーストアから該当のアカウント情報を削除する
        val updatedAccountList = accountList.filter { it.accountName != accountInfo.accountName }
        KeyStoreHelper.saveAccountInfoList(this, updatedAccountList)
    }

    override fun onDestroy() {
        saveAccountInfo()
        super.onDestroy()
    }
}