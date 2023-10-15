/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

class K2DataCompilerArguments : CommonCompilerArguments() {

    companion object {
        @JvmStatic private val serialVersionUID = 0L
    }

    @Argument(value = "-d", valueDescription = "<directory|file>", description = "Destination for generated data files")
    var destination: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(value = "-f", valueDescription = "tree|json", description = "Output format")
    var format: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-classpath",
        shortName = "-cp",
        valueDescription = "<path>",
        description = "Paths where to find library files"
    )
    var classpath: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }
}
