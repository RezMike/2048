import Number.*
import korlibs.image.color.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*

fun Container.block(number: Number) = Block(number).addTo(this)

class Block(val number: Number) : Container() {

    init {
        roundRect(
            size = Size(cellSize, cellSize),
            radius = RectCorners(5.0),
            fill = number.color
        )
        val textColor = when (number) {
            ZERO, ONE -> Colors.BLACK
            else -> Colors.WHITE
        }
        text(number.value.toString(), textSizeFor(number), textColor, font).apply {
            centerBetween(0.0, 0.0, cellSize, cellSize)
        }
    }
}

private fun textSizeFor(number: Number) = when (number) {
    ZERO, ONE, TWO, THREE, FOUR, FIVE -> cellSize / 2
    SIX, SEVEN, EIGHT -> cellSize * 4 / 9
    NINE, TEN, ELEVEN, TWELVE -> cellSize * 2 / 5
    THIRTEEN, FOURTEEN, FIFTEEN -> cellSize * 7 / 20
    SIXTEEN -> cellSize * 3 / 10
}
