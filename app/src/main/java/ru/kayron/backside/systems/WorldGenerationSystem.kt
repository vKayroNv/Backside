package ru.kayron.backside.systems

import ru.kayron.dew.Game
import ru.kayron.dew.GameTime
import ru.kayron.dew.components.CameraComponent
import ru.kayron.dew.components.TransformComponent
import ru.kayron.dew.ecs.GameSystem
import ru.kayron.dew.ecs.World
import ru.kayron.dew.input.Touch
import ru.kayron.dew.input.TouchCollection
import ru.kayron.dew.math.Vector2
import kotlin.math.hypot

class WorldGenerationSystem(
    game: Game,
    private val world: World
) : GameSystem(game) {
    override val updateOrder = 0

    override fun update(gameTime: GameTime) {

    }
}