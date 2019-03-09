package one.oktw.muzeipixivsource.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.util.FileUtil
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.ImageViewerPopupView
import com.lxj.xpopup.interfaces.OnSrcViewUpdateListener
import com.lxj.xpopup.interfaces.XPopupImageLoader
import kotlinx.android.synthetic.main.cell_layout.view.*


class IllustAdapter(
    val context: Context,
    private val imageInfos: java.util.ArrayList<Any>
) : RecyclerView.Adapter<IllustAdapter.OneViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): OneViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.cell_layout, viewGroup, false)
        return OneViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: IllustAdapter.OneViewHolder, position: Int) {
        val imageInfo = imageInfos[position] as IllustAdapter.ImageInfo
        val newWidth = context.resources.displayMetrics.widthPixels / 2
        val newHeight = newWidth * imageInfo.height / imageInfo.width
        viewHolder.view.layoutParams.height = newHeight
        viewHolder.view.layoutParams.width = newWidth
        viewHolder.setData(imageInfo.uri)

        val imageView = viewHolder.view.findViewById<ImageView>(R.id.my_image_view)

        viewHolder.view.setOnClickListener {
            View.OnClickListener {
                Log.d("XXX", "clicked!!!!!!!!!!")
                XPopup.get(context).asImageViewer(imageView, position, imageInfos, OnSrcViewUpdateListener { popupView, position ->
                    popupView.updateSrcView(imageView)
                }, ImageLoader()).show()
            }
        }
        viewHolder.view.setOnTouchListener { v, event ->
            false
        }

    }


    override fun getItemCount(): Int {
        return imageInfos.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class OneViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun setData(uri: Uri) {
            Glide.with(view).load(uri)
//                .apply(RequestOptions().override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL))
                .into(view.findViewById(R.id.my_image_view))
        }
    }

    data class ImageInfo(
        val height: Int,
        val width: Int,
        val uri: Uri
    )
}

class ImageLoader : XPopupImageLoader {
    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        Glide.with(imageView).load(uri).apply(RequestOptions().override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)).into(imageView)
    }

}
