import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import kotlin.collections.set
import kotlin.random.*

var font: BitmapFont? = null
var fieldSize: Double = 0.0
var cellSize: Double = 0.0
var paddingLeft: Double = 0.0
var paddingTop: Double = 0.0

fun columnX(number: Int) = paddingLeft + 10 + (cellSize + 10) * number
fun rowY(number: Int) = paddingTop + 10 + (cellSize + 10) * number

var map = PositionMap()
val blocks = mutableMapOf<Int, Block>()

fun numberFor(blockId: Int) = blocks[blockId]!!.number
fun deleteBlock(blockId: Int) = blocks.remove(blockId)!!.removeFromParent()

var animationRunning = false
var isGameOver = false

suspend fun main() = Korge(width = 480, height = 640, bgcolor = RGBA(253, 247, 240)) {
    font = resourcesVfs["clear_sans.fnt"].readBitmapFont()

    cellSize = root.width / 5
    fieldSize = 50 + 4 * cellSize
    paddingLeft = (root.width - fieldSize) / 2
    paddingTop = 100.0

    graphics {
        position(paddingLeft, paddingTop)
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
            else -> Unit
        }
    }
}

fun Stage.moveBlocksTo(direction: Direction) {
    if (animationRunning) return
    if (!map.hasAvailableMoves()) {
        if (!isGameOver) showGameOver()
        isGameOver = true
        return
    }
    val moves = mutableListOf<Pair<Int, Position>>()
    val merges = mutableListOf<Triple<Int, Int, Position>>()
    val oldMap = map.copy()
    val newMap = PositionMap()
    val startIndex = when (direction) {
        Direction.LEFT, Direction.TOP -> 0
        Direction.RIGHT, Direction.BOTTOM -> 3
    }
    var columnRow = startIndex

    fun newPosition(line: Int) = when (direction) {
        Direction.LEFT -> Position(columnRow++, line)
        Direction.RIGHT -> Position(columnRow--, line)
        Direction.TOP -> Position(line, columnRow++)
        Direction.BOTTOM -> Position(line, columnRow--)
    }

    for (line in 0..3) {
        var curPos = map.getNotEmptyPositionFrom(direction, line)
        columnRow = startIndex
        while (curPos != null) {
            val newPos = newPosition(line)
            val curId = map[curPos.x, curPos.y]
            map[curPos.x, curPos.y] = -1

            val nextPos = map.getNotEmptyPositionFrom(direction, line)
            val nextId = nextPos?.let { map[it.x, it.y] }
            //two blocks are equal
            if (nextId != null && numberFor(curId) == numberFor(nextId)) {
                //merge these blocks
                map[nextPos.x, nextPos.y] = -1
                newMap[newPos.x, newPos.y] = curId
                merges += Triple(curId, nextId, newPos)
            } else {
                //add old block
                newMap[newPos.x, newPos.y] = curId
                moves += Pair(curId, newPos)
            }
            curPos = map.getNotEmptyPositionFrom(direction, line)
        }
    }

    if (oldMap != newMap) launchImmediately {
        animationRunning = true
        animateSequence {
            parallel {
                moves.forEach { (id, pos) ->
                    blocks[id]!!.moveTo(columnX(pos.x), rowY(pos.y), 0.15.seconds, Easing.LINEAR)
                }
                merges.forEach { (id1, id2, pos) ->
                    sequence {
                        parallel {
                            blocks[id1]!!.moveTo(columnX(pos.x), rowY(pos.y), 0.15.seconds, Easing.LINEAR)
                            blocks[id2]!!.moveTo(columnX(pos.x), rowY(pos.y), 0.15.seconds, Easing.LINEAR)
                        }
                        block {
                            val nextNumber = numberFor(id1).next()
                            deleteBlock(id1)
                            deleteBlock(id2)
                            createNewBlockWithId(id1, nextNumber, pos)
                        }
                        sequenceLazy {
                            animateScale(blocks[id1]!!)
                        }
                    }
                }
            }
            block {
                map = newMap
                generateBlock()
                animationRunning = false
            }
        }
    } else {
        map = newMap
    }
}

fun Animator.animateScale(block: Block) {
    val x = block.x
    val y = block.y
    val scale = block.scale
    tween(
            block::x[x - 4],
            block::y[y - 4],
            block::scale[scale + 0.1],
            time = 0.1.seconds,
            easing = Easing.LINEAR
    )
    tween(
            block::x[x],
            block::y[y],
            block::scale[scale],
            time = 0.1.seconds,
            easing = Easing.LINEAR
    )
}

fun Container.showGameOver() = container {
    position(paddingLeft, paddingTop)

    graphics {
        fill(Colors.WHITE, 0.2) {
            roundRect(0, 0, fieldSize, fieldSize, 5.0, 5.0)
        }
    }
    text("Game Over", 32.0, Colors.BLACK) {
        filtering = false
        centerBetween(0, 0, fieldSize, fieldSize)
        y -= 60
    }
    val textSkin = DefaultTextSkin.copy(backColor = Colors.TRANSPARENT_WHITE)
    uiText("Try again", skin = textSkin) {
        centerBetween(0, 0, fieldSize, fieldSize)
        y += 20
        onClick {
            this@showGameOver.restart()
            this@container.removeFromParent()
        }
    }
}

fun Container.restart() {
    isGameOver = false
    map = PositionMap()
    blocks.values.forEach { it.removeFromParent() }
    blocks.clear()
    generateBlock()
}

fun Container.generateBlock() {
    val position = map.getRandomFreePosition() ?: return
    val number = if (Random.nextDouble() < 0.9) Number.ZERO else Number.ONE
    val newId = createNewBlock(number, position)
    map[position.x, position.y] = newId
}

var freeId = 0

fun Container.createNewBlockWithId(id: Int, number: Number, position: Position) {
    blocks[id] = block(number).position(columnX(position.x), rowY(position.y))
}

fun Container.createNewBlock(number: Number, position: Position): Int {
    val id = freeId++
    createNewBlockWithId(id, number, position)
    return id
}
