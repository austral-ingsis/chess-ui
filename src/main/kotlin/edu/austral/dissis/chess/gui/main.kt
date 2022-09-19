package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.PlayerColor.Black
import edu.austral.dissis.chess.gui.PlayerColor.White
import javafx.application.Application.launch


fun main() {
    launch(ChessGameApplication::class.java)
}

class SimpleGameEngine : GameEngine {
    private var currentPlayer = White
    private var pieces = listOf(
        ChessPiece(White, Position(1, 1), "rook"),
        ChessPiece(White, Position(1, 2), "king"),
        ChessPiece(White, Position(1, 3), "queen"),
        ChessPiece(White, Position(1, 4), "rook"),
        ChessPiece(Black, Position(6, 1), "rook"),
        ChessPiece(Black, Position(6, 2), "king"),
        ChessPiece(Black, Position(6, 3), "queen"),
        ChessPiece(Black, Position(6, 4), "rook"),
    )

    override fun init(): InitialState {
        return InitialState(BoardSize(4, 6), pieces, White)
    }

    override fun applyMove(move: Move): MoveResult {
        when (move) {
            is MovePiece -> {
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

                    currentPlayer = if (currentPlayer == White) Black else White
                }
            }

            is Castling -> {}
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