package edu.austral.dissis.chess.gui

interface GameEngine {
    fun init(): InitialState

    fun applyMove(move: Move): MoveResult
}

enum class PlayerColor { White, Black }

enum class Side { Right, Left }

data class BoardSize(val columns: Int, val rows: Int)

data class Position(val row: Int, val column: Int) {
    fun isEven(): Boolean = (column + row) % 2 == 0
}

data class ChessPiece(val color: PlayerColor, val position: Position, val pieceId: String)

sealed interface MoveResult

data class InvalidMove(val reason: String) : MoveResult

data class NewGameState(val pieces: List<ChessPiece>, val currentPlayer: PlayerColor) : MoveResult

data class GameOver(val winner: PlayerColor) : MoveResult

sealed interface Move

data class MovePiece(val from: Position, val to: Position) : Move

data class Castling(val side: Side, val player: PlayerColor) : Move

data class InitialState(val boardSize: BoardSize, val pieces: List<ChessPiece>, val currentPlayer: PlayerColor)
