package render;

import assets.Debug;
import assets.Tools;
import contract.datastructure.Array;
import contract.datastructure.Array.MinMaxListener;
import contract.datastructure.DataStructure;
import contract.datastructure.Element;
import contract.datastructure.IndexedElement;
import contract.utility.OperationCounter.OperationCounterHaver;
import gui.dialog.VisualDialog;
import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import render.ARenderAnimation.Effect;
import render.assets.Const;
import render.element.AVElement;
import render.element.ElementShape;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Base class for renders.
 *
 * @author Richard
 */
public abstract class ARender extends Pane implements MinMaxListener {

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    /**
     * The DataStructure this render represents.
     */
    protected final DataStructure struct;

    /**
     * Width of individual elements bounding boxes.
     */
    protected double nodeWidth;
    /**
     * Height of individual elements bounding boxes.
     */
    protected double nodeHeight;

    /**
     * Horizontal space between elements.
     */
    protected double hSpace;
    /**
     * Vertical space between elements.
     */
    protected double vSpace;
    /**
     * The width of the render.
     */
    protected double renderWidth;
    /**
     * The height of the render.
     */
    protected double renderHeight;

    /**
     * A mapping of actual Elements to VisualElements.
     */
    // protected final HashMap<Element, VisualElement> visualMap =
    // new HashMap<Element, VisualElement>();
    protected final HashMap<String, AVElement> visualMap = new HashMap<String, AVElement>();

    /**
     * If true, node sizes will be set relative to their value to the values of the other
     * elements.
     */
    private boolean relativeNodeSize;
    /**
     * The size difference between the largest and the smallest element.
     * {@code factor = 2} means the largest element is twice the size of the smallest one.
     */
    private double factor;

    // ============================================================= //
    /*
     *
     * Containers
     *
     */
    // ============================================================= //

    /**
     * Pane for rendering of visual element nodes. Added to {@link #contentPane}
     * automatically.
     */
    protected final Pane defaultNodePane = new Pane();
    /**
     * The content pane for the render. By default, a Pane for nodes (
     * {@link #defaultNodePane}) will be added, but renders can add their own panes to
     * {@code contentPane} if need be.
     */
    protected Pane contentPane;

    /**
     * The pane used when drawing animated elements.
     */
    public final Pane animPane;
    /**
     * The root for the FXML Render.
     */
    protected GridPane root;
    /**
     * Name label.
     */
    protected Label name;
    /**
     * Header bar.
     */
    protected Node header;
    /**
     * Info labels.
     */
    protected Label xPosLabel, yPosLabel, scaleLabel;
    /**
     * The element style to use.
     */
    protected ElementShape elementStyle;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    /**
     * Default constructor. Will use default values: <br>
     * Element width: {@link Const#DEFAULT_ELEMENT_HSPACE}<br>
     * Element height: {@link Const#DEFAULT_ELEMENT_HEIGHT}<br>
     * Element horizontal space: {@link Const#DEFAULT_ELEMENT_HSPACE}<br>
     * Element vertical space: {@link Const#DEFAULT_ELEMENT_VSPACE}<br>
     *
     * @param struct The DataStructure this Render will draw.
     */
    public ARender (DataStructure struct) {
        this(struct, Const.DEFAULT_ELEMENT_WIDTH, Const.DEFAULT_ELEMENT_HEIGHT, Const.DEFAULT_ELEMENT_HSPACE,
                Const.DEFAULT_ELEMENT_VSPACE);
    }

    /**
     * Creates a new Render.
     *
     * @param struct The structure to render.
     * @param nodeWidth The width of the elements in this Render.
     * @param nodeHeight The height of the elements in this Render.
     * @param hSpace The horizontal space between elements in this Render.
     * @param vSpace The vertical space between elements in this Render.
     */
    public ARender (DataStructure struct, double nodeWidth, double nodeHeight, double hSpace, double vSpace) {
        this.struct = struct;

        this.nodeWidth = nodeWidth;
        this.nodeHeight = nodeHeight;
        this.hSpace = hSpace;
        this.vSpace = vSpace;

        animPane = new Pane();
        // bindAnimPane();

        // Add stacked canvases
        loadFXML();
        initDragAndZoom();
        bindAnimPane();

        setRelativeNodeSize(Const.DEFAULT_RELATIVE_NODE_FACTOR);

        expand();
    }

    private void bindAnimPane () {
        animPane.scaleXProperty().bind(scaleXProperty());
        animPane.scaleYProperty().bind(scaleYProperty());
    }

    private void loadFXML () {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/render/RenderBase.fxml"));
        fxmlLoader.setController(this);

        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        root.setMinSize(150, 20);

        // Content pane
        contentPane = (Pane) fxmlLoader.getNamespace().get("content");
        contentPane.getChildren().add(defaultNodePane);
        reset();

        // Name labels
        name = (Label) fxmlLoader.getNamespace().get("name");
        name.setText(struct.toString());
        Label name_mo = (Label) fxmlLoader.getNamespace().get("name_mo");
        name_mo.textProperty().bind(name.textProperty());

        ToolBar headerButtonBar = (ToolBar) fxmlLoader.getNamespace().get("buttons");
        for (Node node : headerButtonBar.getItems()) {
            if (!(node instanceof ToggleButton)) {
                optionalHeaderContent.add(node);
            }
        }
        header = (Node) fxmlLoader.getNamespace().get("header");
        bindHeader();

        // Info labels
        name = (Label) fxmlLoader.getNamespace().get("name");
        name.setText(struct.toString());
        xPosLabel = (Label) fxmlLoader.getNamespace().get("xpos");
        yPosLabel = (Label) fxmlLoader.getNamespace().get("ypos");
        scaleLabel = (Label) fxmlLoader.getNamespace().get("scale");

        // Hint text
        final Label hintText = (Label) fxmlLoader.getNamespace().get("hintText");
        hintText.visibleProperty().bind(name.visibleProperty().not());

        getChildren().add(root);

        afterParentLoadFXML(fxmlLoader);
    }

    /**
     * Called after the parent class has finished with loading the fxml, in case the child
     * wants to change anything. The default implementation of this method does nothing.
     *
     * @param fxmlLoader The {@code FXMLLoader} used to load the render.
     */
    protected void afterParentLoadFXML (FXMLLoader fxmlLoader) {
        // Do nothing.
    }

    /**
     * Clear the Render, restoring the background image.
     */
    public void reset () {
        contentPane.setBackground(Tools.getRawTypeBackground(struct));
        setRestrictedSize(150, 125); // Size of background images

        for (Node node : contentPane.getChildren()) {
            if (node instanceof Pane) {
                ((Pane) node).getChildren().clear();
            }
        }
    }

    // Make header visible only on mousever.
    private void bindHeader () {
        header.visibleProperty().bind(name.visibleProperty().not());
    }

    // Make header visible and enable manual setting of visiblity
    private void unbindHeader () {
        name.setVisible(false);
        header.visibleProperty().unbind();
    }

    // ============================================================= //
    /*
     *
     * Animation
     *
     */
    // ============================================================= //

    /**
     * Default animation for a Remove operation.
     *
     * @param millis The time in milliseconds the animation should last.
     */
    // @formatter:off
    public void animateToggleScope (Element tar, long millis) {
        if (Debug.ERR) {
            System.err.println("ARender.animateRemove(): " + struct + " is animating.");
        }

        ParallelTransition base = ARenderAnimation.stationary(tar, absX(tar, null), absY(tar, null), millis,
                this, Effect.GHOST, Effect.FLIP);

        base.getNode().setRotationAxis(new Point3D(0, 1, 0));

        Color from = tar.getNumValue() == Double.NaN ? Color.WHITE : Color.BLACK;
        Color to = tar.getNumValue() != Double.NaN ? Color.WHITE : Color.BLACK;

        FillTransition ft = new FillTransition(Duration.millis(millis), from, to);
        ft.setShape(((AVElement) base.getNode()).getElementShape());

        ft.setOnFinished(event -> {
            int[] i = ((IndexedElement) tar).getIndex();
            AVElement orig = visualMap.get(Arrays.toString(i));

            orig.setRotationAxis(new Point3D(0, 1, 0));

            boolean active = orig.getElement().getNumValue() == Double.NaN;
            orig.setRotate(active ? 0 : 180);
        });

        base.play();
        ft.play();
    }
    // @formatter:on

    /**
     * Default animations for a read or write.
     *
     * @param src The source element.
     * @param srcRender The render for the source element.
     * @param tar The target element.
     * @param tarRender The render for the target element.
     * @param millis The time the animation should last in milliseconds.
     */
    public void animateReadWrite (Element src, ARender srcRender, Element tar, ARender tarRender, long millis) {
        boolean hasSource = src != null;
        boolean hasTarget = tar != null;
        double x1 = -1;
        double y1 = -1;
        double x2 = -1;
        double y2 = -1;

        if (hasSource) {
            x1 = srcRender.absX(src, tarRender);
            y1 = srcRender.absY(src, tarRender);
        }

        if (hasTarget) {
            x2 = tarRender.absX(tar, srcRender);
            y2 = tarRender.absY(tar, srcRender);
        }

        if (Debug.ERR) {
            System.err.println("ARender.animateReadWrite(): " + struct + " is animating.");
        }

        if (hasSource && hasTarget) {
            ARenderAnimation.linear(tar, x1, y1, x2, y2, millis, tarRender, Effect.GHOST).play();
        } else if (hasSource) {
            // Source only
            ARenderAnimation.linear(src, x1, y1, x1, y1 - Const.DEFAULT_ELEMENT_HEIGHT * 2, millis, srcRender,
                    Effect.FADE_OUT, Effect.SHRINK).play();
        } else {
            // Target only
            ARenderAnimation.linear(tar, x2, y2 - Const.DEFAULT_ELEMENT_HEIGHT * 2, x2, y2, millis, tarRender,
                    Effect.FADE_IN, Effect.GROW, Effect.GHOST).play();
        }

        if (Debug.ERR && !hasSource && !hasTarget) {
            System.err.println("Failed to resolve target and source.");
        }
    }

    /**
     * Default animation of a swap between two elements.
     *
     * @param var1 The first element.
     * @param render1 The render for the first element.
     * @param var2 The second element.
     * @param render2 The render for the second element.
     * @param millis The time in milliseconds the animation should last.
     */
    // @formatter:off
    public void animateSwap (Element var1, ARender render1, Element var2, ARender render2, long millis) {
        if (Debug.ERR) {
            System.err.println("ARender.animateSwap(): " + struct + " is animating.");
        }

        ARenderAnimation.linear(var1, render1.absX(var2, render2), render2.absY(var2, render2),
                render2.absX(var1, render1), render1.absY(var1, render1), millis, this, Effect.GHOST).play();
    }
    // @formatter:on

    /**
     * Calls, setPrefSize, setMaxSize, setWidth and setHeight. Will limit how small the
     * render can become.
     *
     * @param width The width of this Render.
     * @param height The height of this Render.
     */
    protected void setRestrictedSize (double width, double height) {

        // contentPane not visible indicates that the render is hidden.
        if (contentPane.isVisible()) {
            // Minimum permitted size for aesthetic reasons.
            width = width < 150 ? 150 : width;
            height = height < 0 ? 0 : height;

            contentPane.setMinSize(width, height);
            contentPane.setPrefSize(width, height);
            contentPane.setMaxSize(width, height);

            height = height < 45 ? 45 : height;
            height = height + 45; // Space for header bar.
            root.setPrefSize(width, height);
            root.setMaxSize(width, height);

            setPrefSize(width, height);
            setMaxSize(width, height);
        }
    }

    // ============================================================= //
    /*
     *
     * Controls
     *
     */
    // ============================================================= //

    /**
     * Order the Render to draw the elements of the Data Structure it carries. <br>
     * <br>
     * The default implementation of this method calls:<br>
     * {@link DataStructure#elementsDrawn(javafx.scene.paint.Paint)} <br>
     * {@link #setRelativeNodeSizes()}.
     */
    public void render () {
        struct.elementsDrawn(Color.WHITE);
        setRelativeNodeSizes();
    }

    /**
     * Force the Render to initialise all elements. This method will attempt create
     * elements and add them to the standard pane. It will clear the children of all
     * children in {@link #contentPane} before beginning. {@link #bellsAndWhistles} will
     * be called on every element.
     *
     * @return True if there was anything to draw.
     */
    public boolean repaintAll () {
        if (struct.getElements().isEmpty() || contentPane == null) {
            return false; // Nothing to draw/contentPane not yet loaded.
        }
        struct.setRepaintAll(false);

        // Clear the nodes from all content Panes.
        for (Node n : contentPane.getChildren()) {
            ((Pane) n).getChildren().clear();
        }

        visualMap.clear();
        contentPane.setBackground(null);
        calculateSize();

        // Create nodes
        AVElement newVis;

        for (Element e : struct.getElements()) {
            newVis = this.createVisualElement(e);
            newVis.setLayoutX(getX(e));
            newVis.setLayoutY(getY(e));

            defaultNodePane.getChildren().add(newVis);
            visualMap.put(Arrays.toString(((IndexedElement) e).getIndex()), newVis);

            bellsAndWhistles(e, newVis);
        }

        return true;
    }

    /**
     * Print statistics for the structure this render carries.
     */
    public void printStats () {
        // TODO Callback mechanism
        List<String> strList = OperationCounterHaver.printStatistics(struct);
        System.out.println("Statistics for \"" + struct + "\":");
        strList.forEach(System.out::println);
    }

    /**
     * Show options for the render.
     */
    public void showOptions () {
        new VisualDialog(null).show(struct);
    }

    /**
     * Used to hide other buttons when minimising.
     */
    protected final ArrayList<Node> optionalHeaderContent = new ArrayList<Node>();
    // Centre on button when hiding or showing.
    private double resizingOffset = 0;

    public void toggleHidden (Event e) {
        ToggleButton tb = (ToggleButton) e.getSource();

        if (tb.isSelected()) {
            tb.setText("Show");
            collapse();
        } else {
            tb.setText("Hide");
            expand();
        }
    }

    /**
     * Minimize the render, leaving only the header bar visible.
     */
    public void collapse () {
        // Make the render expand and collapse and NE corner.
        resizingOffset = contentPane.getPrefWidth() - 150;
        setTranslateX(getTranslateX() + resizingOffset);

        // Show only header
        root.setPrefSize(150, 20);
        root.setMaxSize(150, 20);
        setPrefSize(150, 20);
        setMaxSize(150, 20);
        unbindHeader();
        for (Node n : optionalHeaderContent) {
            n.setVisible(false);
        }
        contentPane.setVisible(false);
    }

    /**
     * Expand the render to full size.
     */
    public void expand () {
        contentPane.setVisible(true);
        setTranslateX(getTranslateX() - resizingOffset);
        bindHeader();
        calculateSize(); // Size recalculation is disabled while hidden.

        if (contentPane.getBackground() == null) {
            calculateSize();
        } else {
            setRestrictedSize(150, 90);
        }
        for (Node n : optionalHeaderContent) {
            n.setVisible(true);
        }
    }

    // ============================================================= //
    /*
     *
     * Getters and Setters
     *
     */
    // ============================================================= //

    /**
     * Returns the DataStructure held by this Render.
     *
     * @return The DataStructure held by this Render.
     */
    public DataStructure getDataStructure () {
        return struct;
    }

    /**
     * Returns the absolute x-coordinate for the element e. Returns -1 if the calculation
     * fails.
     *
     * @param e An element owned by this Render.
     * @return The absolute x-coordinates of e.
     */
    public double absX (Element e, ARender relativeTo) {
        double bx = getTranslateX() + getLayoutX();
        return getX(e) + bx;
    }

    /**
     * Returns the absolute y-coordinate for the element e. Returns -1 if the calculation
     * fails.
     *
     * @param e An element owned by this Render.
     * @return The absolute y-coordinates of e.
     */
    public double absY (Element e, ARender relativeTo) {
        double by = getTranslateY() + getLayoutY() + contentPane.getLayoutY();
        return getY(e) + by;
    }

    /**
     * Returns the Pane used to draw element nodes.
     *
     * @return The Pane used to draw element nodes.
     */
    public Pane getNodes () {
        return defaultNodePane;
    }

    /**
     * Returns the Pane used for drawing animated elements.
     */
    public Pane getAnimationPane () {
        return animPane;
    }

    /**
     * Update info labels in the header.
     */
    public void updateInfoLabels () {
        DecimalFormat df = new DecimalFormat("#0.00");
        xPosLabel.setText("XPos: " + (int) (getTranslateX() + 0.5));
        yPosLabel.setText("| YPos: " + (int) (getTranslateY() + 0.5));
        scaleLabel.setText("| Scale: " + df.format(scale));
    }

    /**
     * Makes the header red and play a sound when something goes wrong.
     */
    public void renderFailure () {
        URL resource = getClass().getResource("/assets/short_circuit.mp3");
        Media media = new Media(resource.toString());
        MediaPlayer mp3 = new MediaPlayer(media);
        mp3.play();

        header.setStyle("-fx-background-color: rgba(255, 0, 0, 0.5);");
        contentPane.setStyle("-fx-background-color: rgba(255, 0, 0, 0.5);");
        System.err.println("Render Failure in " + toString() + ".");

    }

    /**
     * Returns the visual map for this Render.
     *
     * @return The visual map for this Render.
     */
    public HashMap<String, AVElement> getVisualMap () {
        return visualMap;
    }

    @Override
    public String toString () {
        return getClass().getSimpleName() + " (" + struct + ")";
    }

    /**
     * Set the currently used element style.
     *
     * @param newStyle The new Style to use.
     */
    public void setElementStyle (ElementShape newStyle) {
        if (newStyle != elementStyle) {
            elementStyle = newStyle;
            repaintAll();
        }
    }

    /**
     * Set the relative node size for the render. If {@code factor == 2}, the largest
     * element will be twice as large as the smallest. Any {@code factor <= 1} disables relative sizing.
     *
     * @param factor The min-max size factor for this render.
     */
    public void setRelativeNodeSize (double factor) {
        relativeNodeSize = factor > 1;

        if (relativeNodeSize && struct instanceof Array) {
            this.factor = factor;
            ((Array) struct).setListener(this);
        } else {
            restoreNodeSizes();
        }
    }

    public void setRelativeNodeSizes () {
        if (!relativeNodeSize) {
            return;
        }

        for (Node n : defaultNodePane.getChildren()) {
            if (n instanceof AVElement)
                this.setRelativeNodeSize((AVElement) n);
        }
    }

    private void setRelativeNodeSize (AVElement ave) {
        double lower = Math.abs(((Array) struct).getMin());
        double upper = Math.abs(((Array) struct).getMax());

        setRelativeNodeSize(ave, lower, upper);
    }

    /**
     * Set the size of a node relative its value in the range [L, H] using this renders current {@link #factor} value.
     *
     * @param ave The visual element to adjust the size for.
     * @param L The lower bound.
     * @param H The upper bound.
     */
    private void setRelativeNodeSize (AVElement ave, double L, double H) {

        if (L == H) {
            restoreNodeSizes();
            return;
        }

        double relNodeWidth, relNodeHeight;
        double r;
        double v = ave.getElement().getNumValue();

        r = ((v - L) / (H - L));
        r = render.assets.Tools.bindInRange(r, 0, 1);
        r = r * (factor - 1);

        relNodeWidth = nodeWidth / factor;
        relNodeHeight = nodeHeight / factor;

        relNodeWidth = relNodeWidth + relNodeWidth * r;
        relNodeHeight = relNodeHeight + relNodeHeight * r;

        ave.setSize(relNodeWidth, relNodeHeight);
    }

    /**
     * Retore the default node size for all nodes by calling {@link AVElement#setSize(double, double)}
     * with ({@link #nodeWidth}, {@link #nodeHeight}).
     */
    public void restoreNodeSizes () {
        for (Node n : defaultNodePane.getChildren()) {
            if (n instanceof AVElement)
                ((AVElement) n).setSize(nodeWidth, nodeHeight);
        }
    }

    /**
     * Returns the node height for this render.
     *
     * @return The node height for this render.
     */
    public double getNodeHeight () {
        return nodeHeight;
    }

    /**
     * Set the node height of the render and returns the result of {@link #repaintAll()}, if the value changed.
     * Returns @code false if the value was the same as before.
     */
    public boolean setNodeHeight (double nodeHeight) {
        this.nodeHeight = nodeHeight;
        return repaintAll();
    }

    public double getNodeWidth () {
        return nodeWidth;
    }

    public boolean setNodeWidth (double nodeWidth) {
        this.nodeWidth = nodeWidth;
        return repaintAll();
    }

    // ============================================================= //
    /*
     *
     * Sizing and moving.
     *
     */
    // ============================================================= //

    private double transX, transY;
    private final double scale = 1;

    /**
     * Create listeners to drag and zoom.
     */
    private void initDragAndZoom () {
        initArrowResize();
        initMouseWheelResize();
        initDrag();
    }

    public void initArrowResize () {
        setOnKeyPressed(event -> {
            if (!event.isControlDown()) {
                return;
            }

            switch (event.getCode()) {
                case UP:
                    setNodeHeight(nodeHeight + Const.DEFAULT_ELEMENT_HEIGHT_DELTA);
                    break;
                case DOWN:
                    setNodeHeight(nodeHeight - Const.DEFAULT_ELEMENT_HEIGHT_DELTA);
                    break;
                case LEFT:
                    setNodeWidth(nodeWidth - Const.DEFAULT_ELEMENT_WIDTH_DELTA);
                    break;
                case RIGHT:
                    setNodeWidth(nodeWidth + Const.DEFAULT_ELEMENT_WIDTH_DELTA);
                    break;
                default:
                    return;
            }

            setNodeWidth(nodeWidth < Const.MIN_NODE_WIDTH ? Const.MIN_NODE_WIDTH : nodeWidth);
            setNodeHeight(nodeHeight < Const.MIN_NODE_HEIGHT ? Const.MIN_NODE_HEIGHT : nodeHeight);

            Platform.runLater(ARender.this::requestFocus);

            repaintAll();
        });
    }

    private void initMouseWheelResize () {
        setOnScroll(event -> {
            if (!event.isControlDown()) {
                return;
            }

            int sign = event.getDeltaY() < 0 ? -1 : 1;

            setNodeWidth(nodeWidth + sign * Const.DEFAULT_ELEMENT_WIDTH_DELTA);
            setNodeHeight(nodeHeight + sign * Const.DEFAULT_ELEMENT_HEIGHT_DELTA);

            hSpace = hSpace + sign * Const.DEFAULT_ELEMENT_HSPACE_DELTA;
            vSpace = vSpace + sign * Const.DEFAULT_ELEMENT_VSPACE_DELTA;

            setNodeWidth(nodeWidth < Const.MIN_NODE_WIDTH ? Const.MIN_NODE_WIDTH : nodeWidth);
            setNodeHeight(nodeHeight < Const.MIN_NODE_HEIGHT ? Const.MIN_NODE_HEIGHT : nodeHeight);

            repaintAll();
        });
    }

    public void initDrag () {
        // Record a delta distance for the drag and drop operation.
        setOnMousePressed(event -> {
            transX = getTranslateX() - event.getSceneX();
            transY = getTranslateY() - event.getSceneY();
            setCursor(Cursor.CLOSED_HAND);
        });
        // Restore cursor
        setOnMouseReleased(event -> setCursor(null));
        // Translate canvases
        setOnMouseDragged(event -> {
            setTranslateX(event.getSceneX() + transX);
            setTranslateY(event.getSceneY() + transY);
            updateInfoLabels();
        });
        // Set cursor
        setOnMouseEntered(event -> {
            requestFocus();
            if (header.visibleProperty().isBound()) {
                name.setVisible(false);
            }
            setBorder(Const.BORDER_MOUSEOVER);
        });
        setOnMouseExited(event -> {
            // this.setCursor(null);
            if (header.visibleProperty().isBound()) {
                name.setVisible(true);
            }
            setBorder(null);
        });
    }

    // ============================================================= //
    /*
     *
     * Abstract methods
     *
     */
    // ============================================================= //

    /**
     * Returns the absolute x-coordinate of an element. Returns -1 if the calculation
     * fails.
     *
     * @param e An element to resolve coordinates for.
     * @return The absolute x-coordinate of the element.
     */
    public abstract double getX (Element e);

    /**
     * Returns the absolute y-coordinate of an element. Returns -1 if the calculation
     * fails.
     *
     * @param e An element to resolve coordinates for.
     * @return The absolute y-coordinate of the element.
     */
    public abstract double getY (Element e);

    /**
     * Order the render to calculate it's size.
     */
    public abstract void calculateSize ();

    /**
     * Create a bound node element in whatever style the Render prefers to use.
     *
     * @param e The element to bind.
     * @return A new bound VisualElement.
     */
    protected abstract AVElement createVisualElement (Element e);

    /**
     * Create an unbound node element in whatever style the Render prefers to use.
     *
     * @param value The value of the element.
     * @param color The colour of the element.
     * @return A new unbound VisualElement.
     */
    protected abstract AVElement createVisualElement (double value, Color color);

    /**
     * Decorator method used to attach bells and whistles to the current element.
     * {@link #repaintAll()} will called this method on every element.
     *
     * @param e The element to attach a whistle to.
     * @param ve The VisualElement to attach a bell to.
     */
    protected abstract void bellsAndWhistles (Element e, AVElement ve);

    // ============================================================= //
    /*
     *
     * Interface methods
     *
     */
    // ============================================================= //

    @Override
    public void maxChanged (double newMax) {
        setRelativeNodeSizes();
    }

    @Override
    public void minChanged (double newMin) {
        setRelativeNodeSizes();
    }
}
