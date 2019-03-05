//package one.oktw.muzeipixivsource.view
//
//import android.content.Context
//import android.util.AttributeSet
//import android.widget.GridView
//
//class CustomGridView (context: Context): GridView(context) {
//
//    var isOnMeasure:Boolean = false
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        isOnMeasure = true
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//    }
//
//    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
//        isOnMeasure = false
//        super.onLayout(changed, l, t, r, b)
//    }
//}
