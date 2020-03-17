import com.soywiz.kds.Array2
import kotlin.random.Random

class Position(val x: Int, val y: Int)

enum class Direction {
    LEFT, RIGHT, TOP, BOTTOM
}

typealias PositionMap = Array2<Int>

fun PositionMap.getOrNull(x: Int, y: Int) = when {
    get(x, y) != -1 -> Position(x, y)
    else -> null
}

fun PositionMap.getNotEmptyPositionFrom(direction: Direction, line: Int): Position? {
    when (direction) {
        Direction.LEFT -> for (i in 0..3) getOrNull(i, line)?.let { return it }
        Direction.RIGHT -> for (i in 3 downTo 0) getOrNull(i, line)?.let { return it }
        Direction.TOP -> for (i in 0..3) getOrNull(line, i)?.let { return it }
        Direction.BOTTOM -> for (i in 3 downTo 0) getOrNull(line, i)?.let { return it }
    }
    return null
}

fun PositionMap.getRandomFreePosition(): Position? {
    val amount = count { it == -1 }
    if (amount == 0) return null
    val chosen = Random.nextInt(amount)
    var current = -1
    forEachIndexed { i, value ->
        if (value == -1) {
            current++
            if (current == chosen) {
                return Position(i / 4, i % 4)
            }
        }
    }
    return null
}
