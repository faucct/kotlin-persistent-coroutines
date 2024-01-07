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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.cast

class TemplateIrGenerationExtension(
  private val messageCollector: MessageCollector,
  private val string: String,
  private val file: String,
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val compiledPersistableContinuationClass =
      pluginContext.referenceClass(FqName(CompiledPersistableContinuation::class.qualifiedName!!))!!
    val compiledPersistableContinuationPersistencePointClass =
      pluginContext.referenceClass(FqName(CompiledPersistableContinuation.PersistencePoint::class.qualifiedName!!))!!
    val compiledPersistableContinuationVariableClass =
      pluginContext.referenceClass(FqName(CompiledPersistableContinuation.Variable::class.qualifiedName!!))!!
    val persistedFieldClass =
      pluginContext.referenceClass(FqName(PersistedField::class.qualifiedName!!))!!
    val persistencePointClass =
      pluginContext.referenceClass(FqName(PersistencePoint::class.qualifiedName!!))!!
    moduleFragment.accept(object : IrElementTransformerVoid() {
      override fun visitFile(file: IrFile): IrFile {
        file.accept(object : IrElementTransformerVoid() {
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
                val persistencePoints = arrayListOf<IrVarargElement>()
                val variables = arrayListOf<IrVarargElement>()
                val statement = object : IrElementTransformerVoid() {
                  override fun visitCall(expression: IrCall): IrExpression {
//                    if (expression.isSuspend) {
//                      throw RuntimeException()
//                    }
                    return super.visitCall(expression)
                  }

                  override fun visitVariable(declaration: IrVariable): IrStatement {
                    for (variableAnnotation in declaration.annotations) {
                      if (variableAnnotation.type == persistedFieldClass.defaultType) {
                        val variable = IrConstructorCallImpl(
                          startOffset = functionAnnotation.startOffset,
                          endOffset = functionAnnotation.endOffset,
                          constructorTypeArgumentsCount = 0,
                          symbol = compiledPersistableContinuationVariableClass.constructors.single(),
                          type = compiledPersistableContinuationVariableClass.defaultType,
                          typeArgumentsCount = 0,
                          valueArgumentsCount = 3,
                        )
                        var argument = 0
                        variable.putValueArgument(
                          argument++, IrConstImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.stringType, IrConstKind.String,
                            declaration.name.asString(),
                          )
                        )
                        variable.putValueArgument(
                          argument++, org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.anyClass.defaultType,
                            declaration.type.cast<IrSimpleType>().classifier, declaration.type,
                          )
                        )
                        variable.putValueArgument(
                          argument++, IrConstImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.stringType, IrConstKind.String,
                            declaration.name.asString(),
                          )
                        )
                        variables.add(variable)
                      }
                      if (variableAnnotation.type == persistencePointClass.defaultType) {
                        val irCall = declaration.initializer.cast<IrCall>()
                        assert(irCall.isSuspend)
                        val persistencePoint = IrConstructorCallImpl(
                          startOffset = functionAnnotation.startOffset,
                          endOffset = functionAnnotation.endOffset,
                          constructorTypeArgumentsCount = 0,
                          symbol = compiledPersistableContinuationPersistencePointClass.constructors.single(),
                          type = compiledPersistableContinuationPersistencePointClass.defaultType,
                          typeArgumentsCount = 0,
                          valueArgumentsCount = 2,
                        )
                        var argument = 0
                        persistencePoint.putValueArgument(
                          argument++, IrConstImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.intType, IrConstKind.Int,
                            file.fileEntry.getLineNumber(irCall.startOffset),
                          )
                        )
                        persistencePoint.putValueArgument(argument++, variableAnnotation.getValueArgument(0))
                        persistencePoints.add(persistencePoint)
                      }
                    }
                    return super.visitVariable(declaration)
                  }
                }.visitFunction(declaration)
                var argument = 0
                val compiledPersistableContinuation = IrConstructorCallImpl(
                  startOffset = functionAnnotation.startOffset,
                  endOffset = functionAnnotation.endOffset,
                  constructorTypeArgumentsCount = 0,
                  symbol = compiledPersistableContinuationClass.constructors.single(),
                  type = compiledPersistableContinuationClass.defaultType,
                  typeArgumentsCount = 0,
                  valueArgumentsCount = 2,
                )
                compiledPersistableContinuation.putValueArgument(
                  argument++, IrVarargImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    compiledPersistableContinuationPersistencePointClass.defaultType,
                    pluginContext.irBuiltIns.intType, persistencePoints
                  )
                )
                compiledPersistableContinuation.putValueArgument(
                  argument++, IrVarargImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    compiledPersistableContinuationVariableClass.defaultType,
                    pluginContext.irBuiltIns.intType, variables,
                  )
                )
                declaration.annotations = declaration.annotations + compiledPersistableContinuation
                return statement
              }
            }
            return super.visitFunction(declaration)
          }
        }, null)
        return super.visitFile(file)
      }
    }, null)
    messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'string' = $string")
    messageCollector.report(CompilerMessageSeverity.INFO, "Argument 'file' = $file")
  }
}
