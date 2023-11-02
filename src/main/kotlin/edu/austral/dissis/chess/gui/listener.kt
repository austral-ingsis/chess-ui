package edu.austral.dissis.chess.gui

interface GameEventListener {
    fun handleMove(move: Move)
}

interface GameStateListener {
    fun handleInitialState(state: InitialState)

    fun handleMoveResult(result: MoveResult)
}
