package ru.kayron.cargo

import kotlin.reflect.KClass
import kotlin.reflect.KType

sealed class TypeKey {

    abstract val raw: KClass<*>

    data class Simple(
        override val raw: KClass<*>
    ) : TypeKey()

    data class Generic(
        override val raw: KClass<*>,
        val arguments: List<TypeKey>
    ) : TypeKey()

    companion object {

        fun from(type: KType): TypeKey {

            val classifier =
                type.classifier as? KClass<*>
                    ?: error("Unsupported type: $type")

            val arguments =
                type.arguments
                    .mapNotNull { it.type }
                    .map { from(it) }

            return if (arguments.isEmpty()) {
                Simple(classifier)
            } else {
                Generic(classifier, arguments)
            }
        }
    }
}