package com.auralyx.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.auralyx.data.local.dao.MediaDao
import com.auralyx.data.local.entity.MediaEntity
import com.auralyx.domain.model.Album
import com.auralyx.domain.model.Artist
import com.auralyx.domain.model.Folder
import com.auralyx.domain.model.MediaItem
import com.auralyx.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao
) : MediaRepository {

    // ── Flows from DB ────────────────────────────────────────────────────

    override fun getAllSongs() = mediaDao.getAllSongs().map { it.map { e -> e.toDomain() } }
    override fun getAllMusicVideos() = mediaDao.getAllMusicVideos().map { it.map { e -> e.toDomain() } }
    override fun getRecentlyPlayed() = mediaDao.getRecentlyPlayed().map { it.map { e -> e.toDomain() } }
    override fun searchAll(query: String) = mediaDao.search(query).map { it.map { e -> e.toDomain() } }

    override fun getSongsByAlbum(albumId: Long): Flow<List<MediaItem>> =
        mediaDao.getSongsByAlbum(albumId.toString()).map { it.map { e -> e.toDomain() } }

    override fun getSongsByArtist(artistId: Long): Flow<List<MediaItem>> =
        mediaDao.getSongsByArtist(artistId.toString()).map { it.map { e -> e.toDomain() } }

    override fun getSongsByFolder(folderId: Long): Flow<List<MediaItem>> =
        mediaDao.getSongsByFolder(folderId).map { it.map { e -> e.toDomain() } }

    // ── Albums / Artists / Folders are derived from songs ────────────────

    override fun getAllAlbums(): Flow<List<Album>> =
        mediaDao.getAllSongs().map { entities ->
            entities.groupBy { it.album }.map { (albumName, songs) ->
                val first = songs.first()
                Album(
                    id = albumName.hashCode().toLong(),
                    name = albumName.ifBlank { "Unknown Album" },
                    artist = first.artist.ifBlank { "Unknown Artist" },
                    artUri = first.albumArtUri,
                    songCount = songs.size
                )
            }.sortedBy { it.name }
        }

    override fun getAllArtists(): Flow<List<Artist>> =
        mediaDao.getAllSongs().map { entities ->
            entities.groupBy { it.artist }.map { (artistName, songs) ->
                val albums = songs.map { it.album }.distinct().size
                Artist(
                    id = artistName.hashCode().toLong(),
                    name = artistName.ifBlank { "Unknown Artist" },
                    artUri = songs.firstOrNull()?.albumArtUri,
                    albumCount = albums,
                    songCount = songs.size
                )
            }.sortedBy { it.name }
        }

    override fun getAllFolders(): Flow<List<Folder>> =
        mediaDao.getAllSongs().map { entities ->
            entities.groupBy { it.folderId }.map { (folderId, songs) ->
                Folder(
                    id = folderId,
                    name = songs.first().folderName.ifBlank { "Unknown" },
                    path = File(songs.first().path).parent ?: "",
                    songCount = songs.size
                )
            }.sortedBy { it.name }
        }

    // ── Storage scanning ─────────────────────────────────────────────────

    override suspend fun scanStorage() = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaEntity>()

        // Scan standard audio via MediaStore
        items += scanAudio()

        // Scan .aD17 files from external storage recursively
        items += scanAD17Files()

        mediaDao.deleteAll()
        mediaDao.insertAll(items)
    }

    private fun scanAudio(): List<MediaEntity> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.TRACK
        )

        val items = mutableListOf<MediaEntity>()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(
            uri, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 5000",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albIdCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val bucketCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
            val bucketNCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
            val dateCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val trackCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val albumId = cursor.getLong(albIdCol)
                val artUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                ).toString()

                items += MediaEntity(
                    title      = cursor.getString(titleCol) ?: "Unknown",
                    artist     = cursor.getString(artistCol) ?: "",
                    album      = cursor.getString(albumCol) ?: "",
                    duration   = cursor.getLong(durCol),
                    path       = cursor.getString(dataCol) ?: continue,
                    albumArtUri = artUri,
                    isAD17     = false,
                    folderId   = cursor.getLong(bucketCol),
                    folderName = cursor.getString(bucketNCol) ?: "",
                    dateAdded  = cursor.getLong(dateCol),
                    size       = cursor.getLong(sizeCol),
                    trackNumber = cursor.getInt(trackCol)
                )
            }
        }
        return items
    }

    /** Recursively scan external storage for .aD17 files */
    private fun scanAD17Files(): List<MediaEntity> {
        val items = mutableListOf<MediaEntity>()
        val externalDirs = context.getExternalFilesDirs(null)
            .mapNotNull { it?.parentFile?.parentFile?.parentFile?.parentFile } // /sdcard

        // Also include standard Music/Downloads directories
        val roots = externalDirs.toMutableList()
        android.os.Environment.getExternalStorageDirectory()?.let { roots += it }

        for (root in roots.distinct()) {
            if (root.exists()) scanDir(root, items)
        }
        return items
    }

    private fun scanDir(dir: File, accumulator: MutableList<MediaEntity>) {
        if (!dir.canRead()) return
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDir(file, accumulator)
                file.extension.lowercase() == "ad17" -> {
                    accumulator += MediaEntity(
                        title      = file.nameWithoutExtension,
                        artist     = "",
                        album      = "",
                        duration   = extractDuration(file.absolutePath),
                        path       = file.absolutePath,
                        albumArtUri = null,
                        isAD17     = true,
                        folderId   = file.parent.hashCode().toLong(),
                        folderName = file.parentFile?.name ?: "",
                        dateAdded  = file.lastModified(),
                        size       = file.length()
                    )
                }
            }
        }
    }

    private fun extractDuration(path: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val dur = retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            retriever.release()
            dur
        } catch (e: Exception) { 0L }
    }

    override suspend fun updateLastPlayed(id: Long, timestamp: Long) {
        mediaDao.updateLastPlayed(id, timestamp)
    }

    override suspend fun getSongCount() = mediaDao.getSongCount()
}
