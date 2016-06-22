package assets;

import contract.datastructure.DataStructure;
import contract.datastructure.Element;
import contract.datastructure.IndexedElement;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import render.ARender;
import render.Visualization;
import render.assets.ARenderManager;
import render.assets.HintPane;
import render.element.AVElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Utility class to reduce clutter.
 *
 * @author Richard Sundqvist
 */
public abstract class Tools {

    private Tools () {
    } // Not to be instantiated.

    // A FXML pane showing user instructions.
    public static final HintPane HINT_PANE = new HintPane();

    /**
     * Tries to simplify the variable name. For example,
     * {@code "package.subpackage.class:var"} becomes {@code "var"}.
     *
     * @param orig A string to simplify.
     * @return A simplified variable name (hopefully).
     */
    public static String stripQualifiers (String orig) {
        String a[] = orig.split("\\p{Punct}");
        a = orig.split(" ");
        return a[a.length - 1];
    }
}
