package com.example.geofencegroups.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.geofencegroups.ui.theme.Orbit
import com.example.geofencegroups.ui.theme.Space

/** Page chrome: warm background, status-bar inset, optional back button and trailing action. */
@Composable
fun OrbitScreen(
    title: String, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Orbit.Paper).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Orbit.Forest)
                }
            } else Spacer(Modifier.width(Space.md))
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1)
            trailing()
        }
        Box(Modifier.fillMaxWidth().weight(1f), content = content)
    }
}

@Composable
fun navBarPadding(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

@Composable
fun OrbitCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, tint: Color = Orbit.Surface, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    val base = modifier.fillMaxWidth().clip(shape).background(tint).let {
        if (onClick != null) it.clickable(onClick = onClick) else it
    }
    Column(base.padding(Space.lg), content = content)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick, enabled = enabled && !loading, modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Orbit.Forest, contentColor = Orbit.Paper,
            disabledContainerColor = Orbit.Sand, disabledContentColor = Orbit.ForestSoft),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Orbit.Paper)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Orbit.Lime, contentColor = Orbit.Forest,
            disabledContainerColor = Orbit.Sand, disabledContentColor = Orbit.ForestSoft),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Orbit.Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Orbit.Forest),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, destructive: Boolean = false) {
    TextButton(onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = if (destructive) Orbit.Ember else Orbit.Moss)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OrbitField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, visualTransformation: VisualTransformation = VisualTransformation.None,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        modifier = modifier.fillMaxWidth(), keyboardOptions = keyboardOptions, visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        supportingText = supporting?.let { { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Orbit.Moss, unfocusedBorderColor = Orbit.Line,
            focusedLabelColor = Orbit.Moss, cursorColor = Orbit.Moss,
            focusedContainerColor = Orbit.Surface, unfocusedContainerColor = Orbit.Surface,
        ),
    )
}

enum class Tone { Good, Alert, Warn, Neutral }

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        Tone.Good -> Orbit.LimeSoft to Orbit.Forest
        Tone.Alert -> Orbit.EmberSoft to Orbit.Ember
        Tone.Warn -> Orbit.AmberSoft to Orbit.Amber
        Tone.Neutral -> Orbit.Sand to Orbit.ForestSoft
    }
    Text(text, style = MaterialTheme.typography.labelMedium, color = fg,
        modifier = modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun Avatar(name: String, size: Dp = 40.dp, tone: Tone = Tone.Neutral) {
    val bg = when (tone) { Tone.Good -> Orbit.Lime; Tone.Alert -> Orbit.EmberSoft; Tone.Warn -> Orbit.AmberSoft; Tone.Neutral -> Orbit.Sand }
    Box(Modifier.size(size).clip(CircleShape).background(bg), Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = Orbit.Forest)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(top = Space.sm))
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Orbit.EmberSoft).padding(Space.md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Orbit.Ember)
        Text(message, color = Orbit.Ember, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingBlock(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, Alignment.Center) { CircularProgressIndicator(color = Orbit.Moss, strokeWidth = 3.dp) }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = Space.xl), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.md)) {
        OrbitMark(Modifier.size(96.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Orbit.ForestSoft, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
fun ConfirmDialog(
    title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Orbit.Surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium, color = Orbit.ForestSoft) },
        confirmButton = { QuietButton(confirmLabel, onConfirm, destructive = destructive) },
        dismissButton = { QuietButton("Cancel", onDismiss) },
    )
}

/** Orbit's mark: two rings and a lime dot. Used for empty states and onboarding. */
@Composable
fun OrbitMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val w = size.minDimension * 0.035f
        drawCircle(Orbit.Line, radius = size.minDimension * 0.47f, center = c, style = Stroke(w))
        drawCircle(Orbit.Moss, radius = size.minDimension * 0.30f, center = c, style = Stroke(w))
        drawCircle(Orbit.Forest, radius = size.minDimension * 0.08f, center = c)
        drawCircle(Orbit.Lime, radius = size.minDimension * 0.07f,
            center = Offset(c.x + size.minDimension * 0.30f * 0.72f, c.y - size.minDimension * 0.30f * 0.69f))
    }
}

