package edu.austral.dissis.chess.gui

object GameViewFactory {
    fun create(imageResolver: ImageResolver): GameView = GameViewImpl(imageResolver)
}
