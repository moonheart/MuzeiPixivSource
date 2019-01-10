package one.oktw.muzeipixivsource.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hide_image")
class HideImage(
    @PrimaryKey
    @ColumnInfo(name = "illust_id")
    val IllustId: String
)
