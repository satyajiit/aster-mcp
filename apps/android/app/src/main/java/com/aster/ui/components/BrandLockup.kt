package com.aster.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aster.R
import com.aster.ui.theme.AsterTheme

/**
 * The co-branded lockup: Aster, by OpenAlly(TM).
 *
 * Mirrors the web dashboard's BrandLockup so the two surfaces read identically.
 * The OpenAlly mark is drawn from openally_mark.xml and is never retinted — the
 * mark is theme-invariant by brand rule.
 *
 * The trademark belongs on brand surfaces only (onboarding, About, the home app
 * bar) and must not leak into buttons, error copy, or system-facing strings.
 */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tagline: String? = null,
) {
    val colors = AsterTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.bolt),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(if (compact) 26.dp else 34.dp),
        )

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Aster",
                    style = if (compact) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.brand_by),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSubtle,
                    )
                    Image(
                        painter = painterResource(R.drawable.openally_mark),
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                    )
                    Text(
                        text = buildAnnotatedString {
                            append("OpenAlly")
                            withStyle(SpanStyle(fontSize = 8.sp, baselineShift = BaselineShift.Superscript)) {
                                append("™")
                            }
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSubtle,
                    )
                }
            }

            if (!compact && tagline != null) {
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }
        }
    }
}
