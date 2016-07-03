package model;

import assets.Debug;
import contract.datastructure.DataStructure;
import contract.operation.Key;
import contract.operation.OP_Message;
import contract.utility.OpUtil;
import contract.wrapper.Locator;
import contract.wrapper.Operation;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model used to estimate execution state of a group of observed variables.
 *
 * @author Richard Sundqvist
 */
public class ExecutionModel {

    /**
     * The default model instance.
     */
    public static final ExecutionModel INSTANCE = new ExecutionModel("INSTANCE");

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    /**
     * The map of data structures in this model.
     */
    private final ObservableMap<String, DataStructure> dataStructures;

    /**
     * The current list from which operations are currently executed.
     */
    private final ObservableList<Operation> currentExecutionList;

    /**
     * List returned to {@link #getOperations()} callers.
     */
    private final ObservableList<Operation> readOnlyCurrentExecutionList;

    /**
     * Current operation index.
     */
    private int index;

    /**
     * List permitting all kinds of operations.
     */
    private final ObservableList<Operation> mixedOperations;

    /**
     * List permitting atomic operations only.
     */
    private final ObservableList<Operation> atomicOperations;

    /**
     * Indicates whether the model is in atomic execution mode.
     */
    private boolean atomicExecution;


    /**
     * Indicates whether parallel execution is allowed.
     * If {@code true}, all operations with the same (non-negative) group number are executed at once.
     */
    private boolean parallelExecution;

    /**
     * The name of the model.
     */
    public final String name;

    /**
     * A list of the most recently executed operations.
     */
    private final ObservableList<Operation> executedOperations;

    /**
     * The operations executed listener for the model.
     */
    private final List<OperationsExecutedListener> operationsExecutedListeners;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    /**
     * Create a new ExecutionModel.
     *
     * @param name The name of the model.
     * @param parallelExecution If {@code true}, the model may execute several operations per step.
     * @param atomicExecution If {@code true}, the model will convert high-level into groups of atomic
     * operations.
     */
    public ExecutionModel (String name, boolean parallelExecution, boolean atomicExecution) {
        this.name = name;

        dataStructures = FXCollections.observableHashMap();

        currentExecutionList = FXCollections.observableArrayList();
        readOnlyCurrentExecutionList = FXCollections.unmodifiableObservableList(currentExecutionList);

        atomicOperations = FXCollections.observableArrayList();
        mixedOperations = FXCollections.observableArrayList();
        executedOperations = FXCollections.observableArrayList();

        operationsExecutedListeners = new ArrayList<>();

        setParallelExecution(parallelExecution);
        setAtomicExecution(atomicExecution);
        setIndex(-1);
    }

    /**
     * Create a new ExecutionModel. {@code parallelExecution} will be set to {@code true}
     * and {@code atomicExecution} will be set to {@code false}. .
     *
     * @param name The name of the model.
     */
    public ExecutionModel (String name) {
        this(name, true, false);
    }

    /**
     * Create a new ExecutionModel with a random name.
     *
     * @param parallelExecution If {@code true}, the model may execute several operations per step.
     * @param atomicExecution If {@code true}, the model will convert high-level into groups of atomic
     * operations.
     */
    public ExecutionModel (boolean parallelExecution, boolean atomicExecution) {
        this(Math.random() * Integer.MAX_VALUE + "", parallelExecution, atomicExecution);
    }

    /**
     * Create a new ExecutionModel with a random name. {@code parallelExecution} will be
     * set to {@code true}
     */
    public ExecutionModel () {
        this(Math.random() * Integer.MAX_VALUE + "");
    }

    // ============================================================= //
    /*
     *
     * Control
     *
     */
    // ============================================================= //

    /**
     * Execute the next operation, if possible.
     *
     * @return A list containing the executed operations.
     */
    public ObservableList<Operation> executeNext () {
        executedOperations.clear();

        if (parallelExecution) {
            executeParallel();
        } else {
            executeLinear();
        }

        notifyExecutedOperationsListeners();

        return executedOperations;
    }

    /**
     * Execute the previous operation, if possible.
     *
     * @return A list containing the executed operations.
     */
    public ObservableList<Operation> executePrevious () {

        if (tryExecutePrevious()) {
            int previousIndex = index - 2;
            reset();
            execute(previousIndex);
        }

        notifyExecutedOperationsListeners();
        return executedOperations;
    }

    private void notifyExecutedOperationsListeners () {
        List<Operation> executedOperations;
        for (OperationsExecutedListener oel : operationsExecutedListeners) {
            executedOperations = new ArrayList<>(this.executedOperations);
            oel.operationsExecuted(executedOperations);
        }
    }

    /**
     * Test to see if it is possible to execute the previous operation(s) in in the queue.
     *
     * @return {@code true} if the model can execute backwards, {@code false} otherwise.
     */
    public boolean tryExecutePrevious () {
        boolean tryExecutePrevious = index > 1 && index - 1 < currentExecutionList.size();

        executePreviousProperty.set(tryExecutePrevious);

        return tryExecutePrevious;
    }

    /**
     * Test to see if it is possible to execute the next operation(s) in in the queue.
     *
     * @return {@code true} if the model can execute forward, {@code false} otherwise.
     */
    public boolean tryExecuteNext () {
        boolean tryExecuteNext = index >= 0 && index + 1 < currentExecutionList.size();

        executeNextProperty.set(tryExecuteNext);
        tryExecutePrevious(); // Update backwards property.

        return tryExecuteNext;
    }

    /**
     * Execute the operation(s) up to and including the given index. If the index is lower
     * than the current index, the model will reset and play from the beginning. Will
     * execute to the end if {@code index} is greater than the number of operations in the
     * queue.
     *
     * @param toIndex The index to execute at.
     * @return A list containing the executed operations.
     */
    public ObservableList<Operation> execute (int toIndex) {
        executedOperations.clear();

        toIndex = toIndex < 0 ? 0 : toIndex;

        if (index == toIndex) {
            return executedOperations;
        }
        if (index < toIndex) {
            reset();
        }

        int targetIndex = toIndex < currentExecutionList.size() ? toIndex : currentExecutionList.size() - 1;

        for (int i = 0; i <= targetIndex; i++) {
            if (tryExecuteNext()) {
                nextOperation();
            }
        }

        return executedOperations;
    }

    /**
     * Reset the model.
     */
    public void reset () {
        dataStructures.values().forEach(DataStructure::clear);
        index = 0;
        updateProperties();
    }

    /**
     * Clear the model.
     */
    public void clear () {
        dataStructures.clear();
        currentExecutionList.clear();

        index = -1;
        updateProperties();
    }

    // ============================================================= //
    /*
     *
     * Model Progression
     *
     */
    // ============================================================= //

    /**
     * Perform execution in parallel mode.
     */
    private void executeParallel () {
        Operation operation = nextOperation();
        if (operation == null) {
            return; // Nothing was executed.
        }

        int group = operation.group;
        if (group < 0) {
            return; // Ignore groups with negative values.
        }

        do {
            execute();
            operation = nextOperation();
        } while (operation != null && operation.group == group);
    }

    /**
     * Perform execution in linear mode.
     */
    private void executeLinear () {
        Operation op = nextOperation();
        if (op != null) {
            execute();
        }
    }

    /**
     * Increment the index by one and fetch the operation from the {@link #currentExecutionList} for execution.
     */
    private void execute () {
        setIndex(index + 1);
        Operation op = currentExecutionList.get(index);
        execute(op);
    }

    /**
     * Returns the operation ahead of the current {@link #index}.
     *
     * @return The operation which is next in line to be executed.
     */
    public Operation nextOperation () {
        int index = this.index + 1;

        if (index < currentExecutionList.size()) {
            return currentExecutionList.get(index);
        }

        return null;
    }

    /**
     * Execute an operation. Argument {@code op} must not be null.
     *
     * @param op The operation to execute.
     * @return The operation which was executed if successful, {@code null} otherwise.
     */
    private Operation execute (Operation op) {
        if (Debug.OUT) {
            System.out.println("ExecutionModel: execute(): " + op);
        }

        switch (op.operation) {

            case message:
                // ============================================================= //
                /*
                * Message
                */
                // ============================================================= //

                // TODO: Callback mechanism.
                System.out.println("MESSAGE: " + ((OP_Message) op).getMessage());
                break;
            case read:
            case write:
                // ============================================================= //
                /*
                 * Read and Write
                 */
                // ============================================================= //
                Locator source = OpUtil.getLocator(op, Key.source);
                if (source != null) {
                    DataStructure sourceStruct = dataStructures.get(source.identifier);
                    if (sourceStruct != null) {
                        sourceStruct.applyOperation(op);
                    }
                }

                Locator target = OpUtil.getLocator(op, Key.target);
                if (target != null) {
                    DataStructure targetStruct = dataStructures.get(target.identifier);
                    if (targetStruct != null) {
                        targetStruct.applyOperation(op);
                    }
                }
                break;
            case swap:
                // ============================================================= //
                /*
                 * Swap
                 */
                // ============================================================= //
                Locator var1 = OpUtil.getLocator(op, Key.var1);
                dataStructures.get(var1.identifier).applyOperation(op);

                Locator var2 = OpUtil.getLocator(op, Key.var2);
                dataStructures.get(var2.identifier).applyOperation(op);
                break;
            case remove:
                // ============================================================= //
                /*
                 * TODO Fix after renaming remove.
                 */
                // ============================================================= //
                Locator removeTarget = OpUtil.getLocator(op, Key.target);
                DataStructure targetStruct = dataStructures.get(removeTarget.identifier);
                if (targetStruct != null) {
                    targetStruct.applyOperation(op);
                }
                break;
            default:
                System.err.print("Bad operation type: \"" + op.operation + "\"");
                return null;
        }
        executedOperations.add(op);
        return op;
    }

    // ============================================================= //
    /*
     *
     * Setters and Getters
     *
     */
    // ============================================================= //

    /**
     * Set the data structures ans operations for this model. Will
     * keep the current collection if the corresponding argument is {@code null}.
     *
     * @param dataStructures A map of data structures.
     * @param operations A list of operations.
     */
    public void set (Map<String, DataStructure> dataStructures, List<Operation> operations) {

        if (dataStructures != null) {
            setDataStructures(dataStructures);
        }
        if (operations != null) {
            setOperations(operations);
        }
    }

    /**
     * Set the data structures for this model.
     *
     * @param dataStructures A map of data structures.
     */
    public void setDataStructures (Map<String, DataStructure> dataStructures) {
        if (dataStructures != null) {
            this.dataStructures.clear();
            this.dataStructures.putAll(dataStructures);
            updateProperties();
        }
    }

    /**
     * Set the operations for this model.
     *
     * @param operations A list of operations.
     */
    public void setOperations (List<Operation> operations) {
        if (operations != null) {

            atomicOperations.setAll(OpUtil.asAtomicList(operations));
            mixedOperations.setAll(operations);

            if (atomicExecution) {
                currentExecutionList.setAll(atomicOperations);
            } else {
                currentExecutionList.setAll(mixedOperations);
            }

            updateProperties();
        }
    }

    /**
     * Returns the list of operations in use by this model as an unmodifiable instance.
     *
     * @return A list of operations.
     */
    public ObservableList<Operation> getOperations () {
        return readOnlyCurrentExecutionList;
    }

    /**
     * Returns the map of data structures in use by this model.
     *
     * @return A map of data structures.
     */
    public Map<String, DataStructure> getDataStructures () {
        return dataStructures;
    }

    /**
     * Set the parallel execution setting of this model.
     *
     * @param parallelExecution The new parallel execution setting.
     */
    public void setParallelExecution (boolean parallelExecution) {
        if (this.parallelExecution != parallelExecution) {

            this.parallelExecution = parallelExecution;
            parallelExecutionProperty.set(parallelExecution);
        }
    }

    /**
     * Returns the parallel execution setting of this model.
     *
     * @return {@code true} if parallel execution is enabled, {@code false} otherwise.
     */
    public boolean isParallelExecution () {
        return parallelExecution;
    }

    /**
     * Set the atomic execution setting for the model. If {@code true}, high level
     * operations such as swap will be replaced with their atomic components.
     *
     * @param atomicExecution The new atomic execution setting.
     */
    public void setAtomicExecution (boolean atomicExecution) {
        if (this.atomicExecution != atomicExecution) {

            this.atomicExecution = atomicExecution;
            atomicExecutionProperty.set(atomicExecution);

            int numAtomic;
            int offset = 0;
            int index = this.index;

            // Translate index
            for (int i = 0; i <= index; i++) {
                numAtomic = mixedOperations.get(i).operation.numAtomicOperations;

                if (numAtomic > 1) {
                    offset += numAtomic - 1;

                    if (atomicExecution) {
                        index += numAtomic - 1; // Increase index as well to ensure all operations are counted.
                    }
                }
            }

            if (atomicExecution) {
                currentExecutionList.setAll(atomicOperations);
                index += offset;
            } else {
                currentExecutionList.setAll(mixedOperations);
                index -= offset;
            }

            reset();
            execute(index);
            updateProperties();
        }
    }

    /**
     * Returns the atomic execution setting of this model.
     *
     * @return {@code true} if atomic execution is enabled, {@code false} otherwise.
     */
    public boolean isAtomicExecution () {
        return atomicExecution;
    }

    /**
     * Get the current execution index.
     *
     * @return The current execution index.
     */
    public int getIndex () {
        return index;
    }

    private void setIndex (int index) {
        if (currentExecutionList.isEmpty()) {
            index = -1;
        } else if (index > currentExecutionList.size()) {
            index = currentExecutionList.size();
        } else if (index < 0) {
            index = 0;
        }

        indexProperty.set(index);
        this.index = index;
    }

    /**
     * Add a listener to be called each time operation(s) are executed.
     *
     * @param operationsExecutedListener A {@code OperationsExecutedListener}.
     */
    public void addOperationsExecutedListener (OperationsExecutedListener operationsExecutedListener) {
        if (Debug.ERR) {
            System.err.println("operationsExecutedListener added: " + operationsExecutedListener);
        }
        operationsExecutedListeners.add(operationsExecutedListener);
    }

    // ============================================================= //
    /*
     *
     * Properties / Getters and Setters
     *
     */
    // ============================================================= //

    private final ReadOnlyBooleanWrapper parallelExecutionProperty = new ReadOnlyBooleanWrapper();
    private final ReadOnlyBooleanWrapper atomicExecutionProperty = new ReadOnlyBooleanWrapper();

    private final ReadOnlyBooleanWrapper executeNextProperty = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper executePreviousProperty = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper clearProperty = new ReadOnlyBooleanWrapper(true);

    private final ReadOnlyIntegerWrapper indexProperty = new ReadOnlyIntegerWrapper();

    /**
     * Force updating of all properties;
     */
    public void updateProperties () {
        isClear();
        tryExecuteNext();
        tryExecutePrevious();
        setIndex(index);
    }

    /**
     * Returns {@code true} if the model is clear, {@code false} otherwise.
     *
     * @return {@code true} if the model is clear, {@code false} otherwise.
     */
    private boolean isClear () {
        boolean isClear = dataStructures.isEmpty() && currentExecutionList.isEmpty();
        clearProperty.set(isClear);
        return isClear;
    }

    /**
     * Returns a property indicating whether this model is cleared.
     *
     * @return A ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty clearProperty () {
        return clearProperty.getReadOnlyProperty();
    }

    /**
     * Returns a property indicating whether this model is in parallel execution mode.
     *
     * @return A ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty parallelExecutionProperty () {
        return parallelExecutionProperty.getReadOnlyProperty();
    }

    /**
     * Returns a property indicating whether this model is in atomic execution mode.
     *
     * @return A ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty atomicExecutionProperty () {
        return atomicExecutionProperty.getReadOnlyProperty();
    }

    /**
     * Returns a property indicating whether this model is able to execute forwards from
     * the current index.
     *
     * @return A ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty executeNextProperty () {
        return executeNextProperty.getReadOnlyProperty();
    }

    /**
     * Returns a property indicating whether this model is able to execute backwards from
     * the current index.
     *
     * @return A ReadOnlyBooleanProperty.
     */
    public ReadOnlyBooleanProperty executePreviousProperty () {
        return executePreviousProperty.getReadOnlyProperty();
    }

    /**
     * Returns a property indicating which index this model is currently at.
     *
     * @return A ReadOnlyIntegerProperty.
     */
    public ReadOnlyIntegerProperty indexProperty () {
        return indexProperty.getReadOnlyProperty();
    }

}
