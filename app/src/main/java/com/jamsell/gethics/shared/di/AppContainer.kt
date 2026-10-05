package com.jamsell.gethics.shared.di

import android.content.Context
import androidx.room.Room
import com.jamsell.gethics.analytics.data.AnalyticsService
import com.jamsell.gethics.analytics.data.repository.AnalyticsRepository
import com.jamsell.gethics.finance.data.FinanceService
import com.jamsell.gethics.finance.data.repository.FinanceRepository
import com.jamsell.gethics.iam.data.AuthService
import com.jamsell.gethics.iam.data.UserService
import com.jamsell.gethics.iam.data.repository.UserRepository
import com.jamsell.gethics.iam.data.repository.AuthRepository
import com.jamsell.gethics.livestock.data.LivestockService
import com.jamsell.gethics.livestock.data.repository.LivestockRepository
import com.jamsell.gethics.sanitary.data.SanitaryService
import com.jamsell.gethics.sanitary.data.repository.SanitaryRepository
import com.jamsell.gethics.shared.data.local.AppDatabase
import com.jamsell.gethics.shared.data.local.SessionStorage
import com.jamsell.gethics.shared.data.remote.ApiClient
import com.jamsell.gethics.subscription.data.SubscriptionService
import com.jamsell.gethics.subscription.data.repository.SubscriptionRepository
import com.jamsell.gethics.veterinary.data.VeterinaryService
import com.jamsell.gethics.veterinary.data.repository.VeterinaryRepository

/**
 * Inyeccion de dependencias manual (en SuperHero esto se hacia dentro de MainActivity;
 * con 7 bounded contexts lo centralizamos aqui). Sin Hilt para mantener la guia de la clase.
 *
 * Al crear un repository nuevo: agregar su `val ... by lazy` aqui y listo.
 */
class AppContainer(context: Context) {

    val sessionStorage = SessionStorage(context.getSharedPreferences("gethics_session", Context.MODE_PRIVATE))

    private val retrofit = ApiClient.create(sessionStorage)

    private val database by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "db-gethics").build()
    }

    // iam
    val authRepository by lazy {
        AuthRepository(retrofit.create(AuthService::class.java), sessionStorage)
    }

    val userRepository by lazy {
        UserRepository(retrofit.create(UserService::class.java))
    }

    // livestock
    val livestockRepository by lazy {
        LivestockRepository(retrofit.create(LivestockService::class.java), database.getAnimalDao())
    }

    // sanitary
    val sanitaryRepository by lazy {
        SanitaryRepository(retrofit.create(SanitaryService::class.java))
    }

    // veterinary
    val veterinaryRepository by lazy {
        VeterinaryRepository(retrofit.create(VeterinaryService::class.java))
    }

    // finance
    val financeRepository by lazy {
        FinanceRepository(retrofit.create(FinanceService::class.java))
    }

    // analytics
    val analyticsRepository by lazy {
        AnalyticsRepository(retrofit.create(AnalyticsService::class.java))
    }

    // subscription
    val subscriptionRepository by lazy {
        SubscriptionRepository(retrofit.create(SubscriptionService::class.java))
    }
}
