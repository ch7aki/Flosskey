package tokyo.leadershouse.flosskey.handler
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale
class ImageDownloader(private val context: Context) {
    @OptIn(DelicateCoroutinesApi::class)
    fun saveImage(imageUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                val fileExtension = getFileExtensionFromUrl(imageUrl)
                // 画像を保存するためのファイルパスを生成
                val currentTime = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
                val filename = "$currentTime.$fileExtension"
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ),
                    "Flosskey"
                )
                if (!directory.exists()) { directory.mkdirs() }
                val file = File(directory, filename)
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "画像が保存されました", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "画像の保存に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun getFileExtensionFromUrl(url: String): String { // 画像をURLから取得
        val fileUrl = URL(url)
        val path = fileUrl.path
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return extension.ifEmpty { "png" }
    }
}
