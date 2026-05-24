package ru.kayron.dew

class GameServiceContainer {
    private val services = mutableMapOf<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getService(type: Class<T>): T? {
        return services[type] as? T
    }

    fun <T> addService(type: Class<T>, provider: T) {
        services[type] = provider as Any
    }

    fun removeService(type: Class<*>) {
        services.remove(type)
    }
}
