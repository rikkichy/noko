package cat.ri.noko.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes

val ShakeSpec = keyframes<Float> {
    durationMillis = 400
    0f at 0
    (-18f) at 50
    18f at 100
    (-14f) at 150
    14f at 200
    (-8f) at 250
    8f at 300
    (-4f) at 350
    0f at 400
}

suspend fun Animatable<Float, *>.shake() {
    animateTo(targetValue = 0f, animationSpec = ShakeSpec)
}
