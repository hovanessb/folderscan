package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leonell.android.composefolderscanner.R

val COMPASS_PADDING = 16.dp

const val UPDATE_FREQUENCY = 250
@Composable
fun Compass(
    direction: Int?,
    rotation: Int,
    name: String,
    hotness: Float,
    duration : Int
)
{
    val (lastRotation, setLastRotation) = remember { mutableStateOf(0) }

    var newRotation = lastRotation
    val modLast = if (lastRotation > 0) lastRotation % 360 else 360 - (-lastRotation % 360)
    
    if (modLast != rotation)
    {
        // new rotation comes in
        val backward = if (rotation > modLast) modLast + 360 - rotation else modLast - rotation
        val forward = if (rotation > modLast) rotation - modLast else 360 - modLast + rotation
        
        newRotation = if (backward < forward)
        {
            // backward rotation is shorter
            lastRotation - backward
        }
        else
        {
            // forward rotation is shorter (or they are equals)
            lastRotation + forward
        }
        
        setLastRotation(newRotation)
    }
    
    val angle: Float by animateFloatAsState(
        targetValue = -newRotation.toFloat(),
        animationSpec = tween(
            durationMillis = UPDATE_FREQUENCY,
            easing = LinearEasing
        ), label = ""
    )
    
    Rose(angle = angle, rotation = rotation, name = name)
    
    if (direction != null)
    {
          CompassDirectionPointer(
            angle = angle + direction.toFloat(),
            pointerIcon = R.drawable.ic_pointerdot,
            contentDsc = "Direction",
            hotness = hotness,
            duration = duration
        )
    }

}

// region previews

@Preview(showBackground = true)
@Composable
fun PreviewCompassWithDirection()
{
   Compass(direction = 45, rotation = 0, name ="Test Name", hotness = 0.0f, 50)
}

@Preview(showBackground = true)
@Composable
fun PreviewCompassWithDirectionDark()
{
   Compass(direction = 45, rotation = -85, name ="Test Name", hotness = 0.0f, 50)
}

@Preview(showBackground = true)
@Composable
fun PreviewCompassWithoutDirection()
{
    Compass(direction = null, rotation = -85, name ="Test Name", hotness = 0.30f, 50)
}


@Preview(showBackground = true)
@Composable
fun PreviewCompassWithoutDirectionDark()
{
    Compass(direction = null, rotation = -85, name ="Test Name", hotness = 0.40f, 50)
}

// endregion