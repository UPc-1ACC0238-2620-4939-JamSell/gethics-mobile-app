package com.jamsell.gethics.shared.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.vector.ImageVector
import com.jamsell.gethics.iam.domain.model.Role

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val Herd = BottomNavItem(Routes.ANIMAL_LIST, "Hato", Icons.Filled.Pets)
private val Clients = BottomNavItem(Routes.ASSIGNED_CLIENTS, "Clientes", Icons.Filled.People)
private val Calendar = BottomNavItem(Routes.SANITARY_CALENDAR, "Calendario", Icons.Filled.CalendarMonth)
private val Finance = BottomNavItem(Routes.FINANCE_SUMMARY, "Finanzas", Icons.Filled.AttachMoney)
private val Reports = BottomNavItem(Routes.REPORTS, "Reportes", Icons.Filled.BarChart)
private val Profile = BottomNavItem(Routes.PROFILE, "Perfil", Icons.Filled.Person)

/** Tabs de la barra inferior segun el rol. TODO: validar con el equipo que ve cada rol. */
fun bottomItemsFor(role: Role): List<BottomNavItem> = when (role) {
    Role.GANADERO -> listOf(Herd, Calendar, Finance, Reports, Profile)
    Role.VETERINARIO -> listOf(Clients, Calendar, Profile)
}
