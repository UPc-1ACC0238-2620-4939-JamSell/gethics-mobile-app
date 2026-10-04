package com.jamsell.gethics.livestock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnimalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animals: List<AnimalEntity>)

    @Query("select * from animals order by name")
    suspend fun fetchAll(): List<AnimalEntity>

    @Query("select * from animals where id = :id")
    suspend fun fetchById(id: Long): AnimalEntity?
}
