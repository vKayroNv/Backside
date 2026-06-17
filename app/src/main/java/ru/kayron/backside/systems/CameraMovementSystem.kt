package ru.kayron.backside.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.ecs.GameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.input.Touch
import ru.kayron.dew.math.Vector2
import kotlin.math.sqrt

class CameraMovementSystem(
    game: Game,
    private val world: World
) : GameSystem(game) {

    override val updateOrder = 0

    private var prevPinchDist = 0f

    override fun update(gameTime: GameTime) {
        val cameraComponent = world.componentManager.get<CameraComponent>()
        val transformComponent = world.componentManager.get<TransformComponent>()
        val camera = cameraComponent.getActive()
        if (camera == -1) return
        
    }
    
}