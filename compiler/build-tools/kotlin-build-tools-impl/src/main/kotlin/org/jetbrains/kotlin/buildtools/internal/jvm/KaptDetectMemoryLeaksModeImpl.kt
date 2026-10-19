/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.jvm.KaptDetectMemoryLeaksMode

internal enum class KaptDetectMemoryLeaksModeImpl {
    /**
     * Default mode.
     */
    DEFAULT,

    /**
     * Paranoid mode.
     */
    PARANOID,

    /**
     * No detection.
     */
    NONE,
    ;

    override fun toString(): String {
        return name.lowercase()
    }

    internal fun toApi() = KaptDetectMemoryLeaksMode.valueOf(name)

    companion object {
        fun KaptDetectMemoryLeaksMode.toImpl() = KaptDetectMemoryLeaksModeImpl.valueOf(this.name)
    }
}
