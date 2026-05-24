package ru.kayron.cargo

import kotlin.reflect.KClass

class CargoModule {

    internal val registrations =
        mutableListOf<(CargoContainer) -> Unit>()

    fun <T : Any> singleton(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        registrations += {
            it.addSingleton(
                type,
                qualifier,
                factory
            )
        }
    }

    inline fun <reified T : Any> singleton(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        singleton(
            T::class,
            qualifier,
            factory
        )
    }

    fun <T : Any> scoped(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        registrations += {
            it.addScoped(
                type,
                qualifier,
                factory
            )
        }
    }

    inline fun <reified T : Any> scoped(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        scoped(
            T::class,
            qualifier,
            factory
        )
    }

    fun <T : Any> transient(
        type: KClass<T>,
        qualifier: String? = null,
        factory: CargoContainer.() -> T
    ) {

        registrations += {
            it.addTransient(
                type,
                qualifier,
                factory
            )
        }
    }

    inline fun <reified T : Any> transient(
        qualifier: String? = null,
        noinline factory: CargoContainer.() -> T
    ) {
        transient(
            T::class,
            qualifier,
            factory
        )
    }
}

fun module(
    block: CargoModule.() -> Unit
): CargoModule {

    return CargoModule().apply(block)
}