/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.template

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspendableExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

class TemplateIrGenerationExtension(
  private val messageCollector: MessageCollector,
  private val string: String,
  private val file: String,
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val compiledPersistableContinuationClass =
      pluginContext.referenceClass(FqName(CompiledPersistableContinuation::class.qualifiedName!!))!!
    val persistedFieldClass =
      pluginContext.referenceClass(FqName(PersistedField::class.qualifiedName!!))!!
    val persistencePointClass =
      pluginContext.referenceClass(FqName(PersistencePoint::class.qualifiedName!!))!!
    moduleFragment.accept(object : IrElementTransformerVoid() {
      override fun visitSuspensionPoint(expression: IrSuspensionPoint): IrExpression {
        return super.visitSuspensionPoint(expression)
      }

      override fun visitSuspendableExpression(expression: IrSuspendableExpression): IrExpression {
        return super.visitSuspendableExpression(expression)
      }

      override fun visitFunction(declaration: IrFunction): IrStatement {
        for (functionAnnotation in declaration.annotations) {
          if (functionAnnotation.type.classFqName?.asString() == PersistableContinuation::class.qualifiedName!!) {
//            val persistencePointsLabels = LinkedHashMap<String, Int>()
//            val persistencePointsNames = LinkedHashSet<String>()
            val statement = object : IrElementTransformerVoid() {
              var labels = 0
              override fun visitSuspensionPoint(expression: IrSuspensionPoint): IrExpression {
                labels++
                return super.visitSuspensionPoint(expression)
              }

              override fun visitSuspendableExpression(expression: IrSuspendableExpression): IrExpression {
                labels++
                return super.visitSuspendableExpression(expression)
              }

              override fun visitVariable(declaration: IrVariable): IrStatement {
                for (variableAnnotation in declaration.annotations) {
                  if (variableAnnotation.type == persistedFieldClass.defaultType) {
                    declaration.name
                  }
                  if (variableAnnotation.type == persistencePointClass.defaultType) {
                    val value = object : IrElementTransformerVoid() {
                      var label = -1
                      override fun visitSuspendableExpression(expression: IrSuspendableExpression): IrExpression {
                        if (label != -1) {
                          throw RuntimeException("persistence point has multiple suspension expressions")
                        }
                        label = labels++
                        return super.visitSuspendableExpression(expression)
                      }
                    }
                    value.visitVariable(declaration)
//                    if (value.label == -1) {
//                      throw RuntimeException("persistence point has no suspension expressions")
//                    }
//                    variableAnnotation.getValueArgument(0) as
//                    persistencePointsLabels.put(variableAnnotation.getValueArgument(0))
                  }
                }
                return super.visitVariable(declaration)
              }
            }.visitFunction(declaration)
            val compiledPersistableContinuation = IrConstructorCallImpl(
              startOffset = functionAnnotation.startOffset,
              endOffset = functionAnnotation.endOffset,
              constructorTypeArgumentsCount = 0,
              symbol = compiledPersistableContinuationClass.constructors.single(),
              type = compiledPersistableContinuationClass.defaultType,
              typeArgumentsCount = 0,
              valueArgumentsCount = 1,
            )
            compiledPersistableContinuation.putValueArgument(
              0, IrVarargImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                pluginContext.irBuiltIns.intArray.defaultType, pluginContext.irBuiltIns.intType, emptyList()
              )
            )
            declaration.annotations = declaration.annotations + compiledPersistableContinuation
            return statement
          }
        }
        return super.visitFunction(declaration)
      }
    }, null)
    messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'string' = $string")
    messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'file' = $file")
  }
}
