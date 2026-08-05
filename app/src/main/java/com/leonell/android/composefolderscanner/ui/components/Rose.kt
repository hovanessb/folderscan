package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.leonell.android.composefolderscanner.R

@Composable
fun Rose(
    angle: Float,
    rotation: Int,
    modifier: Modifier = Modifier,
    name : String
)
{
    Image(
        modifier = modifier
            .padding(COMPASS_PADDING)
            .fillMaxSize()
            .rotate(angle),
        painter = painterResource(id = R.drawable.ic_rose),
        contentDescription = "Content desc",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(
            color = MaterialTheme.colorScheme.onBackground
        )
    )
    
    Text(
        modifier = modifier.padding(COMPASS_PADDING).fillMaxSize(),
        text = name,
        textAlign= TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall
    )

}


// region previews

@Preview(showBackground = true)
@Composable
fun PreviewRoss()
{
        Rose(angle = 30f, rotation = 0, name="Test Name")
}

@Preview(showBackground = true)
@Composable
fun PreviewRossDark()
{

        Rose(angle = 30f, rotation = -45, name="Test Name")
}


// endregion