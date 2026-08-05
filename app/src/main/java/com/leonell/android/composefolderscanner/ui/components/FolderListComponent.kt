package com.leonell.android.composefolderscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.leonell.android.composefolderscanner.models.FolderModel
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme

@Composable
fun FolderList (navController: NavController, data: List<FolderModel>){
   if(data.size > 1){
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
         items(data,key={item-> item.id!! }){ folderData->
            FolderCard(navController,folderData)
         }
      }
   } else {
      Column(  modifier = Modifier.padding(horizontal = 16.dp),
               verticalArrangement = Arrangement.Center,
               horizontalAlignment = Alignment.CenterHorizontally) {
            FolderCard(navController,data[0])
      }
   }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFF191C1E)
fun PreviewFolderList(){
   val myFolderModel = FolderModel(
      id = 0,
      folderNumber = "3",
      barcode = "00030030001312",
      currentLocationName = "Annex",
      addoId = 0,
      currentLocationDate =   888888,
      currentLocationFullName = "Annex",
      currentLocationId = 0,
      currentOrgId = 1,
      currentOrgName = "FSO",
      currentParentId = 0,
      currentParentName = "Warehouse",
      folderSeriesId = "1",
      folderSeriesName = "Non-Confidential",
      folderTypeId = 1,
      folderTypeName = "PC Folder",
      familyName = "Snow",
      fromDate =  0,
      fullName = "John Snow",
      givenName = "John",
      loggedBy = "Hovaness",
      maidenName = "",
      persId = "asdasd",
      shelfA = "",
      shelfB = "",
      shelfC = "",
      toDate =   0,
      warehouseLocationId = 0,
      warehouseLocationName = "",
      warehouseParentId = 123
   )

   val itemsToTest = listOf( myFolderModel,myFolderModel,myFolderModel,myFolderModel)

   IgniteFolderScannerTheme {
      val lists = listOf("PC Folder","Ethics Folders", "Solo Folders","CF Folders")
      LazyColumn {
         items(lists){
            FolderList(NavController(context = LocalContext.current),itemsToTest)
         }
      }
   }
}