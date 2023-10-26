package edu.austral.dissis.chess.gui

interface GameEventListener {
    fun handleMove(move: Move)
}

interface GameStateListener {
    fun handleMoveResult(result: MoveResult)
}
