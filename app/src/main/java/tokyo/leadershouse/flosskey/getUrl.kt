package tokyo.leadershouse.flosskey

var MISSKEY_DOMAIN   = "misskey.io"
fun getMisskeyUrl(): String {
    return "https://$MISSKEY_DOMAIN"
}

fun getMisskeyApiUrl(): String {
    return "${getMisskeyUrl()}/api/i/notifications"
}