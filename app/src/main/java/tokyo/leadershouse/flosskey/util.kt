package tokyo.leadershouse.flosskey
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.time.Instant
const val DEVELOPER_MISSKEY_URL = "https://misskey.io/@ch1ak1"
const val GITHUB_URL            =  "https://github.com/ch1ak1STR/Flosskey"
var MISSKEY_DOMAIN = "misskey.io"
fun getMisskeyUrlData(type: String, value: String): String {
    if (type == "API") { return "https://$value/api/i/notifications" }
    else if (type == "URL") { return "https://$MISSKEY_DOMAIN/" }
    return ""
}
fun changeInstance(pref: SharedPreferences, instanceName: String){
    MISSKEY_DOMAIN = instanceName
    pref.edit().putString("misskeyDomain", instanceName).apply()
    Log.d("debug","instanceName = $instanceName")
    Log.d("debug","MISSKEY_DOMAIN = $MISSKEY_DOMAIN")
}

fun getCurrentTimestamp(): Int {
    return Instant.now().epochSecond.toInt()
}