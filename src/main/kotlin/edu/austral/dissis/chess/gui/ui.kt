package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.util.bindListTo
import edu.austral.dissis.chess.gui.util.map
import javafx.animation.RotateTransition
import javafx.animation.TranslateTransition
import javafx.beans.binding.Bindings.createStringBinding
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.DARKGRAY
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font.font
import javafx.scene.text.Text
import javafx.util.Duration


fun interface PieceMovedListener {
    fun onMovePiece(from: Position, to: Position)
}

fun interface PositionClickedHandler {
    fun onClicked(position: Position)
}

class ChessSquareUI(
    private val position: Position,
    private val selectedPosition: ReadOnlyObjectProperty<Position>,
    private val positionClickedHandler: PositionClickedHandler,
) {
    companion object {
        private const val STROKE_WIDTH = 3.0
        const val SIZE = 70.0
    }

    private fun createBackgroundProperty(): ObjectProperty<Color> {
        val defaultColor = if (position.isEven()) Color.GREY else Color.WHITE
        return selectedPosition.map { if (it == position) DARKGRAY else defaultColor }
    }

    fun render(): Node {
        val rect = Rectangle()
        rect.width = SIZE
        rect.height = SIZE
        rect.stroke = BLACK
        rect.strokeWidth = STROKE_WIDTH

        rect.fillProperty().bind(createBackgroundProperty())

        rect.translateX = SIZE * (position.column - 1)
        rect.translateY = SIZE * (position.row - 1)

        rect.onMouseClicked = EventHandler { positionClickedHandler.onClicked(position) }

        return rect
    }
}

class ChessBoardPiecesUI(
    private val imageResolver: ImageResolver,
    private val pieces: ObservableMap<String, ChessPiece>,
    private val positionClickedHandler: PositionClickedHandler,
) {
    companion object {
        private val ANIMATION_TIME = Duration.millis(500.0)
    }

    fun render(): ObservableList<Node> {
        val nodes = observableArrayList<Node>()

        val pieceNodeMap = pieces.mapValues { renderPiece(it.value) }.toMutableMap()
        pieceNodeMap.values.forEach { nodes.add(it) }

        pieces.addListener(MapChangeListener { change ->
            if (change.valueAdded != null && change.valueRemoved != null)
                pieceNodeMap[change.key]?.let {
                    updateNodePosition(it, change.valueAdded.position)
                }
            else if (change.valueAdded != null)
                pieceNodeMap[change.key] = renderPiece(change.valueAdded)
            else {
                pieceNodeMap[change.key]?.let { deleteNode(nodes, it) }
                pieceNodeMap.remove(change.key)
            }
        })

        return nodes
    }

    private fun renderPiece(piece: ChessPiece): Node {
        val imageId = "${piece.pieceId}_${piece.color.name.lowercase()}"
        val image = imageResolver.resolve(imageId, ChessSquareUI.SIZE, ChessSquareUI.SIZE)
        val imageView = ImageView(image)

        imageView.translateX = toX(piece.position)
        imageView.translateY = toY(piece.position)

        imageView.onMouseClicked = EventHandler { positionClickedHandler.onClicked(piece.position) }

        return imageView
    }

    private fun deleteNode(nodes: ObservableList<Node>, node: Node) {
        val transition = RotateTransition(ANIMATION_TIME, node)
        transition.fromAngle = -10.0
        transition.toAngle = 10.0
        transition.rate = 3.0
        transition.isAutoReverse = true
        transition.cycleCount = 3

        transition.play()

        transition.onFinished = EventHandler { nodes.remove(node) }
    }

    private fun updateNodePosition(node: Node, position: Position) {
        val transition = TranslateTransition(ANIMATION_TIME, node)
        transition.fromX = node.translateX
        transition.toX = toX(position)

        transition.fromY = node.translateY
        transition.toY = toY(position)

        transition.play()

        node.onMouseClicked = EventHandler { positionClickedHandler.onClicked(position) }
    }

    private fun toX(position: Position) = ChessSquareUI.SIZE * (position.column - 1)
    private fun toY(position: Position) = ChessSquareUI.SIZE * (position.row - 1)
}

class ChessBoardUI(
    private val boardSize: BoardSize,
    private val imageResolver: ImageResolver,
    private val pieceMovedListener: PieceMovedListener,
    private val pieces: ObservableMap<String, ChessPiece>
) {
    private val selectedPosition = SimpleObjectProperty<Position>()

    private val positionClickedHandler = PositionClickedHandler { position ->
        if (selectedPosition.value != null) {
            pieceMovedListener.onMovePiece(selectedPosition.value!!, position)
            selectedPosition.set(null)
        } else
            selectedPosition.set(position)
    }

    fun render(): Node {
        val pane = Pane()
        pane.prefWidth = ChessSquareUI.SIZE * boardSize.columns
        pane.prefHeight = ChessSquareUI.SIZE * boardSize.rows

        pane.children.bindListTo(renderSquares())
        pane.children.bindListTo(renderPieces())

        return pane
    }

    private fun renderSquares(): ObservableList<Node> {
        val (columns, rows) = boardSize
        val nodes = observableArrayList<Node>()

        for (column in 1..columns) {
            for (row in 1..rows) {
                nodes.add(renderSquare(Position(row, column)))
            }
        }

        return nodes
    }

    private fun renderSquare(position: Position): Node =
        ChessSquareUI(position, selectedPosition, positionClickedHandler).render()

    private fun renderPieces(): ObservableList<Node> =
        ChessBoardPiecesUI(imageResolver, pieces, positionClickedHandler).render()
}

class ChessGameUI(private val gameEngine: GameEngine, private val imageResolver: ImageResolver) {
    private val initialState = gameEngine.init()
    private val boardSize = initialState.boardSize
    private var pieces = observableHashMap<String, ChessPiece>()
    private val playerProperty = SimpleObjectProperty(initialState.currentPlayer)
    private val errorMessage = SimpleStringProperty()
    private val winner = SimpleObjectProperty<PlayerColor>()

    private fun handleNewState(pieces: List<ChessPiece>, currentPlayer: PlayerColor) {
        setPieces(pieces)
        playerProperty.set(currentPlayer)
        errorMessage.set(null)
    }

    private fun handleInvalidMove(message: String) {
        errorMessage.set("Invalid move: $message")
    }

    private fun handleGameOver(winner: PlayerColor) {
        this.winner.set(winner)
    }

    private fun handleMove(from: Position, to: Position) {
        when (val result = gameEngine.applyMove(Move(from, to))) {
            is NewGameState -> handleNewState(result.pieces, result.currentPlayer)
            is InvalidMove -> handleInvalidMove(result.reason)
            is GameOver -> handleGameOver(result.winner)
        }
    }

    fun render(): Node {
        setPieces(initialState.pieces)

        val pane = BorderPane()
        pane.padding = Insets(20.0, 20.0, 20.0, 20.0)

        pane.top = renderPlayerText(pane)
        pane.center = ChessBoardUI(boardSize, imageResolver, { from, to -> handleMove(from, to) }, pieces).render()
        pane.bottom = renderLastMoveMessage(pane)

        winner.addListener { _, _, newValue -> if (newValue != null) pane.center = renderWinner(pane) }

        return pane
    }

    private fun renderPlayerText(region: Region): Node {
        val text = Text()
        text.textProperty()
            .bind(createStringBinding({ "Current player: ${playerProperty.value.displayName}" }, playerProperty))

        return renderHBoxWithText(region, text)
    }


    private fun renderWinner(region: Region): Node {
        val text = Text()
        text.textProperty()
            .bind(createStringBinding({ "${winner.value.displayName} Won!!" }, winner))
        text.font = font(40.0)

        return renderHBoxWithText(region, text)
    }

    private fun renderLastMoveMessage(region: Pane): Node {
        val text = Text()
        text.textProperty().bind(errorMessage)
        return renderHBoxWithText(region, text)
    }

    private fun renderHBoxWithText(parent: Region, text: Text): Node {
        val box = HBox()
        box.prefWidthProperty().bind(parent.prefWidthProperty())
        box.padding = Insets(20.0, 20.0, 20.0, 20.0)
        box.alignment = Pos.BASELINE_CENTER
        box.children.add(text)

        return box
    }

    private fun setPieces(pieces: List<ChessPiece>) {
        val newPieces = pieces.filter { !this.pieces.contains(it.id) }
        this.pieces.putAll(newPieces.associateBy { it.id })

        val updatedPieces = pieces.filter { this.pieces.contains(it.id) && this.pieces[it.id] != it }
        this.pieces.putAll(updatedPieces.associateBy { it.id })

        val removedPieces = this.pieces.filter { pieces.find { entry -> entry.id == it.key } == null }
        removedPieces.forEach {
            this.pieces.remove(it.key)
        }
    }
}
