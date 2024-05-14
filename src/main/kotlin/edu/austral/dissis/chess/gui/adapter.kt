package edu.austral.dissis.chess.gui

fun createGameViewFrom(gameEngine: GameEngine, imageResolver: ImageResolver): GameView {
    val gameView = GameView(imageResolver)

    val simpleEventListener = object : GameEventListener {
        override fun handleMove(move: Move) {
            val result = gameEngine.applyMove(move)
            gameView.handleMoveResult(result)
        }

        override fun handleUndo() {
            val result = gameEngine.undo()
            gameView.handleMoveResult(result)
        }

        override fun handleRedo() {
            val result = gameEngine.redo()
            gameView.handleMoveResult(result)
        }
    }

    gameView.addListener(simpleEventListener)
    gameView.handleInitialState(gameEngine.init())

    return gameView
}
