package edu.austral.dissis.chess.gui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

abstract class AbstractChessGameApplication : Application() {
    protected abstract val gameEngine: GameEngine
    protected abstract val imageResolver: ImageResolver

    companion object {
        const val GameTitle = "Chess"
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = GameTitle

        val root = GameView(gameEngine, imageResolver)
        primaryStage.scene = Scene(root)

        primaryStage.show()
    }
}