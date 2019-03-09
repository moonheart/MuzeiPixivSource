package one.oktw.muzeipixivsource.activity

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.GridView
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import one.oktw.muzeipixivsource.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import one.oktw.muzeipixivsource.adapter.IllustAdapter
import one.oktw.muzeipixivsource.adapter.IllustAdapter3
import one.oktw.muzeipixivsource.util.FileUtil
import one.oktw.muzeipixivsource.util.GridItemDecoration
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

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
                    val uri = fileUtil.openFile(Artwork.fromCursor(cursor))
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
