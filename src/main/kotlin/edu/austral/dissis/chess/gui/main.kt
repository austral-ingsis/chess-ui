package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.PlayerColor.BLACK
import edu.austral.dissis.chess.gui.PlayerColor.WHITE
import javafx.application.Application.launch


fun main() {
    launch(ChessGameApplication::class.java)
}

data class GameState(val currentPlayer: PlayerColor, val pieces: List<ChessPiece>)

class SimpleGameEngine : GameEngine {
    private var gameState =
        GameState(
            WHITE,
            listOf(
                ChessPiece("1", WHITE, Position(1, 1), "rook"),
                ChessPiece("2", WHITE, Position(1, 2), "king"),
                ChessPiece("3", WHITE, Position(1, 3), "queen"),
                ChessPiece("4", WHITE, Position(1, 4), "rook"),
                ChessPiece("5", BLACK, Position(6, 1), "rook"),
                ChessPiece("6", BLACK, Position(6, 2), "king"),
                ChessPiece("7", BLACK, Position(6, 3), "queen"),
                ChessPiece("8", BLACK, Position(6, 4), "rook"),
            )
        )

    private var pastStates = listOf<GameState>()
    private var undoneStates = listOf<GameState>()

    private fun setNewGameState(newCurrentPlayer: PlayerColor, newPieces: List<ChessPiece>) {
        pastStates = pastStates + gameState

        gameState = GameState(newCurrentPlayer, newPieces)
    }

    private fun getNewGameState(): NewGameState = NewGameState(
        gameState.pieces,
        gameState.currentPlayer,
        getUndoState()
    )

    private fun getUndoState(): UndoState = UndoState(pastStates.isNotEmpty(), undoneStates.isNotEmpty())

    override fun init(): InitialState {
        return InitialState(BoardSize(4, 6), gameState.pieces, WHITE)
    }

    override fun applyMove(move: Move): MoveResult {
        val currentPlayer = gameState.currentPlayer
        val pieces = gameState.pieces

        val fromPiece = pieces.find { it.position == move.from }
        val toPiece = pieces.find { it.position == move.to }

        return if (fromPiece == null)
            InvalidMove("No piece in (${move.from.row}, ${move.from.column})")
        else if (fromPiece.color != currentPlayer)
            InvalidMove("Piece does not belong to current player")
        else if (toPiece != null && toPiece.color == currentPlayer)
            InvalidMove("There is a piece in (${move.to.row}, ${move.to.column})")
        else {
            setNewGameState(
                if (currentPlayer == WHITE) BLACK else WHITE,
                pieces
                    .filter { it != fromPiece && it != toPiece }
                    .plus(fromPiece.copy(position = move.to))
                    .map {
                        if ((it.color == WHITE && it.position.row == 6) || it.color == BLACK && it.position.row == 1)
                            it.copy(pieceId = "queen")
                        else
                            it
                    },
            )

            if (pieces.size == 1)
                return GameOver(pieces[0].color)
            else
                getNewGameState()
        }
    }


    override fun undo(): NewGameState {
        if (pastStates.isNotEmpty()) {
            val lastState = pastStates.last()
            pastStates = pastStates.dropLast(1)
            undoneStates = undoneStates + gameState
            gameState = lastState
        }

        return getNewGameState()
    }

    override fun redo(): NewGameState {
        if (undoneStates.isNotEmpty()) {
            val nextState = undoneStates.last()
            undoneStates = undoneStates.dropLast(1)
            pastStates = pastStates + gameState
            gameState = nextState
        }

        return getNewGameState()
    }


}

class MovePrinter : PieceMovedListener {
    override fun onMovePiece(from: Position, to: Position) {
        print("Move: from ")
        print(from)
        print(" to ")
        println(to)
    }
}

class ChessGameApplication : AbstractChessGameApplication() {
    override val gameEngine = SimpleGameEngine()
    override val imageResolver = CachedImageResolver(DefaultImageResolver())
}