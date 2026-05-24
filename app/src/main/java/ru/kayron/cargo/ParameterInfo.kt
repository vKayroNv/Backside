package ru.kayron.cargo

import kotlin.reflect.KParameter

internal data class ParameterInfo(
    val parameter: KParameter,
    val type: TypeKey,
    val qualifier: String?,
    val lazy: Boolean,
    val provider: Boolean,
    val nullable: Boolean
)