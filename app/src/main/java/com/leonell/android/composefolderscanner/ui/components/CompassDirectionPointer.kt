package com.leonell.android.composefolderscanner.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun CompassDirectionPointer(
   @DrawableRes pointerIcon: Int,
   contentDsc: String,
   modifier: Modifier = Modifier,
   angle: Float = 0f,
   hotness: Float = 0.1f,
   duration: Int
)
{
   // Calculate the interpolated color based on the given value
   val interpolatedColor = interpolateColor(hotness)

   // Apply a color filter to the image with the interpolated color
   val colorFilter = ColorFilter.tint(interpolatedColor)

    Image(
        painterResource(id = pointerIcon),
        modifier = modifier
            .padding(COMPASS_PADDING)
            .rotate(angle)
            .graphicsLayer(alpha = 1.0f)
            .fillMaxSize(),
        colorFilter = colorFilter,
        contentDescription = contentDsc,
        contentScale = ContentScale.Fit,
    )

      AnimatedContent(targetState = duration,transitionSpec= {
         (fadeIn(animationSpec = tween(30, delayMillis = 5)) +
               scaleIn(initialScale = 0.92f, animationSpec = tween(30, delayMillis = 5)))
            .togetherWith(fadeOut(animationSpec = tween(20)) + scaleOut())
      } ,label = "",) { myState ->
         when(myState){
            0 -> {
               DoubleBounce(
                  modifier = modifier.padding(COMPASS_PADDING).fillMaxSize(),
                  animatableSpeed = 500 ,
                  delayMillis = 0,
                  color = interpolateColor(hotness),
                  size = DpSize(150.dp, 150.dp)
               )}
            1->{
               DoubleBounce(
                  modifier = modifier.padding(COMPASS_PADDING).fillMaxSize(),
                  animatableSpeed = 1000 ,
                  delayMillis = 0,
                  color = interpolateColor(1.0f),
                  size = DpSize(150.dp, 150.dp)
               )}
            2->{
               DoubleBounce(
                  modifier = modifier.padding(COMPASS_PADDING).fillMaxSize(),
                  animatableSpeed = 2000 ,
                  delayMillis = 0,
                  color = interpolateColor(0.75f),
                  size = DpSize(150.dp, 150.dp)
               )}
            3->{
               DoubleBounce(
                  modifier = modifier.padding(COMPASS_PADDING).fillMaxSize(),
                  animatableSpeed = 6000 ,
                  delayMillis = 0,
                  color = interpolateColor(0.30f),
                  size = DpSize(150.dp, 150.dp)
               )}
            4->{
               DoubleBounce(
                  modifier = modifier.padding(COMPASS_PADDING).fillMaxSize(),
                  animatableSpeed = 10000,
                  delayMillis = 0,
                  color = interpolateColor(0.00f),
                  size = DpSize(150.dp, 150.dp)
               )
            }
            else->{}
         }
      }
}


@Composable
fun interpolateColor(tintValue: Float): Color {
   // Define your cool blue and hot red colors
   val coolBlue = MaterialTheme.colorScheme.primary
   val hotRed = MaterialTheme.colorScheme.error

   // Interpolate between cool blue and hot red based on the tintValue
   val redComponent = abs(coolBlue.red + tintValue * (hotRed.red - coolBlue.red))
   val greenComponent = abs(coolBlue.green + tintValue * (hotRed.green - coolBlue.green))
   val blueComponent = abs(coolBlue.blue + tintValue * (hotRed.blue - coolBlue.blue))

   return Color(redComponent, greenComponent, blueComponent)
}

// region previews

