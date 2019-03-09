package one.oktw.muzeipixivsource.provider

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.apps.muzei.api.UserCommand
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import kotlinx.coroutines.runBlocking
import one.oktw.muzeipixivsource.BuildConfig
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.FETCH_MODE_RECOMMEND
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ALL_PAGE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_CLEANUP
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_FALLBACK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_MODE_RANKING
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_NUMBER
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_ORIGIN
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FETCH_RANDOM
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_BOOKMARK
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_GREY_SCALE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SAFE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_SIZE
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_FILTER_VIEW
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment.Companion.KEY_PIXIV_ACCESS_TOKEN
import one.oktw.muzeipixivsource.db.PixivSourceDatabase
import one.oktw.muzeipixivsource.db.dao.upsert
import one.oktw.muzeipixivsource.pixiv.Pixiv
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.Monthly
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.valueOf
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.util.FileUtil
import one.oktw.muzeipixivsource.util.IllustUtil
import org.jsoup.Jsoup
import java.io.File
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
        runBlocking { updateToken() }

        val token: String? = preference.getString(KEY_PIXIV_ACCESS_TOKEN, null)
        val fallback = preference.getBoolean(KEY_FETCH_FALLBACK, false)
        val pixiv = Pixiv(token = token, number = preference.getInt(KEY_FETCH_NUMBER, 30))

        try {
            when (if (token == null) FETCH_MODE_FALLBACK else preference.getString(KEY_FETCH_MODE, "0")!!.toInt()) {
                FETCH_MODE_FALLBACK -> pixiv.getFallback().let(::publish)
                FETCH_MODE_RECOMMEND -> pixiv.getRecommend().let(::publish)
                FETCH_MODE_RANKING -> pixiv.getRanking(
                    valueOf(preference.getString(KEY_FETCH_MODE_RANKING, Monthly.name)!!)
                ).let(::publish)

                FETCH_MODE_BOOKMARK -> pixiv.getBookmark(
                    preference.getInt(SettingsFragment.KEY_PIXIV_USER_ID, -1),
                    preference.getBoolean(SettingsFragment.KEY_FETCH_MODE_BOOKMARK, false)
                ).let(::publish)
            }
        } catch (e1: Exception) {
            // TODO better except handle
            Log.e("fetch", "fetch update error", e1)

            try {
                if (fallback) pixiv.getFallback().let(::publish) else throw e1
            } catch (e2: Exception) {
                Log.e("fetch", "fetch update fallback error", e2)

                throw e2
            }
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

    private suspend fun updateToken() {
        try {
            PixivOAuth.refresh(
                preference.getString(SettingsFragment.KEY_PIXIV_DEVICE_TOKEN, null) ?: return,
                preference.getString(SettingsFragment.KEY_PIXIV_REFRESH_TOKEN, null) ?: return
            ).response?.let { PixivOAuth.save(preference, it) }
        } catch (e: Exception) {
            Log.e("update_token", "update token error", e)
        }
    }

    private fun publish(list: ArrayList<Illust>) {
        val cleanHistory = preference.getBoolean(KEY_FETCH_CLEANUP, true)
        val filterNSFW = preference.getBoolean(KEY_FILTER_SAFE, true)
        val random = preference.getBoolean(KEY_FETCH_RANDOM, false)
        val filterSize = preference.getInt(KEY_FILTER_SIZE, 0)
        val originImage = if (filterSize > 1200) true else preference.getBoolean(KEY_FETCH_ORIGIN, false)
        val minView = preference.getInt(KEY_FILTER_VIEW, 0)
        val minBookmark = preference.getInt(KEY_FILTER_BOOKMARK, 0)
        val allPage = preference.getBoolean(KEY_FETCH_ALL_PAGE, false)

        val artworkList = ArrayList<Artwork>()

        list.forEach {
            if (filterNSFW && it.sanityLevel >= 4) return@forEach
            if (filterSize > it.height && filterSize > it.width) return@forEach
            if (minView > it.totalView || minBookmark > it.totalBookmarks) return@forEach


            if (it.pageCount > 1 && allPage) {
                it.metaPages.forEachIndexed { index, image ->
                    val imageUrl = if (originImage) {
                        image.imageUrls.original
                    } else {
                        image.imageUrls.large?.replace("/c/600x1200_90", "")
                    }?.toUri()

                    Artwork.Builder()
                        .title(it.title)
                        .byline(it.user.name)
                        .attribution(Jsoup.parse(it.caption).text())
                        .token("${it.id}_p$index")
                        .webUri("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=${it.id}".toUri())
                        .persistentUri(imageUrl)
                        .build()
                        .let(artworkList::add)
                }
            } else {
                val imageUrl = if (it.pageCount > 1) {
                    val image = it.metaPages.first()
                    if (originImage) {
                        image.imageUrls.original
                    } else {
                        image.imageUrls.large?.replace("/c/600x1200_90", "")
                    }?.toUri()
                } else {
                    if (originImage) {
                        it.metaSinglePage.original_image_url
                    } else {
                        it.image_urls.large?.replace("/c/600x1200_90", "")
                    }?.toUri()
                }

                Artwork.Builder()
                    .title(it.title)
                    .byline(it.user.name)
                    .attribution(Jsoup.parse(it.caption).text())
                    .token("${it.id}_p0")
                    .webUri("https://www.pixiv.net/member_illust.php?mode=medium&illust_id=${it.id}".toUri())
                    .persistentUri(imageUrl)
                    .build()
                    .let(artworkList::add)
            }
        }

        val hidelists = PixivSourceDatabase.instance(context).hideDao().getList(artworkList.map { it.token!! }.toTypedArray()).map { it.IllustId }

        artworkList.removeIf { hidelists.contains(it.token) }

        if (cleanHistory && artworkList.isNotEmpty()) {
            val delete = delete(contentUri, null, null)
            Log.d(TAG, "删除历史：$delete")
        }
        if (random) artworkList.shuffle()
        Log.d(TAG, "正在添加数量：${artworkList.size}")
        artworkList.forEach { addArtwork(it) }
        Log.d(TAG, "添加完成")
    }

}
