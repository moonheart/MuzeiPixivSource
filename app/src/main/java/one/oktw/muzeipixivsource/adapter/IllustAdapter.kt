package one.oktw.muzeipixivsource.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.apps.muzei.api.provider.Artwork
import kotlinx.coroutines.GlobalScope
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.util.FileUtil
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.facebook.drawee.view.SimpleDraweeView
import java.io.File
import java.io.FileInputStream
import java.lang.Exception


class IllustAdapter(
    context: Context,
    private val galleryList: ArrayList<Artwork>
) : RecyclerView.Adapter<IllustAdapter.OneViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val fileUtil: FileUtil = FileUtil(context)
    val uris: Array<Uri?> = arrayOfNulls<Uri?>(galleryList.size)
    val map: HashMap<Uri, ImageInfo> = HashMap()


    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): OneViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.cell_layout, viewGroup, false)
        return OneViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: IllustAdapter.OneViewHolder, i: Int) {
//        Log.d("IllustAdapter", "onBindViewHolder: $i")
        GlobalScope.launch {
            val uri = if (uris[i] == null) {
                uris[i] = fileUtil.openFile(galleryList[i])
                uris[i]
            } else {
                uris[i]
            }
            val imgInfo = if (map.containsKey(uri)) {
                map[uri]
            } else {
                val fis = FileInputStream(uri!!.path)
                val bitmap = BitmapFactory.decodeStream(fis)
                map[uri] = ImageInfo(bitmap.height, bitmap.width, uri)
                map[uri]
            }
            viewHolder.setData(imgInfo!!)
        }
    }


    override fun getItemCount(): Int {
        return galleryList.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class OneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivImage: SimpleDraweeView = view.findViewById<View>(R.id.my_image_view) as SimpleDraweeView

        init {
            //            val width = (ivImage.context as Activity).windowManager.defaultDisplay.width
//            ivImage.layoutParams.width = width / 3
//            ivImage.scaleType = ImageView.ScaleType.CENTER
//            ivImage.layoutParams.height = 100

        }

        fun setData(img: ImageInfo) {
            val width = (ivImage.context as Activity).windowManager.defaultDisplay.width
            val newWidth = width / 3
            ivImage.layoutParams.width = newWidth
//                    ivImage.layoutParams.height = newWidth
            ivImage.layoutParams.height = newWidth * img.height / img.width
            Handler(Looper.getMainLooper()).post {
                try {

                    ivImage.setImageURI(img.uri)
//
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


    }

    data class ImageInfo(
        val height: Int,
        val width: Int,
        val uri: Uri
    )
}
