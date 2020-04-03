import com.soywiz.kds.*
import kotlin.random.*

class Position(val x: Int, val y: Int)

enum class Direction {
    LEFT, RIGHT, TOP, BOTTOM
}

class PositionMap(private val array: IntArray2 = IntArray2(4, 4, -1)) {

    private fun getOrNull(x: Int, y: Int) = if (array.get(x, y) != -1) Position(x, y) else null

    private fun getNumber(x: Int, y: Int) = array.tryGet(x, y)?.let { blocks[it]?.number ?: -1 } ?: -1

    fun getNotEmptyPositionFrom(direction: Direction, line: Int): Position? {
        when (direction) {
            Direction.LEFT -> for (i in 0..3) getOrNull(i, line)?.let { return it }
            Direction.RIGHT -> for (i in 3 downTo 0) getOrNull(i, line)?.let { return it }
            Direction.TOP -> for (i in 0..3) getOrNull(line, i)?.let { return it }
            Direction.BOTTOM -> for (i in 3 downTo 0) getOrNull(line, i)?.let { return it }
        }
        return null
    }

    fun getRandomFreePosition(): Position? {
        val amount = array.count { it == -1 }
        if (amount == 0) return null
        val chosen = Random.nextInt(amount)
        var current = -1
        array.each { x, y, value ->
            if (value == -1) {
                current++
                if (current == chosen) {
                    return Position(x, y)
                }
            }
        }
        return null
    }

    private fun hasAdjacentEqualPosition(x: Int, y: Int) = getNumber(x, y).let {
        it == getNumber(x - 1, y) || it == getNumber(x + 1, y) || it == getNumber(x, y - 1) || it == getNumber(x, y + 1)
    }

    fun hasAvailableMoves(): Boolean {
        array.each { x, y, _ ->
            if (hasAdjacentEqualPosition(x, y)) return true
        }
        return false
    }

    operator fun get(x: Int, y: Int) = array[x, y]

    operator fun set(x: Int, y: Int, value: Int) {
        array[x, y] = value
    }

    fun copy() = PositionMap(array.copy(data = array.data.copyOf()))

    override fun equals(other: Any?): Boolean {
        return (other is PositionMap) && this.array.data.contentEquals(other.array.data)
    }
}
