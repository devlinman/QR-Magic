package com.linman.qrmagic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linman.qrmagic.data.Item
import com.linman.qrmagic.data.ItemsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

enum class SortOption {
    NAME, PRICE, QUANTITY, STATUS, RESET
}

/**
 * ViewModel to retrieve all items in the Room database.
 */
class HomeViewModel(private val itemsRepository: ItemsRepository) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.RESET)
    val sortOption: StateFlow<SortOption> = _sortOption

    /**
     * Holds home ui state. The list of items are retrieved from [ItemsRepository] and mapped to
     * [HomeUiState]
     */
    val homeUiState: StateFlow<HomeUiState> =
        combine(itemsRepository.getAllItemsStream(), _sortOption) { items, sortOption ->
            val sortedItems = when (sortOption) {
                SortOption.NAME -> items.sortedBy { it.name }
                SortOption.PRICE -> items.sortedBy { it.price }
                SortOption.QUANTITY -> items.sortedBy { it.quantity }
                SortOption.STATUS -> items.sortedWith(compareBy {
                    when (it.status) {
                        "valid" -> 0
                        "used" -> 1
                        "invalid" -> 2
                        else -> 3
                    }
                })
                SortOption.RESET -> items.sortedBy { it.id }
            }
            HomeUiState(sortedItems)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = HomeUiState()
            )

    fun updateSortOption(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    suspend fun saveItem(string: String) {
        val existingItem = itemsRepository.getItemByName(string)
        if (existingItem != null) {
            if (existingItem.status == "invalid") {
                // Item is invalid, do nothing
                return
            }
            if (existingItem.quantity > 0) {
                 val newQuantity = existingItem.quantity - 1
                 val newStatus = if (newQuantity == 0) "used" else existingItem.status
                 itemsRepository.updateItem(existingItem.copy(quantity = newQuantity, status = newStatus))
            }
        } else {
            itemsRepository.insertItem(Item(
                id = 0,
                name = string,
                price = 0.0,
                quantity = 0,
                status = "invalid"
            ))
        }
    }
    
    suspend fun importItems(items: List<Item>) {
        itemsRepository.insertItems(items)
    }

    suspend fun verifyImport(items: List<Item>): Boolean {
        for (item in items) {
            val existingItem = itemsRepository.getItemByName(item.name)
            if (existingItem != null) {
                return true // Duplicate found
            }
        }
        return false // No duplicates
    }

    suspend fun mergeAndImportItems(items: List<Item>) {
        for (item in items) {
            val existingItem = itemsRepository.getItemByName(item.name)
            if (existingItem != null) {
                if (existingItem.price != item.price || existingItem.status != item.status) {
                    throw Exception("Merge Conflict: Item '${item.name}' has different price or status.")
                }
                val newQuantity = existingItem.quantity + item.quantity
                itemsRepository.updateItem(existingItem.copy(quantity = newQuantity))
            } else {
                itemsRepository.insertItem(item)
            }
        }
    }
    
    suspend fun getAllItems(): List<Item> {
        return itemsRepository.getAllItemsList()
    }

    suspend fun deleteAllItems() {
        itemsRepository.deleteAll()
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Ui State for HomeScreen
 */
data class HomeUiState(val itemList: List<Item> = listOf())
