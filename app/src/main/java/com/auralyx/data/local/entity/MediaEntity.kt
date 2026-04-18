package com.auralyx.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.auralyx.domain.model.MediaItem

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["path"], unique = true)]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String? = null,
    @ColumnInfo(name = "is_ad17") val isAD17: Boolean = false,
    @ColumnInfo(name = "folder_id") val folderId: Long = 0,
    @ColumnInfo(name = "folder_name") val folderName: String = "",
    @ColumnInfo(name = "date_added") val dateAdded: Long = 0,
    val size: Long = 0,
    @ColumnInfo(name = "track_number") val trackNumber: Int = 0,
    @ColumnInfo(name = "last_played") val lastPlayed: Long = 0
) {
    fun toDomain() = MediaItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        albumArtUri = albumArtUri,
        isAD17 = isAD17,
        folderId = folderId,
        folderName = folderName,
        dateAdded = dateAdded,
        size = size,
        trackNumber = trackNumber
    )

    companion object {
        fun fromDomain(item: MediaItem) = MediaEntity(
            id = item.id,
            title = item.title,
            artist = item.artist,
            album = item.album,
            duration = item.duration,
            path = item.path,
            albumArtUri = item.albumArtUri,
            isAD17 = item.isAD17,
            folderId = item.folderId,
            folderName = item.folderName,
            dateAdded = item.dateAdded,
            size = item.size,
            trackNumber = item.trackNumber
        )
    }
}
