package com.aster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aster.R
import com.aster.ui.theme.AsterTheme

private val StarAccent = Color(0xFFF59E0B)

/**
 * An invitation to star the repository — not a nag.
 *
 * Dismissal is persisted by the caller, so it does not reappear on next launch.
 * Opening the repo counts as acting on it and also dismisses.
 */
@Composable
fun StarRepoCard(
    onStar: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AsterTheme.colors

    AsterCard(modifier = modifier.fillMaxWidth()) {
        Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StarAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = StarAccent,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.star_repo_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                )
                Text(
                    text = stringResource(R.string.star_repo_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSubtle,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsterButton(
                onClick = onStar,
                text = stringResource(R.string.star_repo_action),
                modifier = Modifier.weight(1f),
                variant = AsterButtonVariant.PRIMARY,
            )
            Text(
                text = stringResource(R.string.star_repo_dismiss),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        }
    }
}
