package com.projet.app.di

import androidx.room.Room
import com.projet.app.data.local.AppDatabase
import com.projet.app.data.remote.DriveAuthManager
import com.projet.app.data.remote.GoogleDriveService
import com.projet.app.data.repository.ProjectRepositoryImpl
import com.projet.app.data.repository.SyncRepository
import com.projet.app.domain.repository.ProjectRepository
import com.projet.app.ui.detail.ProjectDetailViewModel
import com.projet.app.ui.list.ProjectListViewModel
import com.projet.app.ui.trash.TrashViewModel
import com.projet.app.update.AppUpdaterService
import com.projet.app.work.SyncScheduler
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Dados
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "projet.db").build() }
    single { get<AppDatabase>().projectDao() }
    single { OkHttpClient() }

    // Sync / Drive
    single { SyncScheduler(androidContext()) }
    single { DriveAuthManager(androidContext()) }
    single { GoogleDriveService(get()) }
    single { SyncRepository(get(), get(), get()) }

    // Domínio
    single<ProjectRepository> { ProjectRepositoryImpl(get(), get()) }

    // Auto-update
    single { AppUpdaterService(androidContext(), get()) }

    // ViewModels
    viewModel { ProjectListViewModel(get(), get(), get()) }
    viewModel { (projectId: String) -> ProjectDetailViewModel(projectId, get()) }
    viewModel { TrashViewModel(get()) }
}
