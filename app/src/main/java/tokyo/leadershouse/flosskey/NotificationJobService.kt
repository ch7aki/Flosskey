package tokyo.leadershouse.flosskey
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

@SuppressLint("SpecifyJobSchedulerIdRange")
class NotificationJobService : JobService() {
    private var notificationId = 0 // 通知IDを保持する変数
    companion object {
        private const val NOTIFICATION_CHANNEL_ID   = "flosskey_notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "Flosskey"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("debug", "onStartJob[IN]")
        val instanceName = params?.extras?.getString("instanceName")
        val apiKey = params?.extras?.getString("apiKey")
        val jobId = params?.extras?.getInt("jobId")
        fetchNotifications(apiKey!!, instanceName!!, jobId!!)
        Log.d("debug", "onStartJob[OUT]")
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("debug","onStopJob[IN]")
        jobFinished(params,true)
        Log.d("debug","onStopJob[OUT]")
        return true
    }

    private fun fetchNotifications(apiKey: String, instanceName: String, jobId: Int) {
        Log.d("debug","fetchNotifications[IN]")
        val thread = Thread {
            val client = OkHttpClient()
            // ぶっちゃけsinceIdを変換してハンドリングしたいが一旦はcreatedAtで通知判定する...
            val requestBody = JSONObject()
                .put("i", apiKey)
                .put("limit", 100)
                .toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(getMisskeyUrlData("API",instanceName))
                .post(requestBody)
                .build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) { processNotifications(responseBody, jobId) }
                } else { Log.d("debug", "Failed to retrieve notifications: ${response.code}") }
            } catch (e: Exception) { Log.d("debug", "Failed to retrieve notifications", e) }
        }
        thread.start()
        Log.d("debug","fetchNotifications[OUT]")
    }

    private fun processNotifications(responseBody: String, jobId: Int) {
        Log.d("debug", "processNotifications[IN]")
        var hasValidNotification = false
        val jsonArray = JSONArray(responseBody)
        val keyStore  = KeyStoreHelper.getKeyStore(this)
        // 端末が既知の最新の通知のcratedAt
        val instantDevice = Instant.parse(
            keyStore.getString(
                "createdAt_$jobId",
                "2000-01-01T00:00:00.000Z"
            ) ?: "")
        for (i in 0 until jsonArray.length()) {
            val notification = jsonArray.optJSONObject(i)
            val createdAt = notification.optString("createdAt")
            // API叩いて取得した通知のcratedAt
            val instantApi = Instant.parse(createdAt)
            val comparisonResult = instantApi.compareTo(instantDevice)
            if (comparisonResult > 0) {
                val type = notification.optString("type")
                val user = notification.optJSONObject("user")
                val name = user?.optString("name")
                val reaction = notification.optString("reaction")
                val message = when (type) {
                    "follow"               -> "${name}にフォローされました"
                    "mention"              -> "${name}にメンションされました"
                    "reply"                -> "${name}にリノートされました"
                    "quote"                -> "${name}に引用されました"
                    "reaction"             -> "${name}から${reaction}されました"
                    "receiveFollowRequest" -> "${name}からフォロー申請されました"
                    "allowFollowRequest"   -> "${name}へのフォローが許可されました"
                    else -> continue // 今度対応
                }
                if (message.isNotEmpty()) {
                    hasValidNotification = true
                    sendNotification(message)
                }
            }
            else { break }
        }
        if (!hasValidNotification) {
            sendNotification("通知は無いみたい")
        }
        val tempolaryId = jsonArray.optJSONObject(0).optString("createdAt")
        val editor = keyStore.edit()
        editor.putString("createdAt", tempolaryId)
        editor.apply()
        Log.d("debug", "processNotifications[OUT]")
    }

    private fun sendNotification(message: String) {
        Log.d("debug","sendNotification[IN]")
        val channelId = NOTIFICATION_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // アプリを開くためのIntentを作成
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(NOTIFICATION_CHANNEL_NAME)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent) // PendingIntentを設定
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(notificationId++, notificationBuilder.build())
        Log.d("debug","sendNotification[OUT]")
    }
}
