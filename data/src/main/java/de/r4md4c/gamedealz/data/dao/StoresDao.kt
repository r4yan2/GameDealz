package de.r4md4c.gamedealz.data.dao

import androidx.room.*
import de.r4md4c.gamedealz.data.entity.Store
import io.reactivex.Flowable

@Dao
internal interface StoresDao {

    /**
     * Inserts stores in a transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stores: Collection<Store>)

    /**
     * Retrieves all stored stores.
     *
     * @return a list of stores.
     */
    @Query("SELECT * FROM Store")
    fun all(): Flowable<List<Store>>

    /**
     * Retrieves all stored stores with ids
     *
     * @return a list of stores.
     */
    @Query("SELECT * FROM Store WHERE id IN (:ids)")
    fun all(ids: Set<String>): Flowable<List<Store>>

    @Transaction
    suspend fun replaceAll(stores: Collection<Store>) {
        delete()
        insert(stores)
    }

    /**
     * All selected stores.
     *
     * @return a list of stores.
     */
    @Query("SELECT * FROM Store WHERE selected = 1")
    fun allSelected(): Flowable<List<Store>>

    /**
     * Retrieves a single store.
     *
     * @param storeId the store id that will be used to retrieve the store.
     * @return the retrieved store or null if not found.
     */
    @Query("SELECT * FROM Store WHERE id = :storeId")
    suspend fun singleStore(storeId: String): Store?

    /**
     * Updates the selected field of a set of rows.
     *
     * @param selected The value of the selected column.
     * @param ids the ids of the items to be updated.
     * @return number of updated rows.
     */
    @Query("UPDATE Store SET selected=:selected WHERE id IN (:ids)")
    fun updateSelected(selected: Boolean, ids: Set<String>): Int

    @Query("DELETE FROM STORE")
    fun delete(): Int
}