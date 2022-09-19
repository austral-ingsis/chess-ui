package edu.austral.dissis.chess.gui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage

abstract class AbstractChessGameApplication : Application() {
    protected abstract val gameEngine: GameEngine
    protected abstract val imageResolver: ImageResolver

    companion object {
        const val GameTitle = "Chess"
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = GameTitle

        val boardPane = Pane()
        boardPane.children.add(ChessGameUI(gameEngine, imageResolver).render())
        primaryStage.scene = Scene(boardPane)

        primaryStage.show()
    }
}