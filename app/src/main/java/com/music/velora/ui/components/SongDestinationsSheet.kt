package com.music.velora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.velora.R
import com.music.velora.data.YtMusicRepository
import com.music.velora.data.canvas.AppleArtistArtRepository
import com.music.velora.data.model.ArtistCredit
import com.music.velora.data.model.BrowseType
import com.music.velora.data.model.ROW_ART_PX
import com.music.velora.data.model.SearchFilter
import com.music.velora.data.model.SearchResult
import com.music.velora.data.model.Song
import com.music.velora.data.model.artworkAt
import com.music.velora.ui.theme.ArtworkPalette
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

private val ART_SIZE = 56.dp
private val CREDIT_SEPARATORS = Regex(
    """(?:\s*,\s*|\s*&\s*|\s+×\s+|\s+x\s+|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b)""",
    RegexOption.IGNORE_CASE,
)

/** Display names from a credit line, casing preserved. */
fun creditArtistNames(raw: String): List<String> =
    raw.split(CREDIT_SEPARATORS)
        .map { it.trim() }
        .filter { it.isNotBlank() }

/**
 * Whether tapping the artist credit should open this sheet instead of jumping
 * straight to a single artist page — several credited artists, or an album
 * worth offering beside them.
 */
fun Song.opensDestinationsSheet(): Boolean {
    val artists = destinationArtists()
    return artists.size > 1 || !albumId.isNullOrBlank()
}

/** Artists to list on the sheet; prefers parsed [Song.artists] when present. */
fun Song.destinationArtists(): List<ArtistCredit> {
    if (artists.isNotEmpty()) return artists
    val names = creditArtistNames(artist)
    if (names.isEmpty()) {
        return listOfNotNull(
            artistId?.let { ArtistCredit(artist.ifBlank { "Artist" }, it) },
        )
    }
    return names.mapIndexed { index, name ->
        ArtistCredit(
            name = name,
            browseId = if (index == 0) artistId else null,
        )
    }
}

/**
 * Drawer of places a playing track can go: each credited artist, then its
 * album — Apple Music's "Go to Artist / Go to Album" sheet.
 */
@Composable
fun SongDestinationsSheet(
    song: Song,
    onOpenArtist: (browseId: String, name: String) -> Unit,
    onOpenAlbum: (browseId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    pagePalette: ArtworkPalette? = null,
) {
    FrostedSheet(hazeState = hazeState, palette = pagePalette, modifier = modifier) {
        SongDestinationsContent(
            song = song,
            onOpenArtist = onOpenArtist,
            onOpenAlbum = onOpenAlbum,
        )
    }
}

/**
 * Rows only — used inside [SongDestinationsSheet] and the player's
 * [com.music.velora.ui.player.SongDestinationsDrawer].
 */
@Composable
fun SongDestinationsContent(
    song: Song,
    onOpenArtist: (browseId: String, name: String) -> Unit,
    onOpenAlbum: (browseId: String, title: String) -> Unit,
) {
    val artists = song.destinationArtists()
    val albumId = song.albumId
    val albumTitle = song.albumName?.takeIf { it.isNotBlank() } ?: song.title

    Spacer(Modifier.height(8.dp))
    artists.forEach { artist ->
        ArtistDestinationRow(
            name = artist.name,
            browseId = artist.browseId,
            onOpen = { id -> onOpenArtist(id, artist.name) },
        )
    }
    if (albumId != null) {
        AlbumDestinationRow(
            title = albumTitle,
            artworkUrl = song.artworkAt(ROW_ART_PX),
            onClick = { onOpenAlbum(albumId, albumTitle) },
        )
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ArtistDestinationRow(
    name: String,
    browseId: String?,
    onOpen: (browseId: String) -> Unit,
) {
    val chrome = frostChromeColors()
    val scope = rememberCoroutineScope()
    var resolving by remember(name, browseId) { mutableStateOf(false) }
    val artUrl by produceState<String?>(initialValue = AppleArtistArtRepository.cached(name)?.heroUrl, name) {
        value = AppleArtistArtRepository.artFor(name)?.heroUrl
            ?: AppleArtistArtRepository.cached(name)?.heroUrl
    }
    DestinationRow(
        title = stringResource(R.string.go_to_artist),
        subtitle = name,
        enabled = !resolving,
        onClick = {
            if (browseId != null) {
                onOpen(browseId)
                return@DestinationRow
            }
            // YouTube sometimes names a collaborator without a browse link —
            // look them up under the Artists filter so the row still works.
            resolving = true
            scope.launch {
                val id = resolveArtistBrowseId(name)
                resolving = false
                if (id != null) onOpen(id)
            }
        },
        leading = {
            if (resolving) {
                Box(
                    modifier = Modifier
                        .size(ART_SIZE)
                        .clip(CircleShape)
                        .background(chrome.tint.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = chrome.accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                BoxArt(
                    url = artUrl,
                    circular = true,
                    placeholder = {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = chrome.contentVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                )
            }
        },
    )
}

/** Best browse id for [name] from an Artists search, or null on a miss. */
private suspend fun resolveArtistBrowseId(name: String): String? {
    val page = YtMusicRepository.searchPage(name, SearchFilter.ARTISTS).getOrNull() ?: return null
    val artists = page.rows.mapNotNull { row ->
        (row as? SearchResult.Browse)?.item?.takeIf { it.type == BrowseType.ARTIST }
    }
    return artists.firstOrNull { it.title.equals(name, ignoreCase = true) }?.browseId
        ?: artists.firstOrNull()?.browseId
}

@Composable
private fun AlbumDestinationRow(
    title: String,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    val chrome = frostChromeColors()
    DestinationRow(
        title = stringResource(R.string.go_to_album),
        subtitle = title,
        enabled = true,
        onClick = onClick,
        leading = {
            BoxArt(
                url = artworkUrl,
                circular = false,
                placeholder = {
                    Icon(
                        Icons.Rounded.Album,
                        contentDescription = null,
                        tint = chrome.contentVariant,
                        modifier = Modifier.size(28.dp),
                    )
                },
            )
        },
    )
}

@Composable
private fun DestinationRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val chrome = frostChromeColors()
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = chrome.content.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = chrome.contentVariant.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BoxArt(
    url: String?,
    circular: Boolean,
    placeholder: @Composable () -> Unit,
) {
    val chrome = frostChromeColors()
    val shape = if (circular) CircleShape else RoundedCornerShape(8.dp)
    val base = Modifier
        .size(ART_SIZE)
        .clip(shape)
        .background(chrome.tint.copy(alpha = 0.55f))
    if (url.isNullOrBlank()) {
        Box(modifier = base, contentAlignment = Alignment.Center) {
            placeholder()
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = if (circular) base else base.thumbnailBorder(shape),
        )
    }
}
