package one.oktw.muzeipixivsource.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import one.oktw.muzeipixivsource.db.dao.HideImageDao
import one.oktw.muzeipixivsource.db.dao.ImageGrayscaleDao
import one.oktw.muzeipixivsource.db.model.HideImage
import one.oktw.muzeipixivsource.db.model.ImageGrayscale

@Database(entities = arrayOf(ImageGrayscale::class, HideImage::class), version = 2)
abstract class PixivSourceDatabase:RoomDatabase() {

    companion object {

        private var db:PixivSourceDatabase? = null

        fun instance(context: Context):PixivSourceDatabase {
            synchronized(this){
                if (db == null) {
                    synchronized(this) {
                        db = Room.databaseBuilder(
                            context,
                            PixivSourceDatabase::class.java,
                            "pixiv_source")
//                            .fallbackToDestructiveMigration()
//                            .setJournalMode(JournalMode.TRUNCATE)
                            .addMigrations(object :Migration(1,2){
                                override fun migrate(database: SupportSQLiteDatabase) {
                                    database.execSQL("CREATE TABLE IF NOT EXISTS `hide_image` (`illust_id` TEXT PRIMARY KEY Not null)")
                                }
                            })
                            .build()
                    }
                }
            }
            return db!!
        }
    }
    abstract fun imageDao(): ImageGrayscaleDao
    abstract fun hideDao(): HideImageDao
}


