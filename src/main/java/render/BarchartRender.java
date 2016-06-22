package render;

import render.assets.Const;
import contract.datastructure.Array;
import contract.datastructure.Array.MinMaxListener;
import contract.datastructure.DataStructure;
import contract.datastructure.Element;
import contract.datastructure.IndexedElement;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import render.ARenderAnimation.Effect;
import render.element.AVElement;
import render.element.AVElementFactory;
import render.element.BarchartElement;
import render.element.ElementShape;

import java.util.Arrays;

public class BarchartRender extends ARender implements MinMaxListener {

    public static final ElementShape ELEMENT_STYLE = ElementShape.BAR_ELEMENT;

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    private double renderHeight;

    private final double padding;

    private double xAxisY;

    private double rightWallX;

    private final Pane axes = new Pane();

    // ============================================================= //
    /*
     *
     * Constructor
     *
     */
    // ============================================================= //

    /**
     * Create a new BarchartRender.
     *
     * @param struct The structure to render.
     * @param nodeWidth Width of the bars.
     * @param renderHeight Height of the Render itself.
     * @param nodeHeight The height of the bars per unit.
     * @param hSpace Space between bars.
     */
    public BarchartRender (DataStructure struct, double nodeWidth, double renderHeight, double nodeHeight,
                           double hSpace) {
        super(struct, nodeWidth, nodeHeight, hSpace, 0);

        // Convenient names
        padding = nodeWidth / 2;

        // Axes
        axes.setMouseTransparent(true);
        contentPane.getChildren().add(axes);

        if (renderHeight < 0) {
            if (struct instanceof Array) {
                ((Array) struct).setListener(this);
                calculateHeight(((Array) struct).getMax());
                // permit.
            } else {
                this.renderHeight = Const.DEFAULT_RENDER_HEIGHT;
            }
        } else {
            this.renderHeight = renderHeight;
        }

        this.setRelativeNodeSize(-1);
        reset();
    }

    // ============================================================= //
    /*
     *
     * Superclass Implementations
     *
     */
    // ============================================================= //

    @Override
    public double getX (Element e) {
        if (e == null || !(e instanceof IndexedElement)) {
            return -1;
        }
        int[] index = ((IndexedElement) e).getIndex();
        if (index == null || index.length == 0) {
            System.err.println("Invalid index for element " + e + " in \"" + struct + "\".");
            renderFailure();
            return -1;
        }
        return this.getX(index[0]);
    }

    private double getX (int index) {
        return (nodeWidth + hSpace) * index + hSpace + padding + 5;
    }

    @Override
    public double getY (Element e) {
        return xAxisY + 100;
    }

    @Override
    public void render () {
        if (struct.isRepaintAll()) {
            struct.setRepaintAll(false);
            repaintAll();
        }
        super.render();
    }

    @Override
    public boolean repaintAll () {
        if (struct.getElements().isEmpty() || contentPane == null) {
            return false; // Nothing to render/not yet initialised.
        }
        struct.setRepaintAll(false);

        /*
         * Clear the nodes from all content Panes.
         */
        for (Node n : contentPane.getChildren()) {
            ((Pane) n).getChildren().clear();
        }
        contentPane.setBackground(null);

        visualMap.clear();

        calculateSize();

        // Create nodes
        BarchartElement newVis;

        for (Element e : struct.getElements()) {
            newVis = this.createVisualElement(e);
            newVis.setLayoutX(this.getX(e));

            defaultNodePane.getChildren().add(newVis);
            visualMap.put(Arrays.toString(((IndexedElement) e).getIndex()), newVis);
            bellsAndWhistles(e, newVis);
        }
        positionBars();
        drawAxes();
        return true;
    }

    // ============================================================= //
    /*
     *
     * Utility
     *
     */
    // ============================================================= //

    private void positionBars () {
        for (Node node : defaultNodePane.getChildren()) {
            ((BarchartElement) node).setBotY(xAxisY + 5);
        }
    }

    /**
     * Render the axes.
     */
    private void drawAxes () {
        // if (axes.getChildren().isEmpty()) {
        /*
         * X-Axis
         */
        Line xAxis = new Line(0, xAxisY, rightWallX + 5, xAxisY);
        xAxis.setStrokeWidth(2);
        axes.getChildren().add(xAxis);

        Polyline xArrow = new Polyline(0, 0, 15, 5, 0, 10);
        xArrow.setLayoutX(renderWidth - 20);
        xArrow.setLayoutY(xAxisY - 5);
        xArrow.setStrokeWidth(2);
        axes.getChildren().add(xArrow);

        Label xLabel = new Label("Value");
        xLabel.setLayoutX(padding * 1.5);
        xLabel.setLayoutY(-7);
        axes.getChildren().add(xLabel);

        /*
         * Y-Axis
         */
        Line yAxis = new Line(padding, padding / 2, padding, renderHeight);
        yAxis.setStrokeWidth(2);
        axes.getChildren().add(yAxis);
        notches();
        struct.getElements().forEach(this::createIndexLabel);

        Polyline yArrow = new Polyline(0, 15, 5, 0, 10, 15);
        yArrow.setStrokeWidth(2);
        yArrow.setLayoutX(padding - 5);
        yArrow.setLayoutY(0);
        axes.getChildren().add(yArrow);

        Label yLabel = new Label("Index");
        yLabel.setLayoutX(rightWallX);
        yLabel.setLayoutY(xAxisY + 2);
        axes.getChildren().add(yLabel);

        // showDeveloperGuides();
    }

    /**
     * Draw developer guides where the bar roof, x-axis, y-axis and rightmost limit should
     * be.
     */
    public void drawDeveloperGuides () {

        Line roof = new Line(padding, padding, rightWallX, padding);
        roof.setStroke(Color.HOTPINK);
        roof.setStrokeWidth(2);
        roof.getStrokeDashArray().addAll(20.0);

        Line floor = new Line(padding, xAxisY, rightWallX, xAxisY);
        floor.setStrokeWidth(2);
        floor.setStroke(Color.HOTPINK);
        floor.getStrokeDashArray().addAll(20.0);

        Line left = new Line(padding, padding, padding, xAxisY);
        left.setStroke(Color.HOTPINK);
        left.setStrokeWidth(2);
        left.getStrokeDashArray().addAll(20.0);

        Line right = new Line(renderWidth - padding, padding, renderWidth - padding, xAxisY);
        right.setStroke(Color.HOTPINK);
        right.setStrokeWidth(2);
        right.getStrokeDashArray().addAll(20.0);

        Pane guides = new Pane();
        guides.getChildren().addAll(roof, floor, left, right);
        guides.setOpacity(0.9);
        contentPane.getChildren().add(guides);
    }

    private void notches () {
        double lim = padding / 2 + nodeHeight;
        int i = 1;

        for (double y = xAxisY - nodeHeight; y > lim; y = y - nodeHeight) {
            // Notch
            Line line = new Line(padding - 3, y, padding + 3, y);
            axes.getChildren().add(line);

            // Value
            Label value = new Label();
            value.setLayoutY(lim);
            value.setLayoutY(y - 10);

            value.setText(i++ + "");

            axes.getChildren().add(value);
        }
    }

    @Override
    public void calculateSize () {
        renderWidth = struct.getElements().size() * (nodeWidth + hSpace) + padding * 3;
        xAxisY = renderHeight - padding;
        rightWallX = renderWidth - padding;
        renderHeight = renderHeight < 100 ? 100 : renderHeight;
        setRestrictedSize(renderWidth, renderHeight);
    }

    @Override
    protected BarchartElement createVisualElement (Element e) {
        return (BarchartElement) AVElementFactory.shape(ELEMENT_STYLE, e, nodeWidth, nodeHeight * e.getNumValue());
    }

    @Override
    protected AVElement createVisualElement (double value, Color color) {
        return AVElementFactory.shape(ELEMENT_STYLE, value, color, nodeWidth, nodeHeight * value);
    }

    @Override
    protected void bellsAndWhistles (Element e, AVElement ve) {
        ((BarchartElement) ve).updateSize(nodeHeight, -1);
    }

    private void createIndexLabel (Element e) {
        int[] index = ((IndexedElement) e).getIndex();
        Label info = new Label();
        info.setLayoutY(xAxisY);
        info.setLayoutX(this.getX(e) + 5);
        // info.setLayoutX(100);
        info.setText(Arrays.toString(index));

        info.setMouseTransparent(true);

        axes.getChildren().add(info);
    }

    /**
     * Have to override since elements are translated to position them in the bar.
     *
     * @param e An element owned by this BarcharRender.
     * @return The absolute y-coordinates of e.
     */
    @Override
    public double absY (Element e, ARender relativeTo) {
        double by = getTranslateY() + getLayoutY() + contentPane.getLayoutY();
        return xAxisY + by;
    }

    /**
     * Custom animation for read operations with only one locator.
     *
     * @param src The source element.
     * @param srcRender The render for the source element.
     * @param tar The target element.
     * @param tarRender The render for the target element.
     * @param millis The time in milliseconds the animation should last.
     */
    @Override
    public void animateReadWrite (Element src, ARender srcRender, Element tar, ARender tarRender, long millis) {
        if (tar != null || src == null) {
            super.animateReadWrite(src, srcRender, tar, tarRender, millis);
            return;
        }

        double x = absX(src, tarRender);
        double y1 = absY(src, tarRender);

        double y2 = y1 - nodeWidth / 2;
        int[] i = ((IndexedElement) src).getIndex();
        Arrays.copyOf(i, i.length);
        final AVElement orig = visualMap.get(Arrays.toString(i));
        orig.setGhost(true);

        ParallelTransition up = ARenderAnimation.linear(src, x, y1, x, y2, millis / 3, this);
        ParallelTransition down = ARenderAnimation.linear(src, x, y2, x, y1, millis / 3, this, Effect.GHOST);

        SequentialTransition st = new SequentialTransition();
        st.getChildren().addAll(up, down);
        st.play();
    }

    @Override
    public void maxChanged (double newMax) {
        calculateHeight(newMax);
    }

    @Override
    public void minChanged (double newMin) {
        // Do nothing.
    }

    /**
     * Calculate the height of the render.
     *
     * @param v The maximum value of the array.
     */
    private void calculateHeight (double v) {
        double oldHeight = renderHeight;
        renderHeight = v * nodeHeight + padding * 2 + nodeHeight / 2;
        calculateSize();
        repaintAll();
        setTranslateY(getTranslateY() + (oldHeight - renderHeight));
    }

    @Override
    public void setRelativeNodeSize (double factor) {
        // Do nothing.
    }
}
