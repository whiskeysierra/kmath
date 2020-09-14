package scientifik.kmath.structures

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.and

/**
 * Represents flags to supply additional info about values of buffer.
 *
 * @property mask bit mask value of this flag.
 */
enum class ValueFlag(val mask: Byte) {
    /**
     * Reports the value is NaN.
     */
    NAN(0b0000_0001),

    /**
     * Reports the value doesn't present in the buffer (when the type of value doesn't support `null`).
     */
    MISSING(0b0000_0010),

    /**
     * Reports the value is negative infinity.
     */
    NEGATIVE_INFINITY(0b0000_0100),

    /**
     * Reports the value is positive infinity
     */
    POSITIVE_INFINITY(0b0000_1000)
}

/**
 * A buffer with flagged values.
 */
interface FlaggedBuffer<T> : Buffer<T> {
    fun getFlag(index: Int): Byte
}

/**
 * The value is valid if all flags are down
 */
fun FlaggedBuffer<*>.isValid(index: Int): Boolean = getFlag(index) != 0.toByte()

fun FlaggedBuffer<*>.hasFlag(index: Int, flag: ValueFlag): Boolean = (getFlag(index) and flag.mask) != 0.toByte()

fun FlaggedBuffer<*>.isMissing(index: Int): Boolean = hasFlag(index, ValueFlag.MISSING)

/**
 * A real buffer which supports flags for each value like NaN or Missing
 */
class FlaggedRealBuffer(val values: DoubleArray, val flags: ByteArray) : FlaggedBuffer<Double?>, Buffer<Double?> {
    init {
        require(values.size == flags.size) { "Values and flags must have the same dimensions" }
    }

    override fun getFlag(index: Int): Byte = flags[index]

    override val size: Int get() = values.size

    override operator fun get(index: Int): Double? = if (isValid(index)) values[index] else null

    override operator fun iterator(): Iterator<Double?> = values.indices.asSequence().map {
        if (isValid(it)) values[it] else null
    }.iterator()
}

inline fun FlaggedRealBuffer.forEachValid(block: (Double) -> Unit) {
    contract { callsInPlace(block) }

    indices
        .asSequence()
        .filter(::isValid)
        .forEach { block(values[it]) }
}
