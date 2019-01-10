package one.oktw.muzeipixivsource.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

/**
 * 作品的灰色像素比例
 */
@Entity(tableName = "image_grayscale")
class ImageGrayscale(
    /**
     * 作品ID (格式：87436545_p1)
     */
    @PrimaryKey
    @ColumnInfo(name = "illust_id")
    val IllustId: String,
    /**
     * 灰色像素比例 (0,1)
     */
    @NotNull
    @ColumnInfo(name = "greyscale_value")
    val GreyscaleValue: Float
)
