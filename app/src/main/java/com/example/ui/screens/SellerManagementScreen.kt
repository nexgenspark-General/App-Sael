package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserAccount
import com.example.ui.theme.DonsaelAmber
import com.example.ui.theme.DonsaelGreen
import com.example.ui.theme.DonsaelRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SellerManagementScreen(
    seller: UserAccount?,
    onCreateSeller: (fullName: String, username: String, pass: String) -> Unit,
    onUpdatePermissions: (UserAccount) -> Unit,
    onBlockSeller: (reason: String) -> Unit,
    onUnblockSeller: () -> Unit,
    onResetPassword: (String) -> Unit,
    onClearDevice: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBlockDialog by remember { mutableStateOf(false) }
    var blockReason by remember { mutableStateOf("Suspendu par l'administrateur") }

    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }

    // Creation form fields
    var newFullName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("seller_management_screen")
    ) {
        Text(
            text = "Gestion du Compte Vendeur",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "DONSAEL POS gère un compte vendeur unique sous supervision administrative.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (seller == null) {
            // Creation Form
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Créer", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Créer le compte Vendeur", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newFullName,
                        onValueChange = { newFullName = it },
                        label = { Text("Nom complet du vendeur") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Identifiant de connexion") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Mot de passe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (createError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = createError!!, color = DonsaelRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (newFullName.isBlank() || newUsername.isBlank() || newPass.isBlank()) {
                                createError = "Tous les champs sont obligatoires."
                                return@Button
                            }
                            onCreateSeller(newFullName.trim(), newUsername.trim(), newPass.trim())
                        },
                        modifier = Modifier.fillMaxWidth().testTag("create_seller_button")
                    ) {
                        Text("Créer le Compte Vendeur")
                    }
                }
            }
        } else {
            // Seller exists: Display profile & permissions
            val isBlocked = seller.status == "BLOCKED"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Vendeur", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = seller.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "Identifiant : ${seller.username}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            color = if (isBlocked) DonsaelRed.copy(alpha = 0.15f) else DonsaelGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isBlocked) "SUSPENDU" else "ACTIF",
                                color = if (isBlocked) DonsaelRed else DonsaelGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isBlocked && !seller.blockReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = DonsaelRed.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Motif : ${seller.blockReason}",
                                color = DonsaelRed,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Appareil associé : ${seller.activeDeviceId ?: "Aucun terminal verrouillé"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Granular Permissions
                    Text(
                        text = "Autorisations d'Accès Granulaires :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PermissionSwitchRow(
                        title = "Accès à la Caisse (POS)",
                        description = "Permet de réaliser des ventes et d'encaisser",
                        checked = seller.canAccessPos,
                        onCheckedChange = { onUpdatePermissions(seller.copy(canAccessPos = it)) }
                    )

                    PermissionSwitchRow(
                        title = "Accès à l'Inventaire",
                        description = "Permet de consulter et gérer les stocks",
                        checked = seller.canAccessInventory,
                        onCheckedChange = { onUpdatePermissions(seller.copy(canAccessInventory = it)) }
                    )

                    PermissionSwitchRow(
                        title = "Accès aux Rapports Financiers",
                        description = "Permet de consulter les bénéfices et chiffres d'affaires",
                        checked = seller.canAccessReports,
                        onCheckedChange = { onUpdatePermissions(seller.copy(canAccessReports = it)) }
                    )

                    PermissionSwitchRow(
                        title = "Accès aux Crédits Clients",
                        description = "Permet d'enregistrer des crédits et des remboursements",
                        checked = seller.canAccessCredits,
                        onCheckedChange = { onUpdatePermissions(seller.copy(canAccessCredits = it)) }
                    )

                    PermissionSwitchRow(
                        title = "Accès à l'Historique & Journal",
                        description = "Permet de consulter les transactions et réimprimer des reçus",
                        checked = seller.canAccessHistory,
                        onCheckedChange = { onUpdatePermissions(seller.copy(canAccessHistory = it)) }
                    )

                    PermissionSwitchRow(
                        title = "Modifier le Taux de Change",
                        description = "Permet d'ajuster le taux officiel 1 USD = X HTG",
                        checked = seller.canModifyExchangeRate,
                        onCheckedChange = { onUpdatePermissions(seller.copy(canModifyExchangeRate = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Administrative Actions
                    Text(
                        text = "Actions Administratives :",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasswordResetDialog = true },
                            modifier = Modifier.weight(1f).testTag("reset_seller_password_btn")
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = "Mot de passe", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Changer Pass", fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = { onClearDevice(seller.id) },
                            modifier = Modifier.weight(1f).testTag("clear_seller_device_btn")
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = "Dissocier", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dissocier", fontSize = 11.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isBlocked) {
                        Button(
                            onClick = onUnblockSeller,
                            colors = ButtonDefaults.buttonColors(containerColor = DonsaelGreen),
                            modifier = Modifier.fillMaxWidth().testTag("unblock_seller_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Réactiver", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réactiver le Compte Vendeur")
                        }
                    } else {
                        Button(
                            onClick = { showBlockDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DonsaelRed),
                            modifier = Modifier.fillMaxWidth().testTag("block_seller_btn")
                        ) {
                            Icon(Icons.Default.Block, contentDescription = "Suspendre", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Suspendre le Compte Vendeur")
                        }
                    }
                }
            }
        }
    }

    // Suspend Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Suspendre le Vendeur") },
            text = {
                Column {
                    Text("Indiquez la raison du blocage (affichée lors de sa tentative de connexion) :", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = blockReason,
                        onValueChange = { blockReason = it },
                        label = { Text("Motif de suspension") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBlockSeller(blockReason)
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DonsaelRed)
                ) {
                    Text("Confirmer la suspension")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Reset Password Dialog
    if (showPasswordResetDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordResetDialog = false },
            title = { Text("Réinitialiser le Mot de Passe") },
            text = {
                Column {
                    Text("Définissez le nouveau mot de passe pour le compte vendeur :", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nouveau mot de passe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isNotBlank()) {
                            onResetPassword(newPassword.trim())
                            showPasswordResetDialog = false
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordResetDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun PermissionSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
