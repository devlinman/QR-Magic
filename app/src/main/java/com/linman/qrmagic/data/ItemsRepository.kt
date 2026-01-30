package com.linman.qrmagic.data

import kotlinx.coroutines.flow.Flow

interface ItemsRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllItemsStream(): Flow<List<Item>>

    /**
     * Retrieve all items as a list (snapshot).
     */
    suspend fun getAllItemsList(): List<Item>

    /**
     * Retrieve an item from the given data source that matches with the [id].
     */
    fun getItemStream(id: Int): Flow<Item?>

    /**
     * Retrieve an item from the given data source that matches with the [name].
     */
    suspend fun getItemByName(name: String): Item?

    /**
     * Insert item in the data source
     */
    suspend fun insertItem(item: Item)

    /**
     * Insert multiple items in the data source
     */
    suspend fun insertItems(items: List<Item>)

    /**
     * Delete item from the data source
     */
    suspend fun deleteItem(item: Item)

    /**
     * Update item in the data source
     */
    suspend fun updateItem(item: Item)

    suspend fun deleteAll()
}
