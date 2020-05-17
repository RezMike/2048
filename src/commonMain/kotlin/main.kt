import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.html.*
import com.soywiz.korge.input.*
import com.soywiz.korge.service.storage.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
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

val score = ObservableProperty(0)
val best = ObservableProperty(NativeStorage.getOrNull("best")?.toInt() ?: 0)

var freeId = 0
var animationRunning = false
var isGameOver = false

suspend fun main() = Korge(width = 480, height = 640, bgcolor = RGBA(253, 247, 240)) {
    font = resourcesVfs["clear_sans.fnt"].readBitmapFont()

    score.observe {
        if (it > best.value) best.update(it)
    }
    best.observe {
        NativeStorage["best"] = it.toString()
    }

    cellSize = views.virtualWidth / 5.0
    fieldSize = 50 + 4 * cellSize
    paddingLeft = (views.virtualWidth - fieldSize) / 2
    paddingTop = 150.0

    val bgField = roundRect(fieldSize, fieldSize, 5, color = RGBA(185, 174, 160)) {
        position(paddingLeft, paddingTop)
    }
    graphics {
        position(paddingLeft, paddingTop)
        fill(RGBA(206, 192, 178)) {
            for (i in 0..3) {
                for (j in 0..3) {
                    roundRect(10 + (10 + cellSize) * i, 10 + (10 + cellSize) * j, cellSize, cellSize, 5)
                }
            }
        }
    }

    val bg2048 = roundRect(cellSize, cellSize, 5, color = RGBA(237, 196, 3)) {
        position(paddingLeft, 30)
    }
    text("2048", cellSize * 0.5, Colors.WHITE, font!!).centerOn(bg2048)

    val bgBest = roundRect(cellSize * 1.5, cellSize * 0.8, 5, color = RGBA(187, 174, 158)) {
        alignRightToRightOf(bgField)
        alignTopToTopOf(bg2048)
    }
    text("BEST", cellSize * 0.25, RGBA(239, 226, 210), font!!) {
        centerXOn(bgBest)
        alignTopToTopOf(bgBest, 5)
    }
    text(best.value.toString(), cellSize * 0.5, Colors.WHITE, font!!) {
        setTextBounds(Rectangle(0.0, 0.0, bgBest.width, cellSize - 24.0))
        format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
        alignTopToTopOf(bgBest, 12)
        centerXOn(bgBest)
        best {
            text = it.toString()
        }
    }

    val bgScore = roundRect(cellSize * 1.5, cellSize * 0.8, 5, color = RGBA(187, 174, 158)) {
        alignRightToLeftOf(bgBest, 24)
        alignTopToTopOf(bgBest)
    }
    text("SCORE", cellSize * 0.25, RGBA(239, 226, 210), font!!) {
        centerXOn(bgScore)
        alignTopToTopOf(bgScore, 5)
    }
    text(score.value.toString(), cellSize * 0.5, Colors.WHITE, font!!) {
        setTextBounds(Rectangle(0.0, 0.0, bgScore.width, cellSize - 24.0))
        format = format.copy(align = Html.Alignment.MIDDLE_CENTER)
        centerXOn(bgScore)
        alignTopToTopOf(bgScore, 12)
        score {
            text = it.toString()
        }
    }

    generateBlock()

    onSwipe(20.0) {
        when (it.direction) {
            SwipeDirection.LEFT -> moveBlocksTo(Direction.LEFT)
            SwipeDirection.RIGHT -> moveBlocksTo(Direction.RIGHT)
            SwipeDirection.TOP -> moveBlocksTo(Direction.TOP)
            SwipeDirection.BOTTOM -> moveBlocksTo(Direction.BOTTOM)
        }
    }

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
        if (!isGameOver) {
            isGameOver = true
            showGameOver {
                isGameOver = false
                restart()
            }
        }
        return
    }

    val moves = mutableListOf<Pair<Int, Position>>()
    val merges = mutableListOf<Triple<Int, Int, Position>>()

    val newMap = calculateNewMap(map.copy(), direction, moves, merges)

    if (map != newMap) {
        animationRunning = true
        showAnimation(moves, merges) {
            map = newMap
            generateBlock()
            animationRunning = false
            var points = 0
            merges.forEach {
                points += numberFor(it.first).value
            }
            score.update(score.value + points)
        }
    } else {
        map = newMap
    }
}

fun calculateNewMap(
        map: PositionMap,
        direction: Direction,
        moves: MutableList<Pair<Int, Position>>,
        merges: MutableList<Triple<Int, Int, Position>>
): PositionMap {
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
    return newMap
}

fun Stage.showAnimation(
        moves: List<Pair<Int, Position>>,
        merges: List<Triple<Int, Int, Position>>,
        onEnd: () -> Unit
) = launchImmediately {
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
            onEnd()
        }
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

fun Container.showGameOver(onRestart: () -> Unit) = container {
    val format = TextFormat(RGBA(0, 0, 0), 40, Html.FontFace.Bitmap(font!!))
    val skin = TextSkin(
            normal = format,
            over = format.copy(RGBA(90, 90, 90)),
            down = format.copy(RGBA(120, 120, 120))
    )

    fun restart() {
        this@container.removeFromParent()
        onRestart()
    }

    position(paddingLeft, paddingTop)

    graphics {
        fill(Colors.WHITE, 0.2) {
            roundRect(0, 0, fieldSize, fieldSize, 5, 5)
        }
    }
    text("Game Over", 60.0, Colors.BLACK, font!!) {
        centerBetween(0, 0, fieldSize, fieldSize)
        y -= 60
    }
    uiText("Try again", 120, 35, skin) {
        centerBetween(0, 0, fieldSize, fieldSize)
        y += 20
        onClick { restart() }
    }
    onKeyDown {
        when (it.key) {
            Key.ENTER, Key.SPACE -> restart()
            else -> Unit
        }
    }
}

fun Container.restart() {
    map = PositionMap()
    blocks.values.forEach { it.removeFromParent() }
    blocks.clear()
    score.update(0)
    generateBlock()
}

fun Container.generateBlock() {
    val position = map.getRandomFreePosition() ?: return
    val number = if (Random.nextDouble() < 0.9) Number.ZERO else Number.ONE
    val newId = createNewBlock(number, position)
    map[position.x, position.y] = newId
}

fun Container.createNewBlock(number: Number, position: Position): Int {
    val id = freeId++
    createNewBlockWithId(id, number, position)
    return id
}

fun Container.createNewBlockWithId(id: Int, number: Number, position: Position) {
    blocks[id] = block(number).position(columnX(position.x), rowY(position.y))
}
