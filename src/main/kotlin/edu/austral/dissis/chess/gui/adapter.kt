package edu.austral.dissis.chess.gui

fun createGameViewFrom(gameEngine: GameEngine, imageResolver: ImageResolver): GameView {
    val initialState = gameEngine.init()

    val gameView = GameView(initialState, imageResolver)

    gameView.addListener(object : GameEventListener {
        override fun handleMove(move: Move) {
            val result = gameEngine.applyMove(move)
            gameView.handleMoveResult(result)
        }
    })

    return gameView
}
