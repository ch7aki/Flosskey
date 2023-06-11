package tokyo.leadershouse.miskeywebview

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText

class ApiKeyInputDialog(
    private val context: Context,
    private var listener: ApiKeyListener? = null
) {

    interface ApiKeyListener {
        fun onApiKeyEntered(apiKey: String)
    }

    fun setApiKeyListener(listener: ApiKeyListener) {
        this.listener = listener
    }

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_api_key, null)
        val apiKeyEditText = dialogView.findViewById<EditText>(R.id.apiKeyEditText)

        val dialog = AlertDialog.Builder(context)
            .setTitle("API Key")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val apiKey = apiKeyEditText.text.toString()
                listener?.onApiKeyEntered(apiKey)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}