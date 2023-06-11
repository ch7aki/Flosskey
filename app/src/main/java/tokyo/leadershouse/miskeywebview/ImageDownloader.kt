package tokyo.leadershouse.miskeywebview

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale

class ImageDownloader(private val context: Context) {
    fun saveImage(imageUrl: String) {
        Thread {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)

                // 画像の拡張子を取得
                val fileExtension = getFileExtensionFromUrl(imageUrl)

                // 画像を保存するためのファイルパスを生成
                val currentTime = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
                val filename = "$currentTime.$fileExtension"
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES),
                    "misskeywebview")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, filename)

                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                // 画像の保存が完了したことをユーザーに通知する
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "画像が保存されました", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // 画像の保存が失敗したことをユーザーに通知する
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "画像の保存に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun getFileExtensionFromUrl(url: String): String {
        val fileUrl = URL(url)
        val path = fileUrl.path
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return extension.ifEmpty { "png" }
    }
}