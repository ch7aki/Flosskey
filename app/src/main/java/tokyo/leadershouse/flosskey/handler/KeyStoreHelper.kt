package tokyo.leadershouse.flosskey.handler

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

data class AccountInfo(
    var accountName: String,
    var instanceName: String,
    var apiKey: String,
    var jobId: Int
)

object KeyStoreHelper {
    private const val KEYSTORE_NAME = "AccountInfo"
    fun saveAccountInfoList(context: Context, accountList: List<AccountInfo>) {
        val keyStore = getKeyStore(context)
        val editor   = keyStore.edit()
        editor.putInt("accountCount", accountList.size)
        // 削除対象のインデックス
        val indexToRemove = mutableListOf<Int>()
        for (i in accountList.indices) {
            if (accountList[i].accountName == "ユーザ情報追加") {
                // 削除対象のインデックスを記録
                indexToRemove.add(i)
                continue
            }
            // インデックスが削除対象のインデックスより大きい場合、1つずらす
            val newIndex = i - indexToRemove.count { it < i }
            editor.putString("accountName_$newIndex", accountList[i].accountName)
            Log.d("debug","accountName[$newIndex] = ${accountList[i].accountName}")
            editor.putString("instanceName_$newIndex", accountList[i].instanceName)
            Log.d("debug","instanceName[$newIndex] = ${accountList[i].instanceName}")
            editor.putString("apiKey_$newIndex", accountList[i].apiKey)
            Log.d("debug","apiKey[$newIndex]= ${accountList[i].apiKey}")
            editor.putInt("jobId_$newIndex", accountList[i].jobId)
            Log.d("debug","jobId)[$newIndex]= ${accountList[i].jobId}")
        }
        editor.apply()
    }

    fun loadAccountInfo(context: Context): List<AccountInfo> {
        val keyStore = getKeyStore(context)
        val accountCount = keyStore.getInt("accountCount", 0)
        val accountList = mutableListOf<AccountInfo>()
        for (i in 0 until accountCount) {
            val accountName  = keyStore.getString("accountName_$i", "") ?: ""
            val instanceName = keyStore.getString("instanceName_$i", "") ?: ""
            val apiKey       = keyStore.getString("apiKey_$i", "") ?: ""
            val jobId        = keyStore.getInt("jobId_$i", 0)
            accountList.add(AccountInfo(accountName, instanceName, apiKey, jobId))
        }
        return accountList
    }

    fun getKeyStore(context: Context): EncryptedSharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            KEYSTORE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
}

