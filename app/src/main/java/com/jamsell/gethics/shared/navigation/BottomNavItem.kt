package com.jamsell.gethics.shared.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.vector.ImageVector
import com.jamsell.gethics.R
import com.jamsell.gethics.iam.domain.model.Role

/** labelRes: el texto sale de strings.xml para que cambie con el idioma (ES / EN). */
data class BottomNavItem(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val Herd = BottomNavItem(Routes.ANIMAL_LIST, R.string.nav_herd, Icons.Filled.Pets)
private val Clients = BottomNavItem(Routes.ASSIGNED_CLIENTS, R.string.nav_clients, Icons.Filled.People)
private val Calendar = BottomNavItem(Routes.SANITARY_CALENDAR, R.string.nav_calendar, Icons.Filled.CalendarMonth)
private val Finance = BottomNavItem(Routes.FINANCE_SUMMARY, R.string.nav_finance, Icons.Filled.AttachMoney)
private val Reports = BottomNavItem(Routes.REPORTS, R.string.nav_reports, Icons.Filled.BarChart)
private val Profile = BottomNavItem(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person)

/** Tabs de la barra inferior segun el rol. TODO: validar con el equipo que ve cada rol. */
fun bottomItemsFor(role: Role): List<BottomNavItem> = when (role) {
    Role.GANADERO -> listOf(Herd, Calendar, Finance, Reports, Profile)
    Role.VETERINARIO -> listOf(Clients, Calendar, Profile)
}
