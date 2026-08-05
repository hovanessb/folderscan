package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar (
    modifier: Modifier = Modifier
){
   TextField(value = "", onValueChange = {}, leadingIcon = {
        Icon(imageVector = Icons.Default.Search, contentDescription = null)
      },
      placeholder = {
         Text(text = "Search for Location")
      },
      modifier = modifier
         .fillMaxWidth()
         .heightIn(min = 56.dp)
   )
}
@Composable
@Preview
fun PreviewSearchBar(){
   IgniteFolderScannerTheme {
      SearchBar()
   }
}