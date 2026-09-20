package com.example.everfit.assignment.di

import com.example.everfit.assignment.data.WorkoutRepositoryImpl
import com.example.everfit.assignment.data.local.EverfitDatabase
import com.example.everfit.assignment.data.remote.WorkoutApi
import com.example.everfit.assignment.core.domain.WeekProvider
import com.example.everfit.assignment.core.domain.WorkoutRemoteSource
import com.example.everfit.assignment.core.domain.WorkoutRepository
import com.example.everfit.assignment.core.ext.json.JsonHelper
import com.example.everfit.assignment.feature.calendar.CalendarViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { EverfitDatabase.build(androidContext()) }
    single { get<EverfitDatabase>().workoutDao() }
    single { get<EverfitDatabase>().completionDao() }

    single {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) { json(JsonHelper.json) }
        }
    }
    single<WorkoutRemoteSource> { WorkoutApi(get(), WORKOUTS_ENDPOINT) }

    single<Clock> { Clock.systemDefaultZone() }
    single<CoroutineDispatcher>(named(IO_DISPATCHER)) { Dispatchers.IO }
    single { WeekProvider(get()) }

    single<WorkoutRepository> {
        WorkoutRepositoryImpl(
            remote = get(),
            workoutDao = get(),
            completionDao = get(),
            ioDispatcher = get(named(IO_DISPATCHER)),
        )
    }

    viewModel { CalendarViewModel(get(), get()) }
}

const val IO_DISPATCHER = "io"
private const val WORKOUTS_ENDPOINT = "https://mock.internalef.com/workouts"
