package tokyo.leadershouse.miskeywebview
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class AccountAdapter(context: Context, private val accountList: List<AccountInfo>)
    : ArrayAdapter<AccountInfo>(context, 0, accountList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_account, parent, false)
        val accountInfo = accountList[position]
        val accountNameTextView = view.findViewById<TextView>(R.id.accountNameTextView)
        val apiKeyTextView = view.findViewById<TextView>(R.id.apiKeyTextView)
        accountNameTextView.text = accountInfo.accountName
        apiKeyTextView.text = accountInfo.apiKey
        return view
    }
}