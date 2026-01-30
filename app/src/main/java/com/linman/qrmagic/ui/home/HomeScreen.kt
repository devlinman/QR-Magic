package com.linman.qrmagic.ui.home

//import androidx.compose.material.icons.filled.Add
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linman.qrmagic.InventoryTopAppBar
import com.linman.qrmagic.R
import com.linman.qrmagic.R.drawable.baseline_qr_code_36
import com.linman.qrmagic.data.Item
import com.linman.qrmagic.ui.AppViewModelProvider
import com.linman.qrmagic.ui.item.formatedPrice
import com.linman.qrmagic.ui.navigation.NavigationDestination
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter


object HomeDestination : NavigationDestination {
    override val route = "home"
    override val titleRes = R.string.app_name
}


@Composable
fun provideBarcodeScanner(context: Context): GmsBarcodeScanner {
    val options = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    return remember(context, options) {
        GmsBarcodeScanning.getClient(context, options)
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
//    navigateToItemEntry: () -> Unit,
    navigateToItemUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    val homeUiState by viewModel.homeUiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    val moduleInstall = ModuleInstall.getClient(context)
    val moduleInstallRequest = ModuleInstallRequest.newBuilder()
        .addApi(GmsBarcodeScanning.getClient(context))
        .build()
    moduleInstall
        .installModules(moduleInstallRequest)
//        .addOnSuccessListener {
//            if (it.areModulesAlreadyInstalled()) {
//            }
//        }
        .addOnFailureListener {
            Log.d("debug", "Homescreen.kt::158")
        }

    val scanner = provideBarcodeScanner(context = context)

    // Logic for Import/Export (Duplicated from SettingsScreen)
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
                                        // Strict validation
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
                                        // Check consistency
                                        val consistent = group.all { it.price == first.price && it.status == first.status }
                                        if (!consistent) {
                                            internalConflict = true
                                            break
                                        }
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            InventoryTopAppBar(
                title = stringResource(HomeDestination.titleRes),
                canNavigateBack = false,
                scrollBehavior = scrollBehavior
            )
        },

        floatingActionButton = {
            FloatingActionButton(
//                onClick = navigateToItemEntry,
                onClick = {
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            Toast.makeText(context, "Scan Successful!", Toast.LENGTH_SHORT).show()
                            val messageString: String? = barcode.rawValue
                            coroutineScope.launch {
                                if (messageString != null) {
                                    viewModel.saveItem(messageString)
                                }
                            }

                        }
                        .addOnCanceledListener {
                            Toast.makeText(context, "Scan Cancelled!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "Scan Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            exception.printStackTrace()
                        }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_large))
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = baseline_qr_code_36),
                    contentDescription = stringResource(R.string.item_entry_title)
                )
            }
        },
    ) { innerPadding ->
        HomeBody(
            itemList = homeUiState.itemList,
            onItemClick = navigateToItemUpdate,
            onImportClick = { importLauncher.launch(arrayOf("text/csv", "*/*")) },
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
private fun HomeBody(
    itemList: List<Item>, 
    onItemClick: (Int) -> Unit, 
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (itemList.isEmpty()) Arrangement.Center else Arrangement.Top,
        modifier = modifier
    ) {
        if (itemList.isEmpty()) {
            Text(
                text = stringResource(R.string.welcome_message),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onImportClick) {
                Text(text = stringResource(R.string.import_database))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(R.string.csv_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val rowModifier = Modifier.padding(vertical = 2.dp)
                    Text(text = "• " + stringResource(R.string.csv_col_name), style = MaterialTheme.typography.bodyMedium, modifier = rowModifier)
                    Text(text = "• " + stringResource(R.string.csv_col_qty), style = MaterialTheme.typography.bodyMedium, modifier = rowModifier)
                    Text(text = "• " + stringResource(R.string.csv_col_price), style = MaterialTheme.typography.bodyMedium, modifier = rowModifier)
                    Text(text = "• " + stringResource(R.string.csv_col_status), style = MaterialTheme.typography.bodyMedium, modifier = rowModifier)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.csv_note),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = stringResource(R.string.no_item_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            InventoryList(
                itemList = itemList,
                onItemClick = { onItemClick(it.id) },
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
            )
        }
    }
}

@Composable
private fun InventoryList(
    itemList: List<Item>, onItemClick: (Item) -> Unit, modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items = itemList, key = { it.id }) { item ->
            InventoryItem(item = item,
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.padding_small))
                    .clickable { onItemClick(item) })
        }
    }
}

@Composable
private fun InventoryItem(
    item: Item, modifier: Modifier = Modifier
) {
    val cardColor = when (item.status) {
        "invalid" -> Color.Red
        "used" -> Color.Gray
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_large)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.formatedPrice(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = stringResource(R.string.in_stock, item.quantity),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
