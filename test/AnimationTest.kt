import com.soywiz.klock.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.interpolation.*
import kotlin.test.*

class AnimationTest : ViewsForTesting() {

    @Test
    fun complexTest() = viewsTest {
        val views = mutableMapOf(
                0 to solidRect(100, 100, Colors.RED).position(100, 100),
                1 to solidRect(100, 100, Colors.RED).position(100, 100),
                2 to solidRect(100, 100, Colors.RED).position(100, 100),
                3 to solidRect(100, 100, Colors.RED).position(100, 100),
                4 to solidRect(100, 100, Colors.RED).position(100, 100),
                5 to solidRect(100, 100, Colors.RED).position(100, 100)
        )
        val moves = arrayOf(0, 1)
        val merges = arrayOf(2 to 3, 4 to 5)
        val log = arrayListOf<String>()
        animateSequence {
            block { log += views.map { it.value.pos.toString() } }
            parallel {
                moves.forEach { id ->
                    views[id]!!.moveBy(20.0 * id, 30.0 * id, 0.1.seconds, Easing.LINEAR)
                }
                merges.forEach { (id1, id2) ->
                    sequence {
                        val newId = id1 * 3
                        parallel {
                            views[id1]!!.moveTo(id1 + 20, id1 + 20, 0.1.seconds, Easing.LINEAR)
                            views[id2]!!.moveTo(id2 + 20, id2 + 20, 0.1.seconds, Easing.LINEAR)
                        }
                        block {
                            views += newId to solidRect(100, 100, Colors.RED).position(10, 10)
                        }
                        sequenceLazy {
                            val view = views[newId]!!
                            val x = view.x
                            tween(view::x[x + 10], time = 0.1.seconds, easing = Easing.LINEAR)
                            tween(view::x[x], time = 0.1.seconds, easing = Easing.LINEAR)
                        }
                        sequence {
                            tween({ views[6]!!::x[views[6]!!.x + 10] }, time = 0.1.seconds, easing = Easing.LINEAR)
                            tween({ views[6]!!::x[views[6]!!.x - 10] }, time = 0.1.seconds, easing = Easing.LINEAR)
                        }
                    }
                }
            }
            block { log += views.map { it.key to it.value }.sortedBy { it.first }.map { it.second.pos.toString() } }
        }
        val start = "(100, 100), (100, 100), (100, 100), (100, 100), (100, 100), (100, 100)"
        val finish = "(100, 100), (120, 130), (22, 22), (23, 23), (24, 24), (25, 25), (10, 10), (10, 10)"
        assertEquals("[$start, $finish]", log.toString())
    }
}