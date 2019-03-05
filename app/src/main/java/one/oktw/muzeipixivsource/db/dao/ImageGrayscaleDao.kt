package one.oktw.muzeipixivsource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import one.oktw.muzeipixivsource.db.model.ImageGrayscale

@Dao
interface ImageGrayscaleDao {

    @Query("select greyscale_value from image_grayscale where illust_id=:illustId")
    fun getGreyscaleValue(illustId:String):Float?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(g: ImageGrayscale):Long

}
fun ImageGrayscaleDao.upsert(token:String, percent:Float):Long{
    return insert(ImageGrayscale(token, percent))
}
