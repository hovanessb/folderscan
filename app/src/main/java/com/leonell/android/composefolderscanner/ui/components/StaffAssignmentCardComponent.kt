package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme

/**
 * #{aDatum.item.folderNumber} - ID: {aDatum.item.barcode} Date: {aDatum.item.fromDate}-{aDatum.item.toDate} {"\n"}
TYPE: {aDatum.item.folderTypeName}{"\n"}
LOCATION: {aDatum.item.currentLocationName} {aDatum.item.currentLocationName &&
aDatum.item.currentLocationName.indexOf(aDatum.item.shelfA) < 0 && aDatum.item.shelfA + "-" + aDatum.item.shelfB + "-" + aDatum.item.shelfC }</Text>
 */
@Composable
fun  StaffAssignmentCard(data: BarcodeEntityModel.StaffAssignment){
   ElevatedCard(elevation = CardDefaults.cardElevation(1.dp)) {
      Row{
         Column(modifier = Modifier.padding(16.dp)) {
            val title = "Staff Assignment: ${data.type.lowercase().replace("_"," ")}"
            Text( title,
                  style = MaterialTheme.typography.bodySmall,
                  textAlign = TextAlign.Right)
            Text("${data.staffName} - ${data.procurementAreaName}",
                  modifier= Modifier.padding(vertical = 8.dp),
                  style = MaterialTheme.typography.bodyLarge)
         }
      }
   }
}

@Composable
@Preview
fun StaffAssignmentCard(){
   val myFolderModel = BarcodeEntityModel.StaffAssignment(
      id = 0,
      fullName = "John Snow",
      associateId = 0,
      staffId = 0,
      canEdit = true,
      staffName = "Hovi B",
      procurementAreaId = 0,
      type = "PARTIALLY_PAID",
      procurementAreaName = "Dept 5"
   )

   IgniteFolderScannerTheme {
      StaffAssignmentCard(data=myFolderModel)
   }
}