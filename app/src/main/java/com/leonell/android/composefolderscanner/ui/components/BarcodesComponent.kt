package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.leonell.android.composefolderscanner.models.ScanModel


@Composable
fun ShowBarcodeItem(modifier: Modifier,barcode: ScanModel) {
   Row(modifier = modifier.padding(10.dp)){
      Text(text = barcode.barcodeId)
      Spacer(modifier = Modifier.padding(16.dp))
      if (barcode.logged) {
         Text(text = "Success")
      }
   }
}

@Composable
fun Barcodes(barcodeList : List<ScanModel>, aEmptyMessage : String= "SCAN A BARCODE", listState : LazyListState = rememberLazyListState()) {
   Column(modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
      if(barcodeList.isEmpty()){
         Empty(aEmptyMessage =  aEmptyMessage)
      }  else {
         LazyColumn(state = listState){
            items(count = barcodeList.size,
                  key={index->barcodeList[index].barcodeId},
                  contentType = {index->barcodeList[index]}) { index ->
               if (barcodeList.isNotEmpty())
                  ShowBarcodeItem(modifier=Modifier,barcode = barcodeList[index])
            }
         }
      }
   }
}