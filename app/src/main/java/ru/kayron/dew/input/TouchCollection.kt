package ru.kayron.dew.input

class TouchCollection : ArrayList<TouchLocation>() {
    fun isAnyTouch(): Boolean = isNotEmpty()

    fun findById(id: Int): TouchLocation? = find { it.id == id }

    fun clearAll() {
        for (i in indices) {
            if (this[i].state != TouchLocation.TouchLocationState.Released) {
                this[i] = this[i].copy(state = TouchLocation.TouchLocationState.Released)
            }
        }
    }
}
