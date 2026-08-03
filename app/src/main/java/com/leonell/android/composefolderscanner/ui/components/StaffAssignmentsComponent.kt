package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.FolderModel
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import kotlinx.datetime.Instant

@Composable
fun StaffAssignmentList (data: List<BarcodeEntityModel.StaffAssignment>){
   if(data.size > 1){
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
         items(data,key={item-> item.id!! }){ folderData->
            StaffAssignmentCard(folderData)
         }
      }
   } else {
      Column(  modifier = Modifier.padding(horizontal = 16.dp),
               verticalArrangement = Arrangement.Center,
               horizontalAlignment = Alignment.CenterHorizontally) {
         StaffAssignmentCard(data[0])
      }
   }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF191C1E)
fun PreviewStaffListList(){
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

   val itemsToTest = listOf( myFolderModel,myFolderModel,myFolderModel,myFolderModel)

   IgniteFolderScannerTheme {
      val lists = listOf("PC Folder","Ethics Folders", "Solo Folders","CF Folders")
      LazyColumn {
         items(lists){
            StaffAssignmentList(itemsToTest)
         }
      }
   }
}