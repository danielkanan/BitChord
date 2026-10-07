/*
 * Staged "Add Music" sheet for an editable playlist — search + suggestions,
 * then confirm or discard the selection as a batch.
 */
package com.music.velora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.velora.R
import com.music.velora.data.YtMusicRepository
import com.music.velora.data.model.ROW_ART_PX
import com.music.velora.data.model.SearchFilter
import com.music.velora.data.model.SearchResult
import com.music.velora.data.model.Song
import com.music.velora.data.model.UserPlaylist
import com.music.velora.data.model.artworkAt
import com.music.velora.ui.theme.ArtworkPalette
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/** Same pagination as home Recents — four rows per column, scroll sideways. */
private const val SUGGESTION_TRACKS_PER_COLUMN = 4
private val SELECT_CIRCLE = 36.dp
private val SEARCH_LIST_MAX = 56.dp * 4

/**
 * Process-wide cache so reopening the sheet does not wait on history / Quick
 * picks again. Filtered against [alreadyInPlaylist] at display time.
 */
private object AddMusicSuggestionsCache {
    @Volatile
    var songs: List<Song> = emptyList()
}

/**
 * Add tracks to an editable playlist: search (same catalogue path as the
 * Search tab, without recent searches), plus a short list of suggestions
 * drawn from the account's listening history and Quick picks.
 *
 * Selection is staged. ✕ discards everything; ✓ commits the batch via
 * [onConfirm]. Rows already on the playlist stay off the suggestion list and
 * cannot be re-staged from search.
 */
@Composable
fun AddMusicToPlaylistSheet(
    playlist: UserPlaylist,
    alreadyInPlaylist: Set<String>,
    onConfirm: (List<Song>) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    pagePalette: ArtworkPalette? = null,
) {
    val chrome = pagePalette?.toFrostChrome() ?: frostChromeColors()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Map<String, Song>>(emptyMap()) }
    var cachedSuggestions by remember { mutableStateOf(AddMusicSuggestionsCache.songs) }
    var suggestionsLoading by remember {
        mutableStateOf(AddMusicSuggestionsCache.songs.isEmpty())
    }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (AddMusicSuggestionsCache.songs.isNotEmpty()) {
            cachedSuggestions = AddMusicSuggestionsCache.songs
            suggestionsLoading = false
            return@LaunchedEffect
        }
        suggestionsLoading = true
        val history = YtMusicRepository.history().getOrNull().orEmpty()
        val picks = YtMusicRepository.quickPicks().getOrNull().orEmpty()
        val loaded = (history + picks).distinctBy { it.videoId }.take(40)
        AddMusicSuggestionsCache.songs = loaded
        cachedSuggestions = loaded
        suggestionsLoading = false
    }

    val suggestions = remember(cachedSuggestions, alreadyInPlaylist) {
        cachedSuggestions.filterNot { it.videoId in alreadyInPlaylist }
    }

    // Debounced catalogue search — same endpoint as the Search tab's Songs
    // filter, without writing the query into that page's state or history.
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searchResults = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(280)
        val page = YtMusicRepository.searchPage(trimmed, SearchFilter.SONGS).getOrNull()
        searchResults = page?.rows.orEmpty().mapNotNull { row ->
            when (row) {
                is SearchResult.Track -> row.song
                is SearchResult.TopTrack -> row.song
                is SearchResult.Browse -> null
            }
        }.distinctBy { it.videoId }
        searching = false
    }

    val selectedCount = selected.size
    val title = if (selectedCount == 0) {
        stringResource(R.string.add_to_named_playlist, playlist.title)
    } else {
        pluralStringResource(
            R.plurals.songs_added_to_named_playlist,
            selectedCount,
            selectedCount,
            playlist.title,
        )
    }

    val showingSearch = query.isNotBlank()

    FrostedSheet(
        hazeState = hazeState,
        palette = pagePalette,
        scrollable = false,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDiscard) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = chrome.content,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = chrome.contentVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    if (selected.isNotEmpty()) onConfirm(selected.values.toList())
                    else onDiscard()
                },
                enabled = selected.isNotEmpty(),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.save),
                    tint = if (selected.isNotEmpty()) {
                        chrome.accent
                    } else {
                        chrome.contentVariant.copy(alpha = 0.35f)
                    },
                )
            }
        }

        SearchField(
            query = query,
            onQueryChange = { query = it },
            onSubmit = { /* live results already update as you type */ },
            placeholder = stringResource(R.string.search),
            hazeState = hazeState,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            !showingSearch && suggestionsLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SEARCH_LIST_MAX),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = chrome.accent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp),
                )
            }

            !showingSearch -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columnWidth = trackColumnWidth(maxWidth)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        suggestions.chunked(SUGGESTION_TRACKS_PER_COLUMN),
                        key = { column -> column.firstOrNull()?.videoId ?: "empty" },
                    ) { column ->
                        Column(Modifier.width(columnWidth)) {
                            column.forEach { song ->
                                val isSelected = song.videoId in selected
                                AddMusicSongRow(
                                    song = song,
                                    selected = isSelected,
                                    enabled = true,
                                    onToggle = {
                                        selected = if (isSelected) {
                                            selected - song.videoId
                                        } else {
                                            selected + (song.videoId to song)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            searching && searchResults.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SEARCH_LIST_MAX),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = chrome.accent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp),
                )
            }

            searchResults.isEmpty() -> Text(
                text = stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = chrome.contentVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SEARCH_LIST_MAX),
            ) {
                items(searchResults, key = { it.videoId }) { song ->
                    val inPlaylist = song.videoId in alreadyInPlaylist
                    val isSelected = song.videoId in selected
                    AddMusicSongRow(
                        song = song,
                        selected = isSelected,
                        enabled = !inPlaylist,
                        onToggle = {
                            selected = if (isSelected) {
                                selected - song.videoId
                            } else {
                                selected + (song.videoId to song)
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AddMusicSongRow(
    song: Song,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val chrome = frostChromeColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(7.dp))
                .thumbnailBorder(RoundedCornerShape(7.dp))
                .background(chrome.tint.copy(alpha = 0.55f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium,
                color = chrome.content.copy(alpha = if (enabled) 1f else 0.45f),
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = chrome.contentVariant.copy(alpha = if (enabled) 1f else 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        // Already on the playlist reads as selected-and-locked: filled tick,
        // not tappable — so it can't be staged again from search.
        SelectCircle(filled = selected || !enabled, enabled = enabled)
    }
}

/**
 * Outline circle with +, or filled accent with a tick — same 36.dp control
 * either way, so the row doesn't shift when a track is staged.
 */
@Composable
private fun SelectCircle(
    filled: Boolean,
    enabled: Boolean,
) {
    val chrome = frostChromeColors()
    val fill = if (filled) chrome.accent else Color.Transparent
    val stroke = if (filled) chrome.accent else chrome.edge
    val glyph = if (filled) {
        if (chrome.accent.luminance() > 0.45f) Color(0xFF1A1A1A) else Color.White
    } else {
        chrome.contentVariant.copy(alpha = if (enabled) 1f else 0.35f)
    }
    Box(
        modifier = Modifier
            .size(SELECT_CIRCLE)
            .clip(CircleShape)
            .background(fill, CircleShape)
            .border(GLASS_EDGE_WIDTH, stroke, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (filled) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = null,
            tint = glyph,
            modifier = Modifier.size(20.dp),
        )
    }
}
