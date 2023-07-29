package tokyo.leadershouse.flosskey
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: Activity) : ActivityCompat.OnRequestPermissionsResultCallback {
    private val permissionCode = 1
    private val permissions = arrayOf(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    fun requestPermission() {
        val permissionsToRequest = permissions.filterNot { checkPermission(it) }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                permissionCode
            )
        } else {
            // パーミッションが許可されているなら何もしない
        }
    }

    private fun checkPermission(permission: String): Boolean {
        val result = ContextCompat.checkSelfPermission(activity, permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            permissionCode -> {
                val permissionsGranted = permissions.filterIndexed { index, _ ->
                    grantResults[index] == PackageManager.PERMISSION_GRANTED
                }
                val permissionsDenied  = permissions.filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }
                if (permissionsGranted.isNotEmpty()) {
                    Toast.makeText(activity, "権限が正しく許可されました", Toast.LENGTH_SHORT).show()
                }
                if (permissionsDenied.isNotEmpty()) {
                    Toast.makeText(
                        activity,
                        "権限を許可していただけないと通知や画像保存ができない可能性があります。",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}