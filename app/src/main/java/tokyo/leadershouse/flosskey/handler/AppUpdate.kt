package tokyo.leadershouse.flosskey.handler

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AppUpdate(private val activity: Activity, version: String) {

    private val apkName = "app_release.apk"
    private val apkUrl = "https://github.com/ch1ak1STR/Flosskey/releases/download/$version/app-release.apk"

    fun downloadAndInstallNewVersion() {
        val client = OkHttpClient()
        Log.d("debug", "APK_URL = $apkUrl")
        val request = Request.Builder()
            .url(apkUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val apkFile = File(activity.cacheDir, apkName)
                    val outputStream = FileOutputStream(apkFile)
                    val inputStream = response.body?.byteStream()
                    inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
                    showInstallDialog()
                }
            }
        })
    }

    private fun showInstallDialog() {
        activity.runOnUiThread {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("新しいバージョンがあります")
                .setMessage("最新版をダウンロードしますか？")
                .setPositiveButton("はい") { _, _ -> downloadNewVersion() }
                .setNegativeButton("いいえ") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }
    }

    private fun downloadNewVersion() {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("App Update")
            .setDescription("Downloading new version of the app")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName)
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}
