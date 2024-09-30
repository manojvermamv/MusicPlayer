package com.anubhav.musicplayer.di

import com.anubhav.musicplayer.viewmodel.MainActivityViewModel
import com.anubhav.musicplayer.model.repository.NavRepository
import com.anubhav.musicplayer.model.local.ContentDataSource
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    single { ContentDataSource(androidContext().contentResolver) }
    single { NavRepository(get()) }
    viewModel {
        MainActivityViewModel(androidApplication(), get())
    }
}