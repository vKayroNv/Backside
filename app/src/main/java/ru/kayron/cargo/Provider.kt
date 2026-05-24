package ru.kayron.cargo

fun interface Provider<T> {
    fun get(): T
}