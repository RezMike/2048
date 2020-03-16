import com.soywiz.kds.Array2
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.input.onKeyDown
import com.soywiz.korge.tween.moveTo
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.position
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.vector.roundRect
import com.soywiz.korma.interpolation.Easing
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.random.Random

var font: BitmapFont? = null
var cellSize: Double = 0.0
var paddingLeft: Double = 0.0
var paddingTop: Double = 0.0

fun columnX(number: Int) = paddingLeft + 10 + (cellSize + 10) * number
fun rowY(number: Int) = paddingTop + 10 + (cellSize + 10) * number

var map = PositionMap(4, 4, -1)
val blocks = mutableMapOf<Int, Block>()

fun numberFor(blockId: Int) = blocks[blockId]!!.number
fun deleteBlock(nextId: Int) = blocks.remove(nextId)?.removeFromParent()

suspend fun main() = Korge(width = 480, height = 640, bgcolor = RGBA(253, 247, 240)) {
    font = resourcesVfs["clear_sans.fnt"].readBitmapFont()

    cellSize = root.width / 5
    val fieldSize = 50 + 4 * cellSize
    paddingLeft = (root.width - fieldSize) / 2
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

    Number.values().forEachIndexed { i, num ->
        createNewBlock(num, Position(i % 4, i / 4))
    }
    //generateBlock()

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

fun Container.generateBlock() {
    val position = map.getRandomFreePosition() ?: return
    val number = if (Random.nextDouble() < 0.7) Number.ZERO else Number.ONE
    val newId = createNewBlock(number, position)
    map[position.x, position.y] = newId
}

var freeId = 0

fun Container.createNewBlock(number: Number, position: Position = Position(0, 0)): Int {
    val id = freeId++
    blocks[id] = block(number).position(columnX(position.x), rowY(position.y))
    return id
}
