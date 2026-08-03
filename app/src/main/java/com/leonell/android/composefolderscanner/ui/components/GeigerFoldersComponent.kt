package com.leonell.android.composefolderscanner.ui.components

import android.util.Log
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.services.dBuV_dBm
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel


@Composable
fun ShowGiegerItem(barcode: BarcodeEntityModel.Person, onClickDelete : () -> Unit) {
   Row(modifier = Modifier
      .padding(10.dp)
      .clickable(true, onClickLabel = null, onClick = onClickDelete, role = Role.Button)){
      val folder = barcode.folders?.firstOrNull { barcode.barcodeId == it.barcode }
      if (folder != null) {
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = barcode.fullName +" - "+ barcode.addoId.toString(), style = MaterialTheme.typography.labelMedium)
            FolderCard(null, data = folder, aShowGeigerBar = true)

         }
      }
   }
}
@Composable
fun GeigerFolders(scanModel: ScanViewModel,
                  aEmptyMessage: String= "SCAN A BARCODE" ) {

   val barcodeData by scanModel.uiState.collectAsStateWithLifecycle()
   Column(modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally) {
      if(barcodeData.geigerFolders.isEmpty()){
         Empty(aEmptyMessage =  aEmptyMessage)
      }  else {
         LazyColumn{
               items(count = barcodeData.geigerFolders.size,
                     key={index-> barcodeData.geigerFolders[index].id!! },
                     contentType = {index ->  barcodeData.geigerFolders[index]}) { index ->
               ShowGiegerItem(barcode = barcodeData.geigerFolders[index],
                              onClickDelete = {scanModel.removeGeiger(barcodeData.geigerFolders[index].id!!)}                              )
            }
         }
      }
   }
}