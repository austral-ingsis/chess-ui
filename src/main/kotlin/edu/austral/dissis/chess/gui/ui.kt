package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.PieceView.Companion.ANIMATION_TIME
import edu.austral.dissis.chess.gui.util.bindListTo
import edu.austral.dissis.chess.gui.util.map
import javafx.animation.*
import javafx.beans.binding.Bindings.*
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
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
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
    fun onClick(position: Position)
}

class SquareView(
    private val position: Position,
    private val selectedPosition: ReadOnlyObjectProperty<Position>,
    private val positionClickedHandler: PositionClickedHandler,
) : Rectangle() {
    companion object {
        private const val STROKE_WIDTH = 3.0
        const val SIZE = 70.0
    }

    init {
        width = SIZE
        height = SIZE
        stroke = BLACK
        strokeWidth = STROKE_WIDTH

        fillProperty().bind(createBackgroundProperty())

        translateX = SIZE * (position.column - 1)
        translateY = SIZE * (position.row - 1)

        onMouseClicked = EventHandler { positionClickedHandler.onClick(position) }
    }

    private fun createBackgroundProperty(): ObjectProperty<Color> {
        val defaultColor = if (position.isEven()) Color.GREY else Color.WHITE
        return selectedPosition.map { if (it == position) DARKGRAY else defaultColor }
    }
}

class PieceView(
    private val imageResolver: ImageResolver,
    val piece: ObjectProperty<ChessPiece>,
    private val clickedHandler: PositionClickedHandler,
) : Pane() {
    companion object {
        val ANIMATION_TIME: Duration = Duration.millis(500.0)
    }

    private val position = SimpleObjectProperty(piece.value.position)
    private val image = SimpleObjectProperty(getImage(piece.value))
    private val imageView = ImageView()
    private val positionTransition = createPositionTransition()

    init {
        position.bind(createObjectBinding({ piece.value.position }, piece))
        image.bind(createObjectBinding({ getImage(piece.value) }, piece))

        imageView.image = image.value
        children.add(imageView)

        onMouseClickedProperty()
            .bind(createObjectBinding({ EventHandler { clickedHandler.onClick(position.value) } }, position))

        translateX = toX(position.value)
        translateY = toY(position.value)

        position.addListener { _, _, _ -> positionTransition.play() }
    }

    private fun createPositionTransition(): Transition {
        val transition = TranslateTransition(ANIMATION_TIME, this)
        transition.toXProperty().bind(createDoubleBinding({ toX(position.value) }, position))
        transition.toYProperty().bind(createDoubleBinding({ toY(position.value) }, position))
        transition.onFinishedProperty().bind(createObjectBinding({ EventHandler { imageView.image = image.value } }, image))
        return transition
    }


    private fun getImage(piece: ChessPiece): Image {
        val imageId = "${piece.pieceId}_${piece.color.name.lowercase()}"
        return imageResolver.resolve(imageId, SquareView.SIZE, SquareView.SIZE)
    }

    private fun toX(position: Position) = SquareView.SIZE * (position.column - 1)
    private fun toY(position: Position) = SquareView.SIZE * (position.row - 1)
}

class PiecesRenderer(
    private val imageResolver: ImageResolver,
    private val pieces: ObservableMap<String, ChessPiece>,
    private val positionClickedHandler: PositionClickedHandler,
) {

    fun render(): ObservableList<Node> {
        val nodes = observableArrayList<Node>()

        val pieceNodeMap = pieces.mapValues { renderPiece(it.value) }.toMutableMap()
        pieceNodeMap.values.forEach { nodes.add(it) }

        pieces.addListener(MapChangeListener { change ->
            if (change.valueAdded != null && change.valueRemoved != null)
                pieceNodeMap[change.key]?.piece?.set(change.valueAdded)
            else if (change.valueAdded != null)
                pieceNodeMap[change.key] = renderPiece(change.valueAdded)
            else {
                pieceNodeMap[change.key]?.let { deleteNode(nodes, it) }
                pieceNodeMap.remove(change.key)
            }
        })

        return nodes
    }

    private fun renderPiece(piece: ChessPiece): PieceView =
        PieceView(imageResolver, SimpleObjectProperty(piece), positionClickedHandler)

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
}

class BoardView(
    private val boardSize: BoardSize,
    private val imageResolver: ImageResolver,
    private val pieceMovedListener: PieceMovedListener,
    private val pieces: ObservableMap<String, ChessPiece>
) : Pane() {
    private val selectedPosition = SimpleObjectProperty<Position>()

    private val positionClickedHandler = PositionClickedHandler { position ->
        if (selectedPosition.value != null) {
            pieceMovedListener.onMovePiece(selectedPosition.value!!, position)
            selectedPosition.set(null)
        } else
            selectedPosition.set(position)
    }

    init {
        prefWidth = SquareView.SIZE * boardSize.columns
        prefHeight = SquareView.SIZE * boardSize.rows

        children.bindListTo(renderSquares())
        children.bindListTo(renderPieces())
    }

    private fun renderSquares(): ObservableList<Node> {
        val (columns, rows) = boardSize
        val nodes = observableArrayList<Node>()

        for (column in 1..columns) {
            for (row in 1..rows) {
                nodes.add(SquareView(Position(row, column), selectedPosition, positionClickedHandler))
            }
        }

        return nodes
    }

    private fun renderPieces(): ObservableList<Node> =
        PiecesRenderer(imageResolver, pieces, positionClickedHandler).render()
}

class MessageView(text: Text) : HBox() {
    init {
        padding = Insets(20.0, 20.0, 20.0, 20.0)
        alignment = Pos.BASELINE_CENTER
        children.add(text)
    }
}

class GameView(private val gameEngine: GameEngine, imageResolver: ImageResolver) : BorderPane() {
    // Properties
    private val boardSize: BoardSize
    private val pieces = observableHashMap<String, ChessPiece>()
    private val currentPlayer: ObjectProperty<PlayerColor>
    private val errorMessage = SimpleStringProperty()
    private val winner = SimpleObjectProperty<PlayerColor>()

    init {
        // Styles
        padding = Insets(20.0, 20.0, 20.0, 20.0)

        val initialState = gameEngine.init()
        boardSize = initialState.boardSize
        currentPlayer = SimpleObjectProperty(initialState.currentPlayer)
        setPieces(initialState.pieces)

        top = renderPlayerText()
        center = BoardView(boardSize, imageResolver, { from, to -> handleMove(from, to) }, pieces)
        bottom = renderLastMoveMessage()

        winner.addListener { _, _, newValue -> if (newValue != null) center = renderWinner() }
    }

    private fun handleNewState(pieces: List<ChessPiece>, currentPlayer: PlayerColor) {
        setPieces(pieces)
        this.currentPlayer.set(currentPlayer)
        errorMessage.set(null)
    }

    private fun handleInvalidMove(message: String) = errorMessage.set("Invalid move: $message")

    private fun handleGameOver(winner: PlayerColor) = this.winner.set(winner)

    private fun handleMove(from: Position, to: Position) {
        when (val result = gameEngine.applyMove(Move(from, to))) {
            is NewGameState -> handleNewState(result.pieces, result.currentPlayer)
            is InvalidMove -> handleInvalidMove(result.reason)
            is GameOver -> handleGameOver(result.winner)
        }
    }

    private fun renderPlayerText(): Node {
        val text = Text()
        text.textProperty()
            .bind(createStringBinding({ "Current player: ${currentPlayer.value.displayName}" }, currentPlayer))

        return renderHBoxWithText(text)
    }


    private fun renderWinner(): Node {
        val text = Text()
        text.textProperty()
            .bind(createStringBinding({ "${winner.value.displayName} Won!!" }, winner))
        text.font = font(40.0)

        return renderHBoxWithText(text)
    }

    private fun renderLastMoveMessage(): Node {
        val text = Text()
        text.textProperty().bind(errorMessage)
        return renderHBoxWithText(text)
    }

    private fun renderHBoxWithText(text: Text): Node {
        val textView = MessageView(text)
        textView.prefWidthProperty().bind(this.prefWidthProperty())
        return textView
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
