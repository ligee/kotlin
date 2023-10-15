/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.data

import org.jetbrains.kotlin.backend.common.lower.ConstEvaluationLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

private val validateIrBeforeLowering = makeCustomPhase(
    ::validateIr,
    name = "ValidateIrBeforeLowering",
    description = "Validate IR before lowering"
)

private val constEvaluationPhase = makeIrModulePhase<IrDataBackendContext>(
    ::ConstEvaluationLowering,
    name = "ConstEvaluationLowering",
    description = "Evaluate functions that are marked as `IntrinsicConstEvaluation`"
)

internal fun validateIr(context: IrDataBackendContext, module: IrModuleFragment) {
    validationCallback(context, module, checkProperties = true)
}

internal val dataLoweringPhases = SameTypeNamedCompilerPhase(
    name = "IrDataLowering",
    description = "IR lowering",
    nlevels = 1,
    actions = setOf(defaultDumper, validationAction),
    lower = validateIrBeforeLowering then
            constEvaluationPhase
)
