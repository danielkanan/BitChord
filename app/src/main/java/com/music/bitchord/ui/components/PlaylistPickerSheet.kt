package com.music.bitchord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette
import dev.chrisbanes.haze.HazeState
import java.util.Locale

/**
 * Where a track goes: one of the account's playlists, or a new one.
 *
 * Two panels in one sheet rather than a sheet that opens a dialog. Creating a
 * playlist here is nearly always in service of adding the track that opened
 * this — so the form comes back to the same place, and the create request
 * carries the track with it instead of leaving a new empty playlist behind
 * for the user to add to a second time.
 *
 * [song] is null when the flow started from the Library tab rather than from a
 * track, which is the one case where the header has no track to draw and
 * "New playlist" is the whole point of the sheet.
 */
@Composable
fun PlaylistPickerSheet(
    playlists: List<UserPlaylist>,
    loading: Boolean,
    onPick: (UserPlaylist) -> Unit,
    onCreate: (String, PlaylistPrivacy) -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
    startCreating: Boolean = false,
    hazeState: HazeState? = null,
    pagePalette: ArtworkPalette? = null,
) {
    var creating by remember { mutableStateOf(startCreating) }
    val songPalette = rememberArtworkPalette(song?.thumbnailUrl, artPx = ROW_ART_PX)
    val palette = pagePalette ?: song?.let { songPalette }
    val chrome = palette?.toFrostChrome() ?: frostChromeColors()
    val divider = palette?.divider ?: chrome.edge.copy(alpha = 0.55f)

    if (creating) {
        FrostedSheet(hazeState = hazeState, palette = palette, modifier = modifier) {
            NewPlaylistForm(
                // Nowhere to go back to when the sheet opened straight onto the
                // form; the sheet's own dismiss is the way out.
                onBack = if (startCreating) null else ({ creating = false }),
                onCreate = onCreate,
            )
        }
        return
    }

    FrostedSheet(
        hazeState = hazeState,
        palette = palette,
        scrollable = false,
        modifier = modifier,
    ) {
        if (song != null) {
            SheetTrackHeader(song, subtitleColor = chrome.contentVariant)
            HorizontalDivider(thickness = 0.5.dp, color = divider)
        }
        SheetHeading(
            stringResource(if (song != null) R.string.add_to_playlist else R.string.your_playlists)
                .uppercase(Locale.getDefault()),
        )

        ActionRow(
            icon = BitChordIcons.Plus,
            label = stringResource(R.string.new_playlist),
            accent = chrome.accent,
            onClick = { creating = true },
        )

        when {
            // Only while there is nothing to show: re-fetching under a list
            // that is already up would replace it with a spinner for no gain.
            loading && playlists.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = chrome.accent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(26.dp),
                )
            }

            playlists.isEmpty() -> Text(
                text = stringResource(R.string.no_playlists_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = chrome.contentVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            )

            else -> {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 0.5.dp,
                    color = divider,
                )
                // Capped so a long list can't push the sheet past the screen;
                // it scrolls inside the sheet instead.
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(playlists, key = { it.playlistId }) { playlist ->
                        PlaylistRow(playlist = playlist, onClick = { onPick(playlist) })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlaylistRow(playlist: UserPlaylist, onClick: () -> Unit) {
    val chrome = frostChromeColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(7.dp))
                .thumbnailBorder(RoundedCornerShape(7.dp))
                .background(chrome.tint.copy(alpha = 0.55f)),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                color = chrome.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (playlist.subtitle.isNotBlank()) {
                Text(
                    text = playlist.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = chrome.contentVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Name and visibility, and nothing else. YouTube also takes a description,
 * which nobody fills in from a phone at the moment of saving a song.
 */
@Composable
private fun NewPlaylistForm(
    onBack: (() -> Unit)?,
    onCreate: (String, PlaylistPrivacy) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf(PlaylistPrivacy.PRIVATE) }
    val focusManager = LocalFocusManager.current
    // Shared inset for the header and the foot under Create — equal so the
    // form doesn't open tighter at the top than it closes at the bottom.
    val edgePad = 16.dp

    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            focusManager.clearFocus()
            onCreate(name, privacy)
        }
    }

    val chrome = frostChromeColors()
    Column(
        modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (onBack != null) 8.dp else 22.dp,
                    end = 22.dp,
                    top = edgePad,
                    bottom = edgePad,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = chrome.content,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.new_playlist),
                    style = MaterialTheme.typography.titleMedium,
                    color = chrome.content,
                )
                Text(
                    text = stringResource(R.string.saved_to_youtube_music_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = chrome.contentVariant,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = chrome.edge.copy(alpha = 0.55f))

        // Same pill geometry as [SearchField] — height, radius, clear control —
        // minus the magnifier, since this field is naming rather than searching.
        val fieldShape = RoundedCornerShape(percent = 50)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .height(46.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, fieldShape)
                .border(GLASS_EDGE_WIDTH, chrome.edge, fieldShape)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.playlist_name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = chrome.contentVariant,
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = chrome.content,
                    ),
                    cursorBrush = SolidColor(chrome.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (name.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { name = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.clear_name),
                        tint = chrome.contentVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        SheetHeading(stringResource(R.string.who_can_see_it))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlaylistPrivacy.entries.forEach { option ->
                PrivacyPill(
                    icon = option.icon,
                    label = option.label,
                    selected = option == privacy,
                    onClick = { privacy = option },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        val buttonShape = RoundedCornerShape(percent = 50)
        Button(
            onClick = submit,
            enabled = name.isNotBlank(),
            shape = buttonShape,
            border = BorderStroke(GLASS_EDGE_WIDTH, chrome.edge),
            colors = ButtonDefaults.buttonColors(
                containerColor = chrome.accent,
                contentColor = if (chrome.accent.luminance() > 0.45f) {
                    Color(0xFF1A1A1A)
                } else {
                    Color.White
                },
                disabledContainerColor = chrome.tint.copy(alpha = 0.55f),
                disabledContentColor = chrome.contentVariant.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(46.dp),
        ) {
            Text(stringResource(R.string.create_playlist))
        }
        Spacer(Modifier.height(edgePad))
    }
}

/**
 * The rename panel of [BrowseActionsSheet], which swaps itself out for this
 * rather than opening a dialog over itself — same reason the create form lives
 * inside [PlaylistPickerSheet].
 */
@Composable
internal fun RenamePlaylistForm(
    playlist: UserPlaylist,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(playlist.title) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit: () -> Unit = {
        if (name.isNotBlank()) {
            focusManager.clearFocus()
            onRename(name)
        }
    }

    val chrome = frostChromeColors()
    // The form opens with the keyboard already up, which on a bottom sheet
    // would otherwise sit over the button the form exists to reach.
    Column(
        modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = chrome.content,
                )
            }
            Text(
                text = stringResource(R.string.rename_playlist),
                style = MaterialTheme.typography.titleLarge,
                color = chrome.content,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = chrome.edge.copy(alpha = 0.55f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .background(chrome.tint.copy(alpha = 0.55f), RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = chrome.content,
                ),
                cursorBrush = SolidColor(chrome.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }
        Button(
            onClick = submit,
            enabled = name.isNotBlank() && name != playlist.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        ) {
            Text(stringResource(R.string.save_name))
        }
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * The glyph that says what a visibility actually means — a padlock, a shared
 * link, a globe. Three words that all sound like degrees of the same thing
 * read much faster as three different shapes.
 */
private val PlaylistPrivacy.icon: ImageVector
    get() = when (this) {
        PlaylistPrivacy.PRIVATE -> Icons.Rounded.Lock
        PlaylistPrivacy.UNLISTED -> Icons.Rounded.Link
        PlaylistPrivacy.PUBLIC -> Icons.Rounded.Public
    }

/** The search filters' pill, carrying an icon ahead of its label. */
@Composable
private fun PrivacyPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = frostChromeColors()
    // Accent from a sleeve can be pale; white-on-pale (and light-on-light in
    // dark mode) disappears, so pick the label that actually contrasts.
    val content = if (selected) {
        if (chrome.accent.luminance() > 0.45f) Color(0xFF1A1A1A) else Color.White
    } else {
        chrome.contentVariant
    }
    val pillShape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .clip(pillShape)
            .background(
                if (selected) {
                    chrome.accent
                } else {
                    chrome.tint.copy(alpha = 0.55f)
                },
                pillShape,
            )
            .border(GLASS_EDGE_WIDTH, chrome.edge, pillShape)
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
