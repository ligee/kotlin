/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.data

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName

internal class IrDataBackendContext(
    override val builtIns: KotlinBuiltIns,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    override val configuration: CompilerConfiguration, // TODO: remove configuration from backend context
) : CommonBackendContext {

    override val irFactory: IrFactory = symbolTable.irFactory

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)

    override val sharedVariablesManager: SharedVariablesManager
        get() = TODO("Not yet implemented")

    override val internalPackageFqn: FqName
        get() = TODO("Not yet implemented")

    override var inVerbosePhase: Boolean = false

    override fun log(message: () -> String) {}

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {}

    override val ir = object : Ir<IrDataBackendContext>(this) {
        override val symbols = object : Symbols(irBuiltIns, symbolTable) {
            override val throwNullPointerException: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val throwTypeCastException: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val throwKotlinNothingValueException: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val stringBuilder: IrClassSymbol
                get() = TODO("Not yet implemented")
            override val defaultConstructorMarker: IrClassSymbol
                get() = TODO("Not yet implemented")
            override val coroutineImpl: IrClassSymbol
                get() = TODO("Not yet implemented")
            override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val getContinuation: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val continuationClass: IrClassSymbol
                get() = TODO("Not yet implemented")
            override val coroutineContextGetter: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val coroutineGetContext: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val returnIfSuspended: IrSimpleFunctionSymbol
                get() = TODO("Not yet implemented")
            override val functionAdapter: IrClassSymbol
                get() = TODO("Not yet implemented")

        }
    }

    override val scriptMode: Boolean = false

    override val mapping: Mapping = DefaultMapping()
}