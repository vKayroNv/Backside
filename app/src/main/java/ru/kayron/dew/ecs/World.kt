package ru.kayron.dew.ecs

import ru.kayron.dew.managers.*

class World(
    val entityManager: EntityManager,
    val componentManager: ComponentManager,
    val systemManager: SystemManager
) {
    
}