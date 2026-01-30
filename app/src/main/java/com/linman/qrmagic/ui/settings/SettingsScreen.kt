package com.linman.qrmagic.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linman.qrmagic.InventoryTopAppBar
import com.linman.qrmagic.R
import com.linman.qrmagic.data.Item
import com.linman.qrmagic.ui.AppViewModelProvider
import com.linman.qrmagic.ui.home.HomeViewModel
import com.linman.qrmagic.ui.home.SortOption
import com.linman.qrmagic.ui.navigation.NavigationDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object SettingsDestination : NavigationDestination {
    override val route = "settings"
    override val titleRes = R.string.settings_title
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateUp: () -> Unit,
    canNavigateBack: Boolean = false,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Logic for Import/Export
    var showImportErrorDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showInternalDuplicateDialog by remember { mutableStateOf(false) }
    var showMergeErrorDialog by remember { mutableStateOf(false) }
    var mergeErrorMessage by remember { mutableStateOf("") }
    var pendingImportItems by remember { mutableStateOf<List<Item>>(emptyList()) }

    // Helper to proceed to DB duplicate check
    val processImportStep2: (List<Item>) -> Unit = { items ->
        coroutineScope.launch {
            if (viewModel.verifyImport(items)) {
                pendingImportItems = items
                showDuplicateDialog = true
            } else {
                try {
                    viewModel.mergeAndImportItems(items)
                    Toast.makeText(context, "Imported ${items.size} items", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    mergeErrorMessage = e.message ?: "Unknown merge error"
                    showMergeErrorDialog = true
                }
            }
        }
    }

    if (showImportErrorDialog) {
        AlertDialog(
            onDismissRequest = { showImportErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showImportErrorDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Import Failed") },
            text = { Text("The imported database is malformed. Please ensure:\n1. All rows have correct data types.\n2. Quantity 0 items have 'used' or 'invalid' status.\n3. Duplicate entries in the file have matching Price and Status.") }
        )
    }

    if (showInternalDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showInternalDuplicateDialog = false 
                pendingImportItems = emptyList()
            },
            confirmButton = {
                TextButton(onClick = {
                    showInternalDuplicateDialog = false
                    processImportStep2(pendingImportItems)
                }) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showInternalDuplicateDialog = false
                    pendingImportItems = emptyList()
                    Toast.makeText(context, "Import Aborted", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Abort")
                }
            },
            title = { Text("Data Duplication Detected") },
            text = { Text("The imported file contains duplicate entries for the same item name.\n\nDo you want to proceed and merge them?") }
        )
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDuplicateDialog = false 
                pendingImportItems = emptyList()
            },
            confirmButton = {
                TextButton(onClick = {
                    showDuplicateDialog = false
                    coroutineScope.launch {
                        try {
                            viewModel.mergeAndImportItems(pendingImportItems)
                            Toast.makeText(context, "Imported with Merge!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            mergeErrorMessage = e.message ?: "Unknown merge error"
                            showMergeErrorDialog = true
                        }
                        pendingImportItems = emptyList()
                    }
                }) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDuplicateDialog = false
                    pendingImportItems = emptyList()
                    Toast.makeText(context, "Import Aborted", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Abort")
                }
            },
            title = { Text("Database Duplicates Found") },
            text = { Text("Some imported items already exist in the database.\n\nDo you want to proceed and merge them (adding quantities) or abort?") }
        )
    }

    if (showMergeErrorDialog) {
        AlertDialog(
            onDismissRequest = { showMergeErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showMergeErrorDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Cannot Merge / Merge Conflict") },
            text = { Text(mergeErrorMessage) }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            val reader = BufferedReader(InputStreamReader(inputStream))
                            val itemsToImport = mutableListOf<Item>()
                            val lines = reader.readLines()
                            var isValidFile = true

                            for ((_, line) in lines.withIndex()) {
                                if (line.isBlank()) continue
                                
                                val parts = line.split(",")
                                if (parts.size >= 4) {
                                    val name = parts[0].trim()
                                    val countStr = parts[1].trim()
                                    val priceStr = parts[2].trim()
                                    val status = parts[3].trim()

                                    val count = countStr.toIntOrNull()
                                    val price = priceStr.toDoubleOrNull()
                                    
                                    val isValidStatus = status == "valid" || status == "invalid" || status == "used"

                                    if (count != null && price != null && isValidStatus) {
                                        // Strict validation: if quantity is 0, status must be used or invalid
                                        if (count == 0 && status == "valid") {
                                            isValidFile = false
                                            break
                                        }
                                        itemsToImport.add(Item(name = name, quantity = count, price = price, status = status))
                                    } else {
                                        isValidFile = false
                                        break
                                    }
                                } else {
                                    isValidFile = false
                                    break
                                }
                            }

                            if (isValidFile && itemsToImport.isNotEmpty()) {
                                // Internal Duplicate Check
                                val grouped = itemsToImport.groupBy { it.name }
                                var internalConflict = false
                                var hasInternalDuplicates = false
                                val mergedFileItems = mutableListOf<Item>()

                                for ((_, group) in grouped) {
                                    if (group.size > 1) {
                                        hasInternalDuplicates = true
                                        val first = group[0]
                                        // Check consistency: Price and Status must match
                                        val consistent = group.all { it.price == first.price && it.status == first.status }
                                        if (!consistent) {
                                            internalConflict = true
                                            break
                                        }
                                        // Merge quantities
                                        val totalQty = group.sumOf { it.quantity }
                                        mergedFileItems.add(first.copy(quantity = totalQty))
                                    } else {
                                        mergedFileItems.add(group[0])
                                    }
                                }

                                if (internalConflict) {
                                    launch(Dispatchers.Main) { showImportErrorDialog = true }
                                } else if (hasInternalDuplicates) {
                                    pendingImportItems = mergedFileItems
                                    launch(Dispatchers.Main) { showInternalDuplicateDialog = true }
                                } else {
                                    // No internal dupes, proceed to DB check
                                    launch(Dispatchers.Main) { processImportStep2(mergedFileItems) }
                                }
                            } else {
                                launch(Dispatchers.Main) {
                                    showImportErrorDialog = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val items = viewModel.getAllItems()
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            val writer = BufferedWriter(OutputStreamWriter(outputStream))
                            for (item in items) {
                                writer.write("${item.name},${item.quantity},${item.price},${item.status}")
                                writer.newLine()
                            }
                            writer.flush()
                        }
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Exported ${items.size} items", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            InventoryTopAppBar(
                title = stringResource(SettingsDestination.titleRes),
                canNavigateBack = canNavigateBack,
                navigateUp = navigateUp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sort Options Section
            SettingsSection(title = "Sort Options") {
                val currentSortOption by viewModel.sortOption.collectAsState()
                Column(Modifier.selectableGroup()) {
                    val sortOptions = listOf(
                        SortOption.NAME to "Name",
                        SortOption.PRICE to "Price",
                        SortOption.QUANTITY to "Quantity",
                        SortOption.STATUS to "Status",
                        SortOption.RESET to "Reset (Default)"
                    )
                    
                    sortOptions.forEach { (option, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.RadioButton) {
                                    viewModel.updateSortOption(option)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentSortOption == option),
                                onClick = null 
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }

            // Import/Export Section
            SettingsSection(title = "Database Management") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { importLauncher.launch(arrayOf("text/csv", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Database")
                    }
                    Button(
                        onClick = { exportLauncher.launch("inventory_backup.csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Database")
                    }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.deleteAllItems()
                                Toast.makeText(context, "Deleted All Items!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete All Items")
                    }
                }
            }

            // About Section
            SettingsSection(title = "About") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Developer",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "devlinman",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/devlinman"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Visit Website")
                    }

                    Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "License",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Apache License\nVersion 2.0, January 2004",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}
