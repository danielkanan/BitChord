package com.music.velora.ui.screens

import com.music.velora.R

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.music.velora.data.canvas.AppleArtistArt
import com.music.velora.data.canvas.AppleArtistArtRepository
import com.music.velora.data.canvas.CanvasArtwork
import com.music.velora.data.canvas.CanvasRepository
import com.music.velora.data.canvas.keyColors
import kotlinx.coroutines.delay
import com.music.velora.data.model.BrowseType
import com.music.velora.data.model.DetailPage
import com.music.velora.data.model.CARD_ART_PX
import com.music.velora.data.model.HEADER_ART_PX
import com.music.velora.data.model.ROW_ART_PX
import com.music.velora.data.model.HomeShelf
import com.music.velora.data.model.PlaylistPrivacy
import com.music.velora.data.model.ShelfItem
import com.music.velora.data.settings.SongSort
import com.music.velora.data.model.Song
import com.music.velora.data.model.SubscriptionState
import com.music.velora.data.model.UiState
import com.music.velora.data.model.artworkAt
import com.music.velora.data.model.durationMillis
import com.music.velora.data.model.isSameTrackAs
import com.music.velora.data.settings.AppSettings
import com.music.velora.ui.components.DownloadedBadge
import com.music.velora.ui.components.ExplicitSongTitle
import com.music.velora.ui.components.LIBRARY_GRID_SPACING
import com.music.velora.ui.components.MessageState
import com.music.velora.ui.components.PAGE_GUTTER
import com.music.velora.ui.components.NUMBERED_ROW_DIVIDER_INSET
import com.music.velora.ui.components.ROW_DIVIDER_INSET
import com.music.velora.ui.components.SHELF_CARD_WIDTH
import com.music.velora.ui.components.SongRow
import com.music.velora.ui.components.libraryGrid
import com.music.velora.ui.components.GLASS_EDGE_WIDTH
import com.music.velora.ui.components.FOOT_FADE_START
import com.music.velora.ui.components.HeroFoot
import com.music.velora.ui.components.rememberHeroFoot
import com.music.velora.ui.components.thumbnailBorder
import com.music.velora.ui.components.videoBlur
import com.music.velora.ui.components.detailSkeleton
import com.music.velora.ui.components.topBarContentPadding
import com.music.velora.ui.components.RECENT_COLUMN_DIVIDER_INSET
import com.music.velora.ui.components.trackColumnWidth
import com.music.velora.ui.haptics.Haptic
import com.music.velora.ui.haptics.rememberHaptics
import com.music.velora.ui.icons.VeloraIcons
import com.music.velora.ui.player.CanvasArtworkPlayer
import com.music.velora.ui.player.FootBlurSpec
import com.music.velora.ui.theme.ArtworkPalette
import com.music.velora.ui.theme.onSolid
import com.music.velora.ui.theme.rememberArtworkPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt
import java.util.Locale

private const val MAX_ARTIST_SONGS = 20
private const val SONGS_PER_COLUMN = 4
private const val ARTIST_ROW_MAX_ITEMS = 5

/** The artist photo, very slightly taller than it is wide. */
private const val ARTIST_PHOTO_RATIO = 0.95f

/** Fraction of the header width Apple's title logo may occupy. */
private const val ARTIST_LOGO_WIDTH = 0.70f

/** Cap so multi-line logos don't swallow the photograph. */
private const val ARTIST_LOGO_MAX_HEIGHT = 0.24f

private const val LOGO_RETRIES = 2
private const val LOGO_RETRY_DELAY_MS = 800L

private val CREDIT_SEPARATOR = Regex(""",\s|\s&\s|\s(?:x|and|feat\.?|ft\.?|with)\s""", RegexOption.IGNORE_CASE)

/** Whether this reads as several artists credited together rather than one name. */
private fun String.looksLikeCredit() = CREDIT_SEPARATOR.containsMatchIn(this)

/** A release's sleeve, given a little more height than the artist photo. */
private const val SLEEVE_RATIO = 0.92f

/** The sleeve on a release page, as a fraction of the page width. */
private const val SLEEVE_FRACTION = 0.80f

private val SLEEVE_SHAPE = RoundedCornerShape(12.dp)

/** The inset the header text and the action pills share. */
private val HEADER_GUTTER = PAGE_GUTTER + 14.dp

/** Extra breathing room for the editorial copy on album and artist pages. */
private val ABOUT_GUTTER = PAGE_GUTTER + 12.dp

/** The artist bio and everything below it: the page edge every page shares. */
private val ARTIST_CONTENT_GUTTER = PAGE_GUTTER

/**
 * How far past the foot of the artwork the title block is allowed to hang.
 *
 * Sat flush to the bottom of the picture it lands wherever the picture happens
 * to be busy, and on a sleeve with anything going on down there the title reads
 * as part of the artwork rather than as a caption to it. Dropped clear, it sits
 * on the blurred colour instead, which has nothing in it to compete.
 */
private val HEADER_DROP = 44.dp

/** How far the artist name/logo and everything under it sit above the usual [HEADER_DROP]. */
private val ARTIST_HEADER_LIFT = 64.dp

/** The artist action row: a large Play flanked by two smaller glass circles. */
private val ARTIST_PLAY_BUTTON = 70.dp

/** A larger share of a smaller circle than the release headers' 0.44. */
private const val ARTIST_PLAY_ICON_SCALE = 0.56f
private val ARTIST_SIDE_BUTTON = 52.dp
private val ARTIST_ACTION_GAP = 24.dp

/** The white veil under Apple-style containers and their buttons. */
private const val LIGHT_FILL_ALPHA = 0.07f

/** Brighter than the default 0.15 hairlines, as on Apple Music's own card. */
private const val TOP_RELEASE_EDGE_ALPHA = 0.10f
private const val TOP_RELEASE_COVER_EDGE_ALPHA = 0.215f

/**
 * Album / artist / playlist page. Rendered inside the main content area
 * rather than as a sheet, so the tab bar and mini player stay visible.
 *
 * The page paints itself in the artwork's own colours — a tint behind
 * everything, the artwork itself across the top of it, and an accent taken off
 * the sleeve for the credit line and the Play/Shuffle pair. See
 * [rememberArtworkPalette] for how those are derived and kept legible.
 *
 * It is built in two layers rather than the obvious one:
 *
 *  1. [PageBackground] — the page's colour and the artwork, and nothing you can read.
 *  2. The list — titles, buttons and rows, drawn over it and so never blurred.
 *
 * The artwork's foot is blurred into the page for real, before it is drawn —
 * see [HeroFoot] — and the page is painted exactly the colour that blur
 * settles into. A picture faded out over a page only *resembles* it, and the
 * eye finds that edge every time; a blur that runs on until it is the page has
 * no edge to find.
 */
/**
 * One [LazyListState] per detail [browseId]. [AnimatedContent] keeps the outgoing
 * page composed while it fades; wiring every slot to the live top-of-stack state
 * made the exit animation drive the page underneath and left that list stuck
 * after a pop.
 */
private val detailPageListStates = mutableMapOf<String, LazyListState>()

/** Artist "Show all" grids — keyed by [artistShelfGridKey]. */
private val artistShelfGridStates = mutableMapOf<String, LazyGridState>()

private fun artistShelfGridKey(artistBrowseId: String, shelf: HomeShelf): String =
    "$artistBrowseId:${shelf.title}"

/**
 * Drops scroll memory for detail pages that are no longer on [activeBrowseIds].
 * Called when the stack changes so leaving an album and opening it again starts
 * at the top instead of reusing a parked [LazyListState] and hero offset.
 */
fun syncDetailPageScrollMemory(activeBrowseIds: Set<String>) {
    detailPageListStates.keys.toList().forEach { id ->
        if (id !in activeBrowseIds) detailPageListStates.remove(id)
    }
    heroScrollCacheByBrowseId.keys.toList().forEach { id ->
        if (id !in activeBrowseIds) heroScrollCacheByBrowseId.remove(id)
    }
    artistShelfGridStates.keys.toList().forEach { key ->
        val artistId = key.substringBefore(':')
        if (artistId !in activeBrowseIds) artistShelfGridStates.remove(key)
    }
}

@Composable
private fun rememberArtistShelfGridState(artistBrowseId: String, shelf: HomeShelf): LazyGridState {
    val key = artistShelfGridKey(artistBrowseId, shelf)
    return remember(key) {
        artistShelfGridStates.getOrPut(key) { LazyGridState() }
    }
}

@Composable
fun rememberDetailPageListState(browseId: String): LazyListState {
    return remember(browseId) {
        detailPageListStates.getOrPut(browseId) { LazyListState() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    page: DetailPage,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onSectionItemClick: (ShelfItem) -> Unit,
    onArtistClick: (String, String) -> Unit,
    onAddSuggested: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    activeShelf: HomeShelf? = null,
    onActiveShelfChange: (HomeShelf?) -> Unit = {},
    /**
     * The colour the page is painted — what its hero's blur settles into — so
     * the bars floating over it can be painted the same. Null while there is no
     * artwork colour yet, when the page is on its palette's own background.
     */
    onPageColorChange: (Color?) -> Unit = {},
    /**
     * Haze source for frosted chrome while a header video plays — the still
     * hero only, so the clip does not invalidate blur every frame.
     */
    chromeHazeState: HazeState? = null,
    /** True while [PageBackground] is showing a looping header clip. */
    onHeaderVideoActive: (Boolean) -> Unit = {},
    /**
     * Holding one of the album cards on an artist page — the same menu the
     * shelves on every other tab open, so a release can be queued from
     * wherever it is seen rather than only from its own page.
     */
    onSectionItemLongPress: ((ShelfItem) -> Unit)? = null,
    /**
     * Opens the Add Music sheet for an editable playlist. Null hides the
     * control — albums, artist pages, and playlists this account does not own.
     */
    onAddMusic: (() -> Unit)? = null,
    /**
     * Saves this release to the account's library, or takes it out —
     * [DetailPage.library] says which way round. Null hides the control
     * entirely, which is the answer for a guest and for the pages YouTube never
     * offers to save; there is nothing to show a signed-out user here that
     * wouldn't just be refused.
     */
    onToggleLibrary: (() -> Unit)? = null,
    /**
     * Subscribes to this artist's channel, or unsubscribes —
     * [DetailPage.subscription] says which way round. The artist page's answer
     * to [onToggleLibrary], and null in the same circumstances: a guest, or a
     * page whose header never offered the button.
     */
    onToggleSubscription: (() -> Unit)? = null,
    /**
     * How the track list is ordered — the release's own running order by
     * default, or alphabetical. Owned by the caller rather than this page
     * because the control for it lives in the top bar, alongside the account
     * photo, not in this header.
     */
    songSort: SongSort = SongSort.DEFAULT,
) {
    val rawSongs = (page.songs as? UiState.Success)?.data.orEmpty()
    val songs = remember(rawSongs, songSort) { rawSongs.sortedForDetail(songSort) }
    // What a numbered row shows regardless of [songSort] — an album's track
    // numbers are the sleeve's own and must not relabel themselves to match
    // wherever a sort put the row.
    val originalTrackNumbers = remember(rawSongs) {
        rawSongs.withIndex().associate { (i, s) -> s.videoId to i + 1 }
    }
    val isArtist = page.type == BrowseType.ARTIST
    // Apple Music's hero photo and title logo for this artist, found by name.
    // Null until (and unless) it arrives. While the lookup is in flight the
    // YouTube photograph is held back — showing it first flashes the wrong
    // picture and locks the bars to its colours before Apple's arrive.
    var appleArt by remember(page.browseId, page.title) {
        mutableStateOf(if (isArtist) AppleArtistArtRepository.cached(page.title) else null)
    }
    var appleSettled by remember(page.browseId, page.title) {
        mutableStateOf(!isArtist || AppleArtistArtRepository.lookedUp(page.title))
    }
    // Opened from a track, the title is the whole credit ("A, B & C") until the
    // page loads and swaps in the one artist's name. Searching a credit finds
    // nobody, so a title that reads as several artists waits for that swap.
    val nameSettled = !page.title.looksLikeCredit() || page.songs !is UiState.Loading
    LaunchedEffect(page.browseId, page.title, nameSettled) {
        if (!isArtist || !nameSettled) return@LaunchedEffect
        appleArt = AppleArtistArtRepository.artFor(page.title) ?: appleArt
        appleSettled = true
    }
    // Prefer Apple's hero + key colours. Until that lookup settles, leave the
    // image empty so the wash stays on the theme rather than the YouTube shot.
    val headerArtUrl = when {
        appleArt?.heroUrl != null -> appleArt?.heroUrl
        isArtist && !appleSettled -> null
        else -> page.thumbnailUrl
    }
    val artPalette = rememberArtworkPalette(
        imageUrl = headerArtUrl,
        keyColors = appleArt?.keyColors(),
    )
    val suggested = page.suggestedSongs

    // Animated cover art on the header, the same feature the player has.
    // Albums only for catalogue canvases; artist pages use Apple's looping
    // header video when one exists.
    val canvasEnabled by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val prioritizeSpotifyCanvas by AppSettings.prioritizeSpotifyCanvas.collectAsStateWithLifecycle()
    // The credit line the header shows is the artist as far as the catalogue
    // services are concerned. A browse card's subtitle sometimes omits it, in
    // which case the tracks themselves know who it is.
    val credit = page.headerLines(songs.size, songs.playtime()).first.ifBlank { songs.firstOrNull()?.artist.orEmpty() }
    var canvas by remember(page.browseId) { mutableStateOf<CanvasArtwork?>(null) }
    LaunchedEffect(page.browseId, page.title, credit, canvasEnabled, prioritizeSpotifyCanvas) {
        // An artist's clip is set below, by the Apple lookup.
        if (isArtist) return@LaunchedEffect
        if (!canvasEnabled || page.type != BrowseType.ALBUM) {
            canvas = null
            return@LaunchedEffect
        }
        // As on the player: the credit fills in once the tracks load, so this
        // can run twice. Keep a clip that is already playing if the second
        // pass comes back empty.
        canvas = CanvasRepository.canvasForAlbum(page.title, credit) ?: canvas
    }
    val artistVideo = appleArt?.videoUrl
    val expectsHeaderVideo = isArtist && canvasEnabled && !artistVideo.isNullOrBlank()
    LaunchedEffect(artistVideo, canvasEnabled) {
        if (isArtist) canvas = artistVideo?.takeIf { canvasEnabled }?.let { CanvasArtwork(url = it) }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
    // The artwork is drawn behind the list rather than in it, so both need
    // to agree on its height without being able to ask each other. The
    // width is the page's, so the ratio decides it and both can work it out
    // alone.
    //
    // Measured rather than read off the window. The player runs
    // full-screen at every window size, so this page's own width in
    // landscape is the *whole* window. Straight off that width, the ratio hands back
    // a hero taller than the window itself — the artwork and track list
    // end up scrolled out of sight beneath what reads as a blank page.
    // Capping against the window's own height is what keeps the ratio's
    // math honest once the width it's fed is no longer guaranteed to be
    // the narrow one it was written for.
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val artHeight = (maxWidth / if (isArtist) ARTIST_PHOTO_RATIO else SLEEVE_RATIO)
        .coerceAtMost(minOf(maxHeight * 0.6f, screenHeight * 0.55f))
    val heroWidthPx = constraints.maxWidth
    val photoHeightPx = with(LocalDensity.current) { artHeight.roundToPx() }
    // Artist pages resolve [headerArtUrl] themselves — including "nothing
    // yet" — so there is no falling back to the YouTube photo here.
    val foot = rememberHeroFoot(
        headerArtUrl.artworkAt(HEADER_ART_PX),
        heroWidthPx,
        photoHeightPx,
        // Apple's own colour for this photograph, the one their app paints.
        // An album has none, so its picture's bottom colour is deepened instead.
        solid = appleArt?.background,
    )
    // What the blur settles into once the photograph has been read, and Apple's
    // colour on its own until then.
    val pageSolid = foot?.solid ?: appleArt?.let { Color(it.background) }
    val palette = remember(artPalette, pageSolid) { artPalette.onSolid(pageSolid) }
    SideEffect {
        if (pageSolid != null) onPageColorChange(pageSolid)
    }

    // What marks a row as already downloaded, tinted from the sleeve like the
    // rest of the page. Null on any page that is itself a reading of this
    // device — the Downloads folder, one downloaded playlist — where every row
    // qualifies and the badge would be decoration rather than information.
    val downloadedTint = palette.accent.takeUnless { page.browseId.startsWith("local:") }

    // When the track list replaces the loading skeleton the column height can
    // jump enough that a few Samsung builds keep a stale offset and refuse to
    // scroll — park at the top once real content is on screen.
    val songsLoading = page.songs is UiState.Loading
    var tracksWereLoading by remember(page.browseId) {
        mutableStateOf(songsLoading)
    }
    LaunchedEffect(page.songs, page.browseId) {
        fun resetHeroScrollCache() {
            heroScrollCacheByBrowseId[page.browseId]?.let { cache ->
                cache.heldScrollPx = 0
                cache.itemHeightsPx.clear()
            }
        }
        when (page.songs) {
            is UiState.Loading -> {
                tracksWereLoading = true
                resetHeroScrollCache()
                listState.scrollToItem(0, 0)
            }
            else -> if (tracksWereLoading) {
                tracksWereLoading = false
                resetHeroScrollCache()
                listState.scrollToItem(0, 0)
            }
        }
    }

    val density = LocalDensity.current
    val maxHeroScrollPx = remember(artHeight, foot, density) {
        with(density) {
            val run = foot?.runFraction ?: 0f
            (artHeight * (1f + run)).roundToPx()
        }
    }
    val heroScrollPx = rememberHeroScrollPx(
        listState = listState,
        browseId = page.browseId,
        maxScrollPx = maxHeroScrollPx,
        freezeScroll = songsLoading,
    )
    LaunchedEffect(page.browseId, activeShelf) {
        if (!isArtist || activeShelf != null) return@LaunchedEffect
        val index = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        listState.scrollToItem(index, offset)
    }
    LaunchedEffect(page.browseId) {
        if (page.songs is UiState.Loading) {
            listState.scrollToItem(0, 0)
            heroScrollCacheByBrowseId[page.browseId]?.let { cache ->
                cache.heldScrollPx = 0
                cache.itemHeightsPx.clear()
            }
        } else if (
            listState.firstVisibleItemIndex == 0 &&
            listState.firstVisibleItemScrollOffset == 0
        ) {
            heroScrollCacheByBrowseId[page.browseId]?.heldScrollPx = 0
        }
    }
    Box(Modifier.fillMaxSize()) {
        // Kept outside the shelf fade so returning from "Show all" does not
        // remount the hero at scroll zero while the list is still partway down.
        if (activeShelf == null) {
            PageBackground(
                foot = foot,
                pageColor = palette.wash,
                canvas = canvas,
                artHeight = artHeight,
                heroScrollPx = heroScrollPx,
                videoBlur = foot?.videoBlur(heroWidthPx, photoHeightPx),
                chromeHazeState = chromeHazeState,
                expectsHeaderVideo = expectsHeaderVideo,
                onHeaderVideoActive = onHeaderVideoActive,
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(0f),
            )
        }

        AnimatedContent(
            targetState = activeShelf,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "artist_shelf_transition",
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
        ) { targetShelf ->
            if (targetShelf == null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Skeleton rows sit below the header; scrolling while they are
                    // still placeholders desyncs the hero and header clip from the
                    // list until real content lands.
                    userScrollEnabled = !songsLoading,
                    // Both artist photos and release artwork run edge-to-edge up under
                    // the glass bar — the image is the top of the page, not a card on it.
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                ) {
            item(key = "header") {
                if (isArtist) {
                    ArtistHeader(page = page, palette = palette, artHeight = artHeight, appleArt = appleArt)
                } else {
                    ReleaseHeader(
                        page = page,
                        palette = palette,
                        artHeight = artHeight,
                        trackCount = songs.size,
                        songs = songs,
                        onPlay = { onSongClick(songs, 0) },
                        onShuffle = { onShuffle(songs) },
                        onArtistClick = onArtistClick,
                        onToggleLibrary = onToggleLibrary,
                        onAddMusic = onAddMusic,
                        // Only once we know the list is empty — while songs are
                        // still Loading the Success path hasn't landed yet and
                        // treating that as empty flashes the full-width pill.
                        showEmptyAddMusic = onAddMusic != null && when (val state = page.songs) {
                            is UiState.Success -> state.data.isEmpty()
                            is UiState.Error -> state.message == stringResource(R.string.no_tracks_here)
                            else -> false
                        },
                    )
                }
            }

            if (songs.isNotEmpty() && isArtist) {
                item(key = "actions") {
                    ActionRow(
                        palette = palette,
                        onPlay = { onSongClick(songs, 0) },
                        onShuffle = { onShuffle(songs) },
                        subscription = page.subscription?.takeIf { onToggleSubscription != null },
                        onToggleSubscription = onToggleSubscription,
                        bottomSpace = 22.dp,
                        playColor = appleArt?.keyColor?.let { Color(it) },
                    )
                }
            }

            // The first release on the shelves below, pulled out as a card of its own.
            val topRelease = if (isArtist) page.sections.topRelease() else null
            if (topRelease != null) {
                item(key = "top-release") {
                    TopReleaseCard(
                        item = topRelease,
                        palette = palette,
                        onClick = { onSectionItemClick(topRelease) },
                        onLongPress = onSectionItemLongPress?.let { { it(topRelease) } },
                    )
                }
            }

            // YouTube's own editorial blurb — an album or an artist only, per
            // [DetailPage.description]. A playlist never carries one, and the
            // section is skipped for it even on the rare response that does.
            //
            // On an artist page it closes the page, after the shelves, rather
            // than sitting between the action row and the songs.
            // The artist's counts close it, under the text, so a page whose blurb is
            // empty still gets the section for them.
            val description = page.description
            val hasStats = isArtist &&
                (page.subscriberCountText != null || page.monthlyListenerCount != null)
            val showAbout = (!description.isNullOrBlank() &&
                (page.type == BrowseType.ALBUM || isArtist)) || hasStats
            fun LazyListScope.aboutItem() {
                if (!showAbout) return
                item(key = "about") {
                    // Clear of the last shelf above it.
                    Box(Modifier.padding(top = if (isArtist) 28.dp else 0.dp)) {
                        AboutSection(
                            title = stringResource(
                                if (isArtist) R.string.about_artist else R.string.about_album,
                            ),
                            text = description?.takeIf { it.isNotBlank() },
                            palette = palette,
                            horizontalPadding = if (isArtist) ARTIST_CONTENT_GUTTER else ABOUT_GUTTER,
                            stats = if (hasStats) {
                                {
                                    ArtistStatsRow(
                                        subscriberCountText = page.subscriberCountText,
                                        monthlyListenerCount = page.monthlyListenerCount,
                                        palette = palette,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            if (!isArtist) aboutItem()

            when (val state = page.songs) {
                is UiState.Loading -> detailSkeleton(isArtist)
                is UiState.Error -> item { MessageState(state.message) }
                is UiState.Success -> if (isArtist) {
                    // An artist's full song list would bury the album shelves, so
                    // it pages sideways four at a time and stops at twenty.
                    item {
                        val top = state.data.take(MAX_ARTIST_SONGS)
                        SectionHeading(
                            title = stringResource(R.string.top_songs),
                            palette = palette,
                            horizontalPadding = ARTIST_CONTENT_GUTTER,
                        )
                        BoxWithConstraints {
                            val columnWidth = trackColumnWidth(maxWidth)
                            val rowState = rememberLazyListState()
                            val snapFling = rememberSnapFlingBehavior(lazyListState = rowState)
                            LazyRow(
                                state = rowState,
                                flingBehavior = snapFling,
                                modifier = Modifier.forwardVerticalScrollTo(listState),
                                contentPadding = PaddingValues(horizontal = ARTIST_CONTENT_GUTTER),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(top.chunked(SONGS_PER_COLUMN)) { column ->
                                    Column(Modifier.width(columnWidth)) {
                                        column.forEachIndexed { index, song ->
                                            CompactSongRow(
                                                song = song,
                                                palette = palette,
                                                onClick = { onSongClick(top, top.indexOf(song)) },
                                                onLongPress = { onSongLongPress(song) },
                                                downloadedTint = downloadedTint,
                                            )
                                            if (index < column.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(
                                                        start = RECENT_COLUMN_DIVIDER_INSET,
                                                    ),
                                                    thickness = 0.5.dp,
                                                    color = palette.divider,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Every row on an album carries the same sleeve, which is
                    // already the largest thing on the page — Apple Music
                    // numbers those rows instead, and so does this.
                    val numbered = page.type == BrowseType.ALBUM
                    itemsIndexed(
                        items = songs,
                        key = { index, song -> "${index}_${song.videoId}" },
                    ) { position, song ->
                        val isCurrent = song.isSameTrackAs(currentSong)
                        // On the album artist's own tracks the credit is already
                        // in the header — only guests / features stay under the
                        // title. Exact match only: "Gareth & X" is still shown.
                        val rowArtist = when {
                            !numbered || credit.isBlank() -> song.artist
                            song.artist.equals(credit, ignoreCase = true) -> ""
                            else -> song.artist
                        }
                        if (numbered && position == 0) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = palette.divider,
                            )
                        }
                        SongRow(
                            song = when {
                                numbered -> song.copy(artist = rowArtist)
                                else -> song.copy(
                                    artist = rowArtist,
                                    thumbnailUrl = song.thumbnailUrl ?: page.thumbnailUrl,
                                )
                            },
                            onClick = {
                                onSongClick(songs, position)
                            },
                            onLongPress = { onSongLongPress(song) },
                            onSwipeToQueue = { onSongSwipe(song) },
                            rowBackground = Color.Transparent,
                            // The track's place on the release, not wherever a
                            // sort put the row.
                            trackNumber = originalTrackNumbers[song.videoId].takeIf { numbered },
                            subtitleColor = palette.onBackgroundVariant,
                            downloadedTint = downloadedTint,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            activeTint = palette.accent,
                            // Albums and playlists both drop the trailing time —
                            // the running order is the point, not the clock.
                            showDuration = false,
                        )
                        if (position < songs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (numbered) NUMBERED_ROW_DIVIDER_INSET else ROW_DIVIDER_INSET,
                                ),
                                thickness = 0.5.dp,
                                color = palette.divider,
                            )
                        } else if (numbered) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = palette.divider,
                            )
                        }
                    }
                }
            }

            // Tracks YouTube offers to round the playlist out, never folded
            // into the list above — see [DetailPage.suggestedSongs].
            if (suggested.isNotEmpty()) {
                item(key = "suggested-heading") {
                    SectionHeading(stringResource(R.string.suggested), palette)
                }
                item(key = "suggested-songs") {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val columnWidth = trackColumnWidth(maxWidth)
                        val rowState = rememberLazyListState()
                        val snapFling = rememberSnapFlingBehavior(lazyListState = rowState)
                        val columns = suggested.chunked(SONGS_PER_COLUMN)
                        LazyRow(
                            state = rowState,
                            flingBehavior = snapFling,
                            modifier = Modifier.forwardVerticalScrollTo(listState),
                            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(
                                columns,
                                key = { _, column -> column.first().videoId },
                            ) { columnIndex, column ->
                                Column(Modifier.width(columnWidth)) {
                                    column.forEachIndexed { index, song ->
                                        val songIndex = columnIndex * SONGS_PER_COLUMN + index
                                        CompactSuggestedSongRow(
                                            song = song,
                                            palette = palette,
                                            onClick = { onSongClick(suggested, songIndex) },
                                            onLongPress = { onSongLongPress(song) },
                                            onAdd = { onAddSuggested(song) },
                                            downloadedTint = downloadedTint,
                                        )
                                        if (index < column.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(
                                                    start = RECENT_COLUMN_DIVIDER_INSET,
                                                ),
                                                thickness = 0.5.dp,
                                                color = palette.divider,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Albums / Singles & EPs carousels (artist pages).
            items(page.sections) { shelf ->
                val canShowAll = shelf.items.size > ARTIST_ROW_MAX_ITEMS
                val displayItems = remember(shelf.items) {
                    if (canShowAll) shelf.items.take(ARTIST_ROW_MAX_ITEMS) else shelf.items
                }
                Column(Modifier.padding(top = 22.dp)) {
                    SectionHeading(
                        title = shelf.title,
                        palette = palette,
                        onShowAll = if (canShowAll) { { onActiveShelfChange(shelf) } } else null,
                        horizontalPadding = if (isArtist) ARTIST_CONTENT_GUTTER else PAGE_GUTTER,
                    )
                    LazyRow(
                        modifier = if (isArtist) {
                            Modifier.forwardVerticalScrollTo(listState)
                        } else {
                            Modifier
                        },
                        contentPadding = PaddingValues(
                            horizontal = if (isArtist) ARTIST_CONTENT_GUTTER else PAGE_GUTTER,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(displayItems) { item ->
                            SectionCard(
                                item = item,
                                palette = palette,
                                onClick = { onSectionItemClick(item) },
                                onLongPress = onSectionItemLongPress?.let { { it(item) } },
                            )
                        }
                    }
                }
            }
            if (isArtist) aboutItem()
                }
            } else {
                ArtistShelfGridPage(
                    artistBrowseId = page.browseId,
                    shelf = targetShelf,
                    palette = palette,
                    onItemClick = onSectionItemClick,
                    onItemLongPress = onSectionItemLongPress,
                    contentPadding = contentPadding,
                )
            }
        }
    }
    }
}

/**
 * An album or playlist: the title, credit, meta and action buttons that sit
 * over the foot of the artwork.
 *
 * The artwork itself is not here — [PageBackground] draws it, blurred foot and
 * all, behind the list. What this item holds in
 * its place is a spacer of exactly the picture's height, which is what keeps
 * the two in step: the list reserves the room, the background fills it.
 */
@Composable
private fun ReleaseHeader(
    page: DetailPage,
    palette: ArtworkPalette,
    artHeight: Dp,
    trackCount: Int,
    songs: List<Song>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onArtistClick: (String, String) -> Unit,
    onToggleLibrary: (() -> Unit)?,
    onAddMusic: (() -> Unit)? = null,
    /** True only when the playlist is known empty (not still loading). */
    showEmptyAddMusic: Boolean = false,
) {
    val (credit, meta) = page.headerLines(trackCount, songs.playtime())
    // Every row on a release carries the same credit — see [pageCredit] — so
    // the first one speaks for the whole page, the same source the rows'
    // own long-press "Open artist" already reads from.
    val artist = songs.firstOrNull()

    // The outer Box just needs to be as tall as its content — we don't force
    // an aspect ratio here so the action buttons can extend below the artwork.
    Box(Modifier.fillMaxWidth()) {

        Spacer(Modifier.fillMaxWidth().height(artHeight + HEADER_DROP))

        // Text + action row stacked, pinned to the bottom of the Box.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                color = palette.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = HEADER_GUTTER),
            )
            // Artist / credit line
            if (credit.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = credit,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.W400,
                        fontSize = 20.sp,
                    ),
                    color = palette.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = HEADER_GUTTER)
                        .let { m ->
                            // Playlist credit is the account that owns it, not
                            // the first track's artist — don't wire the tap.
                            val id = artist?.artistId?.takeIf {
                                page.type != BrowseType.PLAYLIST
                            }
                            if (id == null) {
                                m
                            } else {
                                m.clip(RoundedCornerShape(6.dp))
                                    .clickable { onArtistClick(id, artist.artist) }
                            }
                        },
                )
            }
            // Metadata (kind • year • count)
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
                    color = palette.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = HEADER_GUTTER),
                )
            }

            // Action buttons — live inside the header so there is zero gap
            // between the cover zone and the first song row. Shuffle and add
            // flank a wide Play pill, the Apple Music release row.
            //
            // An empty editable playlist has nothing to play yet, so Add Music
            // takes that spot instead of leaving a blank strip under the title.
            when {
                songs.isNotEmpty() -> {
                    // Only where YouTube said the release can be saved and the
                    // caller is willing to take the write — see [onToggleLibrary].
                    val library = page.library?.takeIf { onToggleLibrary != null }
                    val trailingAdd = onAddMusic.takeIf { library == null }
                    // Shuffle + library/add at most — overflow lives in the top bar
                    // next to Sort.
                    val circleSize = if (library != null || trailingAdd != null) 46.dp else 50.dp
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = HEADER_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (library != null || trailingAdd != null) 8.dp else 10.dp,
                            Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircleIconButton(
                            icon = VeloraIcons.Shuffle,
                            contentDescription = stringResource(R.string.shuffle),
                            palette = palette,
                            onClick = onShuffle,
                            haptic = Haptic.Resume,
                            size = circleSize,
                        )
                        PlayPill(
                            onClick = onPlay,
                            // Roughly half the row — Apple Music's pill, not a
                            // stretch that eats every gap between the circles.
                            modifier = Modifier.fillMaxWidth(0.58f),
                            size = circleSize,
                        )
                        if (library != null) {
                            CircleIconButton(
                                // A tick, not a filled-in plus: the pair reads as
                                // "not yet / done", which is what the state is.
                                icon = if (library.saved) VeloraIcons.Check else VeloraIcons.Plus,
                                contentDescription = if (library.saved) {
                                    stringResource(R.string.remove_from_library)
                                } else {
                                    stringResource(R.string.add_to_library)
                                },
                                palette = palette,
                                onClick = { onToggleLibrary?.invoke() },
                                haptic = if (library.saved) Haptic.ToggleOff else Haptic.ToggleOn,
                                size = circleSize,
                            )
                        } else if (trailingAdd != null) {
                            CircleIconButton(
                                icon = VeloraIcons.Plus,
                                contentDescription = stringResource(R.string.add_music),
                                palette = palette,
                                onClick = trailingAdd,
                                haptic = Haptic.Tap,
                                size = circleSize,
                            )
                        }
                    }
                    // Owned playlists that already show a library circle still
                    // need a way into Add Music — a second row under Play.
                    if (onAddMusic != null && library != null) {
                        Spacer(Modifier.height(10.dp))
                        AddMusicPill(onClick = onAddMusic, palette = palette)
                    }
                }
                onAddMusic != null && showEmptyAddMusic -> {
                    Spacer(Modifier.height(14.dp))
                    AddMusicPill(onClick = onAddMusic, palette = palette)
                }
            }
        }
    }
}

/** Full-width Add Music control for empty (or library-flanked) playlists. */
@Composable
private fun AddMusicPill(
    onClick: () -> Unit,
    palette: ArtworkPalette,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HEADER_GUTTER)
            .height(50.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(palette.elevated, RoundedCornerShape(percent = 50))
            .border(GLASS_EDGE_WIDTH, palette.onBackground.copy(alpha = 0.18f), RoundedCornerShape(percent = 50))
            .clickable {
                haptics.play(Haptic.Tap)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            VeloraIcons.Plus,
            contentDescription = null,
            tint = palette.onBackground,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.add_music),
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
        )
    }
}

private fun List<Song>.sortedForDetail(sort: SongSort): List<Song> = when (sort) {
    SongSort.DEFAULT -> this
    SongSort.TITLE_ASC -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.TITLE_DESC -> sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
    // A catalogue row carries no added date, so its place in the running
    // order stands in for one: a playlist is appended to as songs are added,
    // and reversed that puts the most recent addition on top. Rows that do
    // carry a MediaStore timestamp — device tracks, downloads — are dated
    // properly, with the position order left to break the ties.
    SongSort.DATE_ADDED_ASC -> withIndex()
        .sortedWith(
            compareBy<IndexedValue<Song>> { (_, song) ->
                song.localDateAddedSeconds ?: Long.MAX_VALUE
            }.thenBy { (position, _) -> position },
        )
        .map { it.value }
    SongSort.DATE_ADDED_DESC -> withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Song>> { (_, song) ->
                song.localDateAddedSeconds ?: Long.MIN_VALUE
            }.thenByDescending { (position, _) -> position },
        )
        .map { it.value }
}

/**
 * An artist: their Apple title logo (when one exists) or name across the foot
 * of the photo [PageBackground] is drawing behind this. See [ReleaseHeader]
 * for why the picture isn't here.
 */
@Composable
private fun ArtistHeader(
    page: DetailPage,
    palette: ArtworkPalette,
    artHeight: Dp,
    appleArt: AppleArtistArt?,
) {
    Box(Modifier.fillMaxWidth()) {
        Spacer(Modifier.fillMaxWidth().height(artHeight + HEADER_DROP - ARTIST_HEADER_LIFT))
        val logoUrl = appleArt?.logoUrl
        // Whether the logo has actually been drawn. Until it has, the name
        // stands in so the header is never left empty.
        var logoShown by remember(logoUrl) { mutableStateOf(false) }
        if (appleArt != null && logoUrl != null) {
            val context = LocalContext.current
            var attempt by remember(logoUrl) { mutableIntStateOf(0) }
            var failed by remember(logoUrl) { mutableStateOf(false) }
            val request = remember(logoUrl, attempt) {
                ImageRequest.Builder(context).data(logoUrl).size(coil3.size.Size.ORIGINAL).build()
            }
            LaunchedEffect(failed) {
                if (failed && attempt < LOGO_RETRIES) {
                    delay(LOGO_RETRY_DELAY_MS)
                    failed = false
                    attempt++
                }
            }
            val aspect = appleArt.logoAspect.coerceIn(0.4f, 6f)
            val maxLogoHeight = artHeight * ARTIST_LOGO_MAX_HEIGHT
            AsyncImage(
                model = request,
                contentDescription = page.title,
                contentScale = ContentScale.Fit,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> logoShown = true
                        is AsyncImagePainter.State.Error -> failed = true
                        else -> Unit
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 14.dp, bottom = 20.dp)
                    .layout { measurable, constraints ->
                        val widest = constraints.maxWidth * ARTIST_LOGO_WIDTH
                        val height = minOf(widest / aspect, maxLogoHeight.toPx()).roundToInt()
                        val width = (height * aspect).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(width, height) { placeable.place(0, 0) }
                    },
            )
        }
        if (!logoShown) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.displayLarge,
                color = palette.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Bottom is half the top inset — the action row sits closer
                    // under the name than the name sits under the artwork.
                    .padding(start = HEADER_GUTTER, end = HEADER_GUTTER, top = 14.dp, bottom = 7.dp),
            )
        }
    }
}

/**
 * Everything on a detail page that is colour rather than words: the page's
 * solid [pageColor], and over it the hero photograph with its blurred foot.
 */
@Composable
private fun PageBackground(
    foot: HeroFoot?,
    pageColor: Color,
    canvas: CanvasArtwork?,
    artHeight: Dp,
    heroScrollPx: State<Int>,
    videoBlur: FootBlurSpec?,
    chromeHazeState: HazeState?,
    expectsHeaderVideo: Boolean,
    onHeaderVideoActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val snapPageColor = reduceAnimation || expectsHeaderVideo
    val fill by animateColorAsState(
        targetValue = pageColor,
        animationSpec = if (snapPageColor) snap() else tween(PAGE_SOLID_FADE_MS),
        label = "pageSolid",
    )
    // Faded in only when it had to be read first: a hero read before is there
    // on the page's first frame.
    val heroAlpha = remember { Animatable(if (foot != null) 1f else 0f) }
    LaunchedEffect(foot != null, expectsHeaderVideo) {
        when {
            foot == null -> heroAlpha.snapTo(0f)
            reduceAnimation || expectsHeaderVideo -> heroAlpha.snapTo(1f)
            else -> heroAlpha.animateTo(1f, tween(HERO_FADE_MS))
        }
    }
    val clip = canvas?.takeIf {
        videoBlur != null && !reduceDynamicBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    var clipCover by remember(clip?.url) { mutableFloatStateOf(0f) }
    val clipHandoff = clip != null && clipCover >= HEADER_CLIP_HANDOFF_COVER
    val hazeStill = foot != null && chromeHazeState != null && (expectsHeaderVideo || clip != null)
    val headerVideoActive = clip != null && chromeHazeState != null
    SideEffect {
        onHeaderVideoActive(
            (expectsHeaderVideo && foot != null) ||
                (headerVideoActive && clipHandoff),
        )
    }
    DisposableEffect(Unit) {
        onDispose { onHeaderVideoActive(false) }
    }
    val density = LocalDensity.current
    val heroBoxHeightPx = foot?.let {
        with(density) { (artHeight * (1f + it.runFraction)).roundToPx() }
    } ?: 0
    val heroDecodeActive by remember(heroBoxHeightPx) {
        derivedStateOf {
            heroBoxHeightPx <= 0 || heroScrollPx.value < heroBoxHeightPx
        }
    }

    Box(modifier.background(fill).clipToBounds()) {
        if (foot != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(artHeight * (1f + foot.runFraction))
                    .heroParallaxScroll(heroScrollPx),
            ) {
                val stillModifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = heroAlpha.value
                        // The clip fades on its own. Leaving the still under
                        // it would fade twice, and the picture would be gone
                        // before the bottom of the box.
                        // The still's foot is only cut away once the clip is
                        // actually drawing — otherwise the page colour shows
                        // through bare for a moment under the fade line.
                        if (clipHandoff) {
                            this.clip = true
                            shape = StillAboveFade(foot.runFraction)
                        }
                    }
                if (hazeStill) {
                    chromeHazeState?.let { haze ->
                        Box(Modifier.hazeSource(haze).matchParentSize()) {
                            Image(
                                bitmap = foot.image,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = stillModifier,
                            )
                        }
                    }
                } else {
                    Image(
                        bitmap = foot.image,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = stillModifier,
                    )
                }
                if (clip != null) {
                    CanvasArtworkPlayer(
                        canvas = clip,
                        isPlaying = heroDecodeActive,
                        footBlur = videoBlur,
                        onCoverChanged = { clipCover = it },
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

/**
 * Lets vertical drags on a nested horizontal row reach the parent [LazyColumn].
 * Some OEM touch stacks (notably Samsung on One UI 8) otherwise keep the row
 * and the page feels stuck.
 */
private fun Modifier.forwardVerticalScrollTo(parent: LazyListState): Modifier = composed {
    nestedScroll(
        remember(parent) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y == 0f) return Offset.Zero
                    return Offset(0f, parent.dispatchRawDelta(available.y))
                }
            }
        },
    )
}

/**
 * Parallax from list scroll. Scroll offset is read in the offset lambda so the
 * hero moves every frame; reading it only in a [layout] placeable left album
 * covers pinned to the viewport while the list moved underneath.
 */
private fun Modifier.heroParallaxScroll(heroScrollPx: State<Int>): Modifier =
    offset { IntOffset(0, -heroScrollPx.value) }

/** Cuts the still off where the fade starts, so a clip's own fade is the only one. */
private class StillAboveFade(private val runFraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cut = size.height * (FOOT_FADE_START / (1f + runFraction))
        return Outline.Rectangle(Rect(0f, 0f, size.width, cut.coerceIn(0f, size.height)))
    }
}

/** The page settling into its hero's colour once that has been read. */
private const val PAGE_SOLID_FADE_MS = 220

/** A hero that had to be read first, arriving. */
private const val HERO_FADE_MS = 240

/**
 * Still-to-clip handoff waits until the clip fade has nearly finished.
 * [CanvasArtworkPlayer] sets [onRenderedChanged] at first frame while alpha
 * is still easing up — cutting the still early exposes the page colour.
 */
private const val HEADER_CLIP_HANDOFF_COVER = 0.98f

private class HeroScrollCacheEntry {
    val itemHeightsPx = mutableMapOf<Int, Int>()
    var heldScrollPx = 0
}

private val heroScrollCacheByBrowseId = mutableMapOf<String, HeroScrollCacheEntry>()

/**
 * How far the list has scrolled, so the hero behind it stays in step after
 * the header has left the screen. Parking it off once that happened is what
 * cut the picture straight to the page colour.
 *
 * [browseId] keys a process-wide cache so opening an album or "Show all" does
 * not reset the offset when this composable is torn down and the list is still
 * parked partway down the page.
 */
@Composable
private fun rememberHeroScrollPx(
    listState: LazyListState,
    browseId: String,
    maxScrollPx: Int,
    freezeScroll: Boolean,
): State<Int> {
    val entry = remember(browseId) {
        heroScrollCacheByBrowseId.getOrPut(browseId) { HeroScrollCacheEntry() }
    }
    return remember(listState, browseId, maxScrollPx, freezeScroll) {
        derivedStateOf {
            if (freezeScroll) {
                entry.heldScrollPx = 0
                return@derivedStateOf 0
            }
            val layoutInfo = listState.layoutInfo
            for (item in layoutInfo.visibleItemsInfo) {
                entry.itemHeightsPx[item.index] = item.size
            }

            // While the header row is on screen, its layout offset is the true
            // scroll distance — no need to sum earlier item heights yet.
            val item0 = layoutInfo.visibleItemsInfo.find { it.index == 0 }
            val raw = if (item0 != null) {
                (-item0.offset).coerceAtLeast(0)
            } else {
                var total = listState.firstVisibleItemScrollOffset
                for (i in 0 until listState.firstVisibleItemIndex) {
                    val height = entry.itemHeightsPx[i]
                    if (height == null) {
                        return@derivedStateOf entry.heldScrollPx.coerceIn(0, maxScrollPx)
                    }
                    total += height
                }
                total
            }
            val scroll = raw.coerceIn(0, maxScrollPx)
            entry.heldScrollPx = scroll
            scroll
        }
    }
}

/**
 * Shuffle · Play · Star — the artist page's action row, as Apple Music lays
 * it out: a large Play circle with a small glass circle either side.
 *
 * The star is the page's existing subscribe toggle. Where the page offers none
 * (a guest) an empty slot of the same size keeps Play in the middle.
 */
@Composable
private fun ActionRow(
    palette: ArtworkPalette,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    bottomSpace: Dp = 22.dp,
    /** The artist header's subscribe state, or null where it isn't offered. */
    subscription: SubscriptionState? = null,
    onToggleSubscription: (() -> Unit)? = null,
    /** Apple's fill for the Play circle; null leaves it white. */
    playColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HEADER_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(ARTIST_ACTION_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = VeloraIcons.Shuffle,
            contentDescription = stringResource(R.string.shuffle),
            palette = palette,
            onClick = onShuffle,
            haptic = Haptic.Resume,
            size = ARTIST_SIDE_BUTTON,
            lightFill = true,
        )

        PlayPill(
            onClick = onPlay,
            iconOnly = true,
            size = ARTIST_PLAY_BUTTON,
            iconScale = ARTIST_PLAY_ICON_SCALE,
            containerColor = playColor ?: Color.White,
            cutout = true,
        )

        if (subscription != null) {
            CircleIconButton(
                icon = if (subscription.subscribed) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(
                    if (subscription.subscribed) R.string.unsubscribe else R.string.subscribe,
                ),
                palette = palette,
                onClick = { onToggleSubscription?.invoke() },
                haptic = if (subscription.subscribed) Haptic.ToggleOff else Haptic.ToggleOn,
                size = ARTIST_SIDE_BUTTON,
                lightFill = true,
            )
        } else {
            Spacer(Modifier.size(ARTIST_SIDE_BUTTON))
        }
    }
    Spacer(Modifier.height(bottomSpace))
}

/**
 * The prominent Play control that anchors the action row. Releases keep the
 * labeled pill; the artist page uses its icon-only cutout circle. Both use a
 * fixed light surface so the primary action survives every palette.
 */
@Composable
private fun PlayPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 28.dp,
    iconOnly: Boolean = false,
    size: Dp = 50.dp,
    iconScale: Float = 0.44f,
    containerColor: Color = Color.White,
    /** Punch the triangle out of the circle, so the page shows through it, as Apple's does. */
    cutout: Boolean = false,
) {
    // Black on the light fills Apple picks, white on the rare dark one.
    val contentColor = if (containerColor.luminance() > 0.35f) Color.Black else Color.White
    // Resume rather than a flat tap: this button starts a queue, and the rising
    // pair says so.
    val haptics = rememberHaptics()
    if (iconOnly && cutout) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .clickable {
                    haptics.play(Haptic.Resume)
                    onClick()
                }
                // Offscreen so the clear below cuts the circle drawn under it,
                // not whatever the page has behind.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawCircle(containerColor)
                    val box = this.size.minDimension * iconScale
                    val unit = box / 24f
                    val origin = Offset((this.size.width - box) / 2f, (this.size.height - box) / 2f)
                    val triangle = androidx.compose.ui.graphics.Path().apply {
                        moveTo(origin.x + 6.8f * unit, origin.y + 4.8f * unit)
                        lineTo(origin.x + 19.2f * unit, origin.y + 12f * unit)
                        lineTo(origin.x + 6.8f * unit, origin.y + 19.2f * unit)
                        close()
                    }
                    drawPath(triangle, Color.Black, blendMode = BlendMode.Clear)
                    // The same round-joined pen the icon is drawn with.
                    drawPath(
                        triangle,
                        Color.Black,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2f * unit,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                        blendMode = BlendMode.Clear,
                    )
                }
                .semantics { contentDescription = "Play" },
        )
        return
    }
    Row(
        modifier = modifier
            .then(if (iconOnly) Modifier.size(size) else Modifier.height(size))
            .clip(CircleShape)
            .background(containerColor)
            .clickable {
                haptics.play(Haptic.Resume)
                onClick()
            }
            .then(if (iconOnly) Modifier else Modifier.padding(horizontal = horizontalPadding)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = VeloraIcons.Play,
            contentDescription = if (iconOnly) stringResource(R.string.play) else null,
            tint = contentColor,
            modifier = Modifier.size(if (iconOnly) size * iconScale else 18.dp),
        )
        if (!iconOnly) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.play),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
            )
        }
    }
}

/**
 * Small circular icon-only button — used for Shuffle and Download flanking the
 * Play pill. Translucent glassy fill, accent-coloured icon.
 */
@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    haptic: Haptic = Haptic.Tap,
    size: Dp = 50.dp,
    /** Whiter veil in place of the dark glass — the artist page's Apple look. */
    lightFill: Boolean = false,
) {
    val haptics = rememberHaptics()
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (lightFill) {
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), CircleShape)
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(palette.elevated, CircleShape)
                        .border(GLASS_EDGE_WIDTH, palette.onBackground.copy(alpha = 0.18f), CircleShape)
                },
            )
            .clickable {
                haptics.play(haptic)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (lightFill) MaterialTheme.colorScheme.onSurface else palette.onBackground,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/** Track count and running time, the way a release page signs off. */
@Composable
private fun ReleaseFooter(songs: List<Song>, palette: ArtworkPalette) {
    Text(
        text = songs.playtimeSummary(),
        style = MaterialTheme.typography.labelMedium,
        color = palette.onBackgroundVariant,
        modifier = Modifier.padding(start = HEADER_GUTTER, end = HEADER_GUTTER, top = 18.dp),
    )
}

/** "1.2M subscribers" and "3.4M monthly listeners", off the artist header. */
@Composable
private fun ArtistStatsRow(
    subscriberCountText: String?,
    monthlyListenerCount: String?,
    palette: ArtworkPalette,
) {
    // Stacked and left-aligned with the text under it, since this now sits in
    // the About section rather than across the header.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ARTIST_CONTENT_GUTTER, end = ARTIST_CONTENT_GUTTER, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // YouTube's own count text already reads "1.2M subscribers" in full,
        // so only the number is kept and the label re-said in the app's own
        // words — the one way to fit both stats on one line on a narrow
        // screen without either wrapping into two.
        subscriberCountText?.let {
            StatChip(
                icon = Icons.Rounded.Person,
                text = stringResource(R.string.subscribers, it.substringBefore(' ')),
                palette = palette,
            )
        }
        monthlyListenerCount?.let {
            StatChip(
                icon = Icons.Rounded.GraphicEq,
                text = stringResource(R.string.monthly_listeners, it.substringBefore(' ')),
                palette = palette,
            )
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String, palette: ArtworkPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(palette.elevated, CircleShape)
            .border(GLASS_EDGE_WIDTH, palette.onBackground.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.onBackgroundVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = palette.onBackgroundVariant,
        )
    }
}

/**
 * YouTube's own editorial note for a release or an artist, collapsed to a
 * few lines with a tap to read the rest — the same "About" block Apple
 * Music and YouTube Music itself show under the header.
 *
 * Whether there's anything to expand is only knowable once the text has
 * been laid out at the collapsed line count, so the "More" toggle is held
 * back until that measurement says the clipped text actually lost
 * something — otherwise a two-line bio would show a toggle with nothing
 * behind it to reveal.
 */
@Composable
private fun AboutSection(
    title: String,
    text: String?,
    palette: ArtworkPalette,
    horizontalPadding: Dp = ABOUT_GUTTER,
    /** Drawn under the text. */
    stats: (@Composable () -> Unit)? = null,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var clipped by remember(text) { mutableStateOf(false) }
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
            modifier = Modifier.padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 2.dp,
                bottom = 6.dp,
            ),
        )
        if (text != null) Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = palette.onBackgroundVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) clipped = result.hasVisualOverflow },
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = horizontalPadding)
                .let { m -> if (clipped || expanded) m.clickable { expanded = !expanded } else m },
        )
        if (clipped || expanded) {
            Text(
                text = stringResource(if (expanded) R.string.less else R.string.more),
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = 4.dp)
                    .clickable { expanded = !expanded },
            )
        }
        if (stats != null) {
            Spacer(Modifier.height(if (text != null) 12.dp else 4.dp))
            stats()
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    palette: ArtworkPalette,
    onShowAll: (() -> Unit)? = null,
    horizontalPadding: Dp = PAGE_GUTTER,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 10.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.onBackground,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onShowAll != null) {
            Text(
                text = stringResource(R.string.show_all),
                style = MaterialTheme.typography.titleSmall,
                color = palette.accent,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/** Compact suggested row for the horizontal playlist carousel. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactSuggestedSongRow(
    song: Song,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAdd: () -> Unit,
    downloadedTint: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(7.dp))
                .thumbnailBorder(RoundedCornerShape(7.dp))
                .background(palette.elevated),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onBackground,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (downloadedTint != null) {
            DownloadedBadge(song.videoId, downloadedTint)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.16f))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add_to_playlist),
                tint = palette.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Compact row used inside the artist song grid; no swipe, to keep the
 *  horizontal pager's gestures unambiguous. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactSongRow(
    song: Song,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    downloadedTint: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(7.dp))
                .thumbnailBorder(RoundedCornerShape(7.dp))
                .background(palette.elevated),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium,
                color = palette.onBackground,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (downloadedTint != null) {
            DownloadedBadge(song.videoId, downloadedTint)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onLongPress),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = palette.onBackgroundVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The newest release on an artist's shelves — an album, or a single or EP that
 * came out after it. Playlists, videos and related artists share these shelves
 * and are skipped: only an `MPRE…` browse id is a release.
 *
 * Newest by the year in the card's subtitle, which is all a shelf item says
 * about when. Ties and releases with no year keep YouTube's own order, which
 * lists albums first.
 */
private fun List<HomeShelf>.topRelease(): ShelfItem? =
    asSequence()
        .flatMap { it.items.asSequence() }
        .filter { it.browseId?.startsWith("MPRE") == true }
        .withIndex()
        .maxWithOrNull(
            compareBy<IndexedValue<ShelfItem>> { it.value.releaseYear() ?: 0 }
                .thenByDescending { it.index },
        )?.value

private val RELEASE_YEAR = Regex("""\b(19|20)\d{2}\b""")

private fun ShelfItem.releaseYear(): Int? =
    RELEASE_YEAR.find(subtitle)?.value?.toIntOrNull()

/** "Recent Single" and "Recent EP" where the card says so, otherwise "Recent Album". */
@androidx.annotation.StringRes
private fun ShelfItem.recentLabel(): Int {
    val kind = subtitle.lowercase()
    return when {
        kind.contains("single") -> R.string.recent_single
        Regex("""\bep\b""").containsMatchIn(kind) -> R.string.recent_ep
        else -> R.string.recent_album
    }
}

/**
 * The artist page's top-release card: sleeve, what it is and when, and its
 * title, in a rounded glass panel the width of the page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopReleaseCard(
    item: ShelfItem,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(28.dp)
    val coverShape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ARTIST_CONTENT_GUTTER)
            .padding(bottom = 22.dp)
            // A light veil rather than the dark glass: Apple's containers read
            // slightly white against the page, and a dark panel reads as a hole.
            .clip(shape)
            .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), shape)
            .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), shape)
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .clip(coverShape)
                .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_COVER_EDGE_ALPHA), coverShape)
                .background(palette.elevated),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(item.recentLabel()),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionCard(
    item: ShelfItem,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    onLongPress: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .thumbnailBorder(RoundedCornerShape(10.dp))
                .background(palette.elevated),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistShelfGridPage(
    artistBrowseId: String,
    shelf: HomeShelf,
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberArtistShelfGridState(artistBrowseId, shelf)
    BoxWithConstraints(modifier.fillMaxSize().background(palette.background)) {
        val grid = libraryGrid(maxWidth - PAGE_GUTTER * 2)
        LazyVerticalGrid(
            columns = GridCells.Fixed(grid.columns),
            state = gridState,
            contentPadding = PaddingValues(
                top = topBarContentPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PAGE_GUTTER),
        ) {
            items(shelf.items, key = { it.browseId ?: it.title }) { item ->
                SectionCard(
                    item = item,
                    palette = palette,
                    onClick = { onItemClick(item) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Splits the one subtitle a browse row hands over — "Album • Travis Scott •
 * 2023", or sometimes just "Travis Scott" — into the credit line and the
 * metadata line the header shows separately.
 *
 * Everything is optional, because every caller supplies a different amount of
 * it: the player knows an album's artist but not its year, search knows both,
 * and a home card frequently knows neither.
 *
 * [playtime] is the running length of the tracks currently loaded on the page
 * ("41 min"), so a playlist's figure counts up with the list it sits over.
 */
@Composable
private fun DetailPage.headerLines(trackCount: Int, playtime: String? = null): Pair<String, String> {
    // Playlists bill as "Playlist • Alice" or "Playlist • Alice • 12 songs".
    // Keep the owner on the credit line; put a *live* track count on meta next
    // to Playlist — never reuse the subtitle's tally, which goes stale the
    // moment a song is added or removed.
    if (type == BrowseType.PLAYLIST) {
        val parts = PlaylistPrivacy.stripFromSubtitle(subtitle)
            .split("•", "·")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val kind = parts.firstOrNull { it.lowercase(Locale.ROOT) in KIND_WORDS }
        val owner = parts.filter {
            it != kind &&
                !it.matches(PLAYLIST_SUBTITLE_TALLY) &&
                !PlaylistPrivacy.isPrivacyLabel(it)
        }.joinToString(", ")
        val kindLabel = stringResource(R.string.playlist)
        val visibility = playlistPrivacy?.label
            ?: PlaylistPrivacy.fromSubtitle(subtitle)?.label
        val meta = listOfNotNull(
            kindLabel,
            trackCount.takeIf { it > 0 }?.let {
                pluralStringResource(R.plurals.track_count_plural, it, it)
            },
            playtime,
            visibility,
        ).joinToString(" • ")
        return owner to meta
    }
    val parts = subtitle.split("•", "·").map { it.trim() }.filter { it.isNotEmpty() }
    val year = parts.lastOrNull { it.length == 4 && it.all(Char::isDigit) }
    val kind = parts.firstOrNull { it.lowercase(Locale.ROOT) in KIND_WORDS }
    val credit = parts.filter { it != year && it != kind }.joinToString(", ")
    // Title case, not all-caps — "Album • 2025 • 12 tracks", not "ALBUM • …".
    val kindLabel = (kind ?: type.localizedLabel())?.let { label ->
        when (label.lowercase(Locale.ROOT)) {
            "ep" -> "EP"
            else -> label.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
    }
    val meta = listOfNotNull(
        kindLabel,
        year,
        trackCount.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.track_count_plural, it, it)
        },
        playtime,
    ).joinToString(" • ")
    return credit to meta
}

/**
 * Short running length of the tracks currently on screen — "41 min", or
 * "1h 12m" past an hour. Null when nothing has a known duration yet, so the
 * meta line does not invent a figure ahead of the list it counts up with.
 */
@Composable
private fun List<Song>.playtime(): String? {
    val minutes = sumOf { it.durationMillis() } / 60_000
    return when {
        minutes <= 0 -> null
        minutes < 60 -> stringResource(R.string.minutes_short, minutes.toInt())
        else -> stringResource(R.string.hours_minutes_short, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

/** Subtitle words that name what a page *is* rather than who made it. */
private val KIND_WORDS = setOf(
    "album", "single", "ep", "playlist", "artist", "podcast", "episode", "song", "video",
)

/** "12 songs" / "3 tracks" glued onto a playlist subtitle — not a person. */
private val PLAYLIST_SUBTITLE_TALLY = Regex(
    """[\d.,]+\s*(songs?|tracks?)\b.*""",
    RegexOption.IGNORE_CASE,
)

@Composable
private fun BrowseType.localizedLabel(): String? = when (this) {
        BrowseType.ALBUM -> stringResource(R.string.album)
        BrowseType.PLAYLIST -> stringResource(R.string.playlist)
        BrowseType.ARTIST -> stringResource(R.string.artist)
        BrowseType.OTHER -> null
    }

/** "12 songs, 41 minutes" — omitting the time when the rows carry no durations. */
@Composable
private fun List<Song>.playtimeSummary(): String {
    val count = pluralStringResource(R.plurals.song_count_plural, size, size)
    val minutes = sumOf { it.durationText.toSeconds() } / 60
    return when {
        minutes <= 0 -> count
        minutes < 60 -> stringResource(R.string.song_count_with_minutes, count, minutes)
        else -> {
            val hours = minutes / 60
            val rest = minutes % 60
            val hourLabel = pluralStringResource(R.plurals.hour_count, hours.toInt(), hours)
            if (rest == 0) {
                stringResource(R.string.song_count_with_duration, count, hourLabel)
            } else {
                stringResource(R.string.song_count_with_hours_minutes, count, hourLabel, rest)
            }
        }
    }
}

/** "3:45" or "1:02:33" as seconds; 0 for anything that isn't a duration. */
private fun String?.toSeconds(): Int {
    val parts = this?.split(":")?.map { it.trim().toIntOrNull() ?: return 0 } ?: return 0
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> 0
    }
}
