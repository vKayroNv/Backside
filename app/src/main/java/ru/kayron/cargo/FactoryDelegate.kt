package ru.kayron.cargo

fun interface FactoryDelegate {
    fun create(container: CargoContainer): Any
}