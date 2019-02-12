package one.oktw.muzeipixivsource.provider

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.apps.muzei.api.UserCommand
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
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
import one.oktw.muzeipixivsource.db.dao.ImageGrayscaleDao
import one.oktw.muzeipixivsource.db.dao.upsert
import one.oktw.muzeipixivsource.pixiv.Pixiv
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.Monthly
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory.valueOf
import one.oktw.muzeipixivsource.pixiv.model.Illust
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.properties.Delegates

class MuzeiProvider() : MuzeiArtProvider() {
    companion object {
        private const val COMMAND_FETCH = 1
        private const val COMMAND_HIDE = 2
        private const val COMMAND_HIDE_ILLUST = 3
        private const val COMMAND_SHARE_IMAGE = 4
        private const val COMMAND_SHARE_URL = 5
        private const val TAG = "MuzeiProvider"
    }

    private val httpClient = OkHttpClient()
    private lateinit var preference: SharedPreferences
    private val imageGreyscaleDao: ImageGrayscaleDao
        get() = PixivSourceDatabase.instance(context).imageDao()

    override fun onCreate(): Boolean {
        PreferenceManager.setDefaultValues(context, R.xml.prefragment, true)

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

        val greyValue = if (filterGrwyscale) imageGreyscaleDao.getGreyscaleValue(artwork.token!!) else 0f

        if (filterGrwyscale &&
            checkGreyValue(greyValue)) {
            hideImage(artwork)
            throw Exception("跳过灰色图片")
        }

        val filename = artwork.persistentUri.toString().split("/").last()
        Log.d(TAG, "打开文件：$filename")
        val file = File(getPixivCacheDir(), filename)
        if (!file.exists()) {
            val tmpFile = File(getPixivCacheDir(), "$filename.tmp")
            Request.Builder()
                .url(artwork.persistentUri.toString())
                .header("Referer", "https://app-api.pixiv.net/")
                .build()
                .let(httpClient::newCall)
                .execute()
                .use {
                    it.body()!!.byteStream().use { bs ->
                        FileOutputStream(tmpFile).use {
                            bs.copyTo(it)
                        }
                    }
                }
            tmpFile.renameTo(file)
        }

        if (filterGrwyscale
            && checkGreyValue(greyValue)
            && CheckIsGreyScale(artwork.token, file)) {
            file.delete()
            hideImage(artwork)
            throw Exception("跳过灰色图片")
        }
        return file.inputStream()
    }

    private fun CheckIsGreyScale(token: String?, file: File): Boolean {
        val bitmap = BitmapFactory.decodeStream(file.inputStream())
        val start = System.currentTimeMillis()
        var greyPointCount = 0;
        var totalPointCount = 0;
        for (x in 0 until bitmap.width step 3) {
            for (y in 0 until bitmap.height step 3) {
                if (bitmap[x, y].red == bitmap[x, y].green && bitmap[x, y].green == bitmap[x, y].blue) {
                    greyPointCount++
                }
                totalPointCount++
            }
        }
        val end = System.currentTimeMillis()
        val percent = greyPointCount.toFloat() / totalPointCount
        if (token != null) {
            val upsert = imageGreyscaleDao.upsert(token, percent)
            Log.d(TAG, "upsert: $upsert")
        } else Log.d(TAG, "token null")
        Log.d(TAG, "灰阶图片检测：$percent = $greyPointCount / $totalPointCount, 耗时：${end - start}毫秒")
        return checkGreyValue(percent)
    }

    private fun checkGreyValue(percent: Float): Boolean {
        return percent > 0.9
    }


    private fun getPixivCacheDir(): File {
        val savePath = "/pixiv/"
        // 创建外置缓存文件夹
        val pPath = File(Environment.getExternalStorageDirectory().toString() + savePath)
        if (pPath.exists()) {
            if (pPath.isFile) {
                pPath.delete()
                pPath.mkdir()
            }
        } else {
            pPath.mkdir()
        }
        return pPath
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
        COMMAND_HIDE -> hideImage(artwork)
        COMMAND_HIDE_ILLUST -> hideIllust(artwork)
        COMMAND_SHARE_IMAGE -> shareImage(artwork)
        COMMAND_SHARE_URL -> shareUrl(artwork)
        else -> Unit
    }

    private fun shareUrl(artwork: Artwork) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, artwork.webUri.toString())
            type = "text/plain"
        }
        sendIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(sendIntent)
    }

    private fun shareImage(artwork: Artwork) {
        val filename = artwork.persistentUri.toString().split("/").last()
        val file = File(getPixivCacheDir(), filename)

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context!!, "${BuildConfig.APPLICATION_ID}.fileprovider", file))
            type = "image/png"
        }
        shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    private fun hideIllust(artwork: Artwork) {
        val ilustid = "\\d+_".toRegex().find(artwork.token!!)!!.value
        Log.d(TAG, "隱藏作品 $ilustid")
        query(contentUri, arrayOf("persistent_uri"), "token like ?", arrayOf("$ilustid%"), null)
            .use {
                while (it.moveToNext()) {
                    val token = it.getString(0)
                    val filename = token.split("/").last()
                    Log.d(TAG, "隱藏圖片： $filename")
                    val file = File(getPixivCacheDir(), filename)
                    if (file.exists()) file.delete()
                    PixivSourceDatabase.instance(context).hideDao()
                        .upsert(filename.split(".")[0])
                        .let { Log.d(TAG, "添加：$it") }
                }
            }
        delete(contentUri, "token like ?", arrayOf("$ilustid%")).let { Log.d(TAG, "刪除隱藏：$it") }
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

    private fun hideImage(artwork: Artwork) {
        val filename = artwork.persistentUri.toString().split("/").last()
        val file = File(getPixivCacheDir(), filename)
        if (file.exists()) file.delete()
        delete(contentUri, "token=?", arrayOf(artwork.token))
        PixivSourceDatabase.instance(context).hideDao().upsert(artwork.token!!)
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
