/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.data

import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.backend.common.sourceElement
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticReporterWithContext
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrDataSink {
    abstract fun addKeyValue(name: String, value: String)
    abstract fun openGroup(name: String?)
    abstract fun closeGroup()
}

class IrDataSerializer(private val reporter: KtDiagnosticReporterWithContext) : IrElementVisitor<Unit, IrDataSink> {

    override fun visitElement(element: IrElement, data: IrDataSink) {
        error("Should not be here")
    }

    override fun visitFile(declaration: IrFile, data: IrDataSink) {
        declaration.declarations.visitAll(data)
    }

    override fun visitProperty(declaration: IrProperty, data: IrDataSink) {
        if (declaration.origin != IrDeclarationOrigin.DEFINED) return
        when (val inizr = declaration.backingField?.initializer?.expression) {
            is IrConst<*> -> data.addKeyValue(declaration.name.asString(), inizr.value.toString())
            else -> reporter.at(declaration.sourceElement(), declaration.fileParent.path)
                .report(CommonBackendErrors.EVALUATION_ERROR, "unexpected expression:\n${inizr?.dump()}")
        }
    }

    override fun visitClass(declaration: IrClass, data: IrDataSink) {
        if (declaration.origin != IrDeclarationOrigin.DEFINED) return
        data.openGroup(declaration.name.asString())
        declaration.declarations.visitAll(data)
        data.closeGroup()
    }

    override fun visitFunction(declaration: IrFunction, data: IrDataSink) {
        if (declaration.origin != IrDeclarationOrigin.DEFINED) return
        super.visitFunction(declaration, data)
    }

    override fun visitConstructor(declaration: IrConstructor, data: IrDataSink) {
    }

    private fun List<IrDeclaration>.visitAll(data: IrDataSink) {
        forEach {
            it.accept(this@IrDataSerializer, data)
        }
    }
}
