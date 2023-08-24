package tokyo.leadershouse.flosskey.listview
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import tokyo.leadershouse.flosskey.R
import tokyo.leadershouse.flosskey.adapter.AccountAdapter
import tokyo.leadershouse.flosskey.handler.AccountInfo
import tokyo.leadershouse.flosskey.handler.KeyStoreHelper
import tokyo.leadershouse.flosskey.util.getCurrentTimestamp
class AccountListActivity : AppCompatActivity() {
    private var isChanged: Boolean = false
    private val accountList = mutableListOf<AccountInfo>()
    private lateinit var accountListView: ListView
    private lateinit var accountAdapter: AccountAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)
        window.statusBarColor = Color.BLACK
        supportActionBar?.hide()
        accountListView = findViewById(R.id.accountListView)
        val keyStoreAccounts = KeyStoreHelper.loadAccountInfo(this)
        // アカウント情報をリストに追加
        accountList.add(
            AccountInfo(
            "ユーザ情報追加",
            "※ユーザ名はあなたがわかれば何でもOK。",
            "※インスタンス名/APIキーは通知取得に使います。",
            0)
        )
        accountList.addAll(keyStoreAccounts)
        accountAdapter = AccountAdapter(this, accountList)
        accountListView.adapter = accountAdapter
        accountListView.setOnItemClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]
            if (accountInfo.accountName == "ユーザ情報追加") {
                showHandleAccountDialog(true, accountInfo)
            }
        }
        accountListView.setOnItemLongClickListener { _, _, position, _ ->
            val accountInfo = accountList[position]
            if (accountInfo.accountName != "ユーザ情報追加") { showDeleteOrEditDialog(accountInfo) }
            true
        }
    }
    // 「アカウントとAPIキーを登録」のダイアログを表示する関数
    private fun showHandleAccountDialog(isAdd: Boolean, accountInfo: AccountInfo) {
        val dialogView       = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null)
        val accountEditText  = dialogView.findViewById<EditText>(R.id.accountEditText)
        val instanceEditText = dialogView.findViewById<EditText>(R.id.instanceEditText)
        val apiKeyEditText   = dialogView.findViewById<EditText>(R.id.apiKeyEditText)
        val dialogBuilder    = AlertDialog.Builder(this)
        dialogBuilder.setPositiveButton("OK", null) // OKボタンを初期状態では無効にしておく
        if (isAdd) {
            dialogBuilder.setTitle("ユーザ情報の追加")
            dialogBuilder.setView(dialogView)
            dialogBuilder.setPositiveButton("OK") { _, _ ->
                isChanged = true // OKボタンが押されたときの処理
                setResult()
                val account  = accountEditText.text.toString()
                val instance = instanceEditText.text.toString()
                val apiKey   = apiKeyEditText.text.toString()
                if (apiKey.isEmpty() || !accountList.any { it.apiKey == apiKey }) { // 入力されたアカウントとAPIキーをリストに追加
                    accountList.add(AccountInfo(account, instance, apiKey, getCurrentTimestamp()))
                    accountAdapter.notifyDataSetChanged()
                    saveAccountInfo()
                }
            }
        } else {
            dialogBuilder.setTitle("ユーザ情報の更新")
            dialogBuilder.setView(dialogView)
            accountEditText.setText(accountInfo.accountName)
            instanceEditText.setText(accountInfo.instanceName)
            apiKeyEditText.setText(accountInfo.apiKey)
            dialogBuilder.setPositiveButton("OK") { _, _ ->
                isChanged = true // 更新ボタンが押された場合の処理
                setResult()
                val updatedAccount  = accountEditText.text.toString()
                val updatedInstance = instanceEditText.text.toString()
                val updatedApiKey   = apiKeyEditText.text.toString()
                // 既存のアカウント情報を更新
                val existingAccount = accountList.find { it.apiKey == accountInfo.apiKey }
                if (existingAccount != null) {
                    existingAccount.accountName  = updatedAccount
                    existingAccount.instanceName = updatedInstance
                    existingAccount.apiKey       = updatedApiKey
                    saveAccountInfo()
                }
            }
        }
        dialogBuilder.setNegativeButton("キャンセル", null)
        val alertDialog = dialogBuilder.create()
        alertDialog.setOnShowListener {// ダイアログが表示される前に実行する処理を設定
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            fun updatePositiveButtonState() { // OKボタンの有効・無効を制御する関数
                val account  = accountEditText.text.toString()
                val instance = instanceEditText.text.toString()
                val isPositiveButtonEnabled =
                    account.isNotBlank() && instance.isNotBlank()
                positiveButton.isEnabled = isPositiveButtonEnabled
            }
            // 入力欄の内容が変更されたらOKボタンの有効・無効を更新する
            accountEditText.addTextChangedListener  { updatePositiveButtonState() }
            instanceEditText.addTextChangedListener { updatePositiveButtonState() }
            apiKeyEditText.addTextChangedListener   { updatePositiveButtonState() }
            updatePositiveButtonState() // 初回表示時にもOKボタンの有効・無効を更新する
        }
        alertDialog.show()
    }
    private fun saveAccountInfo() {
        // 「ユーザ情報追加」をリストから除外して保存
        val accountListToSave = accountList.filter { it.accountName != "ユーザ情報追加" }
        KeyStoreHelper.saveAccountInfoList(this, accountListToSave)
    }
    private fun showDeleteOrEditDialog(accountInfo: AccountInfo) {
        val options = arrayOf(
            "登録情報の編集",
            "登録情報の削除"
        )
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
        isChanged = true // アカウント情報の削除処理を実装
        setResult()
        accountList.remove(accountInfo)
        accountAdapter.notifyDataSetChanged()
        // キーストアから該当のアカウント情報を削除する
        KeyStoreHelper.saveAccountInfoList(this, accountList.filter { it.accountName != "ユーザ情報追加" })
    }
    private fun setResult() {
        if (isChanged) { setResult(Activity.RESULT_OK, intent) }
        else { setResult(Activity.RESULT_CANCELED, intent) }
    }
    override fun onDestroy() {
        super.onDestroy()
        finish()
    }
}
