package com.leonell.android.composefolderscanner.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.leonell.android.composefolderscanner.models.BarcodeEntityModel
import com.leonell.android.composefolderscanner.models.FolderModel
import com.leonell.android.composefolderscanner.ui.theme.Book
import com.leonell.android.composefolderscanner.ui.theme.Chinese
import com.leonell.android.composefolderscanner.ui.theme.English
import com.leonell.android.composefolderscanner.ui.theme.Field
import com.leonell.android.composefolderscanner.ui.theme.French
import com.leonell.android.composefolderscanner.ui.theme.German
import com.leonell.android.composefolderscanner.ui.theme.Hungarian
import com.leonell.android.composefolderscanner.ui.theme.Italian
import com.leonell.android.composefolderscanner.ui.theme.Japanese
import com.leonell.android.composefolderscanner.ui.theme.Preclear
import com.leonell.android.composefolderscanner.ui.theme.Russian
import com.leonell.android.composefolderscanner.ui.theme.Spanish
import com.leonell.android.composefolderscanner.ui.theme.Student
import com.leonell.android.composefolderscanner.ui.theme.md_theme_dark_inverseOnSurface
import com.leonell.android.composefolderscanner.ui.theme.md_theme_dark_outline
import com.leonell.android.composefolderscanner.ui.theme.md_theme_dark_surface
import com.leonell.android.composefolderscanner.ui.theme.md_theme_dark_surfaceVariant
import com.leonell.android.composefolderscanner.ui.theme.md_theme_light_onSurface
import com.leonell.android.composefolderscanner.ui.theme.md_theme_light_surfaceTint
import com.leonell.android.composefolderscanner.ui.theme.md_theme_light_tertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAvatar (
   modifier: Modifier = Modifier,
   person: BarcodeEntityModel.Person,
   photoUrl: String?
){

   Column(modifier = Modifier
      .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      BadgedBox(badge = {
         var inCfText = "NNCF"
         if (person.inCf){
            inCfText = "CF"
         }
         val languageColor = when(person.language){
            "Hungarian" -> {
               Hungarian
            } "English" -> {
               English
            } "German" ->{
               German
            } "Spanish" ->{
               Spanish
            } "Chinese" ->{
               Chinese
            } "Italian" ->{
               Italian
            } "Russian" ->{
               Russian
            } "Japanese" ->{
               Japanese
            } "French" ->{
               French
            } else ->{
               MaterialTheme.colorScheme.error
            }
         }

         val individualStatus = when(person.individualStatus){
               "Deadfile" ->{
                  MaterialTheme.colorScheme.error
               }
               "Pending Review" ->{
                  md_theme_dark_surfaceVariant
               }
               "Address Unknown" ->{
                  md_theme_dark_outline
               }
               "Duplicate" ->{
                  md_theme_dark_surfaceVariant
               }
               "Address Okay" ->{
                  MaterialTheme.colorScheme.tertiaryContainer}
               "Inactive Record" ->{
                  md_theme_dark_inverseOnSurface}
               "Deadfile SP" ->{
                  MaterialTheme.colorScheme.error
               }
               "Rejected Identity" ->{ md_theme_dark_inverseOnSurface}
               "Invalid Data" ->{ md_theme_dark_inverseOnSurface}
               "Sea Org Member" ->{
                  md_theme_light_surfaceTint}
               "Celebrity" ->{
                  md_theme_light_tertiary}
               "Data Protection" ->{
                  MaterialTheme.colorScheme.error}
               "Asked Off" ->{
                  MaterialTheme.colorScheme.error}
               "Deceased" ->{
                  md_theme_dark_surface
               }
               "Awaiting Invoice" -> {
               MaterialTheme.colorScheme.tertiaryContainer
            }  else ->{
               md_theme_light_onSurface
            }
         }

         val procurementStatus = when(person.procurementCategory){
            "Book Orderer" ->{
               Book
            }
            "Field Auditor" ->{
               Field
            }
            "Preclear" ->{
               Preclear
            }
            "Student" ->{
               Student
            } else ->{
               md_theme_light_onSurface
            }
         }
         Badge(modifier=Modifier.offset(x=(-32).dp, y =32.dp)){Text(inCfText)}
         Badge( modifier=Modifier.offset(x=(-28).dp, y =50.dp),
                containerColor=languageColor){Text(person.language!!)}
         Badge(modifier=Modifier.offset(x=(-24).dp, y =68.dp),
               containerColor=individualStatus){Text(person.individualStatus!!)}
         Badge(modifier=Modifier.offset(x=(-20).dp, y =86.dp),
               containerColor = procurementStatus){Text(person.procurementCategory!!)}
      }) {
         // Authentication is handled by the shared OkHttp client Coil is configured with
         // in MainApplication, so no header is attached here.
         AsyncImage(model=ImageRequest.Builder(LocalContext.current)
            .decoderFactory(SvgDecoder.Factory())
            .data(photoUrl)
            .crossfade(true)
            .build(),null,
            modifier=modifier,
            contentScale = ContentScale.Crop,
            onError = { error ->
               Log.e("ProfileAvatar","Error retrieving image ${person.id}")
               Log.e("ProfileAvatar", error.result.throwable.message.toString())
            })
      }
      person.fullName?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
      Text("${person.caseLevel} - ${person.trainingLevel}", style = MaterialTheme.typography.bodySmall)
   }
}

@Preview
@Composable
fun PreviewProfileAvatar() {
   /*
   val person = BarcodeEntityModel.Person(
      id = 0,
      persId = "123",
      addoId = 0,
      fullName = "Joe Test",
      deceased = false,
      celebrity = false,
      deadfiled = false,
      inCf = false,
      dateOfBirth = 0,
      address = "bogus",
      emailAddress = "email@email.com",
      homePhone = "(408) 499-1111",
      workPhone = "(408) 499-1111",
      altPhone = "(408) 499-1111",
      barcodeId = "000101275180",
      individualStatus="Address Okay",
      language="English",
      procurementCategory = "Book Orderer",
      mailable = true,
      folders = listOf( )

   )
   ProfileAvatar (person =person)*/
}
