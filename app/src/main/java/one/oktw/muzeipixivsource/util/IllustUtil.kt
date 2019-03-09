package one.oktw.muzeipixivsource.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.apps.muzei.api.provider.Artwork
import one.oktw.muzeipixivsource.BuildConfig
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.db.PixivSourceDatabase
import one.oktw.muzeipixivsource.db.dao.upsert
import java.io.File

class IllustUtil(
    val context: Context
) {
    companion object {
        val TAG = "IllustUtil"
    }

    val contentResolver: ContentResolver = context.contentResolver;
    val contentUri = Uri.parse("content://one.oktw.muzeipixivsource")
    val fileUtil = FileUtil(context)

    fun getAllArtworks(): List<Artwork> {
        val cursor = contentResolver.query(contentUri, null, null, null, null)
        cursor.use {
            return generateSequence { if (it.moveToNext()) cursor else null }
                .map { Artwork.fromCursor(it) }
                .toList();
        }
    }

    /**
     * 将图片加入隐藏列表
     */
    fun hideIllust(artwork: Artwork) {
        val illustId = "\\d+_".toRegex().find(artwork.token!!)!!.value
        Log.d(TAG, "隱藏作品 $illustId")

        val selection = "token like ?";
        val selectionArgs = arrayOf("$illustId%")

        contentResolver.query(contentUri, arrayOf("persistent_uri"), selection, selectionArgs, null)
            .use {
                while (it.moveToNext()) {
                    val token = it.getString(0)
                    val filename = token.split("/").last()
                    Log.d(TAG, "隱藏圖片： $filename")
                    val file = File(fileUtil.getPixivCacheDir(), filename)
                    if (file.exists()) file.delete()
                    PixivSourceDatabase.instance(context).hideDao()
                        .upsert(filename.split(".")[0])
                        .let { Log.d(TAG, "添加隱藏圖片：$it") }
                }
            }
        contentResolver.delete(contentUri, selection, selectionArgs).let { Log.d(TAG, "刪除隱藏：$it") }
    }

    fun hideImage(artwork: Artwork) {
        val filename = artwork.persistentUri.toString().split("/").last()
        val file = File(fileUtil.getPixivCacheDir(), filename)
        if (file.exists()) file.delete()
        contentResolver.delete(contentUri, "token=?", arrayOf(artwork.token))
        PixivSourceDatabase.instance(context).hideDao().upsert(artwork.token!!)
    }

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
