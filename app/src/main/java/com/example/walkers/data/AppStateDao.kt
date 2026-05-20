package com.example.walkers.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1")
    fun observeState(): Flow<AppStateEntity?>

    @Query("SELECT * FROM app_state WHERE id = 1")
    suspend fun getState(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AppStateEntity)

    @Transaction
    suspend fun getOrCreate(): AppStateEntity {
        val current = getState()
        if (current != null) return current

        val initial = AppStateEntity()
        upsert(initial)
        return initial
    }
}
