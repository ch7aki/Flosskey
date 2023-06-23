package tokyo.leadershouse.miskeywebview
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
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
    private lateinit var sharedPreferences: SharedPreferences
    companion object {
        private const val JOB_ID_RANGE_START = 1000
        private const val JOB_ID_RANGE_END   = 2000
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("debug","onStartJob[IN]")
        val apiKey = params?.extras?.getString("apiKey")
        if (apiKey != null) { fetchNotifications(apiKey) }
        else { return false }
        scheduleJob()
        Log.d("debug","onStartJob[OUT]")
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("debug","onStopJob[IN]")
        Log.d("debug","onStopJob[OUT]")
        return true
    }

    private fun fetchNotifications(apiKey: String) {
        Log.d("debug","fetchNotifications[IN]")

        val thread = Thread {
            val client = OkHttpClient()
            val url = MISSKEY_API_URL
            val requestBody = JSONObject()
                .put("i", apiKey)
                // ぶっちゃけsinceIdを変換してハンドリングしたいが一旦はcreatedAtで通知判定する...
                .put("limit", 100)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) { processNotifications(responseBody) }
                } else { Log.d("debug", "Failed to retrieve notifications: ${response.code}") }
            } catch (e: Exception) { Log.d("debug", "Failed to retrieve notifications", e) }
        }
        thread.start()
        Log.d("debug","fetchNotifications[OUT]")
    }

    private fun processNotifications(responseBody: String) {
        Log.d("debug", "processNotifications[IN]")
        val jsonArray = JSONArray(responseBody)
        for (i in 0 until jsonArray.length()) {
            val notification = jsonArray.optJSONObject(i)
            val createdAt = notification.optString("createdAt")
            // API叩いて取得した通知のcratedAt
            val instantApi = Instant.parse(createdAt)
            // 端末が既知の最新の通知のcratedAt
            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val instantDevcie = Instant.parse(
                sharedPreferences.getString(
                    "createdAt",
                    "2000-01-01T00:00:00.000Z"
                ) ?: "")

            val comparisonResult = instantApi.compareTo(instantDevcie )
            if (comparisonResult > 0) {
                val type = notification.optString("type")
                val user = notification.optJSONObject("user")
                val name = user?.optString("name")
                val reaction = notification.optString("reaction")
                when (type) {
                    "follow"               -> sendNotification("${name}にフォローされました")
                    "mention"              -> sendNotification("${name}メンションされました")
                    "reply"                -> sendNotification("${name}リノートされました")
                    "quote"                -> sendNotification("${name}引用されました")
                    "reaction"             -> sendNotification("${name}から${reaction}されました")
                    "receiveFollowRequest" -> sendNotification("${name}からフォロー申請されました")
                    "allowFollowRequest"   -> sendNotification("${name}へのフォローが許可されました")
                }
            }
            else {
                break
            }
        }
        // どうせ10件は通知取得するので、条件分岐せず最新のIdをとりあえず保存する設計にする
        // sinceIdだとそもそも落っこちてこないのでもちろんこの手口は不可。
        val tempolaryId = jsonArray.optJSONObject(0).optString("createdAt")
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("createdAt", tempolaryId)
        editor.apply()

        Log.d("debug", "processNotifications[OUT]")
    }

    private fun sendNotification(message: String) {
        Log.d("debug","sendNotification[IN]")
        val channelId = "misskey_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("Misskey Notification")
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Misskey Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0, notificationBuilder.build())
        Log.d("debug","sendNotification[OUT]")
    }

    private fun scheduleJob() {
        Log.d("debug","scheduleJob[IN]")
        val componentName = ComponentName(this, NotificationJobService::class.java)
        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobId = (JOB_ID_RANGE_START..JOB_ID_RANGE_END).random() // 範囲内のランダムなジョブIDを生成
        val jobInfo = JobInfo.Builder(jobId, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(0) // ここも10分だト思う
            .build()
        jobScheduler.schedule(jobInfo)
        Log.d("debug","scheduleJob[OUT]")
    }
}
