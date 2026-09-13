package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserAccount
import com.example.ui.ScreenTab
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelNavy
import com.example.ui.theme.DonsaelRed
import com.example.ui.theme.DonsaelTeal

@Composable
fun AppHeader(
    currentUser: UserAccount?,
    isOnline: Boolean,
    exchangeRate: Double,
    isDarkMode: Boolean,
    alertCount: Int,
    onToggleDarkMode: () -> Unit,
    onToggleOnline: () -> Unit,
    onUpdateExchangeRate: (Double) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenDrawer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRateDialog by remember { mutableStateOf(false) }
    var newRateText by remember(exchangeRate) { mutableStateOf(exchangeRate.toString()) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Drawer button & DONSAEL branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("menu_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Ouvrir le menu"
                        )
                    }

                    // DCM Mark Logo
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dcm_icon),
                            contentDescription = "Logo DCM DONSAEL",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "DONSAEL (DCM)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "COMMUNICATION & MULTISERVICES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // Right: Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Online / Offline Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isOnline) DonsaelGreen.copy(alpha = 0.15f) else DonsaelRed.copy(alpha = 0.15f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggleOnline() }
                            .testTag("network_status_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) DonsaelGreen else DonsaelRed)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isOnline) "En ligne" else "Hors ligne",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOnline) DonsaelGreen else DonsaelRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Exchange Rate Pill (Clickable)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showRateDialog = true }
                            .testTag("exchange_rate_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = "Taux de change",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1$ = $exchangeRate G",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Dark / Light Mode Toggle (Soleil / Lune)
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.size(36.dp).testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = if (isDarkMode) "Passer au Mode Clair" else "Passer au Mode Sombre",
                            tint = if (isDarkMode) Color(0xFFFFB703) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Downloads Shortcut
                    IconButton(
                        onClick = onOpenDownloads,
                        modifier = Modifier.size(36.dp).testTag("downloads_shortcut")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Téléchargements",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // User Chip / Logout
                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentUser.role == "ADMIN") "Admin" else "Vendeur",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                IconButton(
                                    onClick = onLogout,
                                    modifier = Modifier.size(24.dp).testTag("logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Déconnexion",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Exchange Rate Edit Dialog
    if (showRateDialog) {
        val canEdit = currentUser?.role == "ADMIN" || (currentUser?.canModifyExchangeRate == true)
        AlertDialog(
            onDismissRequest = { showRateDialog = false },
            title = {
                Text(
                    text = "Taux de Change USD / HTG",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Définissez le taux de conversion officiel appliqué à toutes les ventes, l'évaluation du stock et les rapports :",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (canEdit) {
                        OutlinedTextField(
                            value = newRateText,
                            onValueChange = { newRateText = it },
                            label = { Text("1 USD = X HTG") },
                            suffix = { Text("HTG") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exchange_rate_input")
                        )
                    } else {
                        Surface(
                            color = DonsaelRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Seul l'Administrateur peut modifier le taux de change. Taux actuel : 1 USD = $exchangeRate HTG",
                                modifier = Modifier.padding(8.dp),
                                color = DonsaelRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (canEdit) {
                    Button(
                        onClick = {
                            val r = newRateText.toDoubleOrNull()
                            if (r != null && r > 0) {
                                onUpdateExchangeRate(r)
                                showRateDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_exchange_rate_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}
