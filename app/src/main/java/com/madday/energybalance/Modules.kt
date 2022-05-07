package com.madday.energybalance

import androidx.lifecycle.MutableLiveData
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

var mainModule = module {
    single { NodeProvider() }
    single { MutableLiveData<Map<String, String>>()}
    single { LabelProvider() }
    single { OkHttpClient() }
    single { DataSourceModule() }
    single { DataSourceScheduler() }
    single { DownloadRepeater() }
    viewModel { MainModelView() }

}