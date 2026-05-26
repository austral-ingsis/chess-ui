package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.GameInputEvent.*
import edu.austral.dissis.chess.gui.GameState.*
import edu.austral.dissis.chess.gui.PlayerColor.BLACK
import edu.austral.dissis.chess.gui.PlayerColor.WHITE
import io.reactivex.rxjava3.core.Observable
import javafx.application.Application.launch

internal fun main() {
    launch(ChessGameApplication::class.java)
}

private data class EngineState(val currentPlayer: PlayerColor, val pieces: List<ChessPiece>)

internal class SimpleGameEngine {
    companion object {
        private val INITIAL_PIECES = listOf(
            ChessPiece("1", WHITE, Position(1, 1), "rook"),
            ChessPiece("2", WHITE, Position(1, 2), "king"),
            ChessPiece("3", WHITE, Position(1, 3), "queen"),
            ChessPiece("4", WHITE, Position(1, 4), "rook"),
            ChessPiece("5", BLACK, Position(6, 1), "rook"),
            ChessPiece("6", BLACK, Position(6, 2), "king"),
            ChessPiece("7", BLACK, Position(6, 3), "queen"),
            ChessPiece("8", BLACK, Position(6, 4), "rook"),
        )

        private val BOARD_SIZE = BoardSize(4, 6)
    }

    private var state = EngineState(WHITE, INITIAL_PIECES)
    private var pastStates = listOf<EngineState>()
    private var undoneStates = listOf<EngineState>()

    fun process(inputs: Observable<GameInputEvent>): Observable<GameState> =
        inputs.map { event ->
            when (event) {
                Init -> playing()
                is MoveEvent -> handleMove(event.move)
                Undo -> handleUndo()
                Redo -> handleRedo()
            }
        }

    private fun handleMove(move: Move): GameState {
        val currentPlayer = state.currentPlayer
        val pieces = state.pieces

        val fromPiece = pieces.find { it.position == move.from }
        val toPiece = pieces.find { it.position == move.to }

        return when {
            fromPiece == null ->
                InvalidMove("No piece in (${move.from.row}, ${move.from.column})")
            fromPiece.color != currentPlayer ->
                InvalidMove("Piece does not belong to current player")
            toPiece != null && toPiece.color == currentPlayer ->
                InvalidMove("There is a piece in (${move.to.row}, ${move.to.column})")
            else -> {
                val newPieces = pieces
                    .filter { it != fromPiece && it != toPiece }
                    .plus(fromPiece.copy(position = move.to))
                    .map {
                        if ((it.color == WHITE && it.position.row == 6) || (it.color == BLACK && it.position.row == 1))
                            it.copy(pieceId = "queen")
                        else it
                    }

                pastStates = pastStates + state
                undoneStates = listOf()
                state = EngineState(if (currentPlayer == WHITE) BLACK else WHITE, newPieces)

                if (pieces.size == 1) GameOver(pieces[0].color) else playing()
            }
        }
    }

    private fun handleUndo(): GameState {
        if (pastStates.isNotEmpty()) {
            undoneStates = undoneStates + state
            state = pastStates.last()
            pastStates = pastStates.dropLast(1)
        }
        return playing()
    }

    private fun handleRedo(): GameState {
        if (undoneStates.isNotEmpty()) {
            pastStates = pastStates + state
            state = undoneStates.last()
            undoneStates = undoneStates.dropLast(1)
        }
        return playing()
    }

    private fun playing(): Playing = Playing(
        BOARD_SIZE,
        state.pieces,
        state.currentPlayer,
        pastStates.isNotEmpty(),
        undoneStates.isNotEmpty(),
    )
}

internal class ChessGameApplication : AbstractChessGameApplication() {
    override val imageResolver = CachedImageResolver(DefaultImageResolver())

    override fun buildEngine(view: GameView) {
        SimpleGameEngine()
            .process(view.inputEvents())
            .subscribe { view.applyState(it) }
    }
}
