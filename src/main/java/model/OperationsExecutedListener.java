package model;

import contract.json.Operation;

import java.util.List;

/**
 * @author Richard Sundqvist
 */
public interface OperationsExecutedListener {

    /**
     * Called when operations have been executed, altering the model.
     */
    void operationsExecuted (List<Operation> executedOperations);
}
