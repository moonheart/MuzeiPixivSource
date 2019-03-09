package one.oktw.muzeipixivsource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import one.oktw.muzeipixivsource.db.model.HideImage

@Dao
interface HideImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hideImage: HideImage): Long

    @Query("select * from hide_image where illust_id in (:illustIds)")
    suspend fun getList(illustIds: Array<String>): Array<HideImage>

    @Query("select * from hide_image")
    suspend fun getList():Array<HideImage>
}

suspend fun HideImageDao.upsert(illustId: String): Long {
    return insert(HideImage(illustId))
}
