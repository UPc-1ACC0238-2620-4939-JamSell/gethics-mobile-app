package com.jamsell.gethics.livestock.data.repository

import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.local.AnimalDao

/**
 * Patron offline-first (igual que HeroRepository de SuperHero con su Dao):
 * 1) intentar traer del backend y guardar en Room, 2) si no hay red, leer de Room.
 */
class LivestockRepository(
    private val service: LivestockService,
    private val dao: AnimalDao
) {
    // TODO: suspend fun getAnimals(): Resource<List<Animal>>
}
