package one.oktw.muzeipixivsource.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.XPopupImageLoader
import one.oktw.muzeipixivsource.R


class IllustAdapter(
    val context: Context,
    val layoutManager: StaggeredGridLayoutManager,
    private val imageInfos: java.util.ArrayList<Any>
) : RecyclerView.Adapter<IllustAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.cell_layout, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: IllustAdapter.ViewHolder, position: Int) {
        val imageInfo = imageInfos[position] as IllustAdapter.ImageInfo
        val newWidth = context.resources.displayMetrics.widthPixels / 2
        val newHeight = newWidth * imageInfo.height / imageInfo.width
        viewHolder.view.layoutParams.height = newHeight
        viewHolder.view.layoutParams.width = newWidth
        viewHolder.setData(imageInfo.uri)
    }


    override fun getItemCount(): Int {
        return imageInfos.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        var imageView: ImageView = view.findViewById(R.id.my_image_view);

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            XPopup.get(context)
                .asImageViewer(imageView, (imageInfos[adapterPosition] as ImageInfo).uri, ImageLoader())
                .show()
        }

        fun setData(uri: Uri) {
            Glide.with(view)
                .load(uri)
                .transition(GenericTransitionOptions.with(R.anim.item_alpha_in))
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
        try {
            Glide.with(imageView)
                .load(uri)
                .into(imageView)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}
