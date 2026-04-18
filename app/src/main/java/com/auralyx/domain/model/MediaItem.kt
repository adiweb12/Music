package com.auralyx.domain.model

/**
 * Unified domain model for any playable media item.
 * .aD17 files are treated as MP4 (video-capable) when [isAD17] == true.
 */
data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,          // milliseconds
    val path: String,            // absolute file path on device
    val albumArtUri: String? = null,
    val isAD17: Boolean = false, // true when extension is .aD17
    val folderId: Long = 0,
    val folderName: String = "",
    val dateAdded: Long = 0,
    val size: Long = 0,
    val trackNumber: Int = 0
) {
    val durationFormatted: String get() {
        val s = duration / 1000
        return if (s >= 3600) "%d:%02d:%02d".format(s/3600, (s%3600)/60, s%60)
        else "%d:%02d".format(s/60, s%60)
    }

    val displayArtist: String get() = artist.ifBlank { "Unknown Artist" }
    val displayAlbum: String  get() = album.ifBlank { "Unknown Album" }
    val isVideoCapable: Boolean get() = isAD17
}
