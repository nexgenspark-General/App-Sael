package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.theme.DonsaelBlue
import com.example.ui.theme.DonsaelNavy
import com.example.ui.theme.DonsaelTeal
import java.util.Locale

@Composable
fun SettingsScreen(
    currentUser: UserAccount?,
    exchangeRate: Double,
    paperWidthMm: Int,
    messageOfTheDay: String,
    onUpdateExchangeRate: (Double) -> Unit,
    onUpdatePaperWidth: (Int) -> Unit,
    onUpdateMessageOfTheDay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rateText by remember(exchangeRate) { mutableStateOf(exchangeRate.toString()) }
    var currentPaperWidth by remember(paperWidthMm) { mutableStateOf(paperWidthMm.toFloat()) }
    var motdText by remember(messageOfTheDay) { mutableStateOf(messageOfTheDay) }

    val isAdmin = currentUser?.role == "ADMIN"
    val canEditRate = isAdmin || (currentUser?.canModifyExchangeRate == true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Paramètres de l'Application",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Personnalisation du matériel, taux et messages",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Thermal Receipt Paper Width
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = "Papier", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Format d'Impression des Reçus (Thermique)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ajustez la largeur du papier selon votre imprimante thermique (44 mm à 112 mm). Standard recommandé : 80 mm ou 58 mm.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Largeur actuelle : ${currentPaperWidth.toInt()} mm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                currentPaperWidth = 58f
                                onUpdatePaperWidth(58)
                            },
                            modifier = Modifier.testTag("set_paper_58_button")
                        ) {
                            Text("58 mm", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                currentPaperWidth = 80f
                                onUpdatePaperWidth(80)
                            },
                            modifier = Modifier.testTag("set_paper_80_button")
                        ) {
                            Text("80 mm", fontSize = 11.sp)
                        }
                    }
                }

                Slider(
                    value = currentPaperWidth,
                    onValueChange = { currentPaperWidth = it },
                    onValueChangeFinished = { onUpdatePaperWidth(currentPaperWidth.toInt()) },
                    valueRange = 44f..112f,
                    steps = 67,
                    modifier = Modifier.fillMaxWidth().testTag("paper_width_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: Exchange Rate
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = "Taux", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Taux de Change Officiel (USD / HTG)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ce taux est utilisé pour la conversion instantanée lors des encaissements et l'évaluation du stock.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        enabled = canEditRate,
                        label = { Text("1 USD = X HTG") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("settings_rate_input")
                    )

                    if (canEditRate) {
                        Button(
                            onClick = {
                                val r = rateText.toDoubleOrNull()
                                if (r != null && r > 0) {
                                    onUpdateExchangeRate(r)
                                }
                            },
                            modifier = Modifier.testTag("settings_save_rate_btn")
                        ) {
                            Text("Sauvegarder")
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            Spacer(modifier = Modifier.height(14.dp))

            // Card 3: Message of the Day (Admin only)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = "Annonce", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message d'Annonce du Jour",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ce message apparaît en haut du tableau de bord pour tous les utilisateurs.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = motdText,
                        onValueChange = { motdText = it },
                        label = { Text("Texte de l'annonce") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onUpdateMessageOfTheDay(motdText.trim()) },
                        modifier = Modifier.fillMaxWidth().testTag("save_motd_btn")
                    ) {
                        Text("Mettre à jour l'Annonce")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 4: Enterprise & Partner Information (DONSAEL & NexGen Spark)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Official DCM Logo Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_donsael_logo),
                        contentDescription = "Logo Officiel DONSAEL COMMUNICATION",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DONSAEL COMMUNICATION ET MULTI-SERVICES",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Vente d'Appareils Électroménagers & Divers",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Belle-Vue, L'Asile, Nippes, Haïti",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Téléphones : 3753-2670 / 3224-4430 | Email : donsaelmondelice08@gmail.com",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Partenaire Technologique & Développement :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NexGen Spark Technologies",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Support technique : support@nexgns.net | (+509) 4200-0315 / 3644-4675",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Architecture : Base de données locale sécurisée Room Offline-First",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
