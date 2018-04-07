package one.oktw.muzeipixivsource.pixiv.mode

import com.google.android.apps.muzei.api.RemoteMuzeiArtSource
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import one.oktw.muzeipixivsource.pixiv.model.Illust
import one.oktw.muzeipixivsource.pixiv.model.IllustList
import java.util.*

class Bookmark(private val token: String, private val user: Int, private val private: Boolean) {
    fun getImage() = getList().let { it[Random().nextInt(it.size)] }

    fun getImage(index: Int) = getList()[index]

    private fun getList(): ArrayList<Illust> {
        val list = ArrayList<Illust>()
        var url = "https://app-api.pixiv.net/v1/user/bookmarks/illust?" +
                "user_id=$user&" +
                "restrict=${if (private) "private" else "public"}"

        for (i in 0..2) {
            val res = request(url) ?: throw RemoteMuzeiArtSource.RetryException()

            if (res.nextUrl != null) url = res.nextUrl else break

            list += res.illusts
        }

        return list
    }

    private fun request(url: String): IllustList? {
        val httpClient = OkHttpClient()

        return Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()
            .let(httpClient::newCall)
            .execute()
            .body()?.let {
                GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                    .fromJson<IllustList>(it.charStream(), IllustList::class.java)
            }
    }
}