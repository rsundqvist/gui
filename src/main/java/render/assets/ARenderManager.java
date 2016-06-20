package render.assets;

import contract.datastructure.DataStructure;
import contract.datastructure.DataStructure.VisualListener;
import contract.datastructure.VisualType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import render.ARender;

import java.util.HashMap;

/**
 * Class maintaining visualisations for a structure.
 *
 * @author Richard Sundqvist
 */
public class ARenderManager extends BorderPane implements VisualListener {

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    /**
     * The data structure this thingy is responsible for.
     */
    private final DataStructure struct;

    /**
     * The pane used for animation.
     */
    private final Pane animPane;

    /**
     * Mapping of renders for the structure.
     */
    private final HashMap<VisualType, ARender> renders = new HashMap<>();

    /**
     * The current render for the structure.
     */
    private ARender currentRender;

    // Used to maintain settings when changing renders.
    private ARender previousRender;

    private boolean translateOnVisualTypeChange = true;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    /**
     * Create a new thingy.
     *
     * @param struct The data structure being visualized.
     * @param animation_container Container for animation.
     */
    public ARenderManager (DataStructure struct, Pane animation_container) {
        struct.resolveVisual();

        this.struct = struct;
        animPane = animation_container;
        setPickOnBounds(false); // Mouse fix.

        setRender(struct.visual);
    }

    // ============================================================= //
    /*
     *
     * Setters and Getters
     *
     */
    // ============================================================= //

    /**
     * Set the visual type to use for this Structure.
     *
     * @param type The type to use.
     */
    public void setRender (VisualType type) {
        currentRender = renders.get(type);

        if (currentRender == null) { // Create new render for the structure.
            // @formatter:off
            currentRender = ARenderFactory.resolveRender(struct, render.assets.Const.DEFAULT_ELEMENT_WIDTH, render.assets.Const.DEFAULT_ELEMENT_HEIGHT,
                    render.assets.Const.DEFAULT_RENDER_WIDTH, render.assets.Const.DEFAULT_RENDER_HEIGHT);
            // @formatter:on
            renders.put(struct.resolveVisual(), currentRender);
        }

        struct.setVisualListener(this);

        initRender();
        setCenter(currentRender);
        if (type == VisualType.single) {
            toFront(); // Single element renders are small.
        }
    }

    /**
     * Returns the current Render for the structure.
     *
     * @return The current Render for the structure.
     */
    public ARender getRender () {
        if (currentRender == null) {
            setRender(struct.resolveVisual());
        }
        return currentRender;
    }

    /**
     * The data structure this thingy is responsible for.
     *
     * @return A DataStructure.
     */
    public DataStructure getDataStructure () {
        return struct;
    }

    @Override
    public String toString () {
        return struct.identifier + ": " + renders.values();
    }

    // ============================================================= //
    /*
     *
     * Controls
     *
     */
    // ============================================================= //

    private void initRender () {
        if (translateOnVisualTypeChange && previousRender != null) {
            double scaleX = previousRender.getScaleX();
            double scaleY = previousRender.getScaleY();
            double translateX = previousRender.getTranslateX();
            double translateY = previousRender.getTranslateY();
            double layoutX = previousRender.getLayoutX();
            double layoutY = previousRender.getLayoutY();

            currentRender.setScaleX(scaleX);
            currentRender.setScaleX(scaleY);
            currentRender.setTranslateX(translateX);
            currentRender.setTranslateY(translateY);
            currentRender.setLayoutX(layoutX);
            currentRender.setLayoutY(layoutY);
        }

        currentRender.repaintAll();
        currentRender.updateInfoLabels();
        animPane.getChildren().remove(currentRender.getAnimationPane());
        animPane.getChildren().add(currentRender.getAnimationPane());
        previousRender = currentRender;
    }

    /**
     * Force the current Render to initialise.
     */
    public void init () {
        currentRender.repaintAll();
    }

    /**
     * Reset the renders held by this manager.
     */
    public void reset () {
        renders.values().forEach(ARender::reset);
    }

    /**
     * Set the relative node size for the all renders. If {@code factor == 2}, the largest
     * element will be twice as large as the smallest. Relation is inversed for
     * {@code 0 < factor < 1}.<br>
     * <br>
     * Will disable for {@code factor <= 0} and {@code factor == 1}
     *
     * @param factor The min-max size factor for this render.
     */
    public void setRelativeNodeSize (double factor) {
        for (ARender render : renders.values()) {
            render.setRelativeNodeSize(factor);
        }
    }

    // ============================================================= //
    /*
     *
     * Interface methods
     *
     */
    // ============================================================= //

    @Override
    public void visualChanged (VisualType newVisual) {
        setRender(newVisual);
    }
}
