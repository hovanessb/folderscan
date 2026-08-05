package com.leonell.android.composefolderscanner.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.services.FolderIcons
import com.leonell.android.composefolderscanner.ui.viewmodels.CurrentLocationState
import com.leonell.android.composefolderscanner.ui.viewmodels.FolderLocationUiState
import com.leonell.android.composefolderscanner.ui.viewmodels.FolderLocationViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel

@Composable
fun ShowLocation(location : BarcodeEntityModel.FolderLocation,
                 folderLocationModel : FolderLocationViewModel,
                 tabState : MutableState<Int>,
                 scanModel : ScanViewModel,
                 search : MutableState<String>){
   val animatedProgress = remember { Animatable(initialValue = -300f) }
   val opacityProgress = remember { Animatable(initialValue = 0f) }
   val view = LocalView.current

   LaunchedEffect(Unit) {
      animatedProgress.animateTo(
         targetValue = 0f,
         animationSpec = tween(25, easing = LinearEasing)
      )
      opacityProgress.animateTo(
         targetValue = 1f,
         animationSpec = tween(150)
      )
   }

   ListItem(headlineContent= { Text(text = location.name)},
            leadingContent = { if(location.favorite) {
               Icon(Icons.Filled.Star,"favorite",  modifier = Modifier.clickable {
                  location.favorite = false
                  folderLocationModel.saveLocation(location)
               })
            } else  {
               Icon(Icons.Outlined.KeyboardArrowRight, "favorite", modifier = Modifier.clickable {
                  location.favorite = true
                  folderLocationModel.saveLocation(location)
               })
            }},
            modifier = Modifier
               .graphicsLayer(translationX = animatedProgress.value)
               .alpha(opacityProgress.value)
               .clickable {   folderLocationModel.setLocation(location)
                  search.value = ""
                  folderLocationModel.search("")
                  scanModel.clearBarcodes()
                  tabState.value = 0
                  view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
               }
              )
   HorizontalDivider()
}

@Composable
fun SelectLocations(folderLocationsState : FolderLocationUiState,
                    folderLocationModel: FolderLocationViewModel ,
                    openDialog : MutableState<Boolean>,
                    snackbarHostState: SnackbarHostState,
                    folderLocations : List<BarcodeEntityModel.FolderLocation>,
                    currentLocationUiState : CurrentLocationState,
                    tabState : MutableIntState ,
                    scanModel : ScanViewModel,
                    folderLocation : BarcodeEntityModel.FolderLocation?
){
   when (folderLocationsState) {
      is FolderLocationUiState.Loading -> {
         Loading()
      }
      is FolderLocationUiState.Success -> {
         Scaffold(floatingActionButton = {
            FloatingActionButton(onClick = {
               folderLocationModel.setLocation(null)
               openDialog.value = true
            }) {
               Icon(FolderIcons.ADD, "Add")
            }
         },
            snackbarHost = { SnackbarHost(snackbarHostState) }
         ) {
            Column(
               modifier = Modifier
                  .fillMaxSize()
                  .padding(it),
               verticalArrangement = Arrangement.Top,
               horizontalAlignment = Alignment.CenterHorizontally
            ) {
               val search  = remember{ mutableStateOf("") }
               TextField(value = search.value,
                  onValueChange = { changed ->
                     search.value=changed
                     folderLocationModel.search(search.value)
                  },
                  modifier = Modifier
                     .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                     .fillMaxWidth(),
                  leadingIcon = {Icon(Icons.Default.Search, "search")} )
               LazyColumn(
                  verticalArrangement = Arrangement.Top,
                  horizontalAlignment = Alignment.CenterHorizontally
               ) {
                  // The view model already applies the search filter to this list.
                  items(folderLocations, key = { item -> item.id }) { location ->
                     ShowLocation(
                        location,
                        folderLocationModel,
                        tabState,
                        scanModel,
                        search
                     )
                  }
               }

               if (openDialog.value) {
                  var name    by rememberSaveable{ mutableStateOf("") }
                  AlertDialog(
                     onDismissRequest = {
                        openDialog.value = false
                     },
                     icon = {Icon(FolderIcons.ADD, "Add") },
                     title = {Text(text = "Add New Location", textAlign = TextAlign.Center)},
                     text = {
                        Column(modifier=Modifier.padding(10.dp),
                           verticalArrangement = Arrangement.SpaceEvenly){
                           AnimatedContent(targetState = folderLocation, label = "") { myFolder ->
                              if(myFolder != null) {
                                 val original = myFolder.name + " " + if(!myFolder.name.contains("-") && myFolder.spaceA?.isNotEmpty() == true) myFolder.spaceA +"-"+ myFolder.spaceB +"-"+ myFolder.spaceC else ""
                                 Text("Add a title to this location.", textAlign = TextAlign.Center)
                                 Spacer(Modifier.padding(5.dp))
                                 TextField(value = name,
                                    onValueChange = {valName -> name =  valName},
                                    label={Text("Location Name")},
                                    leadingIcon = {
                                       Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                    },
                                    placeholder = {
                                       Text(text = original)
                                    },
                                    modifier = Modifier
                                       .fillMaxWidth()
                                 )
                              } else {
                                 Text("Scan the location now to save.")
                              }
                           }
                        }
                     },
                     confirmButton = {
                        if (folderLocation != null){
                           TextButton(
                              onClick = {
                                 openDialog.value = false
                                 folderLocation.original = folderLocation.name
                                 folderLocation.name = name
                                 folderLocation.spaceA = ""
                                 folderLocation.spaceB = ""
                                 folderLocation.spaceC = ""
                                 folderLocation.favorite = true
                                 folderLocation.custom = true
                                 folderLocationModel.saveLocation(folderLocation)
                              }
                           ) {
                              Text("Save")
                           }
                        }
                     },
                     dismissButton = {
                        TextButton(
                           onClick = {
                              openDialog.value = false
                           }
                        ) {
                           Text("Dismiss")
                        }
                     }
                  )
               }
            }
         }
      }

      else -> {}
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHeader(currentLocationUiState : CurrentLocationState, badge : String = "0"){
         var title by remember { mutableStateOf("") }
   Column(
      modifier = Modifier
         .padding(16.dp)
         .padding(top = 32.dp)
         .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
   ) {
      if (currentLocationUiState.location != null) {
         title = currentLocationUiState.location.name
         BadgedBox(badge = {
            if(badge != ""){
               Badge { Text(badge) }
            }
         }) {
            Text("LOGGING TO", style = MaterialTheme.typography.titleLarge)
         }
         Text(
            title,
            modifier = Modifier.padding(bottom = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
         )
      } else {
         Text(
            "NO LOCATION SELECTED",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge
         )
      }
   }
   HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

}