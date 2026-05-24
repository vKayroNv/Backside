package ru.kayron.backside

import ru.kayron.dew.DewActivity
import ru.kayron.dew.Game

class MainActivity : DewActivity() {
    override fun createGame(): Game = BacksideGame()
}
