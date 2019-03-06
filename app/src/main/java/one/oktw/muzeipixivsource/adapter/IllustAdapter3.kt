package one.oktw.muzeipixivsource.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.android.apps.muzei.api.provider.Artwork
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.util.FileUtil
import one.oktw.muzeipixivsource.view.CustomGridView

class IllustAdapter3(val context: Context,
                     val artworkList: ArrayList<Artwork>) : BaseAdapter() {

    val fileUtil: FileUtil = FileUtil(context)

    val map: HashMap<Int, View> = HashMap()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        var view = convertView
        val vh: ViewHolder
        Log.d("XXX", "view == null: ${view == null}")
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.cell_layout, parent, false)!!
            vh = ViewHolder(view.findViewById(R.id.my_image_view))
            val uri = fileUtil.openFile(artworkList[position])

//            val request = ImageRequestBuilder.newBuilderWithSource(uri)
//                .setResizeOptions(ResizeOptions.forDimensions(100,100))
//                .build();

//            vh.imageView.controller = Fresco.newDraweeControllerBuilder()
//                .setOldController(vh.imageView.controller)
//                .setImageRequest(request)
//                .build()
//            vh.imageView.setImageURI(uri)
            Glide.with(view).load(uri).into(vh.imageView)
            view.tag = vh
        } else {
            vh = view.tag as ViewHolder
        }

        if ((parent as CustomGridView).isOnMeasure) {
            return view
        }



        map[position] = view
        return view
    }

    override fun getItem(position: Int): Any {
        return artworkList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return artworkList.size
    }

    class ViewHolder(
        val imageView: ImageView
    )
}
