package tokyo.leadershouse.flosskey

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

data class AccountInfo(
    var accountName: String,
    var instanceName: String,
    var apiKey: String
)

object KeyStoreHelper {
    private const val KEYSTORE_NAME = "AccountInfo"
    fun saveAccountInfoList(context: Context, accountList: List<AccountInfo>) {
        val keyStore = getKeyStore(context)
        val editor   = keyStore.edit()

        editor.putInt("accountCount", accountList.size)
        for (i in accountList.indices) {
            editor.putString("accountName_$i", accountList[i].accountName)
            editor.putString("instanceName_$i", accountList[i].instanceName)
            editor.putString("apiKey_$i", accountList[i].apiKey)
        }
        editor.apply()
    }

    fun loadAccountInfo(context: Context): List<AccountInfo> {
        val keyStore = getKeyStore(context)
        val accountCount = keyStore.getInt("accountCount", 0)

        val accountList = mutableListOf<AccountInfo>()
        for (i in 0 until accountCount) {
            val accountName = keyStore.getString("accountName_$i", "") ?: ""
            val instanceName = keyStore.getString("instanceName_$i", "") ?: ""
            val apiKey = keyStore.getString("apiKey_$i", "") ?: ""
            accountList.add(AccountInfo(accountName, instanceName, apiKey))
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

