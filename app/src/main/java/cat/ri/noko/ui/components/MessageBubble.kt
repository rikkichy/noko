package cat.ri.noko.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.rounded.SendTimeExtension
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.ui.util.parseMarkdown
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TypingIndicator(characterName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        LoadingIndicator(modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$characterName is typing…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    persona: PersonaEntry?,
    character: PersonaEntry?,
    showAvatars: Boolean = true,
    showNames: Boolean = true,
    reduceMotion: Boolean = false,
    isStreaming: Boolean = false,
    isGenerating: Boolean = false,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onRollback: (() -> Unit)? = null,
    onSwipe: ((Int) -> Unit)? = null,
) {
    val swipeIndex = message.activeIndex
    val swipeCount = message.swipeCount
    val isLastBranch = swipeIndex >= swipeCount - 1
    val effectiveRegenerate = if (isLastBranch) onRegenerate else null
    if (message.isGreeting) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = parseMarkdown(message.content),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    if (message.guardBlocked) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NokoGuard interrupted this response.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    if (message.guardReason != null) {
                        Text(
                            text = "Reason: ${message.guardReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        )
                    }
                }
                if (effectiveRegenerate != null) {
                    IconButton(onClick = effectiveRegenerate) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Regenerate",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
        if (swipeCount > 1 && onSwipe != null && !isGenerating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                IconButton(
                    onClick = { onSwipe(-1) },
                    enabled = swipeIndex > 0,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${swipeIndex + 1}/$swipeCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                IconButton(
                    onClick = { onSwipe(1) },
                    enabled = swipeIndex < swipeCount - 1,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }
        }
        return
    }

    val isUser = message.role == ChatMessage.Role.USER
    val name = message.senderName
        ?: if (isUser) persona?.name ?: "You"
        else character?.name ?: "Assistant"
    val avatarFileName = message.senderAvatarFileName
        ?: if (isUser) persona?.avatarFileName
        else character?.avatarFileName
    val canInteract = !isStreaming && !isGenerating
    var showActions by remember { mutableStateOf(false) }

    val swipeOffset = remember { Animatable(0f) }
    val swipeScope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    var regenProgress by remember { mutableStateOf(0f) }
    var swipeDirection by remember { mutableIntStateOf(0) }

    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(message.stoppedByUser, reduceMotion) {
        if (message.stoppedByUser && !reduceMotion) {
            repeat(3) {
                shakeOffset.animateTo(6f, tween(40))
                shakeOffset.animateTo(-6f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
        }
    }

    val currentSwipeIndex by rememberUpdatedState(swipeIndex)
    val currentSwipeCount by rememberUpdatedState(swipeCount)
    val currentEffectiveRegenerate by rememberUpdatedState(effectiveRegenerate)
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    val hasSwipeGesture = canInteract && (onSwipe != null || effectiveRegenerate != null)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier
                .offset(x = shakeOffset.value.dp)
                .offset { IntOffset(swipeOffset.value.toInt(), 0) }
                .then(
                    if (hasSwipeGesture) {
                        Modifier.pointerInput(message.id) {
                            var totalDrag = 0f
                            val swipeThreshold = 80.dp.toPx()
                            val regenThreshold = 80.dp.toPx()
                            detectHorizontalDragGestures(
                                onDragStart = { },
                                onDragEnd = {
                                    val isLastSwipe = currentSwipeIndex >= currentSwipeCount - 1
                                    val isFirstSwipe = currentSwipeIndex <= 0
                                    if (totalDrag < -swipeThreshold && currentOnSwipe != null && !isLastSwipe) {
                                        swipeDirection = 1
                                        currentOnSwipe?.invoke(1)
                                        swipeScope.launch {
                                            regenProgress = 0f
                                            swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    } else if (totalDrag < -regenThreshold && isLastSwipe && currentEffectiveRegenerate != null) {
                                        haptics.confirm()
                                        swipeDirection = 1
                                        currentEffectiveRegenerate?.invoke()
                                        swipeScope.launch {
                                            regenProgress = 0f
                                            swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    } else if (totalDrag > swipeThreshold && currentOnSwipe != null && !isFirstSwipe) {
                                        swipeDirection = -1
                                        currentOnSwipe?.invoke(-1)
                                        swipeScope.launch {
                                            regenProgress = 0f
                                            swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                        }
                                    } else {
                                        swipeScope.launch {
                                            regenProgress = 0f
                                            swipeOffset.animateTo(0f, spring())
                                        }
                                    }
                                    totalDrag = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                    swipeScope.launch { swipeOffset.snapTo(swipeOffset.value + dragAmount) }
                                    val isLastSwipe = currentSwipeIndex >= currentSwipeCount - 1
                                    if (totalDrag < 0 && isLastSwipe && currentEffectiveRegenerate != null) {
                                        val progress = (-totalDrag / regenThreshold).coerceIn(0f, 1f)
                                        if (progress >= 1f && regenProgress < 1f) haptics.tick()
                                        regenProgress = progress
                                    } else {
                                        regenProgress = 0f
                                    }
                                }
                            )
                        }
                    } else Modifier
                ),
        ) {
            if (!isUser && showAvatars) {
                NokoAvatar(
                    name = name,
                    avatarFileName = avatarFileName,
                    fallbackIcon = Icons.Filled.SmartToy,
                    size = 32,
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            ) {
                if (showNames) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                Box {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .then(
                                if (reduceMotion) Modifier
                                else Modifier.animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessHigh,
                                    )
                                )
                            )
                            .then(if (canInteract) Modifier.clickable { showActions = !showActions } else Modifier),
                    ) {
                        var wasStreaming by remember { mutableStateOf(false) }
                        if (isStreaming) wasStreaming = true
                        else wasStreaming = false

                        AnimatedContent(
                            targetState = swipeIndex,
                            transitionSpec = {
                                if (reduceMotion) {
                                    fadeIn(tween(0)) togetherWith fadeOut(tween(0)) using SizeTransform(clip = false)
                                } else {
                                    val dir = swipeDirection
                                    (slideInHorizontally(tween(250)) { it * dir } + fadeIn(tween(200))) togetherWith
                                        (slideOutHorizontally(tween(250)) { -it * dir } + fadeOut(tween(150))) using
                                        SizeTransform(clip = false)
                                }
                            },
                            label = "swipe_content",
                        ) { idx ->
                            if (wasStreaming && idx == swipeIndex && !reduceMotion) {
                                StreamingText(
                                    text = message.content,
                                    isStreaming = isStreaming,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            } else {
                                Text(
                                    text = parseMarkdown(message.content),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    if (regenProgress > 0f && !isUser) {
                        val p = regenProgress
                        val pop = 1f - (1f - p) * (1f - p) * (1f - p)
                        Icon(
                            Icons.Filled.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset { IntOffset((24.dp.toPx() + 18.dp.toPx() * pop).toInt(), 0) }
                                .size(35.dp)
                                .scale(pop)
                                .rotate(360f * p),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showActions && canInteract,
                    enter = if (reduceMotion) expandVertically(tween(0)) else fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = if (reduceMotion) shrinkVertically(tween(0)) else fadeOut(tween(100)) + shrinkVertically(tween(100)),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        if (!isUser && effectiveRegenerate != null) {
                            AssistChip(
                                onClick = { showActions = false; effectiveRegenerate() },
                                label = { Text("Regenerate", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                leadingIcon = { Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                        }
                        if (onEdit != null) {
                            AssistChip(
                                onClick = { showActions = false; onEdit() },
                                label = { Text("Edit", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                leadingIcon = { Icon(Icons.Filled.ModeEdit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                        }
                        if (onRollback != null) {
                            AssistChip(
                                onClick = { showActions = false; onRollback() },
                                label = { Text("Rollback", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                leadingIcon = { Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }
                }
                if (message.stoppedByUser) {
                    Text(
                        text = "Stopped by you",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                AnimatedVisibility(
                    visible = message.emojisTrimmed,
                    enter = if (reduceMotion) fadeIn(tween(0))
                            else fadeIn(tween(300, delayMillis = 100)) + slideInHorizontally(tween(300, delayMillis = 100)) { if (isUser) it else -it },
                    exit = if (reduceMotion) fadeOut(tween(0))
                           else fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (isUser) it else -it },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Rounded.SendTimeExtension,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.size(3.dp))
                        Text(
                            text = "Emojis trimmed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = message.actionsStructured,
                    enter = if (reduceMotion) fadeIn(tween(0))
                            else fadeIn(tween(300, delayMillis = 100)) + slideInHorizontally(tween(300, delayMillis = 100)) { if (isUser) it else -it },
                    exit = if (reduceMotion) fadeOut(tween(0))
                           else fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { if (isUser) it else -it },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Rounded.SendTimeExtension,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.size(3.dp))
                        Text(
                            text = "Actions structured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
                if (swipeCount > 1 && onSwipe != null && canInteract) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onSwipe(-1) },
                            enabled = swipeIndex > 0,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${swipeIndex + 1}/$swipeCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        IconButton(
                            onClick = { onSwipe(1) },
                            enabled = swipeIndex < swipeCount - 1,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isUser && showAvatars) {
                Spacer(Modifier.width(8.dp))
                NokoAvatar(
                    name = name,
                    avatarFileName = avatarFileName,
                    fallbackIcon = Icons.Filled.Person,
                    size = 32,
                )
            }
        }
    }
}


@Composable
private fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle,
) {
    val targetText by rememberUpdatedState(text)
    var displayed by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var lastFrameMs = 0L
        while (true) {
            withFrameMillis { frameMs ->
                val target = targetText
                if (displayed.length < target.length) {
                    val behind = target.length - displayed.length


                    val dt = if (lastFrameMs == 0L) 16f else (frameMs - lastFrameMs).toFloat()
                    val scale = dt / 16f
                    val baseChunk = when {
                        behind > 200 -> 30
                        behind > 100 -> 15
                        behind > 50 -> 8
                        behind > 20 -> 4
                        else -> 1
                    }
                    val chunk = (baseChunk * scale).toInt().coerceAtLeast(1)
                    displayed = target.substring(0, (displayed.length + chunk).coerceAtMost(target.length))
                }
                lastFrameMs = frameMs
            }
        }
    }

    if (displayed.length > text.length) displayed = text


    val renderText = if (isStreaming && displayed.length < text.length) {
        stripTrailingDelimiters(displayed)
    } else {
        displayed
    }

    Text(
        text = parseMarkdown(renderText),
        modifier = modifier,
        style = style,
    )
}

private fun stripTrailingDelimiters(text: String): String {
    val trailing = listOf("***", "**", "~~", "*", "`")
    for (d in trailing) {
        if (text.endsWith(d)) return text.dropLast(d.length)
    }
    return text
}
