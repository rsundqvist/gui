package gui.panel;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import contract.json.Operation;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import model.ExecutionTickListener;
import render.Visualization;
import render.assets.VisualController;

/**
 *
 * @author Richard Sundqvist
 *
 */
public class ControlPanel extends Pane implements ExecutionTickListener {

    private static final int          tickCount = 100;

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    private final VisualController    visualController;
    private final Visualization       visualization;
    private final ProgressBar         animationProgress;

    // Model progress list + related items.
    private final ProgressBar         modelProgress;
    private final ListView<Operation> operationList;
    private final TextField           listSizeLabel;
    private final TextField           currentOperationLabel;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    /**
     * Create a new ControlPanel
     *
     * @param executionModelController
     *            Used to control the model
     * @param visualization
     *            Used to control visualization.
     */
    @SuppressWarnings("unchecked")
    public ControlPanel (VisualController executionModelController, Visualization visualization) {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/panel/ControlPanel.fxml"));
        fxmlLoader.setController(this);

        Pane root = null;
        try {
            root = (Pane) fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        getChildren().add(root);

        visualController = executionModelController;
        visualController.getModelController().setExecutionTickListener(this, tickCount);
        this.visualization = visualization;

        visualController.getModelController().getExecutionModel().indexProperty()
                .addListener((InvalidationListener) observable -> {
                    int index = visualController.getModelController().getExecutionModel().indexProperty().get();
                    updateOperationOverview(index);
                });

        // ============================================================= //
        /*
         * Initialise namespace items.
         */
        // ============================================================= //

        Map<String, Object> namespace = fxmlLoader.getNamespace();

        animationProgress = (ProgressBar) namespace.get("animationProgress");

        Slider speedSlider = (Slider) namespace.get("speedSlider");
        speedSlider.setValue(-visualController.getAutoExecutionSpeed());
        speedSlider.setOnMouseReleased(event -> {
            visualController.setAutoExecutionSpeed(Math.abs((long) speedSlider.getValue()));
        });

        // Panel sizing.
        root.prefWidthProperty().bind(widthProperty());
        root.prefHeightProperty().bind(heightProperty());

        CheckBox animate = (CheckBox) namespace.get("animate");
        animate.setSelected(visualization.getAnimate());

        // ============================================================= //
        /*
         * Binding
         */
        // ============================================================= //

        Button play = (Button) namespace.get("play");
        play.disableProperty()
                .bind(visualController.getModelController().getExecutionModel().executeNextProperty().not());

        visualController.getModelController().autoExecutingProperty()
                .addListener((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {

                    if (newValue) {
                        play.setText("Pause");
                    } else {
                        play.setText("Play");
                    }
                });

        Button forward = (Button) namespace.get("forward");
        forward.disableProperty()
                .bind(visualController.getModelController().getExecutionModel().executeNextProperty().not());

        Button back = (Button) namespace.get("back");
        back.disableProperty()
                .bind(visualController.getModelController().getExecutionModel().executePreviousProperty().not());

        // Operation progress bar
        operationList = (ListView<Operation>) namespace.get("operationList");
        modelProgress = (ProgressBar) namespace.get("modelProgress");
        listSizeLabel = (TextField) namespace.get("listSizeLabel");
        currentOperationLabel = (TextField) namespace.get("currentOperationLabel");
        ObservableList<Operation> obsList = visualController.getModelController().getExecutionModel().getOperations();
        operationList.setItems(obsList);

    }

    // ============================================================= //
    /*
     *
     * Control / FXML onAction()
     *
     */
    // ============================================================= //

    public void play () {
        visualController.toggleAutoExecution();
    }

    public void forward () {
        visualController.executeNext();
    }

    public void back () {
        visualController.executePrevious();
    }

    public void restart () {
        visualController.reset();
    }

    public void clear () {
        visualController.clear();
    }

    public void toggleAnimate (Event e) {
        CheckBox cb = (CheckBox) e.getSource();
        visualization.setAnimate(cb.isSelected());
    }

    public void oooooOOoooOOOooooOOooo (Event e) {
        Button b = (Button) e.getSource();
        b.setOpacity(0.05);

        // https://www.youtube.com/watch?v=inli9ukUKIs
        URL resource = this.getClass().getResource("/assets/oooooOOoooOOOooooOOooo.mp3");
        Media media = new Media(resource.toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.play();
    }

    public void textFieldGoto (Event e) {
        TextField tf = (TextField) e.getSource();

        String input = tf.getText();
        try {
            int index = Integer.parseInt(input) - 1;
            visualController.execute(index);
        } catch (Exception exc) {
            tf.setText(visualController.getModelController().getExecutionModel().getIndex() + "");
            URL resource = this.getClass().getResource("/assets/shortcircuit.mp3");
            Media media = new Media(resource.toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(0.1);
            mediaPlayer.play();
        }

    }

    @SuppressWarnings("rawtypes")
    public void listViewGoto (Event e) {
        ListView lw = (ListView) e.getSource();

        int index = lw.getSelectionModel().getSelectedIndex() - 1;
        visualController.execute(index);
    }

    // ============================================================= //
    /*
     *
     * Interface
     *
     */
    // ============================================================= //

    @Override
    public void tickUpdate (int tickNumber) {
        animationProgress.setProgress((double) tickNumber / tickCount);
    }

    private void updateOperationOverview (Number index) {
        int currOp = index.intValue() + 1;
        int totOps = operationList.getItems().size();

        operationList.getFocusModel().focus(currOp - 3);
        operationList.scrollTo(currOp - 3);
        operationList.getSelectionModel().select(currOp - 1);

        modelProgress.setProgress((double) currOp / totOps);

        currentOperationLabel.setText(currOp + "");
        listSizeLabel.setText(totOps + "");
    }
}
