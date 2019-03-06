package one.oktw.muzeipixivsource.activity

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
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
import one.oktw.muzeipixivsource.util.DensityUtil
import one.oktw.muzeipixivsource.util.GridItemDecoration
import kotlin.coroutines.CoroutineContext

class MainActivity: AppCompatActivity(), CoroutineScope {

    lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    lateinit var mRecyclerView: RecyclerView

//    lateinit var gridView: GridView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val config = ImagePipelineConfig.newBuilder(applicationContext)
//            .setDownsampleEnabled(true)
//            .build()

//        Fresco.initialize(applicationContext, config)
        setContentView(R.layout.activity_main)

//        gridView = findViewById(R.id.grid_view)
        mRecyclerView = findViewById(R.id.recyclerView)

        initView()
        initData()

        mRecyclerView.setHasFixedSize(true)

        job = Job()

        var cursor = contentResolver.query(Uri.parse("content://one.oktw.muzeipixivsource"), null, null, null, null)
        val list = ArrayList<Artwork>()
        while(cursor.moveToNext())
        {
            if(cursor!=null)
            list.add(Artwork.fromCursor(cursor))
        }

    val illustAdapter = IllustAdapter(applicationContext, list)
//        val illustAdapter = IllustAdapter3(applicationContext, list)
//        gridView.adapter = illustAdapter
        mRecyclerView.adapter = illustAdapter

        val layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
            .apply {
            }
        mRecyclerView.layoutManager = layoutManager

    }

    fun initView(){

    }

    fun initData(){

    }


}
