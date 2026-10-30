/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrChecker
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Makes sure that all annotations of declarations are [IrAnnotation]s, not plain [IrConstructorCall]s.
 * This includes annotations passed as arguments of other annotations.
 */
object IrAnnotationUsageChecker : IrElementChecker<IrDeclaration>(IrDeclaration::class) {
    override fun check(element: IrDeclaration, context: CheckerContext) {
        element.annotations.forEach { checkAllAnnotationsAreIrAnnotations(root = it, declaration = element, context) }
    }
}

/**
 * Reports every plain [IrConstructorCall] (i.e., not an [IrAnnotation]) found in the [root] subtree,
 * which is expected to contain only annotations and their arguments.
 */
context(checker: IrChecker)
private fun checkAllAnnotationsAreIrAnnotations(root: IrElement, declaration: IrDeclaration, context: CheckerContext) {
    root.acceptVoid(PlainConstructorCallFinder(declaration, checker, context))
}

private class PlainConstructorCallFinder(
    private val declaration: IrDeclaration,
    private val checker: IrChecker,
    private val context: CheckerContext,
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        if (expression !is IrAnnotation) {
            context.error(
                declaration,
                checker,
                "Annotation must be represented by IrAnnotation, not by a plain IrConstructorCall: ${expression.render()}"
            )
        }
        super.visitConstructorCall(expression)
    }
}
