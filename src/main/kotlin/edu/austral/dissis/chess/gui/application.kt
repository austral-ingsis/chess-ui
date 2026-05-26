package edu.austral.dissis.chess.gui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

internal abstract class AbstractChessGameApplication : Application() {
    protected abstract val imageResolver: ImageResolver

    protected abstract fun buildEngine(view: GameView)

    companion object {
        const val GameTitle = "Chess"
    }

    override fun start(primaryStage: Stage) {
        primaryStage.title = GameTitle
        val view = GameViewFactory.create(imageResolver)
        buildEngine(view)
        primaryStage.scene = Scene(view.pane())
        primaryStage.show()
    }
}
