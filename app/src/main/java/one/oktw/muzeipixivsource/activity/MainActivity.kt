package one.oktw.muzeipixivsource.activity

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.apps.muzei.api.provider.Artwork
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.adapter.IllustAdapter
import one.oktw.muzeipixivsource.adapter.ImageLoader
import one.oktw.muzeipixivsource.util.FileUtil
import one.oktw.muzeipixivsource.util.ItemClickSupport
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.setHasFixedSize(true)

        val layoutManager = CustomStaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            .apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        mRecyclerView.layoutManager = layoutManager

        val list = java.util.ArrayList<Any>()
        val illustAdapter = IllustAdapter(this, layoutManager, list)
        mRecyclerView.adapter = illustAdapter

        GlobalScope.launch {
            val fileUtil = FileUtil(applicationContext)
            val cursor = contentResolver.query(Uri.parse("content://one.oktw.muzeipixivsource"), null, null, null, null)
            while (cursor.moveToNext()) {
                if (cursor != null) {
                    val artwork = Artwork.fromCursor(cursor)
                    launch {
                        val uri = fileUtil.openFile(artwork)
                        val fis = FileInputStream(uri.path)
                        val bitmap = BitmapFactory.decodeStream(fis)
                        val imageInfo = IllustAdapter.ImageInfo(bitmap.height, bitmap.width, uri)
                        list.add(imageInfo)
                        val position = list.size - 1
                        mRecyclerView.post {
                            illustAdapter.notifyItemInserted(position)
                        }
                    }
                }
            }
        }
    }

    class CustomStaggeredGridLayoutManager(spanCount: Int, orientation: Int) : StaggeredGridLayoutManager(spanCount, orientation) {
        override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {

            }
        }
    }

}
