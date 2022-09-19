package edu.austral.dissis.chess.gui.util

import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleObjectProperty

fun <T, R> ReadOnlyProperty<T>.map(mapper: (t: T) -> R): ObjectProperty<R> {
    val result = SimpleObjectProperty<R>()

    addListener { _, _, newValue ->
        result.set(mapper(newValue))
    }
    result.set(mapper(value))

    return result
}
