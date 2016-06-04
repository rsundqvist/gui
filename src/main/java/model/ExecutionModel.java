package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import assets.Debug;
import contract.datastructure.DataStructure;
import contract.json.Locator;
import contract.json.Operation;
import contract.operation.HighLevelOperation;
import contract.operation.Key;
import contract.operation.OP_Message;
import contract.operation.OperationType;
import contract.utility.OpUtil;
import gui.Main;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 *
 * @author Richard Sundqvist
 *
 */
public class ExecutionModel {

    /**
     * The default model instance.
     */
    public static final ExecutionModel                 INSTANCE = new ExecutionModel("INSTANCE");

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
     * The current list from which operations are currently executed.<br>
     * <b>Atomic operations:</br>
     * {@link OperationType#read}<br>
     * {@link OperationType#write}<br>
     * {@link OperationType#message}<br>
     */
    private final ObservableList<Operation>            currentExecutionList;

    private final ObservableList<Operation>            readOnlyCurrentExecutionList;

    /**
     * List permitting all kinds of operations.
     */
    private final ObservableList<Operation>            unrstrOperations;

    /**
     * List permitting atomic operations only.
     */
    private final ObservableList<Operation>            atomicOperations;

    /**
     * Indicates whether the model is in atomic execution mode.
     */
    private boolean                                    atomicExecution;

    /**
     * Current operation index.
     */
    private int                                        index;

    /**
     * Indicates whether parallel execution is permitted.
     */
    private boolean                                    parallelExecution;

    /**
     * The name of the model.
     */
    public final String                                name;

    /**
     * A list of the most recently executed operations.
     */
    private final ObservableList<Operation>            executedOperations;

    /**
     * The operations executed listener for the model.
     */
    private final List<OperationsExecutedListener>     operationsExecutedListeners;

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
     * @param name
     *            The name of the model.
     * @param parallelExecution
     *            If {@code true}, the model may execute several operations per step.
     * @param atomicExecution
     *            If {@code true}, the model will convert high-level into groups of atomic
     *            operations.
     */
    public ExecutionModel (String name, boolean parallelExecution, boolean atomicExecution) {
        this.name = name;

        dataStructures = FXCollections.observableHashMap();

        currentExecutionList = FXCollections.observableArrayList();
        readOnlyCurrentExecutionList = FXCollections.unmodifiableObservableList(currentExecutionList);

        atomicOperations = FXCollections.observableArrayList();
        unrstrOperations = FXCollections.observableArrayList();
        executedOperations = FXCollections.observableArrayList();

        operationsExecutedListeners = new ArrayList<OperationsExecutedListener>();

        setParallelExecution(parallelExecution);
        setAtomicExecution(atomicExecution);
        setIndex(-1);
    }

    /**
     * Create a new ExecutionModel. {@code parallelExecution} will be set to {@code true}
     * and {@code atomicExecution} will be set to {@code false}. .
     *
     * @param name
     *            The name of the model.
     */
    public ExecutionModel (String name) {
        this(name, true, false);
    }

    /**
     * Create a new ExecutionModel with a random name.
     *
     * 
     * @param parallelExecution
     *            If {@code true}, the model may execute several operations per step.
     * @param atomicExecution
     *            If {@code true}, the model will convert high-level into groups of atomic
     *            operations.
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
        for (OperationsExecutedListener opl : operationsExecutedListeners) {
            executedOperations = new ArrayList<Operation>(this.executedOperations);
            opl.operationsExecuted(executedOperations);
        }
    }

    /**
     * Test to see if it is possible to execute the previous operation(s) in in the queue.
     *
     * @return {@code true} if the model can execute backwards, {@code false} otherwise.
     */
    public boolean tryExecutePrevious () {
        boolean tryExecutePrevious = index > 1 && index < currentExecutionList.size() + 1;
        executePreviousProperty.set(tryExecutePrevious);
        return tryExecutePrevious;
    }

    /**
     * Test to see if it is possible to execute the next operation(s) in in the queue.
     *
     * @return {@code true} if the model can execute forward, {@code false} otherwise.
     */
    public boolean tryExecuteNext () {
        boolean tryExecuteNext = index >= 0 && index + 1 < currentExecutionList.size()
                && !currentExecutionList.isEmpty();
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
     * @param toIndex
     *            The index to execute at.
     * @return A list containing the executed operations.
     */
    public ObservableList<Operation> execute (int toIndex) {
        executedOperations.clear();

        if (toIndex < 0) {
            toIndex = 0;
        }
        if (index == toIndex) {
            return executedOperations;
        }
        if (index < toIndex) {
            reset();
        }

        int targetIndex = toIndex < currentExecutionList.size() ? toIndex : currentExecutionList.size() - 1;

        for (int i = 0; i <= targetIndex; i++) {
            if (tryExecuteNext()) {
                execute();
            }
        }

        return executedOperations;
    }

    /**
     * Reset the model.
     */
    public void reset () {
        dataStructures.values().forEach(dataStructure -> {
            dataStructure.clear();
        });
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
     * Execute the
     */
    private void executeParallel () {
        // TODO: Implement parallel execution.
        executeLinear();
    }

    /**
     * Execute the next operation.
     */
    private void executeLinear () {
        execute();
    }

    /**
     * Execute the next operation in the queue and add it to {@link executedOperations}.
     */
    private void execute () {
        setIndex(index + 1);

        if (index >= 0 && index < currentExecutionList.size()) {
            Operation op = currentExecutionList.get(index);
            executedOperations.add(op);
            execute(op);
        }
    }

    /**
     * Execute an operation.
     *
     * @param op
     *            The operation to execute.
     */
    private void execute (Operation op) {
        switch (op.operation) {

        case message:
            // ============================================================= //
            /*
             * Message
             */
            // ============================================================= //
            // TODO: Callback mechanism.
            Main.console.info("MESSAGE: " + ((OP_Message) op).getMessage());
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
            System.err.print("Unknown operation type: \"" + op.operation + "\"");
            break;
        }

        if (Debug.OUT) {
            System.out.println("ExecutionModel: execute(): " + op);
        }
    }

    // ============================================================= //
    /*
     *
     * Setters and Getters
     *
     */
    // ============================================================= //

    /**
     * Set the data structures, operations, and atomic operations for this model. Will
     * keep the current collection if the corresponding argument is {@code null}.
     *
     * @param dataStructures
     *            A map of data structures.
     * @param operations
     *            A list of operations.
     * @param atomicOperations
     *            A list of atomic operations.
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
     * @param dataStructures
     *            A map of data structures.
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
     * @param operations
     *            A list of operations.
     */
    public void setOperations (List<Operation> operations) {
        if (operations != null) {

            atomicOperations.setAll(asAtomic(operations));
            unrstrOperations.setAll(operations);

            if (atomicExecution) {
                currentExecutionList.setAll(atomicOperations);
            } else {
                currentExecutionList.setAll(unrstrOperations);
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
     * Returns the parallel execution setting of this model.
     *
     * @return {@code true} if parallel execution is enabled, false otherwise.
     */
    public boolean isParallelExecution () {
        return parallelExecution;
    }

    /**
     * Set the parallel execution setting of this model.
     *
     * @param parallelExecution
     *            The new parallel execution setting.
     */
    public void setParallelExecution (boolean parallelExecution) {
        if (this.parallelExecution != parallelExecution) {

            this.parallelExecution = parallelExecution;
            parallelExecutionProperty.set(parallelExecution);
        }
    }

    /**
     * Set the atomic execution setting for the model. If {@code true}, high level
     * operations such as swap will be replaced with their atomic components.
     * 
     * @param atomicExecution
     *            The new atomic execution setting.
     */
    public void setAtomicExecution (boolean atomicExecution) {
        if (this.atomicExecution != atomicExecution) {

            this.atomicExecution = atomicExecution;
            atomicExecutionProperty.set(atomicExecution);

            int preSwitchIndex = index;
            
            if (atomicExecution) {
                currentExecutionList.setAll(atomicOperations);

                Operation op;
                int numAtom;
                for (int i = 0; i <= preSwitchIndex; i++) {
                    op = unrstrOperations.get(i);
                    numAtom = op.operation.numAtomicOperations;
                    
                    if (numAtom > 1) {
                        index = index + (numAtom - 1);
                    }
                }
            } else {
                currentExecutionList.setAll(unrstrOperations);

                int numAtom;
                Operation op;
                for (int i = 0; i <= preSwitchIndex; i++) {
                    op = unrstrOperations.get(i);
                    numAtom = op.operation.numAtomicOperations;
                    
                    if (numAtom > 1) {
                        index = index - (numAtom - 1);
                    }
                }
            }

            updateProperties();
        }
    }

    /**
     * Converts any {@link #HighLevelOperation} found into a group of low level
     * operations.
     * 
     * @param mixedList
     *            A list of atomic and high level operations.
     * @return A list a atomic operations.
     */
    public List<Operation> asAtomic (List<Operation> mixedList) {
        List<Operation> answer = new ArrayList<Operation>(mixedList.size() * 2);

        for (Operation op : mixedList) {
            if (op.operation.numAtomicOperations > 1) {
                HighLevelOperation hlo = (HighLevelOperation) op;

                if (hlo.atomicOperations == null || hlo.atomicOperations.size() != hlo.operation.numAtomicOperations) {
                    System.err.println("WARNING: Bad atomic operations list: " + hlo.atomicOperations + " in " + hlo);
                    answer.add(hlo);
                } else {
                    answer.addAll(hlo.atomicOperations);
                }

            } else {
                answer.add(op);
            }
        }

        return answer;
    }

    /**
     * Get the current execution index. Return value may vary from the value returned by
     * {@link getAtomicIndex()} if {@code parallelExecution} is set to {@code true}.
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

        indexPropery.set(index);
        this.index = index;
    }

    /**
     * Add a listener to be called each time operation(s) are executed.
     *
     * @param operationsExecutedListener
     *            A {@code OperationsExecutedListener}.
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
    private final ReadOnlyBooleanWrapper atomicExecutionProperty   = new ReadOnlyBooleanWrapper();

    private final ReadOnlyBooleanWrapper executeNextProperty       = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper executePreviousProperty   = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper clearProperty             = new ReadOnlyBooleanWrapper(true);

    private final ReadOnlyIntegerWrapper indexPropery              = new ReadOnlyIntegerWrapper();

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
        return indexPropery.getReadOnlyProperty();
    }
}
