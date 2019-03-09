package one.oktw.muzeipixivsource.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionButton
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionHelper
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionLayout
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RFACLabelItem
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RapidFloatingActionContentLabelList
import com.wangjie.rapidfloatingactionbutton.util.RFABTextUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.adapter.IllustAdapter
import one.oktw.muzeipixivsource.util.FileUtil
import one.oktw.muzeipixivsource.util.IllustUtil
import java.io.FileInputStream
import java.util.*

class MainActivity : AppCompatActivity(), RapidFloatingActionContentLabelList.OnRapidFloatingActionContentLabelListListener<Int> {


    lateinit var mRecyclerView: RecyclerView
    lateinit var rfaLayout: RapidFloatingActionLayout
    lateinit var rfaButton: RapidFloatingActionButton
    lateinit var rfabHelper: RapidFloatingActionHelper
    lateinit var illustAdapter: IllustAdapter
    lateinit var illustUtil: IllustUtil


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        illustUtil = IllustUtil(applicationContext)
        rfaLayout = findViewById(R.id.label_list_sample_rfal)
        rfaButton = findViewById(R.id.label_list_sample_rfab)

        setupFloatButton()

        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.setHasFixedSize(true)

        val layoutManager = CustomStaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            .apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
            }
        mRecyclerView.layoutManager = layoutManager

        val list = java.util.ArrayList<Any>()
        illustAdapter = IllustAdapter(this,mRecyclerView, layoutManager, list)
        mRecyclerView.adapter = illustAdapter

        updateList()
    }

    private fun updateList() {
        GlobalScope.launch {

            val fileUtil = FileUtil(applicationContext)

            illustAdapter.imageInfos.removeAll { true }
            mRecyclerView.post {
                illustAdapter.notifyDataSetChanged()
            }
            val allArtworks = illustUtil.getAllArtworks()
            allArtworks.forEach {
                launch {
                    val uri = fileUtil.openFile(it)
                    val fis = FileInputStream(uri.path)
                    val bitmap = BitmapFactory.decodeStream(fis)
                    val imageInfo = IllustAdapter.ImageInfo(bitmap.height, bitmap.width, uri, it)
                    illustAdapter.imageInfos.add(imageInfo)
                    val position = illustAdapter.imageInfos.size - 1
                    mRecyclerView.post {
                        illustAdapter.notifyItemInserted(position)
                    }
                }
            }
        }
    }

    override fun onRFACItemLabelClick(position: Int, item: RFACLabelItem<Int>?) {
        onRFACItemLabelClick(item)
    }

    override fun onRFACItemIconClick(position: Int, item: RFACLabelItem<Int>?) {
        onRFACItemLabelClick(item)
    }

    private fun onRFACItemLabelClick(item: RFACLabelItem<Int>?) {
        when (item?.wrapper) {
            0 -> updateList()
            1 -> GlobalScope.launch {
                    illustUtil.fetchNewIllust()
                    updateList()
                }
            2 -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        rfabHelper.toggleContent()
    }

    private fun setupFloatButton() {
        val rfaContent = RapidFloatingActionContentLabelList(this)
        rfaContent.setOnRapidFloatingActionContentLabelListListener(this)
        val items = ArrayList<RFACLabelItem<*>>()
        items.add(RFACLabelItem<Int>().apply {
            label = "刷新列表"
            resId = R.drawable.ic_baseline_refresh_24px
            wrapper = 0
        })
        items.add(RFACLabelItem<Int>().apply {
            label = "抓取新图片"
            resId = R.drawable.ic_baseline_arrow_downward_24px
            wrapper = 1
        })
        items.add(RFACLabelItem<Int>().apply {
            label = "打开设置"
            resId = R.drawable.ic_baseline_settings_20px
            wrapper = 2
        })

        rfaContent
            .setItems(items)
            .setIconShadowRadius(RFABTextUtil.dip2px(this, 5f))
            .setIconShadowColor(-0x777778)
            .setIconShadowDy(RFABTextUtil.dip2px(this, 5f))

        rfabHelper = RapidFloatingActionHelper(
            this,
            rfaLayout,
            rfaButton,
            rfaContent
        ).build()
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
