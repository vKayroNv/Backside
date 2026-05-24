package ru.kayron.dew.input

data class GamePadState(
    val isConnected: Boolean = false,
    val packetNumber: Int = 0,
    val buttons: GamePadButtons = GamePadButtons.None,
    val dPad: GamePadDPad = GamePadDPad.None,
    val thumbSticks: GamePadThumbSticks = GamePadThumbSticks.None,
    val triggers: GamePadTriggers = GamePadTriggers.None,
)
