import com.soywiz.kds.Array2
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.input.onKeyDown
import com.soywiz.korge.tween.moveTo
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.interpolation.Easing
import kotlin.random.Random

var cellSize: Double = 0.0
var paddingLeft: Double = 0.0
var paddingTop: Double = 0.0

fun columnX(number: Int) = paddingLeft + 10 + (cellSize + 10) * number
fun rowY(number: Int) = paddingTop + 10 + (cellSize + 10) * number

typealias PositionMap = Array2<Int>

var map = PositionMap(4, 4, -1)
var blocks = mutableMapOf<Int, Block>()
var numbers = mutableMapOf<Int, Number>()
var freeId = 0

fun numberFor(blockId: Int) = blocks[blockId]!!.number

fun deleteBlock(nextId: Int) {
    blocks.remove(nextId)?.removeFromParent()
}

suspend fun main() = Korge(width = 480, height = 640, bgcolor = RGBA(253, 247, 240)) {
    cellSize = root.height / 5
    val fieldSize = 50 + 4 * cellSize
    paddingLeft = (root.height - fieldSize) / 2
    paddingTop = 100.0

    graphics {
        x = paddingLeft
        y = paddingTop
        fill(RGBA(185, 174, 160)) {
            roundRect(0, 0, fieldSize, fieldSize, 5)
        }
        fill(RGBA(206, 192, 178)) {
            for (i in 0..3) {
                for (j in 0..3) {
                    roundRect(10 + (10 + cellSize) * i, 10 + (10 + cellSize) * j, cellSize, cellSize, 5)
                }
            }
        }
    }

    generateBlock()

    onKeyDown {
        when (it.key) {
            Key.LEFT -> moveBlocksTo(Direction.LEFT)
            Key.RIGHT -> moveBlocksTo(Direction.RIGHT)
            Key.UP -> moveBlocksTo(Direction.TOP)
            Key.DOWN -> moveBlocksTo(Direction.BOTTOM)
            //Key.LEFT -> block1.tween(block1::x[columnX[0]], time = 0.2.seconds, easing = Easing.LINEAR)
            //Key.UP -> block1.tween(block1::y[rowY[0]], time = 0.2.seconds, easing = Easing.LINEAR)
            //Key.DOWN -> block1.tween(block1::y[rowY[3]], time = 0.2.seconds, easing = Easing.LINEAR)
            else -> Unit
        }
    }
}

suspend fun Container.moveBlocksTo(direction: Direction) {
    val moves = mutableListOf<Pair<Int, Position>>()
    val newMap = Array2(4, 4, -1)
    var columnNumber = 3
    var newPos: Position
    // TODO: implement different directions
    for (rowNumber in 0..3) {
        var curPos = map.getFirstFreePositionFrom(direction, rowNumber)
        while (curPos != null) {
            newPos = Position(columnNumber--, rowNumber)
            val curId = map[curPos.x, curPos.y]
            map[curPos.x, curPos.y] = -1

            val nextPos = map.getFirstFreePositionFrom(direction, rowNumber)
            val nextId = nextPos?.let { map[it.x, it.y] }
            //two blocks are equal
            if (nextId != null && numberFor(curId) == numberFor(nextId)) {
                //merge these blocks
                map[nextPos.x, nextPos.y] = -1
                val newId = createNewBlock(numberFor(curId).next(), newPos)
                newMap[newPos.x, newPos.y] = newId
                deleteBlock(curId)
                deleteBlock(nextId)
                moves += curId to newPos
                moves += nextId to newPos
            } else {
                //add old block
                newMap[newPos.x, newPos.y] = curId
                moves += curId to newPos
            }
            curPos = map.getFirstFreePositionFrom(direction, rowNumber)
        }
        columnNumber = 3
    }

    if (map != newMap) {
        // TODO: correct animation
        moves.forEach { (id, pos) ->
            blocks[id]?.moveTo(columnX(pos.x), rowY(pos.y), 0.2.seconds, Easing.LINEAR)
        }
        map = newMap
        generateBlock()
    }
}

fun PositionMap.getOrNull(x: Int, y: Int) = when {
    get(x, y) != -1 -> Position(x, y)
    else -> null
}

enum class Direction {
    LEFT, RIGHT, TOP, BOTTOM
}

fun PositionMap.getFirstFreePositionFrom(direction: Direction, line: Int): Position? {
    when (direction) {
        Direction.LEFT -> for (i in 0..3) getOrNull(i, line)?.let { return it }
        Direction.RIGHT -> for (i in 3 downTo 0) getOrNull(i, line)?.let { return it }
        Direction.TOP -> for (i in 0..3) getOrNull(line, i)?.let { return it }
        Direction.BOTTOM -> for (i in 3 downTo 0) getOrNull(line, i)?.let { return it }
    }
    return null
}

fun Container.generateBlock() {
    val position = map.getRandomFreePosition() ?: return
    val number = if (Random.nextDouble() < 0.7) Number.ZERO else Number.ONE
    val newId = createNewBlock(number, position)
    map[position.x, position.y] = newId
}

fun Container.createNewBlock(number: Number, position: Position = Position(0, 0)): Int {
    val id = freeId++
    blocks[id] = block(number).position(columnX(position.x), rowY(position.y))
    numbers[id] = number
    return id
}

private fun PositionMap.getRandomFreePosition(): Position? {
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

fun Container.block(number: Number) = Block(number).addTo(this)

class Block(val number: Number) : Container() {

    init {
        graphics {
            fill(number.color) {
                roundRect(0, 0, cellSize, cellSize, 5)
            }
            val textColor = when (number) {
                Number.ZERO, Number.ONE -> Colors.BLACK
                else -> Colors.WHITE
            }
            text(number.value.toString(), cellSize / 3, textColor).apply {
                centerBetween(0.0, 0.0, cellSize, cellSize)
                filtering = false
            }
        }
    }
}

class Position(val x: Int, val y: Int)

private fun View.centerBetween(x0: Double, y0: Double, x1: Double, y1: Double) {
    position((x1 - x0 - width) / 2, (y1 - y0 - height) / 2)
}

enum class Number(val value: Int, val color: RGBA = Colors.WHITE) {
    ZERO(1),
    ONE(2),
    TWO(4),
    THREE(8),
    FOUR(16),
    FIVE(32),
    SIX(64),
    SEVEN(128),
    EIGHT(256),
    NINE(512),
    TEN(1024),
    ELEVEN(2048),
    TWELVE(4096),
}

private fun Number.next() = Number.values().let { it[(this.ordinal + 1) % it.size] }
