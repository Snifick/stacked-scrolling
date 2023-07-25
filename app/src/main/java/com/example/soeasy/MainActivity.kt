package com.example.soeasy

import android.os.Bundle
import android.util.Log

import android.view.MotionEvent

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.soeasy.ui.theme.SoeasyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = listOf(
            Item(0, Color.Red),
            Item(1, Color.Green),
            Item(2, Color.Yellow),
            Item(3, Color.Cyan),
            Item(4, Color.Blue),
            Item(5, Color.Magenta),
            Item(6, Color.Red),
            Item(7, Color.Green),
            Item(8, Color.Yellow),
            Item(9, Color.Cyan),
            Item(10, Color.Blue),
            Item(11, Color.Magenta)
        )

        setContent {
            SoeasyTheme {
                val bypassState = remember {
                    BypassState(items = items, mutableStateOf(0))
                }
                Bypass(state = bypassState) {
                    Text(text = "${it.id}")
                }
                Text(text = "current item №${bypassState.currentItem.value}")
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Bypass(state: BypassState, content: @Composable BoxScope.(item: Item) -> Unit) {
    val density = LocalDensity.current.density
    val cardOffset = 0.dp
    val leftOffset = 40.dp
    val leftOffsetToSmall = 64.dp
    val rightOffset = 170.dp
    val scaleRight = 0.8f

    val previousOffset = remember { mutableStateOf(0f) }
    val startOffset = remember { mutableStateOf(0f) }
    val cardSize = remember { Size(160f, 214f) }
    val downPoint = remember { mutableStateOf(0f) }
    val upPoint = remember { mutableStateOf(0f) }
    val differentX = remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val coroutineScopeUP = rememberCoroutineScope()
    val trueOffset =
        remember { AnimationState((leftOffsetToSmall.value - leftOffset.value) * density) }
    val mainOffset =
        trueOffset.value
    val active = remember { mutableStateOf(false) }
    Box(modifier = Modifier
        .padding(top = 144.dp)
        .fillMaxWidth()
        .height(214.dp)
        .pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousOffset.value = trueOffset.value
                    startOffset.value = it.x
                    downPoint.value = it.x
                    active.value = false
                }

                MotionEvent.ACTION_MOVE -> {
                    active.value = false
                    coroutineScope.launch {
                        trueOffset.animateTo(
                            previousOffset.value - startOffset.value + it.x,
                            tween(0)
                        )
                    }


                }

                MotionEvent.ACTION_UP -> {

                    upPoint.value = it.x
                    differentX.value = upPoint.value - downPoint.value
                    coroutineScopeUP.launch {


                        val time = abs(differentX.value.toInt())
                        val easing = Easing { fraction -> 1 - (1 - fraction).pow(2) }
                        val minTime = if (abs(differentX.value) > 100) 1000 else 200

                        val tween =
                            tween<Float>(max(minTime, time * 2 / 3), easing = easing)
                        val minScroll = when {
                            differentX.value >= 100 -> 250f
                            differentX.value < -100 -> -250f
                            else -> {
                                differentX.value
                            }
                        }
                        active.value = true
                        trueOffset.animateTo(
                            trueOffset.value + myMax(minScroll, differentX.value),
                            animationSpec = tween
                        ) {
                            if (!active.value)
                                this.cancelAnimation()
                        }


                    }

                }
            }
            true
        }) {

        state.items.forEach { item ->

            var offsetX =
                mainOffset.dp / density + (item.id * cardSize.width).dp + item.id.dp * cardOffset + leftOffset
            var alpha = 1f
            var scale = 1f
            if (offsetX.value < leftOffset.value) {
                val different = abs(offsetX.value - leftOffset.value) * 0.1f
                offsetX = leftOffset - different.dp
            }
            if (offsetX < leftOffset) {
                val dif = (leftOffset.value - offsetX.value) * 0.7f / 100f
                alpha = max(0f, 1f - (dif * 3.3f))
            }
            if (offsetX < leftOffsetToSmall) {
                val dif = (leftOffsetToSmall.value - offsetX.value) * 0.7f / 100f
                scale = max(0.5f, 1f - (dif * 0.85f))
                scale *= max(0.8f, 1f - dif)
            }
            if (offsetX > rightOffset) {
                scale = max(scale - (offsetX.value - rightOffset.value) / 230, scaleRight)
            }
            if (offsetX.value > leftOffsetToSmall.value && offsetX.value < rightOffset.value + cardSize.width / 4)
                state.currentItem.value = item.id

            Box(
                modifier = Modifier
                    .offset(
                        x = offsetX,
                        0.dp
                    )
                    .scale(scale)
                    .width(cardSize.width.dp)
                    .height(cardSize.height.dp)
                    .alpha(alpha)
                    .background(item.color, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            )
            {
                content(item)
            }


        }
    }

}

fun myMax(minScroll: Float, value: Float): Float {
    return if (minScroll >= 0) max(minScroll, value)
    else min(minScroll, value)

}

private operator fun Dp.times(dp: Dp): Dp {
    return (this.value * dp.value).dp
}

@Stable
data class BypassState(
    val items: List<Item>,
    val currentItem: MutableState<Int>
)







