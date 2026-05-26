package edu.austral.dissis.chess.gui

import io.reactivex.rxjava3.core.Observable
import javafx.scene.layout.Pane
import java.beans.Transient

interface GameView {
    fun inputEvents(): Observable<GameInputEvent>

    fun applyState(state: GameState)

    fun pane(): Pane
}

enum class PlayerColor(val displayName: String) { WHITE("White"), BLACK("Black") }

data class BoardSize(val columns: Int, val rows: Int)

data class Position(val row: Int, val column: Int) {
    @Transient
    fun isEven(): Boolean = (column + row) % 2 == 0
}

data class ChessPiece(val id: String, val color: PlayerColor, val position: Position, val pieceId: String)

data class Move(val from: Position, val to: Position)

sealed interface GameInputEvent {
    data object Init : GameInputEvent
    data class MoveEvent(val move: Move) : GameInputEvent
    data object Undo : GameInputEvent
    data object Redo : GameInputEvent
}

sealed interface GameState {
    data class Playing(
        val boardSize: BoardSize,
        val pieces: List<ChessPiece>,
        val currentPlayer: PlayerColor,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    ) : GameState

    data class InvalidMove(val reason: String) : GameState

    data class GameOver(val winner: PlayerColor) : GameState
}
