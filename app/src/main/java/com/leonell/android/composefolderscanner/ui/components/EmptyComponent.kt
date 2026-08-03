package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leonell.android.composefolderscanner.R
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import com.leonell.android.composefolderscanner.ui.theme.md_theme_light_background

@Composable
fun Empty(modifier: Modifier = Modifier, aEmptyMessage : String = "SCAN A BARCODE") {
   Column(
      modifier = modifier
         .padding(16.dp)
         .fillMaxSize()
         .testTag("bookmarks:empty"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      val logo = if(MaterialTheme.colorScheme.background.value == md_theme_light_background.value)
         R.drawable.ic_launcher_foreground_blue else R.drawable.ic_launcher_foreground

      Image(painterResource(id = logo), contentDescription = "logo")
      Spacer(modifier = Modifier.height(16.dp))
      Text(
         text = aEmptyMessage,
         modifier = Modifier.fillMaxWidth(),
         textAlign = TextAlign.Center,
         style = MaterialTheme.typography.titleMedium,
         fontWeight = FontWeight.Bold
      )
   }
}


@Preview
@Composable
private fun EmptyStatePreview() {
   IgniteFolderScannerTheme {
     Empty()
   }
}
