import com.soywiz.kds.iterators.*

class History(from: String?, private val onUpdate: (History) -> Unit) {

    class Element(val numberIds: IntArray, val score: Int)

    private val history = mutableListOf<Element>()
    val currentElement: Element get() = history.last()

    init {
        from?.split(';')?.fastForEach {
            val element = elementFromString(it)
            history.add(element)
        }
    }

    private fun elementFromString(string: String): Element {
        val numbers = string.split(',').map { it.toInt() }
        if (numbers.size != 17) throw IllegalArgumentException("Incorrect history")
        return Element(IntArray(16) { numbers[it] }, numbers[16])
    }

    fun add(numberIds: IntArray, score: Int) {
        history.add(Element(numberIds, score))
        onUpdate(this)
    }

    fun undo(): Element {
        if (history.size > 1) {
            history.removeAt(history.size - 1)
            onUpdate(this)
        }
        return history.last()
    }

    fun clear() {
        history.clear()
        onUpdate(this)
    }

    fun isEmpty() = history.isEmpty()

    override fun toString(): String {
        return history.joinToString(";") {
            it.numberIds.joinToString(",") + "," + it.score
        }
    }
}