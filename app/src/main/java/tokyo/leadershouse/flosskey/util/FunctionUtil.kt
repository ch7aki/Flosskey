package tokyo.leadershouse.flosskey.util
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.time.Instant
fun getCurrentTimestamp(): Int { return Instant.now().epochSecond.toInt() }
fun getMisskeyApiUrl(instanceName: String): String { return "https://$instanceName/api/i/notifications" }
fun getMisskeyInstanceUrl(): String { return "https://$MISSKEY_DOMAIN/"}
fun getApkUrl(version: String): String { return "https://github.com/ch1ak1STR/Flosskey/releases/download/$version/Flosskey.apk"}
fun doActivity(activity: AppCompatActivity, intent: Intent) {
    activity.startActivity(intent)
    activity.finish()
}
fun changeInstance(pref: SharedPreferences, instanceName: String){
    MISSKEY_DOMAIN = instanceName
    pref.edit().putString("misskeyDomain", instanceName).apply()
    Log.d("debug","instanceName = $instanceName")
    Log.d("debug","MISSKEY_DOMAIN = $MISSKEY_DOMAIN")
}
