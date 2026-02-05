package com.aster.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aster.ui.theme.AsterColors
import com.aster.ui.theme.AsterTheme
import com.aster.ui.theme.AsterTypography
import com.aster.ui.theme.TerminalTextStyles
import compose.icons.FeatherIcons
import compose.icons.feathericons.*

// ============================================================================
// TERMINAL WINDOW
// ============================================================================

/**
 * Terminal window container with macOS-style title bar.
 * Matches the Nuxt dashboard TerminalWindow.vue component.
 */
@Composable
fun TerminalWindow(
    title: String,
    modifier: Modifier = Modifier,
    showScanlines: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, colors.terminalBorder, RoundedCornerShape(2.dp))
            .background(colors.terminalSurface.copy(alpha = 0.95f))
            .drawScanlines(showScanlines)
    ) {
        // Title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0894A3B8)) // ~3% opacity slate
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Window dots (macOS style)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WindowDot(color = colors.dotClose)
                WindowDot(color = colors.dotMinimize)
                WindowDot(color = colors.dotMaximize)
            }

            // Title - centered
            Text(
                text = title.uppercase(),
                style = TerminalTextStyles.WindowTitle,
                color = colors.terminalDim,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Spacer for symmetry with dots
            Spacer(modifier = Modifier.width(52.dp))
        }

        // Border divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.terminalBorder)
        )

        // Content area
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun WindowDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.8f))
    )
}

/**
 * Modifier to draw subtle scanline effect.
 */
private fun Modifier.drawScanlines(enabled: Boolean): Modifier {
    if (!enabled) return this
    return this.drawWithContent {
        drawContent()
        // Draw scanlines overlay
        val lineHeight = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color(0x0494A3B8), // ~1.5% opacity
                start = Offset(0f, y + lineHeight / 2),
                end = Offset(size.width, y + lineHeight / 2),
                strokeWidth = 2.dp.toPx()
            )
            y += lineHeight
        }
    }
}

// ============================================================================
// STATUS INDICATOR
// ============================================================================

/**
 * Status indicator with pulse animation and glow effect.
 */
@Composable
fun StatusIndicator(
    status: StatusType,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
    animated: Boolean = true
) {
    val colors = AsterTheme.colors

    val color = when (status) {
        StatusType.ONLINE -> colors.emerald
        StatusType.PENDING -> colors.amber
        StatusType.OFFLINE -> colors.terminalDim
        StatusType.ERROR -> colors.error
    }

    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )

    val shouldAnimate = animated && (status == StatusType.ONLINE || status == StatusType.PENDING)

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // Glow effect for active states
                if (shouldAnimate) {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = if (shouldAnimate) glowRadius else 6f
                    )
                }
            }
            .clip(CircleShape)
            .background(color)
    )
}

enum class StatusType {
    ONLINE, PENDING, OFFLINE, ERROR
}

// ============================================================================
// TEXT FIELD
// ============================================================================

/**
 * Terminal-style text input using native Android EditText for reliable text scrolling.
 * Material Design minimum height: 56dp for comfortable touch targets.
 */
@Composable
fun TerminalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    prefix: String? = null,
    enabled: Boolean = true
) {
    val colors = AsterTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) colors.primary else colors.terminalBorder
    val backgroundColor = if (isFocused) Color(0x0D94A3B8) else Color(0x0894A3B8)

    // Convert Compose colors to Android color ints
    val textColorInt = colors.terminalText.toArgb()
    val hintColorInt = colors.terminalDim.toArgb()
    val cursorColorInt = colors.primary.toArgb()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (prefix != null) {
                Text(
                    text = prefix,
                    style = AsterTypography.bodyMedium,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            AndroidView(
                factory = { context ->
                    android.widget.EditText(context).apply {
                        // Basic setup
                        setText(value)
                        hint = placeholder
                        isSingleLine = singleLine
                        isEnabled = enabled

                        // Remove default background/underline
                        background = null

                        // Apply JetBrains Mono font
                        typeface = androidx.core.content.res.ResourcesCompat.getFont(
                            context,
                            com.aster.R.font.jetbrains_mono_regular
                        )

                        // Text appearance
                        setTextColor(textColorInt)
                        setHintTextColor(hintColorInt)
                        textSize = 14f

                        // Set keyboard type
                        inputType = when (keyboardType) {
                            KeyboardType.Uri -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
                            KeyboardType.Email -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            KeyboardType.Number -> android.text.InputType.TYPE_CLASS_NUMBER
                            KeyboardType.Phone -> android.text.InputType.TYPE_CLASS_PHONE
                            else -> android.text.InputType.TYPE_CLASS_TEXT
                        }

                        // Horizontal scroll for long text
                        setHorizontallyScrolling(true)

                        // Cursor color (API 29+)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            textCursorDrawable?.setTint(cursorColorInt)
                        }

                        // Text change listener
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                val newValue = s?.toString() ?: ""
                                if (newValue != value) {
                                    onValueChange(newValue)
                                }
                            }
                        })

                        // Focus change listener
                        setOnFocusChangeListener { _, hasFocus ->
                            isFocused = hasFocus
                        }

                        // Padding
                        setPadding(0, 0, 0, 0)
                    }
                },
                update = { editText ->
                    // Only update if text changed externally
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                        editText.setSelection(value.length)
                    }
                    editText.isEnabled = enabled
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            )
        }
    }
}

// Extension to convert Compose Color to Android color int
private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

// ============================================================================
// BLINKING CURSOR
// ============================================================================

/**
 * Terminal blinking cursor with step timing (not smooth fade).
 */
@Composable
fun BlinkingCursor(
    modifier: Modifier = Modifier,
    color: Color = AsterTheme.colors.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 500
                0f at 501
                0f at 1000
            }
        ),
        label = "cursor_alpha"
    )

    Box(
        modifier = modifier
            .width(8.dp)
            .height(16.dp)
            .background(color.copy(alpha = alpha))
    )
}

// ============================================================================
// BUTTONS
// ============================================================================

/**
 * Terminal button with multiple variants matching dashboard styling.
 */
@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.NORMAL
) {
    val colors = AsterTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (textColor, borderColor, bgColor) = when {
        !enabled -> Triple(
            colors.terminalMuted,
            colors.terminalBorder,
            Color.Transparent
        )
        variant == ButtonVariant.PRIMARY -> Triple(
            colors.primary,
            if (isPressed) colors.primary.copy(alpha = 0.4f) else colors.primary.copy(alpha = 0.25f),
            if (isPressed) colors.primary.copy(alpha = 0.15f) else colors.primary.copy(alpha = 0.1f)
        )
        variant == ButtonVariant.SUCCESS -> Triple(
            colors.emerald,
            if (isPressed) colors.emerald.copy(alpha = 0.4f) else colors.emerald.copy(alpha = 0.25f),
            if (isPressed) colors.emerald.copy(alpha = 0.15f) else colors.emerald.copy(alpha = 0.1f)
        )
        variant == ButtonVariant.DANGER -> Triple(
            colors.rose,
            if (isPressed) colors.rose.copy(alpha = 0.4f) else colors.rose.copy(alpha = 0.25f),
            if (isPressed) colors.rose.copy(alpha = 0.15f) else colors.rose.copy(alpha = 0.1f)
        )
        variant == ButtonVariant.WARNING -> Triple(
            colors.amber,
            if (isPressed) colors.amber.copy(alpha = 0.4f) else colors.amber.copy(alpha = 0.25f),
            if (isPressed) colors.amber.copy(alpha = 0.15f) else colors.amber.copy(alpha = 0.1f)
        )
        variant == ButtonVariant.GHOST -> Triple(
            if (isPressed) colors.terminalText else colors.terminalMuted,
            if (isPressed) colors.terminalBorderBright else colors.terminalBorder,
            Color.Transparent
        )
        else -> Triple(
            colors.terminalText,
            colors.terminalBorderBright,
            Color.Transparent
        )
    }

    val padding = when (size) {
        ButtonSize.SMALL -> PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ButtonSize.NORMAL -> PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    }

    val textStyle = when (size) {
        ButtonSize.SMALL -> TerminalTextStyles.ButtonSmall
        ButtonSize.NORMAL -> TerminalTextStyles.Button
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .background(bgColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = textStyle,
            color = textColor
        )
    }
}

enum class ButtonVariant {
    PRIMARY, SUCCESS, DANGER, WARNING, GHOST, SECONDARY
}

enum class ButtonSize {
    SMALL, NORMAL
}

/**
 * Terminal button with leading icon.
 */
@Composable
fun TerminalIconButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.GHOST
) {
    val colors = AsterTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (textColor, borderColor, bgColor) = when {
        !enabled -> Triple(
            colors.terminalMuted,
            colors.terminalBorder,
            Color.Transparent
        )
        variant == ButtonVariant.PRIMARY -> Triple(
            colors.primary,
            if (isPressed) colors.primary.copy(alpha = 0.4f) else colors.primary.copy(alpha = 0.25f),
            if (isPressed) colors.primary.copy(alpha = 0.15f) else colors.primary.copy(alpha = 0.1f)
        )
        variant == ButtonVariant.GHOST -> Triple(
            if (isPressed) colors.terminalText else colors.terminalMuted,
            if (isPressed) colors.terminalBorderBright else colors.terminalBorder,
            Color.Transparent
        )
        else -> Triple(
            colors.terminalText,
            colors.terminalBorderBright,
            Color.Transparent
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .background(bgColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text.uppercase(),
            style = TerminalTextStyles.ButtonSmall,
            color = textColor
        )
    }
}

// ============================================================================
// STAT CARD
// ============================================================================

/**
 * Stat card with icon, value, and accent bar matching dashboard design.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    variant: StatCardVariant = StatCardVariant.CYAN
) {
    val colors = AsterTheme.colors

    val (accentColor, iconBg, iconBorder) = when (variant) {
        StatCardVariant.CYAN -> Triple(colors.primary, colors.primary.copy(alpha = 0.1f), colors.primary.copy(alpha = 0.25f))
        StatCardVariant.EMERALD -> Triple(colors.emerald, colors.emerald.copy(alpha = 0.1f), colors.emerald.copy(alpha = 0.25f))
        StatCardVariant.AMBER -> Triple(colors.amber, colors.amber.copy(alpha = 0.1f), colors.amber.copy(alpha = 0.25f))
        StatCardVariant.VIOLET -> Triple(colors.violet, colors.violet.copy(alpha = 0.1f), colors.violet.copy(alpha = 0.25f))
        StatCardVariant.ROSE -> Triple(colors.rose, colors.rose.copy(alpha = 0.1f), colors.rose.copy(alpha = 0.25f))
        StatCardVariant.BLUE -> Triple(colors.blue, colors.blue.copy(alpha = 0.1f), colors.blue.copy(alpha = 0.25f))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, colors.terminalBorder, RoundedCornerShape(2.dp))
            .background(colors.terminalSurface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon container
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, iconBorder, RoundedCornerShape(6.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Value
                Text(
                    text = value,
                    style = TerminalTextStyles.StatValue,
                    color = accentColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Label with // prefix
                Text(
                    text = "// ${label.uppercase()}",
                    style = TerminalTextStyles.StatLabel,
                    color = accentColor.copy(alpha = 0.7f)
                )
            }
        }

        // Accent bar at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accentColor)
        )
    }
}

enum class StatCardVariant {
    CYAN, EMERALD, AMBER, VIOLET, ROSE, BLUE
}

// ============================================================================
// STATUS BADGE
// ============================================================================

/**
 * Badge for status display with semantic colors.
 */
@Composable
fun StatusBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    val (textColor, bgColor, borderColor) = when (type) {
        BadgeType.CYAN -> Triple(colors.primary, colors.primary.copy(alpha = 0.1f), colors.primary.copy(alpha = 0.3f))
        BadgeType.EMERALD -> Triple(colors.emerald, colors.emerald.copy(alpha = 0.1f), colors.emerald.copy(alpha = 0.3f))
        BadgeType.AMBER -> Triple(colors.amber, colors.amber.copy(alpha = 0.1f), colors.amber.copy(alpha = 0.3f))
        BadgeType.ROSE -> Triple(colors.rose, colors.rose.copy(alpha = 0.1f), colors.rose.copy(alpha = 0.3f))
        BadgeType.VIOLET -> Triple(colors.violet, colors.violet.copy(alpha = 0.1f), colors.violet.copy(alpha = 0.3f))
        BadgeType.BLUE -> Triple(colors.blue, colors.blue.copy(alpha = 0.1f), colors.blue.copy(alpha = 0.3f))
        BadgeType.MUTED -> Triple(colors.terminalMuted, Color.Transparent, colors.terminalBorder)
    }

    Text(
        text = text.uppercase(),
        style = TerminalTextStyles.Badge,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, borderColor, RoundedCornerShape(2.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

enum class BadgeType {
    CYAN, EMERALD, AMBER, ROSE, VIOLET, BLUE, MUTED
}

// ============================================================================
// LOG ENTRY
// ============================================================================

/**
 * Activity log entry component matching dashboard LogEntry.vue
 */
@Composable
fun LogEntry(
    time: String,
    level: LogLevel,
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    val (levelColor, levelIcon, bgColor, borderColor) = when (level) {
        LogLevel.DEBUG -> Quadruple(colors.terminalDim, FeatherIcons.Circle, Color.Transparent, Color.Transparent)
        LogLevel.INFO -> Quadruple(colors.primary, FeatherIcons.Info, Color.Transparent, Color.Transparent)
        LogLevel.WARN -> Quadruple(colors.amber, FeatherIcons.AlertTriangle, colors.amber.copy(alpha = 0.02f), colors.amber)
        LogLevel.ERROR -> Quadruple(colors.error, FeatherIcons.X, colors.error.copy(alpha = 0.02f), colors.error)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .drawBehind {
                // Left border for warn/error
                if (level == LogLevel.WARN || level == LogLevel.ERROR) {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timestamp
        Text(
            text = time,
            style = TerminalTextStyles.LogTime,
            color = colors.terminalDim,
            modifier = Modifier.widthIn(min = 65.dp)
        )

        // Level indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.widthIn(min = 55.dp)
        ) {
            Icon(
                imageVector = levelIcon,
                contentDescription = level.name,
                tint = levelColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = level.name,
                style = TerminalTextStyles.LogLevel,
                color = levelColor
            )
        }

        // Message
        Text(
            text = message,
            style = AsterTypography.bodyMedium,
            color = colors.terminalText,
            modifier = Modifier.weight(1f)
        )
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ============================================================================
// SECTION HEADER
// ============================================================================

/**
 * ASCII-style section header.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "[ ",
            style = TerminalTextStyles.SectionHeader,
            color = colors.terminalBorder
        )
        Text(
            text = title.uppercase(),
            style = TerminalTextStyles.SectionHeader,
            color = colors.primaryDim
        )
        Text(
            text = " ]",
            style = TerminalTextStyles.SectionHeader,
            color = colors.terminalBorder
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.terminalBorder)
        )
    }
}

// ============================================================================
// PROMPT LINE
// ============================================================================

/**
 * Terminal prompt line display.
 */
@Composable
fun PromptLine(
    command: String,
    modifier: Modifier = Modifier,
    path: String = "~",
    showCursor: Boolean = false
) {
    val colors = AsterTheme.colors

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$",
            style = TerminalTextStyles.Prompt,
            color = colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = path,
            style = TerminalTextStyles.Prompt,
            color = colors.emerald
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = command,
            style = TerminalTextStyles.Prompt,
            color = colors.terminalText
        )
        if (showCursor) {
            Spacer(modifier = Modifier.width(2.dp))
            BlinkingCursor()
        }
    }
}

// ============================================================================
// TERMINAL PANEL
// ============================================================================

/**
 * Simple terminal panel without title bar.
 */
@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    showScanlines: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, colors.terminalBorder, RoundedCornerShape(2.dp))
            .background(colors.terminalSurface.copy(alpha = 0.95f))
            .drawScanlines(showScanlines)
            .padding(16.dp),
        content = content
    )
}

// ============================================================================
// ASCII HEADER
// ============================================================================

/**
 * ASCII art header with glow effect.
 */
@Composable
fun AsciiHeader(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = TerminalTextStyles.AsciiArt,
            color = colors.primary,
            modifier = Modifier.drawBehind {
                // Multi-layer glow effect
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = size.maxDimension
                    )
                )
            }
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "// $subtitle",
                style = AsterTypography.labelSmall.copy(
                    letterSpacing = 2.sp
                ),
                color = colors.terminalMuted
            )
        }
    }
}

private val Int.sp: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

// ============================================================================
// DIVIDER
// ============================================================================

/**
 * Terminal-style divider line.
 */
@Composable
fun TerminalDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AsterTheme.colors.terminalBorder)
    )
}

// ============================================================================
// TERMINAL PAGE HEADER
// ============================================================================

private val ASTER_ASCII = """
 █████╗ ███████╗████████╗███████╗██████╗
██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔══██╗
███████║███████╗   ██║   █████╗  ██████╔╝
██╔══██║╚════██║   ██║   ██╔══╝  ██╔══██╗
██║  ██║███████║   ██║   ███████╗██║  ██║
╚═╝  ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝
""".trimIndent()

/**
 * Terminal page header matching the Nuxt dashboard TerminalPageHeader.vue
 * Features centered ASCII logo with description, left/right action slots.
 * Set edgeToEdge = true for sticky toolbar use with full-width styling
 */
@Composable
fun TerminalPageHeader(
    description: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    edgeToEdge: Boolean = false
) {
    val colors = AsterTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (edgeToEdge) {
                    Modifier
                        .background(colors.terminalSurface)
                        .drawBehind {
                            // Bottom border only for edge-to-edge
                            drawLine(
                                color = colors.terminalBorder,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                } else {
                    Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .border(1.dp, colors.terminalBorder, RoundedCornerShape(2.dp))
                        .background(colors.terminalSurface.copy(alpha = 0.95f))
                }
            )
            .drawScanlines(true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (edgeToEdge) {
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    } else {
                        Modifier.padding(20.dp, 20.dp)
                    }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button or spacer
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (showBackButton && onBackClick != null) {
                    TerminalIconButton(
                        icon = FeatherIcons.ArrowLeft,
                        text = "Back",
                        onClick = onBackClick
                    )
                }
            }

            // Center: ASCII Logo + Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.drawBehind {
                    // Glow effect behind ASCII
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            radius = size.maxDimension
                        )
                    )
                }
            ) {
                Text(
                    text = ASTER_ASCII,
                    style = TerminalTextStyles.AsciiArt.copy(
                        fontSize = 8.sp,
                        lineHeight = 9.sp
                    ),
                    color = colors.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "// ${description.uppercase()}",
                    style = AsterTypography.labelSmall.copy(
                        letterSpacing = 2.sp
                    ),
                    color = colors.terminalMuted
                )
            }

            // Right: Action slot or spacer
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                rightContent?.invoke()
            }
        }
    }
}

/**
 * Simpler detail header for inner pages (like the device detail header)
 * Set edgeToEdge = true for sticky toolbar use with full-width styling
 */
@Composable
fun TerminalDetailHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    statusBadge: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    edgeToEdge: Boolean = false
) {
    val colors = AsterTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (edgeToEdge) {
                    Modifier
                        .background(colors.terminalSurface)
                        .drawBehind {
                            // Bottom border only for edge-to-edge
                            drawLine(
                                color = colors.terminalBorder,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                } else {
                    Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .border(1.dp, colors.terminalBorder, RoundedCornerShape(2.dp))
                        .background(colors.terminalSurface.copy(alpha = 0.95f))
                }
            )
            .drawScanlines(true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (edgeToEdge) {
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    } else {
                        Modifier.padding(20.dp)
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Back button + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBackButton && onBackClick != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, colors.terminalBorder, RoundedCornerShape(4.dp))
                                .background(colors.primary.copy(alpha = 0.1f))
                                .clickable(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FeatherIcons.ArrowLeft,
                                contentDescription = "Back",
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = title.uppercase(),
                                style = AsterTypography.headlineMedium,
                                color = colors.terminalTextBright
                            )
                            statusBadge?.invoke()
                        }

                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "// $subtitle",
                                style = AsterTypography.labelSmall.copy(
                                    letterSpacing = 1.sp
                                ),
                                color = colors.terminalMuted
                            )
                        }
                    }
                }

                // Right: Actions
                rightContent?.invoke()
            }
        }
    }
}

