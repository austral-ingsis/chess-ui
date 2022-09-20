package edu.austral.dissis.chess.gui.util

import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList

fun <T, R> ReadOnlyProperty<T>.map(mapper: (t: T) -> R): ObjectProperty<R> {
    val result = SimpleObjectProperty<R>()

    addListener { _, _, newValue ->
        result.set(mapper(newValue))
    }
    result.set(mapper(value))

    return result
}

fun <T> ObservableList<T>.bindListTo(observed: ObservableList<T>) {
    observed.addListener(ListChangeListener { change ->
        if (change.next() && (change.wasAdded() || change.wasRemoved())) {
            addAll(change.addedSubList)
            removeAll(change.removed)
        }
    })

    addAll(observed)
}