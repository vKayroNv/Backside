package ru.kayron.cargo

internal data class Registration(
    val abstraction: TypeKey,
    val implementation: TypeKey?,
    val factory: FactoryDelegate?,
    val lifetime: Lifetime,
    val qualifier: String?,
    val eager: Boolean
)