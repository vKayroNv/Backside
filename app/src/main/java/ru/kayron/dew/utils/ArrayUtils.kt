package ru.kayron.dew.utils

class ArrayUtils {
    companion object {
        const val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8
        
        fun growCapacity(old: Int, minRequired: Int): Int {
            if (old >= MAX_ARRAY_SIZE) return MAX_ARRAY_SIZE
            var newSize = old + (old shr 1) // x1.5
            if (newSize < minRequired) {
                newSize = minRequired
            }
            
            if (newSize < 0 || newSize > MAX_ARRAY_SIZE) {
                newSize = MAX_ARRAY_SIZE
            }
            return newSize
        }
        
        @JvmName("ensureCapacityInt")
        fun ensureCapacity(array: IntArray, target: Int, grow: ((Int) -> Unit)? = null) : IntArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityBoolean")
        fun ensureCapacity(array: BooleanArray, target: Int, grow: ((Int) -> Unit)? = null) : BooleanArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityByte")
        fun ensureCapacity(array: ByteArray, target: Int, grow: ((Int) -> Unit)? = null) : ByteArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityChar")
        fun ensureCapacity(array: CharArray, target: Int, grow: ((Int) -> Unit)? = null) : CharArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityShort")
        fun ensureCapacity(array: ShortArray, target: Int, grow: ((Int) -> Unit)? = null) : ShortArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityLong")
        fun ensureCapacity(array: LongArray, target: Int, grow: ((Int) -> Unit)? = null) : LongArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityFloat")
        fun ensureCapacity(array: FloatArray, target: Int, grow: ((Int) -> Unit)? = null) : FloatArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityDouble")
        fun ensureCapacity(array: DoubleArray, target: Int, grow: ((Int) -> Unit)? = null) : DoubleArray {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
        
        @JvmName("ensureCapacityNulls")
        fun <T> ensureCapacity(array: Array<T?>, target: Int, grow: ((Int) -> Unit)? = null) : Array<T?> {
            if (target >= array.size) {
                var newSize = growCapacity(array.size, target + 1)
                if (grow != null) grow(newSize)
                return array.copyOf(newSize)
            }
            return array
        }
    }
}