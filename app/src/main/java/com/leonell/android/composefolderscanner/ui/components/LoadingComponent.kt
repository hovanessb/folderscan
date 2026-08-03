package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun Loading(modifier: Modifier = Modifier, message : String? = "") {

   Column(modifier = Modifier
      .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
      if(message != null){
         Text(text=message)
      }
      FolderLoadingWheel(
         modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .testTag("profile:loading"),
         contentDesc = "Loading",
      )
   }
}