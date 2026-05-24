package ru.kayron.dew.managers

import ru.kayron.dew.ecs.Entity
import ru.kayron.dew.ecs.Component
import kotlin.reflect.KClass

class ComponentManager {
    private val storages = mutableMapOf<KClass<out Component>, Component>()
    
    fun <T : Component> get(type: KClass<T>): T = getOrThrow(type) as T
    
    inline fun <reified T : Component> get(): T = get(T::class)

    fun getEntitiesWith(
        type: KClass<out Component>,
        vararg otherTypes: KClass<out Component>
    ): List<Entity> {
        val queryStorages = (listOf(type) + otherTypes)
            .map { getOrThrow(it) }
            .sortedBy { it.entitiesCount }
        val smallestStorage = queryStorages.first()
        val otherStorages = queryStorages.drop(1)

        return smallestStorage.entities().filter { entity ->
            otherStorages.all { storage -> storage.hasEntity(entity) }
        }
    }

    @JvmName("getEntitiesWithOne")
    inline fun <reified T : Component> getEntitiesWith(): List<Entity> =
        getEntitiesWith(T::class)

    @JvmName("getEntitiesWithTwo")
    inline fun <
        reified T1 : Component,
        reified T2 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class)

    @JvmName("getEntitiesWithThree")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class)

    @JvmName("getEntitiesWithFour")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class)

    @JvmName("getEntitiesWithFive")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component,
        reified T5 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class, T5::class)

    @JvmName("getEntitiesWithSix")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component,
        reified T5 : Component,
        reified T6 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class, T5::class, T6::class)

    @JvmName("getEntitiesWithSeven")
    inline fun <
        reified T1 : Component,
        reified T2 : Component,
        reified T3 : Component,
        reified T4 : Component,
        reified T5 : Component,
        reified T6 : Component,
        reified T7 : Component
    > getEntitiesWith(): List<Entity> =
        getEntitiesWith(T1::class, T2::class, T3::class, T4::class, T5::class, T6::class, T7::class)

    fun add(storage: Component) {
        storages[storage::class] = storage
    }
    
    fun removeEntity(entity: Entity) {
        storages.values.forEach {
            if (it.hasEntity(entity)) {
                it.remove(entity)
            }
        }
    }
    
    fun reset() {
        storages.clear()
    }

    private fun getOrThrow(type: KClass<out Component>): Component {
        return storages[type] ?: error("Storage not found: ${type.simpleName}")
    }
}
