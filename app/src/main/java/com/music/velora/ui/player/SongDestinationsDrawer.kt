package com.music.velora.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.velora.data.model.ROW_ART_PX
import com.music.velora.data.model.Song
import com.music.velora.ui.components.SongDestinationsContent
import com.music.velora.ui.theme.rememberArtworkPalette
import dev.chrisbanes.haze.HazeState

/**
 * Go to Artist / Go to Album over Now Playing.
 *
 * A [PlayerDrawer] on [hazeState] (the player's own haze) so the frost
 * samples the player and its cover — not the tab under the player sheet.
 */
@Composable
internal fun SongDestinationsDrawer(
    song: Song,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onOpenArtist: (browseId: String, name: String) -> Unit,
    onOpenAlbum: (browseId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkPalette(song.thumbnailUrl, artPx = ROW_ART_PX)
    PlayerDrawer(
        hazeState = hazeState,
        title = "",
        onDismiss = onDismiss,
        palette = palette,
        modifier = modifier,
    ) {
        SongDestinationsContent(
            song = song,
            onOpenArtist = onOpenArtist,
            onOpenAlbum = onOpenAlbum,
        )
    }
}
