package one.oktw.muzeipixivsource.activity

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.ProviderContract
import one.oktw.muzeipixivsource.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import one.oktw.muzeipixivsource.adapter.IllustAdapter
import one.oktw.muzeipixivsource.util.DensityUtil
import one.oktw.muzeipixivsource.util.GridItemDecoration
import kotlin.coroutines.CoroutineContext

class MainActivity: AppCompatActivity(), CoroutineScope {

    lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fresco.initialize(this)
        setContentView(R.layout.activity_main)

        mRecyclerView = findViewById(R.id.recyclerView)

        initView()
        initData()

        mRecyclerView.setHasFixedSize(true)

        job = Job()

        val layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
            .apply {
//            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        }
//        val layoutManager = GridLayoutManager(applicationContext, 3)
        mRecyclerView.layoutManager = layoutManager
        mRecyclerView.itemAnimator = null
//        mRecyclerView.addItemDecoration(GridItemDecoration(
//            2, DensityUtil.dip2px(this, 4.0f), false))

//        val projections = arrayOf(ProviderContract.Artwork.TITLE, ProviderContract.Artwork.PERSISTENT_URI)
        var cursor = contentResolver.query(Uri.parse("content://one.oktw.muzeipixivsource"), null, null, null, null)
        val list = ArrayList<Artwork>()
        while(cursor.moveToNext())
        {
            if(cursor!=null)
            list.add(Artwork.fromCursor(cursor))
        }

        val illustAdapter = IllustAdapter(applicationContext, list)
        mRecyclerView.adapter = illustAdapter


    }

    fun initView(){

    }

    fun initData(){

    }


}
