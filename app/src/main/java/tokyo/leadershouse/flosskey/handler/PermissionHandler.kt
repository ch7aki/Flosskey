package tokyo.leadershouse.flosskey.handler
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: Activity) : ActivityCompat.OnRequestPermissionsResultCallback {
    private val permissionCode = 1
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissions = arrayOf(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPermission() {
        val permissionsToRequest = permissions.filterNot { checkPermission(it) }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                permissionCode
            )
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
                        "権限を許可していただけないと一部機能に支障がある恐れがあります。",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    fun requestPermissionsLegacy(context: Context) {
        val permissionsToRequest = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        val permissionsToCheck = mutableListOf<String>()

        for (permission in permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToCheck.add(permission)
            }
        }

        if (permissionsToCheck.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToCheck.toTypedArray(), 0)
        } else {
            // 権限がすでに付与されている場合の処理
        }
    }
}