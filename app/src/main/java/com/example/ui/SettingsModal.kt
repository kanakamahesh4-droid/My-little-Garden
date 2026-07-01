package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    viewModel: GardenViewModel,
    onDismiss: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val systemTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Hindi", "Telugu", "Tamil", "Kannada")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Settings".localized(appLanguage),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF112611)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Logged-in User Google Profile Area
                userProfile?.let { profile ->
                    if (profile.isLoggedIn) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Initials Avatar
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF386B4F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = profile.displayName.split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .map { it.first().uppercase() }
                                        .joinToString("")
                                    Text(
                                        text = initials.ifEmpty { "G" },
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.displayName.ifEmpty { "Google User" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF112611)
                                    )
                                    Text(
                                        text = profile.email,
                                        fontSize = 12.sp,
                                        color = Color(0xFF5A695E)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Google Connection Tag
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0x22386B4F), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Connected via Google",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF386B4F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Firebase Cloud Sync Status Card
                val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isFirebaseAvailable) Color(0xFFF1F6F2) else Color(0xFFFFF8F8)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (viewModel.isFirebaseAvailable) Color(0xFFE2EBE5) else Color(0xFFFEECEB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.isFirebaseAvailable) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = "Sync",
                                tint = if (viewModel.isFirebaseAvailable) Color(0xFF386B4F) else Color(0xFFC62828),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cloud Synchronization".localized(appLanguage),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isFirebaseAvailable) Color(0xFF112611) else Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = firebaseSyncStatus.localized(appLanguage),
                            fontSize = 12.sp,
                            color = if (viewModel.isFirebaseAvailable) Color(0xFF5A695E) else Color(0xFF9E4242)
                        )
                        if (!viewModel.isFirebaseAvailable) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "To enable cloud sync across multiple devices, download your google-services.json from the Firebase Console and place it in the app's root folder.".localized(appLanguage),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF916A6A)
                            )
                        } else if (userProfile?.isLoggedIn == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.syncPlantsWithFirebase() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp).align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Sync Now".localized(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = Color(0xFF386B4F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "App Language".localized(appLanguage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF414941)
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.width(130.dp)
                    ) {
                        OutlinedTextField(
                            value = appLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF386B4F),
                                unfocusedBorderColor = Color(0xFFE0E3DA)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        viewModel.setAppLanguage(selectionOption)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = Color(0xFF386B4F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dark Mode".localized(appLanguage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF414941)
                        )
                    }

                    Switch(
                        checked = isDarkMode ?: systemTheme,
                        onCheckedChange = { checked ->
                            viewModel.setDarkMode(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF386B4F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sign Out Button
                    OutlinedButton(
                        onClick = {
                            viewModel.signOut()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", fontWeight = FontWeight.SemiBold)
                    }

                    // Close Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF386B4F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                    ) {
                        Text("Close".localized(appLanguage), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
