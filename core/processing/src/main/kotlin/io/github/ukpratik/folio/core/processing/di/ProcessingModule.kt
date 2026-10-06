// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.di

import android.app.ActivityManager
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ukpratik.folio.core.domain.concurrency.ProcessingDispatcher
import io.github.ukpratik.folio.core.domain.engine.ImportEngine
import io.github.ukpratik.folio.core.processing.ProcessingConfig
import io.github.ukpratik.folio.core.processing.detect.EdgeDetector
import io.github.ukpratik.folio.core.processing.detect.OpenCvEdgeDetector
import io.github.ukpratik.folio.core.processing.enhance.ImageEnhancer
import io.github.ukpratik.folio.core.processing.enhance.OpenCvEnhancer
import io.github.ukpratik.folio.core.processing.image.BitmapImageNormalizer
import io.github.ukpratik.folio.core.processing.importing.EdgeDetectingImportAnalyzer
import io.github.ukpratik.folio.core.processing.importing.ImportAnalyzer
import io.github.ukpratik.folio.core.processing.render.OpenCvPageRenderer
import io.github.ukpratik.folio.core.processing.render.PageRenderer
import io.github.ukpratik.folio.core.processing.image.ImageNormalizer
import io.github.ukpratik.folio.core.processing.importing.ContentOpener
import io.github.ukpratik.folio.core.processing.importing.ContentResolverOpener
import io.github.ukpratik.folio.core.processing.importing.ImportCoordinator
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object ProcessingProvidesModule {
    private const val LOW_MEMORY_CLASS_MB = 256

    @Provides @Singleton
    fun config(@ApplicationContext context: Context): ProcessingConfig {
        val am = context.getSystemService(ActivityManager::class.java)
        val lowRam = am.isLowRamDevice || am.memoryClass < LOW_MEMORY_CLASS_MB
        return ProcessingConfig(parallelism = if (lowRam) 1 else 2)
    }

    @Provides @Singleton @ProcessingDispatcher
    fun processingDispatcher(config: ProcessingConfig): CoroutineDispatcher =
        Dispatchers.Default.limitedParallelism(config.parallelism)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProcessingBindsModule {
    @Binds abstract fun importEngine(impl: ImportCoordinator): ImportEngine

    @Binds abstract fun normalizer(impl: BitmapImageNormalizer): ImageNormalizer

    @Binds abstract fun opener(impl: ContentResolverOpener): ContentOpener

    @Binds abstract fun edgeDetector(impl: OpenCvEdgeDetector): EdgeDetector

    @Binds abstract fun enhancer(impl: OpenCvEnhancer): ImageEnhancer

    @Binds abstract fun renderer(impl: OpenCvPageRenderer): PageRenderer

    @Binds abstract fun importAnalyzer(impl: EdgeDetectingImportAnalyzer): ImportAnalyzer
}
