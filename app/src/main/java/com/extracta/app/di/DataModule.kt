package com.extracta.app.di

import android.content.Context
import androidx.room.Room
import com.extracta.app.data.local.DocumentDao
import com.extracta.app.data.local.DetectedEntityDao
import com.extracta.app.data.local.ExtractaDatabase
import com.extracta.app.data.repository.RoomDetectedEntityRepository
import com.extracta.app.data.repository.RoomDocumentRepository
import com.extracta.app.data.storage.ImageStorage
import com.extracta.app.data.storage.LocalImageStorage
import com.extracta.app.domain.repository.DocumentRepository
import com.extracta.app.domain.repository.DetectedEntityRepository
import com.extracta.app.entity.DefaultEntityExtractor
import com.extracta.app.entity.EntityExtractor
import com.extracta.app.entity.DefaultEntityProcessor
import com.extracta.app.entity.EntityProcessor
import com.extracta.app.feature.capture.CaptureFileStore
import com.extracta.app.feature.capture.LocalCaptureFileStore
import com.extracta.app.ocr.DefaultOcrProcessor
import com.extracta.app.ocr.MlKitTextRecognizer
import com.extracta.app.ocr.OcrProcessor
import com.extracta.app.ocr.TextRecognizer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        repository: RoomDocumentRepository,
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindDetectedEntityRepository(
        repository: RoomDetectedEntityRepository,
    ): DetectedEntityRepository

    @Binds
    @Singleton
    abstract fun bindEntityExtractor(extractor: DefaultEntityExtractor): EntityExtractor

    @Binds
    @Singleton
    abstract fun bindEntityProcessor(processor: DefaultEntityProcessor): EntityProcessor

    @Binds
    @Singleton
    abstract fun bindImageStorage(storage: LocalImageStorage): ImageStorage

    @Binds
    @Singleton
    abstract fun bindCaptureFileStore(storage: LocalCaptureFileStore): CaptureFileStore

    @Binds
    @Singleton
    abstract fun bindTextRecognizer(recognizer: MlKitTextRecognizer): TextRecognizer

    @Binds
    @Singleton
    abstract fun bindOcrProcessor(processor: DefaultOcrProcessor): OcrProcessor

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ExtractaDatabase =
            Room.databaseBuilder(context, ExtractaDatabase::class.java, "extracta.db")
                .addMigrations(ExtractaDatabase.MIGRATION_1_2)
                .build()

        @Provides
        fun provideDocumentDao(database: ExtractaDatabase): DocumentDao = database.documentDao()

        @Provides
        fun provideDetectedEntityDao(database: ExtractaDatabase): DetectedEntityDao =
            database.detectedEntityDao()

        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
