//package one.oktw.muzeipixivsource.adapter
//
//import com.chad.library.adapter.base.BaseQuickAdapter
//import com.chad.library.adapter.base.BaseViewHolder
//import com.google.android.apps.muzei.api.provider.Artwork
//import one.oktw.muzeipixivsource.R
//import one.oktw.muzeipixivsource.widgets.ScaleImageView
//
//
//class IllustAdapter2:BaseQuickAdapter<IllustAdapter.ImageInfo, BaseViewHolder>(R.layout.cell_layout){
//
//
//    /**
//     * Implement this method and use the helper to adapt the view to the given item.
//     *
//     * @param helper A fully initialized helper.
//     * @param item   The item that needs to be displayed.
//     */
//    override fun convert(helper: BaseViewHolder, item: IllustAdapter.ImageInfo) {
//        val imageView = helper.getView<ScaleImageView>(R.id.girl_item_iv)
//        imageView.setInitSize(item.width, item.height)
//
//    }
//
//}
