package render.assets;

import java.util.List;

import contract.json.Operation;
import model.ExecutionModel;
import model.ModelController;
import model.OperationsExecutedListener;
import render.Visualization;

/**
 * ExecutionModel convenience class.
 *
 * @author Richard Sundqvist
 *
 */
public class VisualController implements OperationsExecutedListener {

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    /**
     * The model this controller is responsible for.
     */
    private final ModelController modelController;

    /**
     * The visualization used to animate the model.
     */
    private final Visualization   visualization;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    /**
     * Create a new model controller.
     *
     * @param executionModel
     *            The model to control.
     * @param visualization
     *            The visualization used to animate.
     */
    public VisualController (ModelController executionModel, Visualization visualization) {
        this.modelController = executionModel;
        this.visualization = visualization;

        executionModel.getModel().addOperationsExecutedListener(this);
    }

    /**
     * Create a new model controller for {@link ExecutionModel#INSTANCE}.
     *
     */
    public VisualController () {
        this(new ModelController(ExecutionModel.INSTANCE), new Visualization(ExecutionModel.INSTANCE));
    }

    // ============================================================= //
    /*
     *
     * Control - added functionality
     *
     */
    // ============================================================= //

    /**
     * Toggle automatic execution.
     */
    public void toggleAutoExecution () {
        modelController.toggleAutoExecution();
    }

    /**
     * Begin timed execution for the model.
     */
    public void startAutoExecution () {
        modelController.startAutoExecution();
    }

    @Override
    public void operationsExecuted (List<Operation> executedOperations) {
        executedOperations.forEach(visualization::render);
        executedOperations.clear();
    }

    /**
     * Stop timed execution for the model.
     */
    public void stopAutoExecution () {
        modelController.stopAutoExecution();
    }

    // ============================================================= //
    /*
     *
     * Setters and Getters
     *
     */
    // ============================================================= //

    /**
     * Set the auto execution speed in milliseconds. Will stop auto execution, update the
     * speed, and resume if auto execution was on when this method was called.
     *
     * @param autoExecutionSpeed
     *            The time between execution calls in milliseconds.
     * @throws IllegalArgumentException
     *             If {@code autoExecutionSpeed < 0}.
     */
    public void setAutoExecutionSpeed (long autoExecutionSpeed) {
        if (autoExecutionSpeed < 0) {
            throw new IllegalArgumentException("Time between executions cannot be less than zero.");
        }

        modelController.setAutoExecutionSpeed(autoExecutionSpeed);
        visualization.setAnimationTime(autoExecutionSpeed);
    }

    /**
     * Returns the time between execution calls in milliseconds.
     *
     * @return The time between execution calls in milliseconds.
     */
    public long getAutoExecutionSpeed () {
        return modelController.getAutoExecutionSpeed();
    }

    /**
     * Returns the model controller for this visual controller.
     *
     * @return An {@code ExecutionModel}.
     */
    public ModelController getModelController () {
        return modelController;
    }

    /**
     * Returns the Visualization used by this controller.
     *
     * @return A Visualization.
     */
    public Visualization getVisualization () {
        return visualization;
    }

    /**
     * @see model.ExecutionModel#executeNext()
     */
    public void executeNext () {
        modelController.executeNext();
    }

    /**
     * @see model.ExecutionModel#executePrevious()
     */
    public void executePrevious () {
        modelController.executePrevious();
    }

    public void execute (int toIndex) {
        modelController.execute(toIndex);
    }

    /**
     *
     * @see model.ExecutionModel#reset()
     */
    public void reset () {
        visualization.reset();
        modelController.reset();
    }

    /**
     *
     * @see model.ExecutionModel#clear()
     */
    public void clear () {
        visualization.clear();
        modelController.clear();
    }

}
