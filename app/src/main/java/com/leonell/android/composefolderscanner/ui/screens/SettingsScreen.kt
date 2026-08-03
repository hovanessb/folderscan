package com.leonell.android.composefolderscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leonell.android.composefolderscanner.ui.viewmodels.SettingsUiState
import com.leonell.android.composefolderscanner.ui.viewmodels.SettingsViewModel

@Composable
internal fun SettingsRoute(
   modifier: Modifier = Modifier,
   viewModel: SettingsViewModel = hiltViewModel(),
) {
   val state by viewModel.uiState.collectAsStateWithLifecycle()
   SettingsScreen(
      state = state,
      modifier = modifier,
      onBaseUrlChange = viewModel::updateBaseUrl,
      onUsernameChange = viewModel::updateUsername,
      onPasswordChange = viewModel::updatePassword,
      onSave = viewModel::save,
      onClear = viewModel::clear,
      onMessageShown = viewModel::consumeMessage,
   )
}

/**
 * Server address and operator credentials.
 *
 * These used to be baked into `Authorization: Basic ` constants in the source, so the app
 * could only ever authenticate as whoever the APK was built for.
 */
@Composable
internal fun SettingsScreen(
   state: SettingsUiState,
   modifier: Modifier = Modifier,
   onBaseUrlChange: (String) -> Unit,
   onUsernameChange: (String) -> Unit,
   onPasswordChange: (String) -> Unit,
   onSave: () -> Unit,
   onClear: () -> Unit,
   onMessageShown: () -> Unit,
) {
   val snackbarHostState = remember { SnackbarHostState() }
   var passwordVisible by remember { mutableStateOf(false) }

   LaunchedEffect(state.savedMessage) {
      state.savedMessage?.let {
         snackbarHostState.showSnackbar(it)
         onMessageShown()
      }
   }

   Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
      Column(
         modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
         horizontalAlignment = Alignment.CenterHorizontally,
      ) {
         Spacer(Modifier.height(24.dp))
         Text("Server Credentials", style = MaterialTheme.typography.headlineSmall)
         Spacer(Modifier.height(8.dp))
         Text(
            "Used for every folder lookup and every scan uploaded to the tracking server.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
         )

         Spacer(Modifier.height(24.dp))

         OutlinedTextField(
            value = state.baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Server address") },
            placeholder = { Text("http://folders.example.org") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
               keyboardType = KeyboardType.Uri,
               imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
         )

         Spacer(Modifier.height(16.dp))

         OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
         )

         Spacer(Modifier.height(16.dp))

         OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
               IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Text(if (passwordVisible) "Hide" else "Show")
               }
            },
            visualTransformation = if (passwordVisible) {
               VisualTransformation.None
            } else {
               PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
               keyboardType = KeyboardType.Password,
               imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
         )

         if (!state.storageEncrypted) {
            Spacer(Modifier.height(16.dp))
            Text(
               "This device's keystore is unavailable, so credentials are stored " +
                  "unencrypted in app-private storage.",
               style = MaterialTheme.typography.bodySmall,
               color = MaterialTheme.colorScheme.error,
            )
         }

         Spacer(Modifier.height(24.dp))

         Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
         ) {
            Button(
               onClick = onSave,
               enabled = state.canSave,
               modifier = Modifier.weight(1f),
            ) {
               Text("Save")
            }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
               Text("Clear")
            }
         }

         Spacer(Modifier.height(32.dp))
      }
   }
}
