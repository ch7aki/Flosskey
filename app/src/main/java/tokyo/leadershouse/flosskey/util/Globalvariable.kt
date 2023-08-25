package tokyo.leadershouse.flosskey.util
import android.app.Activity
import tokyo.leadershouse.flosskey.BuildConfig
var MISSKEY_DOMAIN = ""
const val GITHUB_URL                = "https://github.com/ch1ak1STR/Flosskey/"
const val QA_URL                    = "https://github.com/ch1ak1STR/Flosskey/tree/master/doc/QA.md"
const val GITHUB_API_URL            = "https://api.github.com/repos/ch1ak1STR/Flosskey/releases/latest"
const val LICENSE_URL               = "https://raw.githubusercontent.com/ch1ak1STR/Flosskey/master/LICENSE"
const val SIDEBAR_TITLE             = "Flosskey Version : ${BuildConfig.VERSION_NAME}"
const val NOTIFICATION_CHANNEL_ID   = "flosskey_notifications"
const val NOTIFICATION_CHANNEL_NAME = "Flosskey"
const val RESULT_GO_DEVLOPER_URL    = Activity.RESULT_FIRST_USER + 1
const val apkName = "Flosskey.apk"
const val script  = """
            (function() {
                var imgs = document.querySelectorAll('.pswp__img');
                for(var i=0; i<imgs.length; i++) {
                    imgs[i].addEventListener('contextmenu', function(e) {
                        e.preventDefault();
                        var title = this.getAttribute('alt');
                        window.android.saveImage(this.src, title);
                    });
                }
            })();
        """
