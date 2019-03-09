package one.oktw.muzeipixivsource.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import kotlinx.coroutines.runBlocking
import one.oktw.muzeipixivsource.BuildConfig
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.db.PixivSourceDatabase
import one.oktw.muzeipixivsource.db.dao.upsert
import one.oktw.muzeipixivsource.pixiv.Pixiv
import one.oktw.muzeipixivsource.pixiv.PixivOAuth
import one.oktw.muzeipixivsource.pixiv.mode.RankingCategory
import one.oktw.muzeipixivsource.pixiv.model.Illust
import org.jsoup.Jsoup
import java.io.File

class IllustUtil(
    val context: Context
) {
    companion object {
        val TAG = "IllustUtil"
    }

    val contentResolver: ContentResolver = context.contentResolver;
    val contentUri = ProviderContract.getContentUri(BuildConfig.APPLICATION_ID)
    val preference: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val providerClient = ProviderContract.getProviderClient(context, BuildConfig.APPLICATION_ID)

    val fileUtil = FileUtil(context)

    /**
     * 获取当前所有图片
     */
    fun getAllArtworks(): List<Artwork> {
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        cursor.use {
            return generateSequence { if (it.moveToNext()) cursor else null }
                .map { Artwork.fromCursor(it) }
                .toList();
        }
    }

    /**
     * 获取新图片
     */
    suspend fun fetchNewIllust() {
        runBlocking { updateToken() }
        val token: String? = preference.getString(SettingsFragment.KEY_PIXIV_ACCESS_TOKEN, null)
        val fallback = preference.getBoolean(SettingsFragment.KEY_FETCH_FALLBACK, false)
        val pixiv = Pixiv(token = token, number = preference.getInt(SettingsFragment.KEY_FETCH_NUMBER, 30))

        try {
            when (if (token == null) SettingsFragment.FETCH_MODE_FALLBACK else preference.getString(SettingsFragment.KEY_FETCH_MODE, "0")!!.toInt()) {
                SettingsFragment.FETCH_MODE_FALLBACK -> publish(pixiv.getFallback())
                SettingsFragment.FETCH_MODE_RECOMMEND -> publish(pixiv.getRecommend())
                SettingsFragment.FETCH_MODE_RANKING -> pixiv.getRanking(
                    RankingCategory.valueOf(preference.getString(SettingsFragment.KEY_FETCH_MODE_RANKING, RankingCategory.Monthly.name)!!)
                ).let { publish(it) }

                SettingsFragment.FETCH_MODE_BOOKMARK -> pixiv.getBookmark(
                    preference.getInt(SettingsFragment.KEY_PIXIV_USER_ID, -1),
                    preference.getBoolean(SettingsFragment.KEY_FETCH_MODE_BOOKMARK, false)
                ).let { publish(it) }
            }
        } catch (e1: Exception) {
            // TODO better except handle
            Log.e("fetch", "fetch update error", e1)

            try {
                if (fallback) publish(pixiv.getFallback()) else throw e1
            } catch (e2: Exception) {
                Log.e("fetch", "fetch update fallback error", e2)
                throw e2
            }
        }
    }

    /**
     * 更新token
     */
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

    /**
     * 发布图片
     */
    private suspend fun publish(list: ArrayList<Illust>) {
        val cleanHistory = preference.getBoolean(SettingsFragment.KEY_FETCH_CLEANUP, true)
        val filterNSFW = preference.getBoolean(SettingsFragment.KEY_FILTER_SAFE, true)
        val random = preference.getBoolean(SettingsFragment.KEY_FETCH_RANDOM, false)
        val filterSize = preference.getInt(SettingsFragment.KEY_FILTER_SIZE, 0)
        val originImage = if (filterSize > 1200) true else preference.getBoolean(SettingsFragment.KEY_FETCH_ORIGIN, false)
        val minView = preference.getInt(SettingsFragment.KEY_FILTER_VIEW, 0)
        val minBookmark = preference.getInt(SettingsFragment.KEY_FILTER_BOOKMARK, 0)
        val allPage = preference.getBoolean(SettingsFragment.KEY_FETCH_ALL_PAGE, false)

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
            val delete = contentResolver.delete(contentUri, null, null)
            Log.d(TAG, "删除历史：$delete")
        }
        if (random) artworkList.shuffle()
        Log.d(TAG, "正在添加数量：${artworkList.size}")
        artworkList.forEach { providerClient.addArtwork(it) }
        Log.d(TAG, "添加完成")
    }

    /**
     * 重载图片
     */
    suspend fun reDownloadImage(artwork: Artwork) {
        val filename = artwork.persistentUri.toString().split("/").last()
        val file = File(fileUtil.getPixivCacheDir(), filename)
        if (file.exists()) file.delete()
        fileUtil.openFile(artwork, true)
    }

    /**
     * 将作品加入隐藏列表
     */
    suspend fun hideIllust(artwork: Artwork): List<Artwork> {
        val illustId = "\\d+_".toRegex().find(artwork.token!!)!!.value
        Log.d(TAG, "隱藏作品 $illustId")

        val selection = "token like ?";
        val selectionArgs = arrayOf("$illustId%")

        val cursor = contentResolver.query(contentUri, null, selection, selectionArgs, null)
        val list = cursor.use { generateSequence { if (it.moveToNext()) it else null }.map { Artwork.fromCursor(it) }.toList() }
        list.forEach {
            val filename = it.persistentUri.toString().split("/").last()
            Log.d(TAG, "隱藏圖片： $filename")
            val file = File(fileUtil.getPixivCacheDir(), filename)
            if (file.exists()) file.delete()
            PixivSourceDatabase.instance(context).hideDao()
                .upsert(filename.split(".")[0])
                .let { Log.d(TAG, "添加隱藏圖片：$it") }
        }

        contentResolver.delete(contentUri, selection, selectionArgs).let { Log.d(TAG, "刪除隱藏：$it") }
        return list
    }

    /**
     * 将图片加入隐藏列表
     */
    suspend fun hideImage(artwork: Artwork) {
        val filename = artwork.persistentUri.toString().split("/").last()
        val file = File(fileUtil.getPixivCacheDir(), filename)
        if (file.exists()) file.delete()
        contentResolver.delete(contentUri, "token=?", arrayOf(artwork.token))
        PixivSourceDatabase.instance(context).hideDao().upsert(artwork.token!!)
    }

    /**
     * 分享图片
     */
    fun shareImage(artwork: Artwork) {
        val file = fileUtil.getFileForIllust(artwork.persistentUri!!)

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context!!, "${BuildConfig.APPLICATION_ID}.fileprovider", file))
            type = "image/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, context?.getString(R.string.share_to)))
    }

    /**
     * 分享作品链接
     */
    fun shareUrl(artwork: Artwork) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${artwork.title} | ${artwork.byline} | ${artwork.webUri.toString()}")
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(sendIntent, context?.getString(R.string.share_to)))
    }
}
