package scientifik.kmath.asm

import scientifik.kmath.asm.internal.AsmBuilder
import scientifik.kmath.asm.internal.MstType
import scientifik.kmath.asm.internal.buildAlgebraOperationCall
import scientifik.kmath.asm.internal.buildName
import scientifik.kmath.ast.MST
import scientifik.kmath.ast.MstExpression
import scientifik.kmath.expressions.Expression
import scientifik.kmath.operations.Algebra
import kotlin.reflect.KClass

/**
 * Compiles given MST to an Expression using AST compiler.
 *
 * @param type the target type.
 * @param algebra the target algebra.
 * @return the compiled expression.
 * @author Alexander Nozik
 */
public fun <T : Any> MST.compileWith(type: KClass<T>, algebra: Algebra<T>): Expression<T> {
    fun AsmBuilder<T>.visit(node: MST) {
        when (node) {
            is MST.Symbolic -> {
                val symbol = try {
                    algebra.symbol(node.value)
                } catch (ignored: Throwable) {
                    null
                }

                if (symbol != null)
                    loadTConstant(symbol)
                else
                    loadVariable(node.value)
            }

            is MST.Numeric -> loadNumeric(node.value)

            is MST.Unary -> buildAlgebraOperationCall(
                context = algebra,
                name = node.operation,
                fallbackMethodName = "unaryOperation",
                parameterTypes = arrayOf(MstType.fromMst(node.value))
            ) { visit(node.value) }

            is MST.Binary -> buildAlgebraOperationCall(
                context = algebra,
                name = node.operation,
                fallbackMethodName = "binaryOperation",
                parameterTypes = arrayOf(MstType.fromMst(node.left), MstType.fromMst(node.right))
            ) {
                visit(node.left)
                visit(node.right)
            }
        }
    }

    return AsmBuilder(type, algebra, buildName(this)) { visit(this@compileWith) }.getInstance()
}

/**
 * Compiles an [MST] to ASM using given algebra.
 *
 * @author Alexander Nozik.
 */
public inline fun <reified T : Any> Algebra<T>.expression(mst: MST): Expression<T> = mst.compileWith(T::class, this)

/**
 * Optimizes performance of an [MstExpression] using ASM codegen.
 *
 * @author Alexander Nozik.
 */
public inline fun <reified T : Any> MstExpression<T>.compile(): Expression<T> = mst.compileWith(T::class, algebra)
