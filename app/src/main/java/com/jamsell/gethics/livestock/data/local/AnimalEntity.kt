package com.jamsell.gethics.livestock.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamsell.gethics.livestock.domain.model.Animal

@Entity(tableName = "animals")
data class AnimalEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("tag")
    val tag: String,
    @ColumnInfo("breed")
    val breed: String,
    @ColumnInfo("photo_url")
    val photoUrl: String?
)

fun AnimalEntity.toAnimal() = Animal(id, name, tag, breed, photoUrl)
fun Animal.toEntity() = AnimalEntity(id, name, tag, breed, photoUrl)
