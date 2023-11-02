package edu.austral.dissis.chess.gui

import java.beans.Transient

interface GameEngine {
    fun init(): InitialState

    fun applyMove(move: Move): MoveResult
}

enum class PlayerColor(val displayName: String) { WHITE("White"), BLACK("Black") }

data class BoardSize(val columns: Int, val rows: Int)

data class Position(val row: Int, val column: Int) {
    @Transient
    fun isEven(): Boolean = (column + row) % 2 == 0
}

data class ChessPiece(val id: String, val color: PlayerColor, val position: Position, val pieceId: String)

sealed interface MoveResult

data class InvalidMove(val reason: String) : MoveResult

data class NewGameState(val pieces: List<ChessPiece>, val currentPlayer: PlayerColor) : MoveResult

data class GameOver(val winner: PlayerColor) : MoveResult

data class Move(val from: Position, val to: Position)

data class InitialState(val boardSize: BoardSize, val pieces: List<ChessPiece>, val currentPlayer: PlayerColor)
