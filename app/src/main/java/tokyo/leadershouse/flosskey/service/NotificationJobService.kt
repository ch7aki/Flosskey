package tokyo.leadershouse.flosskey.service
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import tokyo.leadershouse.flosskey.handler.KeyStoreHelper
import tokyo.leadershouse.flosskey.R
import tokyo.leadershouse.flosskey.util.getMisskeyUrlData

@SuppressLint("SpecifyJobSchedulerIdRange")
class NotificationJobService : JobService() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID   = "flosskey_notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "Flosskey"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("debug", "onStartJob[IN]")
        val instanceName = params?.extras?.getString("instanceName")
        val apiKey = params?.extras?.getString("apiKey")
        val jobId = params?.extras?.getInt("jobId")
        // コルーチン内で fetchNotifications を呼び出す
        CoroutineScope(Dispatchers.Main).launch {
            fetchNotifications(apiKey!!, instanceName!!, jobId!!)
        }
        jobFinished(params,true)
        Log.d("debug", "onStartJob[OUT]")
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("debug","onStopJob[IN]")
        val channelId = "job_notification_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Flosskey")
            .setContentText("Flosskeyの通知が止まったかも；。；")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = NotificationManagerCompat.from(this)
        val channel = NotificationChannel(channelId, "Job Notification", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        notificationManager.notify(9, notificationBuilder.build())
        Log.d("debug","onStopJob[OUT]")
        return true
    }

    private suspend fun fetchNotifications(apiKey: String, instanceName: String, jobId: Int) {
        Log.d("debug", "fetchNotifications[IN]")
        val client = OkHttpClient()
        val requestBody = JSONObject()
            .put("i", apiKey)
            .put("limit", 100)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(getMisskeyUrlData("API", instanceName))
            .post(requestBody)
            .build()
        var response: Response? = null
        try {
            response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrBlank()) { processNotifications(responseBody, jobId) }
            } else { Log.d("debug", "Failed to retrieve notifications: ${response.code}") }
        } catch (e: Exception) { Log.d("debug", "Failed to retrieve notifications", e)
        } finally {
            response?.close() // Responseをクローズしてリソースを解放 }
            Log.d("debug", "fetchNotifications[OUT]")
        }
    }

    private fun processNotifications(responseBody: String, jobId: Int) {
        Log.d("debug", "processNotifications[IN]")
        val jsonArray = JSONArray(responseBody)
        val keyStore = KeyStoreHelper.getKeyStore(this)
        // 端末が既知の最新の通知のcratedAt
        val deviceLatestCreatedAt = keyStore.getString(
            "createdAt_$jobId",
            "2000-01-01T00:00:00.000Z"
        ) ?: ""
        val instantDevice = Instant.parse(deviceLatestCreatedAt)
        Log.d("debug",deviceLatestCreatedAt)
        val summaryMessage = "通知がありました。"
        val newNotifications = mutableListOf<String>()
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
                    "reply"                -> "${name}にリプライされました"
                    "renote"               -> "${name}にリノートされました"
                    "quote"                -> "${name}に引用されました"
                    "reaction"             -> "${name}から${reaction}されました"
                    "receiveFollowRequest" -> "${name}からフォロー申請されました"
                    "allowFollowRequest"   -> "${name}へのフォローが許可されました"
                    else -> continue // 今度対応
                }
                if (message.isNotEmpty()) {
                    if (i == 0) { newNotifications.add(summaryMessage) }
                    newNotifications.add(message)
                }
            } else { break }
        }
        if (newNotifications.isNotEmpty()) { sendNotifications(newNotifications) }
        val tempolaryId = jsonArray.optJSONObject(0).optString("createdAt")
        val editor = keyStore.edit()
        editor.putString("createdAt_$jobId", tempolaryId)
        editor.apply()
        Log.d("debug", "processNotifications[OUT]")
    }

    private fun sendNotifications(messages: List<String>) {
        Log.d("debug", "sendNotifications[IN]")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("debug", "Missing notification permission")
            return
        }
        val channelId = NOTIFICATION_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val summaryMessage = messages[0]
        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Flosskey")
            .setContentText(summaryMessage)
            .setGroupSummary(true)
            .setGroup(NOTIFICATION_CHANNEL_NAME)
            .build()
        notificationManager.notify(0, summaryNotification)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(NOTIFICATION_CHANNEL_NAME)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setGroup(NOTIFICATION_CHANNEL_NAME)

        for (i in 1 until messages.size) {
            val message = messages[i]
            notificationBuilder.setContentText(message)
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                this,
                i,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.setContentIntent(pendingIntent)
            notificationManager.notify(i, notificationBuilder.build())
        }
        Log.d("debug", "sendNotifications[OUT]")
    }
}
