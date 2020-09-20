import com.soywiz.kds.*
import com.soywiz.korge.scene.*
import kotlin.test.*

class PositionTest {

    @BeforeTest
    fun init() {
        font = debugBmpFont
    }

    @Test
    fun testPositions() {
        initBlocks(1 to Number.ZERO, 2 to Number.ONE, 4 to Number.TWO)
        checkMap(Direction.BOTTOM, intArrayOf(
                -1, -1,  1, -1,
                 2, -1, -1,  4,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        ), intArrayOf(
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                -1, -1, -1, -1,
                 2, -1,  1,  4
        ))

        initBlocks(1 to Number.ZERO, 2 to Number.ONE, 4 to Number.TWO)
        checkMap(Direction.RIGHT, intArrayOf(
                -1, -1,  1, -1,
                2, -1, -1,  4,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        ), intArrayOf(
                -1, -1, -1,  1,
                -1, -1,  2,  4,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        ))

        initBlocks(1 to Number.ZERO, 2 to Number.ZERO, 3 to Number.TWO, 4 to Number.TWO)
        checkMap(Direction.RIGHT, intArrayOf(
                -1,  2,  1, -1,
                 3, -1, -1,  4,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        ), intArrayOf(
                -1, -1, -1,  1,
                -1, -1, -1,  4,
                -1, -1, -1, -1,
                -1, -1, -1, -1
        ))
    }

    private fun initBlocks(vararg values: Pair<Int, Number>) {
        blocks.clear()
        values.forEach { (i, number) ->
            blocks[i] = Block(number)
        }
    }

    private fun checkMap(direction: Direction, oldMapArray: IntArray, newMapArray: IntArray) {
        val oldMap = PositionMap(IntArray2(4, 4, oldMapArray))
        val moves = mutableListOf<Pair<Int, Position>>()
        val merges = mutableListOf<Triple<Int, Int, Position>>()
        val newMap = calculateNewMap(oldMap, direction, moves, merges)
        assertEquals(PositionMap(IntArray2(4, 4, newMapArray)), newMap)
    }
}