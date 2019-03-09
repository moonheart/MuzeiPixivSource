package one.oktw.muzeipixivsource.activity

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.apps.muzei.api.provider.Artwork
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.adapter.IllustAdapter
import one.oktw.muzeipixivsource.util.FileUtil
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.setHasFixedSize(true)

        val list = java.util.ArrayList<Any>()
        val illustAdapter = IllustAdapter(applicationContext, list)
        mRecyclerView.adapter = illustAdapter
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            .apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        mRecyclerView.layoutManager = layoutManager

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
                        val position = list.size
                        list.add(imageInfo)
                        mRecyclerView.post {
                            illustAdapter.notifyItemRemoved(position)
                            illustAdapter.notifyItemInserted(position)
                        }
                    }
                }
            }
        }
    }


}
