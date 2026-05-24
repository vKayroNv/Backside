package ru.kayron.cargo

import kotlin.reflect.KFunction

internal data class ConstructorInfo(
    val constructor: KFunction<*>,
    val parameters: List<ParameterInfo>
)