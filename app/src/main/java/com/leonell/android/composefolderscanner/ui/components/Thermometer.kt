package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun Thermometer(
    modifier: Modifier = Modifier,
    size: Float = 0.0F,
    shape: Shape = RectangleShape,
    triggered : Boolean = false
) {
    val fill: Float by animateFloatAsState(
        targetValue = size,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        ), label = ""
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ){
        if(triggered){
            Text( text ="Searching",
                modifier = Modifier.fillMaxSize().absoluteOffset(y= (200).dp).align(Alignment.Center),
                color= MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center)
            DoubleBounce(
                modifier = Modifier.fillMaxSize(),
                animatableSpeed = 10000,
                size = DpSize(100.dp,100.dp),
                shape = shape,
                color = interpolateColor(size)
            )
        }
        Surface(
            modifier = Modifier.width(100.dp).height( 300.dp * fill),
            shape = shape,
            color = interpolateColor(size )
        ) {}
    }
}
