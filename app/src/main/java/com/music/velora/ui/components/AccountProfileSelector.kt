package com.music.velora.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.velora.R
import com.music.velora.auth.GoogleAccountSession
import com.music.velora.auth.YouTubeProfile
import dev.chrisbanes.haze.HazeState

/**
 * Account and profile picker — same [FrostedSheet] shell as [SongActionsSheet]
 * and [PlaylistPickerSheet]. The host wraps this in a transparent
 * [androidx.compose.material3.ModalBottomSheet] for system sheet motion and
 * predictive back.
 */
@Composable
fun AccountProfileSelector(
    accounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    hazeState: HazeState,
    onSelect: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = frostChromeColors()
    val canRemoveAccount = accounts.size > 1
    val showAccountLabel = accounts.size > 1
    val profiles = remember(accounts) {
        accounts.flatMap { account ->
            account.profiles.map { profile -> account to profile }
        }
    }
    val activeProfile = profiles.find { (account, profile) ->
        account.accountId == activeAccountId && profile.profileId == activeProfileId
    }
    val otherProfiles = remember(profiles, activeProfile) {
        if (activeProfile == null) profiles else profiles.filter { it != activeProfile }
    }

    FrostedSheet(
        hazeState = hazeState,
        modifier = modifier,
        scrollable = false,
    ) {
        SheetHeading(stringResource(R.string.accounts))

        if (activeProfile != null) {
            val (account, profile) = activeProfile
            ProfileSheetRow(
                profile = profile,
                accountLabel = account.email.ifBlank { account.name }.takeIf { showAccountLabel },
                selected = true,
                onClick = { onSelect(account, profile) },
                onLongClick = if (canRemoveAccount) {
                    { onRemoveAccount(account) }
                } else {
                    null
                },
            )
            ActionRow(
                icon = Icons.Rounded.Add,
                label = stringResource(R.string.add_account),
                accent = chrome.accent,
                onClick = onAddAccount,
            )
        } else {
            ActionRow(
                icon = Icons.Rounded.Add,
                label = stringResource(R.string.add_account),
                accent = chrome.accent,
                onClick = onAddAccount,
            )
        }

        if (otherProfiles.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                thickness = 0.5.dp,
                color = chrome.edge.copy(alpha = 0.55f),
            )
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(
                    items = otherProfiles,
                    key = { (account, profile) -> "${account.accountId}:${profile.profileId}" },
                ) { (account, profile) ->
                    ProfileSheetRow(
                        profile = profile,
                        accountLabel = account.email.ifBlank { account.name }.takeIf { showAccountLabel },
                        selected = account.accountId == activeAccountId && profile.profileId == activeProfileId,
                        onClick = { onSelect(account, profile) },
                        onLongClick = if (canRemoveAccount) {
                            { onRemoveAccount(account) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            thickness = 0.5.dp,
            color = chrome.edge.copy(alpha = 0.55f),
        )
        ActionRow(
            icon = Icons.Rounded.Settings,
            label = stringResource(R.string.settings),
            accent = chrome.accent,
            onClick = onOpenSettings,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileSheetRow(
    profile: YouTubeProfile,
    accountLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val chrome = frostChromeColors()
    val description = stringResource(if (selected) R.string.selected_account else R.string.switch_account)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                },
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (profile.avatar != null) {
            AsyncImage(
                model = profile.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(chrome.tint.copy(alpha = 0.55f)),
            )
        } else {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = chrome.contentVariant,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(chrome.tint.copy(alpha = 0.55f))
                    .padding(10.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge,
                color = chrome.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = accountLabel ?: profile.handle.ifBlank {
                stringResource(if (profile.isBrandAccount) R.string.brand_account else R.string.personal)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = chrome.contentVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) chrome.accent else chrome.contentVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
