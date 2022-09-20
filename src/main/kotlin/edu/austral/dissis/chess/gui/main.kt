package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.PlayerColor.BLACK
import edu.austral.dissis.chess.gui.PlayerColor.WHITE
import javafx.application.Application.launch


fun main() {
    launch(ChessGameApplication::class.java)
}

class SimpleGameEngine : GameEngine {
    private var currentPlayer = WHITE
    private var pieces = listOf(
        ChessPiece("1", WHITE, Position(1, 1), "rook"),
        ChessPiece("2", WHITE, Position(1, 2), "king"),
        ChessPiece("3", WHITE, Position(1, 3), "queen"),
        ChessPiece("4", WHITE, Position(1, 4), "rook"),
        ChessPiece("5", BLACK, Position(6, 1), "rook"),
        ChessPiece("6", BLACK, Position(6, 2), "king"),
        ChessPiece("7", BLACK, Position(6, 3), "queen"),
        ChessPiece("8", BLACK, Position(6, 4), "rook"),
    )

    override fun init(): InitialState {
        return InitialState(BoardSize(4, 6), pieces, WHITE)
    }

    override fun applyMove(move: Move): MoveResult {
        val fromPiece = pieces.find { it.position == move.from }
        val toPiece = pieces.find { it.position == move.to }

        if (fromPiece == null)
            return InvalidMove("No piece in (${move.from.row}, ${move.from.column})")
        else if (fromPiece.color != currentPlayer)
            return InvalidMove("Piece does not belong to current player")
        else if (toPiece != null && toPiece.color == currentPlayer)
            return InvalidMove("There is a piece in (${move.to.row}, ${move.to.column})")
        else {
            pieces = pieces
                .filter { it != fromPiece && it != toPiece }
                .plus(fromPiece.copy(position = move.to))

            currentPlayer = if (currentPlayer == WHITE) BLACK else WHITE

            if (pieces.size == 1)
                return GameOver(pieces[0].color)
        }

        pieces = pieces.map {
            if ((it.color == WHITE && it.position.row == 6) || it.color == BLACK && it.position.row == 1)
                it.copy(pieceId = "queen")
            else
                it
        }

        return NewGameState(pieces, currentPlayer)
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