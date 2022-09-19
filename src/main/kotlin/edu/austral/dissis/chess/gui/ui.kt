package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.util.map
import javafx.beans.binding.Bindings.createStringBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.DARKGRAY
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text

interface PieceMovedListener {
    fun onMovePiece(from: Position, to: Position)
}

class ChessSquareUI(
    private val imageResolver: ImageResolver,
    private val squareClickHandler: EventHandler<MouseEvent>,
    private val piece: ReadOnlyObjectProperty<ChessPiece?>,
    private val background: ReadOnlyObjectProperty<Color>,
) {
    companion object {
        private const val SquareSize = 70.0
    }

    private fun renderPiece(piece: ChessPiece): Node {
        val imageId = "${piece.pieceId}_${piece.color.name.lowercase()}"
        val image = imageResolver.resolve(imageId, SquareSize, SquareSize)
        val imageView = ImageView(image)

        imageView.onMouseClicked = squareClickHandler

        return imageView
    }

    private fun renderSquare(): Node {
        val rect = Rectangle()
        rect.width = SquareSize
        rect.height = SquareSize
        rect.stroke = BLACK
        rect.strokeWidth = 3.0
        rect.fillProperty().bind(background)
        rect.onMouseClicked = squareClickHandler

        return rect
    }


    fun render(): Node {
        val pane = Pane()

        val square = renderSquare()
        pane.children.add(square)

        val pieceNode = piece.map { pieceVal -> pieceVal?.let { renderPiece(it) } }
        pieceNode.addListener { _, oldValue, newValue ->
            oldValue?.let { pane.children.remove(oldValue) }
            newValue?.let { pane.children.add(newValue) }
        }
        pieceNode.value?.let { pane.children.add(it) }

        return pane
    }
}

class ChessBoardUI(
    private val boardSize: BoardSize,
    private val imageResolver: ImageResolver,
    private val pieceMovedListener: PieceMovedListener,
    private val pieces: ObservableList<ChessPiece>
) {
    private val selectedSpace = SimpleObjectProperty<Position>()

    private fun handleSpaceClicked(position: Position) {
        if (selectedSpace.value != null) {
            pieceMovedListener.onMovePiece(selectedSpace.value!!, position)
            selectedSpace.set(null)
        } else
            selectedSpace.set(position)
    }

    fun render(): Node = renderSquares()

    private fun renderSquares(): Node {
        val (columns, rows) = boardSize
        val pane = GridPane()

        for (column in 1..columns) {
            for (row in 1..rows) {
                renderSquare(Position(row, column), pane)
            }
        }

        return pane
    }

    private fun renderSquare(position: Position, pane: GridPane) {
        val piece = createPieceProperty(position)
        val background = createBackgroundProperty(position)

        val node = ChessSquareUI(imageResolver, { handleSpaceClicked(position) }, piece, background)
            .render()

        pane.add(node, position.column, position.row)
    }

    private fun createPieceProperty(position: Position): ObjectProperty<ChessPiece> {
        val piece = SimpleObjectProperty<ChessPiece>(pieces.find { it.position == position })
        pieces.addListener(ListChangeListener { changeEvent ->
            val newValue = changeEvent.list.find { it.position == position }
            if (newValue != piece.value) piece.set(newValue)
        })

        return piece
    }

    private fun createBackgroundProperty(position: Position): ObjectProperty<Color> {
        val defaultColor = if (position.isEven()) Color.GREY else Color.WHITE
        return selectedSpace.map { if (it == position) DARKGRAY else defaultColor }
    }
}

class ChessGameUI(private val gameEngine: GameEngine, imageResolver: ImageResolver) {
    private val initialState = gameEngine.init()
    private val boardSize = initialState.boardSize
    private var pieces = observableArrayList<ChessPiece>()
    private val playerProperty = SimpleObjectProperty(initialState.currentPlayer)

    private val lastMoveMessage = SimpleStringProperty()

    private val chessBoardUI = ChessBoardUI(boardSize, imageResolver, object : PieceMovedListener {
        override fun onMovePiece(from: Position, to: Position) {
            handleMove(from, to)
        }
    }, pieces)

    private fun handleNewState(pieces: List<ChessPiece>, currentPlayer: PlayerColor) {
        setPieces(pieces)
        playerProperty.set(currentPlayer)
        lastMoveMessage.set(null)
    }

    private fun handleInvalidMove(message: String) {
        lastMoveMessage.set("Invalid move: $message")
    }

    private fun handleMove(from: Position, to: Position) {
        when (val result = gameEngine.applyMove(MovePiece(from, to))) {
            is NewGameState -> handleNewState(result.pieces, result.currentPlayer)
            is InvalidMove -> handleInvalidMove(result.reason)
            is GameOver -> {}
        }
    }

    fun render(): Node {
        setPieces(initialState.pieces)

        val pane = BorderPane()
        pane.padding = Insets(20.0, 20.0, 20.0, 20.0)

        pane.top = renderPlayerText(pane)
        pane.center = chessBoardUI.render()
        pane.bottom = renderLastMoveMessage(pane)

        return pane
    }

    private fun renderPlayerText(parent: Pane): Node {
        val text = Text()
        text.textProperty().bind(createStringBinding({ "Current player: ${playerProperty.value.name}" }, playerProperty))

        return renderHBoxWithText(parent, text)
    }

    private fun renderLastMoveMessage(parent: Pane): Node {
        val text = Text()
        text.textProperty().bind(lastMoveMessage)
        return renderHBoxWithText(parent, text)
    }

    private fun renderHBoxWithText(parent: Pane, text: Text): Node {
        val box = HBox()
        box.prefWidthProperty().bind(parent.prefWidthProperty())
        box.padding = Insets(20.0, 20.0, 20.0, 20.0)
        box.alignment = Pos.BASELINE_CENTER
        box.children.add(text)

        return box
    }

    private fun setPieces(pieces: List<ChessPiece>) {
        val newPieces = pieces.filter { !this.pieces.contains(it) }
        this.pieces.removeIf { !pieces.contains(it) }
        this.pieces.addAll(newPieces)
    }
}
