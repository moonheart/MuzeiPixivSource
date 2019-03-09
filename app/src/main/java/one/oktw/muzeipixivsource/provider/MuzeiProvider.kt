package one.oktw.muzeipixivsource.provider

import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.apps.muzei.api.UserCommand
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_GREY_SCALE
import one.oktw.muzeipixivsource.util.FileUtil
import one.oktw.muzeipixivsource.util.IllustUtil
import java.io.InputStream

class MuzeiProvider() : MuzeiArtProvider() {
    companion object {
        private const val COMMAND_FETCH = 1
        private const val COMMAND_HIDE = 2
        private const val COMMAND_HIDE_ILLUST = 3
        private const val COMMAND_SHARE_IMAGE = 4
        private const val COMMAND_SHARE_URL = 5
        private const val TAG = "MuzeiProvider"
    }

    private lateinit var preference: SharedPreferences

    private lateinit var fileUtil: FileUtil

    private lateinit var illustUtil: IllustUtil

    override fun onCreate(): Boolean {
        PreferenceManager.setDefaultValues(context, R.xml.prefragment, true)
        illustUtil = IllustUtil(context)
        fileUtil = FileUtil(context)
        preference = PreferenceManager.getDefaultSharedPreferences(context)
        return super.onCreate()
    }

    override fun onLoadRequested(initial: Boolean) {
        Log.d(TAG, "加载请求")
        GlobalScope.launch {
            illustUtil.fetchNewIllust()
        }
    }

    override fun openFile(artwork: Artwork): InputStream {
        val filterGrwyscale = preference.getBoolean(KEY_FILTER_GREY_SCALE, true)
        return fileUtil.openFile(this, artwork, filterGrwyscale)
    }


    override fun getCommands(artwork: Artwork) = mutableListOf(
        UserCommand(COMMAND_FETCH, context?.getString(R.string.button_update)),
        UserCommand(COMMAND_HIDE, context?.getString(R.string.button_hide)),
        UserCommand(COMMAND_HIDE_ILLUST, context?.getString(R.string.button_hide_illust)),
        UserCommand(COMMAND_SHARE_IMAGE, context?.getString(R.string.button_share_image)),
        UserCommand(COMMAND_SHARE_URL, context?.getString(R.string.button_share_url))
    )

    override fun onCommand(artwork: Artwork, id: Int) = when (id) {
        COMMAND_FETCH -> onLoadRequested(false)
        COMMAND_HIDE -> illustUtil.hideImage(artwork)
        COMMAND_HIDE_ILLUST -> illustUtil.hideIllust(artwork)
        COMMAND_SHARE_IMAGE -> illustUtil.shareImage(artwork)
        COMMAND_SHARE_URL -> illustUtil.shareUrl(artwork)
        else -> Unit
    }

}
