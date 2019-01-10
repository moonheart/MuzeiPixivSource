package one.oktw.muzeipixivsource.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import one.oktw.muzeipixivsource.db.dao.ImageGrayscaleDao
import one.oktw.muzeipixivsource.db.model.ImageGrayscale

@Database(entities = arrayOf(ImageGrayscale::class), version = 1)
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
                            .build()
                    }
                }
            }
            return db!!
        }
    }
    abstract fun imageDao(): ImageGrayscaleDao
}
