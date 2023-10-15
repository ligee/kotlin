/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.data

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2DataCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File
import java.io.PrintWriter

class K2DataCompiler : CLICompiler<K2DataCompilerArguments>(){

    override val defaultPerformanceManager: CommonCompilerPerformanceManager = K2DataCompilerPerformanceManager()

    override fun createArguments() = K2DataCompilerArguments()

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: K2DataCompilerArguments, services: Services
    ) {
        // No specific arguments yet
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2DataCompilerArguments) {}

    override fun doExecute(
        arguments: K2DataCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        try {
            val performanceManager = configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)

            val pluginLoadResult = loadPlugins(paths, arguments, configuration)
            if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

            val moduleName = JvmProtoBufUtil.DEFAULT_MODULE_NAME
            configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

            for (arg in arguments.freeArgs) {
                configuration.addKotlinSourceRoot(arg, isCommon = true, hmppModuleName = null)
            }
            if (arguments.classpath != null) {
                configuration.addJvmClasspathRoots(arguments.classpath!!.split(File.pathSeparatorChar).map(::File))
            }

            configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
            configuration.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

            val environment =
                KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.METADATA_CONFIG_FILES)

            val sourceFiles = environment.getSourceFiles()
            performanceManager.notifyCompilerInitialized(sourceFiles.size, environment.countLinesOfCode(sourceFiles), "$moduleName module")

            if (environment.getSourceFiles().isEmpty()) {
                if (arguments.version) {
                    return ExitCode.OK
                }
                collector.report(CompilerMessageSeverity.ERROR, "No source files")
                return ExitCode.COMPILATION_ERROR
            }

            val destination = arguments.destination

            val printWriter = destination?.let { File(it).printWriter() } ?: PrintWriter(System.err)

            try {
                printWriter.use { writer ->
                    val dumper = object : IrDataSink() {
                        var indentString = ""

                        override fun addKeyValue(name: String, value: String) {
                            writer.println("$indentString$name: $value")
                        }

                        override fun openGroup(name: String?) {
                            writer.println("$indentString${name?.plus(": ") ?: ""}{")
                            indentString += "  "
                        }

                        override fun closeGroup() {
                            indentString = indentString.removeRange(indentString.lastIndex - 1, indentString.length)
                            writer.println("$indentString}")
                        }
                    }

                    val serializer = FirKDataSerializer(configuration, environment, dumper)

                    serializer.analyzeAndSerialize()
                }
            } catch (e: CompilationException) {
                collector.report(
                    CompilerMessageSeverity.EXCEPTION,
                    OutputMessageUtil.renderException(e),
                    MessageUtil.psiElementToMessageLocation(e.element)
                )
                return ExitCode.INTERNAL_ERROR
            }

            return if (collector.hasErrors()) ExitCode.COMPILATION_ERROR else ExitCode.OK
        } finally {
            if (collector.hasErrors()) (collector as GroupingMessageCollector).flush()
        }
    }

    override fun executableScriptFileName(): String = "kotlinc"

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = BuiltInsBinaryVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2DataCompiler(), args)
        }
    }

    protected class K2DataCompilerPerformanceManager : CommonCompilerPerformanceManager("Kotlin to data compiler")
}