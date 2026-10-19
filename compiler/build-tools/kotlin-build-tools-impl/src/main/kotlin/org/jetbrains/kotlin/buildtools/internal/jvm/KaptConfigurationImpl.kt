/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.jvm.KaptConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.KaptDetectMemoryLeaksMode
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.buildtools.internal.arguments.CompilerPluginImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.CompilerPluginOptionImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.checkOptionIsAvailableForVersion
import org.jetbrains.kotlin.buildtools.internal.jvm.KaptDetectMemoryLeaksModeImpl.Companion.toImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.cast
import org.jetbrains.kotlin.buildtools.internal.serializability.throwPropertyNotFound
import java.nio.file.Path

@Serializable
internal class KaptConfigurationImpl(
    override val kaptClasspath: List<Path>,
    override val stubsOutputDir: Path,
    override val annotationProcessorsClasspath: List<Path>,
    var hasAptPhase: Boolean = false,
    var hasStubsPhase: Boolean = false,
) : KaptConfiguration, KaptConfiguration.Builder, KaptConfiguration.AptPhase, KaptConfiguration.AptPhase.Builder,
    KaptConfiguration.StubsPhase, KaptConfiguration.StubsPhase.Builder, DeepCopyable<KaptConfigurationImpl> {

    override val stubsPhase: KaptConfiguration.StubsPhase? get() = if (hasStubsPhase) this else null
    override val aptPhase: KaptConfiguration.AptPhase? get() = if (hasAptPhase) this else null

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptConfiguration.Option<V>): V = when (key.id) {
        "INCREMENTAL_DATA_OUTPUT_DIR" -> incrementalDataOutputDir
        "DUMP_DEFAULT_PARAMETER_VALUES" -> dumpDefaultParameterValues
        "VERBOSE" -> verbose
        "INFO_AS_WARNINGS" -> infoAsWarnings
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "INCREMENTAL_DATA_OUTPUT_DIR" -> incrementalDataOutputDir = value.cast()
            "DUMP_DEFAULT_PARAMETER_VALUES" -> dumpDefaultParameterValues = value.cast()
            "VERBOSE" -> verbose = value.cast()
            "INFO_AS_WARNINGS" -> infoAsWarnings = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override fun withAptPhase(sourcesOutputDir: Path): KaptConfiguration.AptPhase.Builder {
        hasAptPhase = true
        this.sourcesOutputDir = sourcesOutputDir
        return this
    }

    override fun withStubsPhase(): KaptConfiguration.StubsPhase.Builder {
        hasStubsPhase = true
        return this
    }

    override fun toBuilder(): KaptConfiguration.Builder = deepCopy()

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptConfiguration.StubsPhase.Option<V>): V = when (key.id) {
        "STRIP_METADATA" -> stripMetadata
        "STRICT_MODE" -> strictMode
        "MAP_DIAGNOSTIC_LOCATIONS" -> mapDiagnosticLocations
        else -> throwPropertyNotFound(key.id)
    } as V

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: KaptConfiguration.AptPhase.Option<V>): V = when (key.id) {
        "INCLUDE_COMPILE_CLASSPATH" -> includeCompileClasspath
        "CORRECT_ERROR_TYPES" -> correctErrorTypes
        "SOURCE_OUTPUT_DIR" -> sourcesOutputDir
        "CHANGED_FILES" -> changedFiles
        "COMPILED_SOURCES_DIR" -> compiledSourcesDir
        "INCREMENTAL_CACHE" -> incrementalCache
        "CLASSPATH_CHANGES" -> classpathChanges
        "PROCESS_INCREMENTALLY" -> processIncrementally
        "ANNOTATION_PROCESSOR_CLASSPATH" -> annotationProcessorsClasspath
        "ANNOTATION_PROCESSORS" -> annotationProcessors
        "APT_OPTIONS" -> aptOptions
        "JAVAC_OPTIONS" -> javacOptions
        "DETECT_MEMORY_LEAKS" -> detectMemoryLeaks.toApi()
        "DUMP_FILE_READ_HISTORY" -> dumpFileReadHistory
        "SHOW_PROCESSOR_STATS" -> showProcessorStats
        "DUMP_PROCESSOR_STATS" -> dumpProcessorStats
        else -> throwPropertyNotFound(key.id)
    } as V

    fun toCompilerPlugin(): CompilerPluginImpl {
        val copy = deepCopy()
        return CompilerPluginImpl(
            PLUGIN_ID,
            classpath = kaptClasspath,
            rawArguments = copy.toCompilerPluginOptions(),
            orderingRequirements = emptySet(),
        )
    }

    internal fun toCompilerPluginOptions(): List<CompilerPluginOptionImpl> {
        val compilerPluginOptions = mutableListOf<CompilerPluginOptionImpl>()
        val aptMode = when {
            hasAptPhase && hasStubsPhase -> "stubsAndApt"
            hasAptPhase -> "apt"
            hasStubsPhase -> "stubs"
            else -> error("At least one of apt or stubs phase required.")
        }
        compilerPluginOptions.add(CompilerPluginOptionImpl("aptMode", aptMode))
        with(compilerPluginOptions) {
            stubsOutputDir.toCompilerPluginOptions("stubs")
            annotationProcessorsClasspath.toCompilerPluginOptions("apclasspath")
            incrementalDataOutputDir.toCompilerPluginOptions("incrementalData")
            dumpDefaultParameterValues.toCompilerPluginOptions("dumpDefaultParameterValues")
            verbose.toCompilerPluginOptions("verbose")
            infoAsWarnings.toCompilerPluginOptions("infoAsWarnings")
            includeCompileClasspath.toCompilerPluginOptions("includeCompileClasspath")
            correctErrorTypes.toCompilerPluginOptions("correctErrorTypes")
            sourcesOutputDir.toCompilerPluginOptions("sources")
            changedFiles.toCompilerPluginOptions("changedFile")
            compiledSourcesDir.toCompilerPluginOptions("compiledSourcesDir")
            incrementalCache.toCompilerPluginOptions("incrementalCache")
            classpathChanges.toCompilerPluginOptions("classpathChange")
            processIncrementally.toCompilerPluginOptions("processIncrementally")
            annotationProcessors.toCompilerPluginOptions("processors")
            aptOptions.toCompilerPluginOptions("apOption")
            javacOptions.toCompilerPluginOptions("javacOption")
            detectMemoryLeaks.toCompilerPluginOptions("detectMemoryLeaks")
            dumpFileReadHistory.toCompilerPluginOptions("dumpFileReadHistory")
            showProcessorStats.toCompilerPluginOptions("showProcessorStats")
            dumpProcessorStats.toCompilerPluginOptions("dumpProcessorStats")
            stripMetadata.toCompilerPluginOptions("stripMetadata")
            strictMode.toCompilerPluginOptions("strict")
            mapDiagnosticLocations.toCompilerPluginOptions("mapDiagnosticLocations")
        }

        return compilerPluginOptions
    }

    context(compilerPluginOptions: MutableList<CompilerPluginOptionImpl>)
    fun Any?.toCompilerPluginOptions(
        outerKey: String,
    ): List<CompilerPluginOptionImpl> {
        when (this) {
            null -> return compilerPluginOptions
            is Path -> compilerPluginOptions.add(
                CompilerPluginOptionImpl(
                    outerKey, this.absolutePathStringOrThrow()
                )
            )
            is Collection<*> -> this.forEach {
                it?.toCompilerPluginOptions(outerKey)
            }
            is Map<*, *> -> this.forEach { (key, value) ->
                require(key is String && value is String) { "Only Strings supported in key and values for map arguments for now" }
                val keyValue = listOf(key, value).filter { it.isNotEmpty() }.joinToString("=")
                compilerPluginOptions.add(CompilerPluginOptionImpl(outerKey, keyValue))
            }
            else -> compilerPluginOptions.add(CompilerPluginOptionImpl(outerKey, this.toString()))
        }
        return compilerPluginOptions
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptConfiguration.AptPhase.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "INCLUDE_COMPILE_CLASSPATH" -> includeCompileClasspath = value.cast()
            "CORRECT_ERROR_TYPES" -> correctErrorTypes = value.cast()
            "SOURCE_OUTPUT_DIR" -> sourcesOutputDir = value.cast()
            "CHANGED_FILES" -> changedFiles = value.cast()
            "COMPILED_SOURCES_DIR" -> compiledSourcesDir = value.cast()
            "INCREMENTAL_CACHE" -> incrementalCache = value.cast()
            "CLASSPATH_CHANGES" -> classpathChanges = value.cast()
            "PROCESS_INCREMENTALLY" -> processIncrementally = value.cast()
            "ANNOTATION_PROCESSORS" -> annotationProcessors = value.cast()
            "APT_OPTIONS" -> aptOptions = value.cast()
            "JAVAC_OPTIONS" -> javacOptions = value.cast()
            "DETECT_MEMORY_LEAKS" -> detectMemoryLeaks = (value as KaptDetectMemoryLeaksMode).toImpl()
            "DUMP_FILE_READ_HISTORY" -> dumpFileReadHistory = value.cast()
            "SHOW_PROCESSOR_STATS" -> showProcessorStats = value.cast()
            "DUMP_PROCESSOR_STATS" -> dumpProcessorStats = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: KaptConfiguration.StubsPhase.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "STRIP_METADATA" -> stripMetadata = value.cast()
            "STRICT_MODE" -> strictMode = value.cast()
            "MAP_DIAGNOSTIC_LOCATIONS" -> mapDiagnosticLocations = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override fun build(): KaptConfiguration = deepCopy()

    override fun deepCopy(): KaptConfigurationImpl {
        return KaptConfigurationImpl(
            kaptClasspath = kaptClasspath,
            stubsOutputDir = stubsOutputDir,
            annotationProcessorsClasspath = annotationProcessorsClasspath,
            hasAptPhase,
            hasStubsPhase
        ).also {
            it.copyFrom(this)
        }
    }

    private fun copyFrom(other: KaptConfigurationImpl) {
        incrementalDataOutputDir = other.incrementalDataOutputDir
        dumpDefaultParameterValues = other.dumpDefaultParameterValues
        verbose = other.verbose
        infoAsWarnings = other.infoAsWarnings
        includeCompileClasspath = other.includeCompileClasspath
        correctErrorTypes = other.correctErrorTypes
        sourcesOutputDir = other.sourcesOutputDir
        changedFiles = other.changedFiles
        compiledSourcesDir = other.compiledSourcesDir
        incrementalCache = other.incrementalCache
        classpathChanges = other.classpathChanges
        processIncrementally = other.processIncrementally
        annotationProcessors = other.annotationProcessors
        aptOptions = other.aptOptions
        javacOptions = other.javacOptions
        detectMemoryLeaks = other.detectMemoryLeaks
        dumpFileReadHistory = other.dumpFileReadHistory
        showProcessorStats = other.showProcessorStats
        dumpProcessorStats = other.dumpProcessorStats
        stripMetadata = other.stripMetadata
        strictMode = other.strictMode
        mapDiagnosticLocations = other.mapDiagnosticLocations
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    internal var incrementalDataOutputDir: Path? = null
    internal var dumpDefaultParameterValues: Boolean = false
    internal var verbose: Boolean = false
    internal var infoAsWarnings: Boolean = false

    // apt phase
    internal var includeCompileClasspath: Boolean = true
    internal var correctErrorTypes: Boolean = false
    internal var sourcesOutputDir: Path? = null
    internal var changedFiles: List<Path>? = null
    internal var compiledSourcesDir: List<Path>? = null
    internal var incrementalCache: Path? = null
    internal var classpathChanges: List<String>? = null
    internal var processIncrementally: Boolean = false
    internal var annotationProcessors: List<String>? = null
    internal var aptOptions: Map<String, String>? = null
    internal var javacOptions: Map<String, String>? = null
    internal var detectMemoryLeaks: KaptDetectMemoryLeaksModeImpl = KaptDetectMemoryLeaksModeImpl.DEFAULT
    internal var dumpFileReadHistory: Path? = null
    internal var showProcessorStats: Boolean = false
    internal var dumpProcessorStats: Path? = null

    // stubs phase
    internal var stripMetadata: Boolean = false
    internal var strictMode: Boolean = false
    internal var mapDiagnosticLocations: Boolean = false

    companion object {
        const val PLUGIN_ID: String = "org.jetbrains.kotlin.kapt3"

//        val INCREMENTAL_DATA_OUTPUT_DIR: Option<Path?> = Option("incrementalData", null)
//        val DUMP_DEFAULT_PARAMETER_VALUES: Option<Boolean> = Option("dumpDefaultParameterValues", false)
//        val VERBOSE: Option<Boolean> = Option("verbose", false)
//        val INFO_AS_WARNINGS: Option<Boolean> = Option("infoAsWarnings", false)
//
//        // Apt phase options
//
//        val INCLUDE_COMPILE_CLASSPATH: Option<Boolean> = Option("includeCompileClasspath", true)
//        val CORRECT_ERROR_TYPES: Option<Boolean> = Option("correctErrorTypes", false)
//        val SOURCE_OUTPUT_DIR: Option<Path?> = Option("sources", null)
//        val CHANGED_FILES: Option<List<Path>?> = Option("changedFile", null)
//        val COMPILED_SOURCES_DIR: Option<List<Path>?> = Option("compiledSourcesDir", null)
//        val INCREMENTAL_CACHE: Option<Path?> = Option("incrementalCache", null)
//        val CLASSPATH_CHANGES: Option<List<String>?> = Option("classpathChange", null)
//        val PROCESS_INCREMENTALLY: Option<Boolean> = Option("processIncrementally", false)
//        val ANNOTATION_PROCESSOR_CLASSPATH: Option<List<Path>?> = Option("apclasspath", null)
//        val ANNOTATION_PROCESSORS: Option<List<String>?> = Option("processors", null)
//        val APT_OPTIONS: Option<Map<String, String>?> = Option("apOption", null)
//        val JAVAC_OPTIONS: Option<Map<String, String>?> = Option("javacOption", null)
//        val DETECT_MEMORY_LEAKS: Option<KaptDetectMemoryLeaksMode> = Option("detectMemoryLeaks", KaptDetectMemoryLeaksMode.DEFAULT)
//        val DUMP_FILE_READ_HISTORY: Option<Path?> = Option("dumpFileReadHistory", null)
//        val SHOW_PROCESSOR_STATS: Option<Boolean> = Option("showProcessorStats", false)
//        val DUMP_PROCESSOR_STATS: Option<Path?> = Option("dumpProcessorStats", null)
//
//        // Stubs phase options
//
//        val STUBS_OUTPUT_DIR: Option<Path?> = Option("stubs", null)
//        val STRIP_METADATA: Option<Boolean> = Option("stripMetadata", false)
//        val STRICT_MODE: Option<Boolean> = Option("strict", false)
//        val MAP_DIAGNOSTIC_LOCATIONS: Option<Boolean> = Option("mapDiagnosticLocations", false)
    }
}
