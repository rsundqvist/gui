package gui;

import assets.Const;
import assets.examples.Examples;
import assets.examples.Examples.Algorithm;
import gui.panel.ConsolePanel;
import gui.panel.ControlPanel;
import gui.panel.SourcePanel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import model.ExecutionModel;
import model.ModelController;
import render.Visualization;
import render.assets.VisualController;

import java.io.IOException;
import java.util.Map;

/**
 * Entry class for the GUI.
 */
public class Main extends Application {

    // ============================================================= //
    /*
     *
     * Field variables.
     *
     */
    // ============================================================= //
    /**
     * Console for printing system and error messages.
     */
    public static ConsolePanel console;

    /**
     * Controller class for the GUI.
     */
    private Controller controller;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    @Override
    public void start (Stage primaryStage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Root.fxml"));

        // ============================================================= //
        /*
         * Build panels and create controller
         */
        // ============================================================= //
        SourcePanel sourcePanel = new SourcePanel();
        Visualization visualization = new Visualization(ExecutionModel.INSTANCE);
        VisualController vc = new VisualController(new ModelController(ExecutionModel.INSTANCE), visualization);
        ControlPanel controlPanel = new ControlPanel(vc, visualization);

        controller = new Controller(primaryStage, sourcePanel, vc);
        fxmlLoader.setController(controller);

        // ============================================================= //
        /*
         * Load FXML root.
         */
        // ============================================================= //
        VBox root;
        try {
            root = fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // ============================================================= //
        /*
         * Create console.
         */
        // ============================================================= //
        Map<String, Object> namespace = fxmlLoader.getNamespace();
        console = new ConsolePanel((TextArea) namespace.get("console"));

        // ============================================================= //
        /*
         * Place panels
         */
        // ============================================================= //
        GridPane sourceContainer = (GridPane) namespace.get("source");
        sourceContainer.add(sourcePanel, 0, 0);

        GridPane visualizationContainer = (GridPane) namespace.get("visualization");
        visualizationContainer.add(visualization, 0, 0);

        GridPane controlContainer = (GridPane) namespace.get("control");
        controlContainer.add(controlPanel, 0, 0);

        // Load needed components of from main view in Controller.
        controller.loadFXML(fxmlLoader);

        // ============================================================= //
        /*
         * Load examples menu.
         */
        // ============================================================= //
        Menu examples = (Menu) namespace.get("examplesMenu");
        for (Algorithm algo : Examples.Algorithm.values()) {
            MenuItem algoButton = new MenuItem(algo.name);
            algoButton.setOnAction(event -> {
                controller.loadExample(algo);
            });
            examples.getItems().add(algoButton);
        }

        // ============================================================= //
        /*
         * Build and show.
         */
        // ============================================================= //
        primaryStage.setTitle(Const.PROGRAM_NAME);
        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // Better to do this now than missing it later.
            controller.closeProgram();
        });

        Rectangle2D screenSize = Screen.getPrimary().getVisualBounds();
        double windowWidth = screenSize.getWidth() * .9;
        double windowHeight = screenSize.getHeight() * .9;
        Scene scene = new Scene(root, windowWidth, windowHeight);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ============================================================= //
    /*
     *
     * The rest.
     *
     */
    // ============================================================= //

    @Override
    public void stop () {
        controller.closeProgram();
    }

    public static void main (String[] args) {
        launch(args);
    }
}
