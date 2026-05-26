package edu.austral.dissis.chess.gui

import edu.austral.dissis.chess.gui.GameInputEvent.MoveEvent
import edu.austral.dissis.chess.gui.GameInputEvent.Redo
import edu.austral.dissis.chess.gui.GameInputEvent.Undo
import edu.austral.dissis.chess.gui.GameState.GameOver
import edu.austral.dissis.chess.gui.GameState.InvalidMove
import edu.austral.dissis.chess.gui.GameState.Playing
import edu.austral.dissis.chess.gui.PieceView.Companion.ANIMATION_TIME
import edu.austral.dissis.chess.gui.util.bindListTo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import javafx.application.Platform
import javafx.animation.RotateTransition
import javafx.animation.Transition
import javafx.animation.TranslateTransition
import javafx.beans.binding.Bindings.*
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.observableHashMap
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.DARKGRAY
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font.font
import javafx.scene.text.Text
import javafx.util.Duration

internal fun interface PieceMovedListener {
    fun onMovePiece(from: Position, to: Position)
}

internal fun interface PositionClickedHandler {
    fun onClick(position: Position)
}

internal class SquareView(
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

    private fun createBackgroundProperty(): ObservableValue<Color> {
        val defaultColor = if (position.isEven()) Color.GREY else Color.WHITE
        return selectedPosition.map { if (it == position) DARKGRAY else defaultColor }.orElse(defaultColor)
    }
}

internal class PieceView(
    private val imageResolver: ImageResolver,
    val piece: ObjectProperty<ChessPiece>,
    private val clickedHandler: PositionClickedHandler,
    private val pieceIsBeingMoved: BooleanProperty,
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

        position.addListener { _, _, _ ->
            pieceIsBeingMoved.value = true
            positionTransition.play()
        }
    }

    private fun createPositionTransition(): Transition {
        val transition = TranslateTransition(ANIMATION_TIME, this)
        transition.toXProperty().bind(createDoubleBinding({ toX(position.value) }, position))
        transition.toYProperty().bind(createDoubleBinding({ toY(position.value) }, position))
        transition.onFinishedProperty()
            .bind(createObjectBinding({
                EventHandler {
                    imageView.image = image.value
                    pieceIsBeingMoved.value = false
                }
            }, image))
        return transition
    }

    private fun getImage(piece: ChessPiece): Image {
        val imageId = "${piece.pieceId}_${piece.color.name.lowercase()}"
        return imageResolver.resolve(imageId, SquareView.SIZE, SquareView.SIZE)
    }

    private fun toX(position: Position) = SquareView.SIZE * (position.column - 1)

    private fun toY(position: Position) = SquareView.SIZE * (position.row - 1)
}

internal class PiecesRenderer(
    private val imageResolver: ImageResolver,
    private val pieces: ObservableMap<String, ChessPiece>,
    private val positionClickedHandler: PositionClickedHandler,
    private val pieceIsBeingRemoved: BooleanProperty,
    private val pieceIsBeingMoved: BooleanProperty,
) {
    fun render(): ObservableList<Node> {
        val nodes = observableArrayList<Node>()

        val pieceNodeMap = pieces.mapValues { renderPiece(it.value) }.toMutableMap()
        pieceNodeMap.values.forEach { nodes.add(it) }

        pieces.addListener(
            MapChangeListener { change ->
                if (change.valueAdded != null && change.valueRemoved != null) {
                    pieceNodeMap[change.key]?.piece?.set(change.valueAdded)
                } else if (change.valueAdded != null) {
                    pieceNodeMap[change.key] = renderPiece(change.valueAdded)
                    nodes.add(pieceNodeMap[change.key])
                } else {
                    pieceNodeMap[change.key]?.let { deleteNode(nodes, it, pieceNodeMap, change.key) }
                    pieceNodeMap.remove(change.key)
                }
            },
        )

        return nodes
    }

    private fun renderPiece(piece: ChessPiece): PieceView =
        PieceView(imageResolver, SimpleObjectProperty(piece), positionClickedHandler, pieceIsBeingMoved)

    private fun deleteNode(
        nodes: ObservableList<Node>,
        node: Node,
        pieceNodeMap: Map<String, PieceView>,
        pieceId: String,
    ) {
        pieceIsBeingRemoved.value = true

        val transition = RotateTransition(ANIMATION_TIME, node)
        transition.fromAngle = -10.0
        transition.toAngle = 10.0
        transition.rate = 3.0
        transition.isAutoReverse = true
        transition.cycleCount = 3

        transition.play()
        transition.onFinished = EventHandler {
            if (!pieceNodeMap.containsKey(pieceId)) {
                nodes.remove(node)
            }
            pieceIsBeingRemoved.value = false
        }
    }
}

internal class BoardView(
    private val boardSize: BoardSize,
    private val imageResolver: ImageResolver,
    private val pieceMovedListener: PieceMovedListener,
    private val pieces: ObservableMap<String, ChessPiece>,
    private val pieceIsBeingRemoved: BooleanProperty,
    private val pieceIsBeingMoved: BooleanProperty,
) : Pane() {
    private val selectedPosition = SimpleObjectProperty<Position>()

    private val positionClickedHandler = PositionClickedHandler { position ->
        if (selectedPosition.value != null) {
            pieceMovedListener.onMovePiece(selectedPosition.value!!, position)
            selectedPosition.set(null)
        } else {
            selectedPosition.set(position)
        }
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
        PiecesRenderer(imageResolver, pieces, positionClickedHandler, pieceIsBeingRemoved, pieceIsBeingMoved).render()
}

internal class MessageView(text: Text) : HBox() {
    init {
        padding = Insets(20.0, 20.0, 20.0, 20.0)
        alignment = Pos.BASELINE_CENTER
        children.add(text)
    }
}

internal class GameViewImpl(
    private val imageResolver: ImageResolver,
) : BorderPane(), GameView {

    private val inputs = PublishSubject.create<GameInputEvent>()

    override fun inputEvents(): Observable<GameInputEvent> = inputs.startWithItem(GameInputEvent.Init)
    override fun pane(): Pane = this

    private val boardSize = SimpleObjectProperty<BoardSize>()
    private val pieces = observableHashMap<String, ChessPiece>()
    private val currentPlayer = SimpleObjectProperty<PlayerColor>()
    private val errorMessage = SimpleStringProperty()
    private val winner = SimpleObjectProperty<PlayerColor>()

    private val canUndo = SimpleBooleanProperty(false)
    private val canRedo = SimpleBooleanProperty(false)

    private val pieceIsBeingRemoved: BooleanProperty = SimpleBooleanProperty(false)
    private val pieceIsBeingMoved: BooleanProperty = SimpleBooleanProperty(false)

    init {
        padding = Insets(20.0, 20.0, 20.0, 20.0)

        top = renderTopPane()
        center = renderLoading()
        bottom = renderLastMoveMessage()

        boardSize.addListener { _, _, newValue -> newValue?.let { center = renderBoard() } }
        winner.addListener { _, _, newValue -> newValue?.let { center = renderWinner() } }
    }

    override fun applyState(state: GameState) = onFxThread {
        when (state) {
            is Playing -> {
                boardSize.set(state.boardSize)
                setPieces(state.pieces)
                currentPlayer.set(state.currentPlayer)
                canUndo.set(state.canUndo)
                canRedo.set(state.canRedo)
                errorMessage.set(null)
            }
            is InvalidMove -> errorMessage.set("Invalid move: ${state.reason}")
            is GameOver -> winner.set(state.winner)
        }
    }

    private fun onFxThread(action: () -> Unit) {
        if (Platform.isFxApplicationThread()) action() else Platform.runLater(action)
    }

    private fun handleMove(from: Position, to: Position) {
        inputs.onNext(MoveEvent(Move(from, to)))
    }

    private fun handleUndo() {
        inputs.onNext(Undo)
    }

    private fun handleRedo() {
        inputs.onNext(Redo)
    }

    private fun renderPlayerText(): Node {
        val text = Text()
        text.textProperty()
            .bind(createStringBinding({ currentPlayer.value?.run { "Current player: $displayName" } }, currentPlayer))
        return text
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

    private fun renderLoading(): Node {
        val text = Text("Initializing...")
        text.font = font(40.0)
        return renderHBoxWithText(text)
    }

    private fun renderBoard(): Node =
        BoardView(
            boardSize.value,
            imageResolver,
            { from, to -> handleMove(from, to) },
            pieces,
            pieceIsBeingRemoved,
            pieceIsBeingMoved,
        )

    private fun renderUndoButton(): Node {
        val undoButton = Button("Undo")
        undoButton.disableProperty().bind(pieceIsBeingRemoved.or(pieceIsBeingMoved).or(canUndo.not()))
        undoButton.onMouseClicked = EventHandler { handleUndo() }
        return undoButton
    }

    private fun renderRedoButton(): Node {
        val redoButton = Button("Redo")
        redoButton.disableProperty().bind(pieceIsBeingRemoved.or(pieceIsBeingMoved).or(canRedo.not()))
        redoButton.onMouseClicked = EventHandler { handleRedo() }
        return redoButton
    }

    private fun renderTopPane(): Node {
        val topPane = BorderPane()
        topPane.left = HBox(renderUndoButton(), renderRedoButton())
        topPane.center = renderPlayerText()
        topPane.prefWidthProperty().bind(this.prefWidthProperty())
        topPane.padding = Insets(10.0, 10.0, 10.0, 10.0)
        return topPane
    }

    private fun setPieces(pieces: List<ChessPiece>) {
        val newPieces = pieces.filter { !this.pieces.contains(it.id) }
        val updatedPieces = pieces.filter { this.pieces.contains(it.id) && this.pieces[it.id] != it }

        this.pieces.putAll((newPieces + updatedPieces).associateBy { it.id })

        val removedPieces = this.pieces.filter { pieces.find { entry -> entry.id == it.key } == null }
        removedPieces.forEach { this.pieces.remove(it.key) }
    }
}
