package com.jamsell.gethics.livestock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AnimalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(animal: AnimalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animals: List<AnimalEntity>)

    @Query("select * from animals order by name")
    suspend fun fetchAll(): List<AnimalEntity>

    @Query("select * from animals where id = :id")
    suspend fun fetchById(id: String): AnimalEntity?

    @Query(
        """
        select * from animals
        where status = :status
          and (lower(tag) like :pattern escape '!'
               or lower(breed) like :pattern escape '!'
               or lower(name) like :pattern escape '!')
        order by tag
        """
    )
    suspend fun search(pattern: String, status: String): List<AnimalEntity>

    @Query("delete from animals where status = :status")
    suspend fun deleteByStatus(status: String)

    @Transaction
    suspend fun replaceByStatus(status: String, animals: List<AnimalEntity>) {
        deleteByStatus(status)
        insertAll(animals)
    }
}
