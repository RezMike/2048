import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.vector.roundRect

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

private fun View.centerBetween(x0: Double, y0: Double, x1: Double, y1: Double) {
    position((x1 - x0 - width) / 2, (y1 - y0 - height) / 2)
}
