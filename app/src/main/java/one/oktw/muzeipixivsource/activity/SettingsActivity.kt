package one.oktw.muzeipixivsource.activity

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import one.oktw.muzeipixivsource.R
import one.oktw.muzeipixivsource.activity.fragment.SettingsFragment
import one.oktw.muzeipixivsource.util.AppUtil.Companion.checkInstalled
import one.oktw.muzeipixivsource.util.AppUtil.Companion.viewMarket

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val MUZEI_PACKAGE = "net.nurik.roman.muzei"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verifyStoragePermissions()

        // if click update version notification
        intent.getStringExtra("new_version")?.let {
            startActivity(Intent(ACTION_VIEW, Uri.parse(it)))
            finish()
        }

        // check muzei installed
        if (!checkInstalled(this, MUZEI_PACKAGE)) {
            AlertDialog.Builder(this)
                .setMessage(R.string.muzei_not_install)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    startActivity(viewMarket(MUZEI_PACKAGE))
                }
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(false)
                .show()
        }

        // Only create new fragment on first create activity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE")

    fun verifyStoragePermissions() {

        try {
            //检测是否有写的权限
            val permission = ActivityCompat.checkSelfPermission(this,
                "android.permission.WRITE_EXTERNAL_STORAGE")
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}
