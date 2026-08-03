package com.leonell.android.composefolderscanner.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.leonell.android.composefolderscanner.ui.components.Empty
import com.leonell.android.composefolderscanner.ui.components.FolderList
import com.leonell.android.composefolderscanner.ui.components.Loading
import com.leonell.android.composefolderscanner.ui.components.PreviewFolderList
import com.leonell.android.composefolderscanner.ui.components.ProfileAvatar
import com.leonell.android.composefolderscanner.ui.components.StaffAssignmentCard
import com.leonell.android.composefolderscanner.ui.theme.IgniteFolderScannerTheme
import com.leonell.android.composefolderscanner.ui.viewmodels.ProfileUiState
import com.leonell.android.composefolderscanner.ui.viewmodels.ProfileViewModel
import com.leonell.android.composefolderscanner.ui.viewmodels.ScanViewModel

@Composable
internal fun ProfileRoute(
   navController : NavController,
   modifier: Modifier = Modifier,
   viewModel: ProfileViewModel = hiltViewModel(),
   scanModel: ScanViewModel = hiltViewModel()
) {
   val profileState by viewModel.uiState.collectAsStateWithLifecycle()
   ProfileScreen(navController=navController,profileState = profileState, modifier=modifier, viewModel=viewModel, scanModel=scanModel)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ProfileScreen(
   navController : NavController,
   profileState: ProfileUiState,
   modifier: Modifier = Modifier,
   viewModel: ProfileViewModel = hiltViewModel(),
   scanModel: ScanViewModel = hiltViewModel()
) {

   val barcodeData by scanModel.uiState.collectAsStateWithLifecycle()
   val searchPerson by viewModel.searchPerson.collectAsStateWithLifecycle(initialValue = emptyList())
   val textFocus =  LocalFocusManager.current

   LaunchedEffect(key1 =barcodeData.barcode?.barcodeId) {
      if (barcodeData.barcode != null) {
         viewModel.getPerson(barcodeData.barcode!!.barcodeId, null)
      }
   }

   Column(horizontalAlignment = Alignment.CenterHorizontally) {

   TextField(value = viewModel.search,
      onValueChange = {valName ->viewModel.updateSearch(valName)},
      label={Text("Search")},
      leadingIcon = {
         Icon(imageVector = Icons.Default.Edit, contentDescription = null)
      },
      placeholder = {
         Text(text = "Search for a Person")
      },
      modifier = Modifier
         .fillMaxWidth()
   )
      LazyColumn(
         verticalArrangement = Arrangement.Top,
         horizontalAlignment = Alignment.CenterHorizontally
      ) {
         items(searchPerson,
            key = { item -> item.associateId!! }) { person ->
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

            ListItem(headlineContent= { Text(text = person.name!! )},
               leadingContent = {},
               modifier = Modifier
                  .graphicsLayer(translationX = animatedProgress.value)
                  .alpha(opacityProgress.value)
                  .clickable {
                     viewModel.updateSearch("")
                     viewModel.getPerson(null, person.associateId)
                     view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                     textFocus.clearFocus()
                  }
            )
            HorizontalDivider()
         }
      }

      when (profileState) {
         ProfileUiState.Ready   -> Empty(modifier)
         ProfileUiState.Loading -> Loading(modifier)
         is ProfileUiState.Error -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
               Text(
                  text = profileState.message,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Light
               )
               Empty(modifier)
            }
         }
         is ProfileUiState.Success -> if (profileState.person.id != 0) {
            Column(
               modifier = modifier
                  .fillMaxSize(),
               verticalArrangement = Arrangement.Center,
               horizontalAlignment = Alignment.CenterHorizontally
            ) {
               ProfileAvatar(modifier = Modifier
                  .size(124.dp)
                  .padding(16.dp)
                  .border(
                     width = 2.dp,
                     color = MaterialTheme.colorScheme.onBackground,
                     shape = CircleShape
                  )
                  .clip(shape = CircleShape),
                  person = profileState.person,
                  photoUrl = viewModel.photoUrl(profileState.person.id))
               Spacer(modifier = Modifier.height(8.dp))
               if(profileState.staffAssignment.isNotEmpty()){
                  if(profileState.staffAssignment.size > 1){
                     LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)){
                        items(profileState.staffAssignment.filter { !it.staffName.isNullOrEmpty() }){staff->
                           StaffAssignmentCard(staff)
                        }
                     }
                  } else {
                     Column(  modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        StaffAssignmentCard(profileState.staffAssignment[0])
                     }
                  }
               }
               Spacer(modifier = Modifier.height(16.dp))
               LazyColumn {
                  items(profileState.person.getFoldersTypes()){folder->
                     profileState.person.filterFolderTypes(folder)?.let { FolderList(navController,it) }
                     Spacer(modifier = Modifier.height(16.dp))
                  }
               }
            }
         }
      }
   }
}


@Preview
@Composable
internal fun PreviewProfileScreen() {
   IgniteFolderScannerTheme {
      Column(){
         PreviewFolderList()
      }
   }
}