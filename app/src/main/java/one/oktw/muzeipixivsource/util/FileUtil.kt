package one.oktw.muzeipixivsource.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.net.toUri
import com.google.android.apps.muzei.api.provider.Artwork
import okhttp3.*
import one.oktw.muzeipixivsource.db.PixivSourceDatabase
import one.oktw.muzeipixivsource.db.dao.ImageGrayscaleDao
import one.oktw.muzeipixivsource.db.dao.upsert
import one.oktw.muzeipixivsource.provider.MuzeiProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class FileUtil(
    val context: Context
) {
    companion object {
        val TAG: String = "FileUtil"
    }

    private val httpClient = OkHttpClient()


    private val imageGreyscaleDao: ImageGrayscaleDao
        get() = PixivSourceDatabase.instance(context).imageDao()

    fun getFileForIllust(uri: Uri): File {
        val filename = uri.toString().split("/").last()
        val file = File(getPixivCacheDir(), filename)
        return file
    }

    fun getPixivCacheDir(): File {
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

    suspend fun openFile(provider: MuzeiProvider, artwork: Artwork, filterGrwyscale: Boolean): InputStream {

        val greyValue = if (filterGrwyscale) imageGreyscaleDao.getGreyscaleValue(artwork.token!!) else null
        Log.d(TAG, "filterGrwyscale: $filterGrwyscale, greyValue: $greyValue")

        if (filterGrwyscale &&
            greyValue != null &&
            checkGreyValue(greyValue)) {
            hideImage(provider, artwork)
            throw Exception("跳过灰色图片")
        }

        val filename = artwork.persistentUri.toString().split("/").last()
        Log.d(TAG, "打开文件：$filename")
        val file = getFileForIllust(artwork.persistentUri!!)
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
        if (filterGrwyscale) {
            if (greyValue == null) {
                if (CheckIsGreyScale(artwork.token, file)) {
                    file.delete()
                    hideImage(provider, artwork)
                    throw Exception("跳过灰色图片")
                }
            } else {

            }
        }

        return file.inputStream()
    }

    suspend fun openFile(artwork: Artwork, force: Boolean = false): Uri {
        val filename = artwork.persistentUri.toString().split("/").last()
        Log.d(TAG, "打开文件：$filename")
        val file = getFileForIllust(artwork.persistentUri!!)
        if (force || !file.exists()) {
            val tmpFile = File(getPixivCacheDir(), "$filename.tmp")
            downloadFile(artwork.persistentUri.toString(), tmpFile)
            tmpFile.renameTo(file)
        }

        return file.toUri()
    }

    private suspend fun downloadFile(url: String, tmpFile: File) = suspendCoroutine<Int> {
        Request.Builder()
            .url(url)
            .header("Referer", "https://app-api.pixiv.net/")
            .build()
            .let(httpClient::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.body()!!.byteStream().use { bs ->
                        FileOutputStream(tmpFile).use { tf ->
                            bs.copyTo(tf)
                            it.resume(1)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) = it.resumeWithException(e)

            })
    }

    private fun checkGreyValue(percent: Float): Boolean {
        return percent > 0.9
    }

    private suspend fun hideImage(provider: MuzeiProvider, artwork: Artwork) {
        val filename = artwork.persistentUri.toString().split("/").last()
        val file = File(getPixivCacheDir(), filename)
        if (file.exists()) file.delete()
        provider.delete(provider.contentUri, "token=?", arrayOf(artwork.token))
        PixivSourceDatabase.instance(provider.context).hideDao().upsert(artwork.token!!)
    }

    private suspend fun CheckIsGreyScale(token: String?, file: File): Boolean {
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
}
