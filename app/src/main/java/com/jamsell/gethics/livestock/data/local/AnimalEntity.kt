package com.jamsell.gethics.livestock.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamsell.gethics.livestock.domain.model.Animal

@Entity(tableName = "animals")
data class AnimalEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("tag")
    val tag: String,
    @ColumnInfo("breed")
    val breed: String,
    @ColumnInfo("photo_url")
    val photoUrl: String?,
    @ColumnInfo("birth_date")
    val birthDate: String? = null,
    @ColumnInfo("weight_kg")
    val weightKg: Double? = null,
    @ColumnInfo("qr_code")
    val qrCode: String? = null,
    @ColumnInfo("status")
    val status: String? = null
)

fun AnimalEntity.toAnimal() = Animal(id, name, tag, breed, photoUrl, birthDate, weightKg, qrCode, status)
fun Animal.toEntity() = AnimalEntity(id, name, tag, breed, photoUrl, birthDate, weightKg, qrCode, status)
