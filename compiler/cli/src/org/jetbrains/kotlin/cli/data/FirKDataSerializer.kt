/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.data

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.jvm.serialization.DisabledDescriptorMangler
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.messageCollector
import org.jetbrains.kotlin.cli.metadata.FirMetadataSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnosticReporterWithContext
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import java.io.File


class FirKDataSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
    private val dumper: IrDataSink,
) : FirMetadataSerializer(configuration, environment) {

    override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File) {
    }

    private fun serializeToDumper(analysisResult: List<ModuleCompilerAnalyzedOutput>) {
        val signatureComposer = DescriptorSignatureComposerStub(DisabledDescriptorMangler)
        val firMangler = FirDummyKotlinMangler()
        val commonMemberStorage = Fir2IrCommonMemberStorage(signatureComposer, firMangler)
        val fir2IrExtensions = Fir2IrExtensions.Default
        val kotlinBuiltIns = DefaultBuiltIns.Instance
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val fir2IrConfiguration = Fir2IrConfiguration(
            languageVersionSettings = configuration.languageVersionSettings,
            diagnosticReporter = diagnosticsReporter,
            linkViaSignatures = false,
            evaluatedConstTracker = configuration
                .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
            inlineConstTracker = null,
            expectActualTracker = null,
            allowNonCachedDeclarations = false,
            useIrFakeOverrideBuilder = configuration.getBoolean(CommonConfigurationKeys.USE_IR_FAKE_OVERRIDE_BUILDER),
        )

        val fir2irResult = analysisResult.single().convertToIr(
            fir2IrExtensions,
            fir2IrConfiguration,
            commonMemberStorage,
            irBuiltIns = null,
            irMangler = DummyManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = kotlinBuiltIns,
            typeContextProvider = ::IrTypeSystemContextImpl
        )
        val irBuiltIns = fir2irResult.components.irBuiltIns

        val backendContext = IrDataBackendContext(kotlinBuiltIns, irBuiltIns, commonMemberStorage.symbolTable, configuration)

        val phaseConfig = PhaseConfig(dataLoweringPhases)

        dataLoweringPhases.invokeToplevel(phaseConfig, backendContext, fir2irResult.irModuleFragment)

        val diagnosticsReporterWithContext = KtDiagnosticReporterWithContext(diagnosticsReporter, configuration.languageVersionSettings)
        fir2irResult.irModuleFragment.files.forEach {
            IrDataSerializer(diagnosticsReporterWithContext).visitFile(it, dumper)
        }

        if (diagnosticsReporter.hasErrors) {
            val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY), renderDiagnosticNames)
        }
    }

    override fun analyzeAndSerialize() {
        val analysisResult = analyze() ?: return

        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager.notifyGenerationStarted()
        serializeToDumper(analysisResult)
        performanceManager.notifyGenerationFinished()
    }
}
