/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.commands.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.commands.operations.GlobalUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * <p>
 * IOperationHistory tracks a history of operations that can be undone or
 * redone. Operations are added to the history once they have been initially
 * executed. Clients may choose whether to have the operations history perform
 * the initial execution or simply add the operation to the history.
 * </p>
 * <p>
 * Once operations are added to the history, the methods <code>canRedo()</code>
 * and <code>canUndo()</code> are used to determine whether there is an
 * operation available for undo and redo in a given operation context. The
 * context-based protocol implies that there is only one operation that can be
 * undone or redone at a given time in a given context. This is typical of a
 * linear undo model, when only the most recently executed operation is
 * available for undo. When this protocol is used, a linear model is enforced by
 * the history. It is up to clients to determine how to maintain a history that
 * is invalid or stale. For example, when the most recent operation for a
 * context cannot be performed, clients may wish to flush the history for that
 * context.
 * </p>
 * <p>
 * Additional protocol allows direct undo and redo of a specified operation,
 * regardless of its position in the history. When a more flexible undo model is
 * supported, these methods can be implemented to undo and redo directly
 * specified operations. If an implementer of IOperationHistory does not allow
 * direct undo and redo, these methods can return a status indicating that it is
 * not allowed.
 * </p>
 * <p>
 * Listeners (IOperationHistoryListener) can listen for notifications about
 * changes in the history (operations added or removed), and for notification
 * before and after any operation is executed, undone or redone. Notification of
 * operation execution only occurs when clients direct the history to execute
 * the operation. If the operation is added after it is executed, there can be
 * no notification of its execution.
 * </p>
 * <p>
 * IOperationApprover defines an interface for approving an undo or redo before
 * it occurs. This is useful for injecting policy-decisions into the model -
 * whether direct undo and redo are supported, or warning the user about certain
 * kinds of operations. It can also be used when objects have state related to
 * the operation and need to determine whether an undo or redo will cause any
 * conflicts with their local state.
 * </p>
 * 
 * @since 3.1
 */
public interface IOperationHistory {

	/**
	 * An operation is to be opened or closed for execution.
	 */
	public static final int EXECUTE = 0x0001;

	/**
	 * An operation is to be opened for undo.
	 */
	public static final int UNDO = 0x0002;

	/**
	 * An operation is to be opened for redo.
	 */
	public static final int REDO = 0x0004;

	/**
	 * An undo context that can be used to query the global undo history. This
	 * context is not intended to be assigned to operations. Instead, it is used
	 * for querying the history or performing an undo or redo on the global
	 * history.
	 */
	public static final IUndoContext GLOBAL_UNDO_CONTEXT = new GlobalUndoContext();

	/**
	 * An operation info status describing the condition that there is no
	 * available operation for redo.
	 */
	public static final IStatus NOTHING_TO_REDO_STATUS = new OperationStatus(
			IStatus.INFO, OperationStatus.NOTHING_TO_REDO,
			"No operation to redo"); //$NON-NLS-1$

	/**
	 * An operation info status describing the condition that there is no
	 * available operation for undo.
	 */
	public static final IStatus NOTHING_TO_UNDO_STATUS = new OperationStatus(
			IStatus.INFO, OperationStatus.NOTHING_TO_UNDO,
			"No operation to undo"); //$NON-NLS-1$

	/**
	 * An operation error status describing the condition that the operation
	 * available for execution, undo or redo is not in a valid state for the
	 * action to be performed.
	 */
	public static final IStatus OPERATION_INVALID_STATUS = new OperationStatus(
			IStatus.ERROR, OperationStatus.OPERATION_INVALID,
			"Operation is not valid"); //$NON-NLS-1$

	/**
	 * <p>
	 * Add the specified operation to the history without executing it. The
	 * operation should have already been executed by the time it is added to
	 * the history. Listeners will be notified that the operation was added to
	 * the history (<code>OPERATION_ADDED</code>).
	 * </p>
	 * 
	 * @param operation -
	 *            the operation to be added to the history
	 */
	void add(IUndoableOperation operation);

	/**
	 * <p>
	 * Add the specified approver to the list of operation approvers consulted
	 * by the operation history before an undo or redo is allowed to proceed.
	 * </p>
	 * 
	 * @param approver -
	 *            the IOperationApprover to be added as an approver.
	 * 
	 * @see org.eclipse.core.commands.operations.IOperationApprover
	 */
	void addOperationApprover(IOperationApprover approver);

	/**
	 * <p>
	 * Add the specified listener to the list of operation history listeners
	 * that are notified about changes in the history or operations that are
	 * executed, undone, or redone.
	 * </p>
	 * 
	 * @param listener -
	 *            the IOperationHistoryListener to be added as a listener.
	 * 
	 * @see org.eclipse.core.commands.operations.IOperationHistoryListener
	 * @see org.eclipse.core.commands.operations.OperationHistoryEvent
	 */
	void addOperationHistoryListener(IOperationHistoryListener listener);

	/**
	 * <p>
	 * Close the current operation. If the operation has successfully completed,
	 * send listeners a <code>DONE</code>, <code>UNDONE</code>, or
	 * <code>REDONE</code> notification, depending on the mode. Otherwise send
	 * an <code>OPERATION_NOT_OK</code> notification. Add the operation to the
	 * history if specified and send an <code>OPERATION_ADDED</code>
	 * notification.
	 * </p>
	 * <p>
	 * Any operations that are executed and added after this operation is closed
	 * will no longer be considered part of this operation.
	 * </p>
	 * <p>
	 * This method has no effect if the caller has not previously called
	 * {@link #openOperation}.
	 * </p>
	 * 
	 * @param operationOK -
	 *            <code>true</code> if the operation successfully completed.
	 *            Listeners should be notified with <code>DONE</code>,
	 *            <code>UNDONE</code>, or <code>REDONE</code>.
	 *            <code>false</code> if the operation did not successfully
	 *            complete. Listeners should be notified with
	 *            <code>OPERATION_NOT_OK</code>.
	 * @param addToHistory -
	 *            <code>true</code> if the operation should be added to the
	 *            history, <code>false</code> if it should not. If the
	 *            <code>operationOK</code> parameter is <code>false</code>,
	 *            the operation will never be added to the history.
	 * @param mode -
	 *            the mode the operation was opened in. Can be one of
	 *            <code>EXECUTE</code>, <code>UNDO</code>, or
	 *            <code>REDO</code>. This determines what notifications are
	 *            sent.
	 */
	void closeOperation(boolean operationOK, boolean addToHistory, int mode);

	/**
	 * <p>
	 * Return whether there is a redoable operation available in the given
	 * context.
	 * </p>
	 * 
	 * @param context -
	 *            the context to be checked
	 * @return <code>true</code> if there is a redoable operation,
	 *         <code>false</code> otherwise.
	 */

	boolean canRedo(IUndoContext context);

	/**
	 * <p>
	 * Return whether there is an undoable operation available in the given
	 * context
	 * </p>
	 * 
	 * @param context -
	 *            the context to be checked
	 * @return <code>true</code> if there is an undoable operation,
	 *         <code>false</code> otherwise.
	 */
	boolean canUndo(IUndoContext context);

	/**
	 * <p>
	 * Dispose of the specified context in the history. All operations that have
	 * only the given context will be disposed. References to the context in
	 * operations that have more than one context will also be removed.
	 * A history notification for the removal of each operation being
	 * disposed will be sent.  
	 * </p>
	 * 
	 * @param context -
	 *            the context to be disposed
	 * @param flushUndo -
	 *            <code>true</code> if the context should be flushed from the
	 *            undo history, <code>false</code> if it should not
	 * @param flushRedo -
	 *            <code>true</code> if the context should be flushed from the
	 *            redo history, <code>false</code> if it should not.
	 * @param flushContext -
	 *            <code>true</code> if the context is no longer in use and
	 *            references to it should be flushed.
	 */
	void dispose(IUndoContext context, boolean flushUndo, boolean flushRedo,
			boolean flushContext);

	/**
	 * <p>
	 * Execute the specified operation and add it to the operations history if
	 * successful. This method is used by clients who wish operation history
	 * listeners to receive notifications before and after the execution of the
	 * operation. Listeners will be notified before (
	 * <code>ABOUT_TO_EXECUTE</code>) and after (<code>DONE</code> or
	 * <code>OPERATION_NOT_OK</code>).
	 * </p>
	 * <p>
	 * If the operation successfully executes, an additional notification that
	 * the operation has been added to the history (<code>OPERATION_ADDED</code>)
	 * will be sent.
	 * </p>
	 * 
	 * @param operation -
	 *            the operation to be executed and then added to the history
	 * 
	 * @param monitor -
	 *            the progress monitor to be used (or <code>null</code>)
	 *            during the operation.
	 * 
	 * @param info -
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not
	 *            <code>null</code>, it should minimally contain an adapter
	 *            for the org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return the IStatus indicating whether the execution succeeded.
	 * 
	 * <p>
	 * The severity code in the returned status describes whether the operation
	 * succeeded and whether it was added to the history. <code>OK</code>
	 * severity indicates that the execute operation was successful and that the
	 * operation has been added to the history. Listeners will receive
	 * notifications about the operation's success (<code>DONE</code>) and
	 * about the operation being added to the history (<code>OPERATION_ADDED</code>).
	 * </p>
	 * <p>
	 * <code>CANCEL</code> severity indicates that the user cancelled the
	 * operation and that the operation was not added to the history.
	 * <code>ERROR</code> severity indicates that the operation did not
	 * successfully execute and that it was not added to the history. Any other
	 * severity code is not specifically interpreted by the history, and the
	 * operation will not be added to the history. For all severities other than
	 * <code>OK</code>, listeners will receive the
	 * <code>OPERATION_NOT_OK</code> notification instead of the
	 * <code>DONE</code> notification.
	 * </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during execution.
	 * 
	 */
	IStatus execute(IUndoableOperation operation, IProgressMonitor monitor,
			IAdaptable info) throws ExecutionException;

	/**
	 * <p>
	 * Return the limit on the undo and redo history for a particular context.
	 * </p>
	 * 
	 * @param context -
	 *            the context whose limit is requested
	 * 
	 * @return limit - the undo and redo history limit for the specified
	 *         context.
	 */
	int getLimit(IUndoContext context);

	/**
	 * <p>
	 * Get the array of operations in the redo history for a given context. The
	 * operations are in the order that they would be redone if successive
	 * "Redo" commands were invoked.
	 * </p>
	 * 
	 * @param context -
	 *            the context for the redo
	 * @return the array of operations in the history
	 */
	IUndoableOperation[] getRedoHistory(IUndoContext context);

	/**
	 * <p>
	 * Get the operation that will next be redone in the given context.
	 * </p>
	 * 
	 * @param context -
	 *            the context for the redo
	 * @return the operation to be redone or <code>null</code> if there is no
	 *         operation available. There is no guarantee that the returned
	 *         operation is valid for redo.
	 */
	IUndoableOperation getRedoOperation(IUndoContext context);

	/**
	 * <p>
	 * Get the array of operations that can be undone in the specified context.
	 * The operations are in the order that they would be undone if successive
	 * "Undo" commands were invoked.
	 * </p>
	 * 
	 * @param context -
	 *            the context for the undo
	 * @return the array of operations in the history
	 */
	IUndoableOperation[] getUndoHistory(IUndoContext context);

	/**
	 * <p>
	 * Open this composite operation and consider it an operation that contains
	 * other related operations. Consider all operations that are subsequently
	 * executed or added to be part of this operation. When an operation is
	 * opened, listeners will immediately receive a notification for the opened
	 * operation.  The specific notification depends on
	 * the mode in which the operation is opened (<code>ABOUT_TO_EXECUTE</code>,
	 * <code>ABOUT_TO_UNDO</code>, <code>ABOUT_TO_REDO</code>).
	 * Notifications for any other execute or add while this
	 * operation is open will not occur. Instead, those operations will be added
	 * to the current operation.
	 * </p>
	 * <p>
	 * Note: This method is intended to be used by legacy undo frameworks that
	 * do not expect related undo operations to appear in the same undo history
	 * as the triggering undo operation. When an operation is open, any
	 * subsequent requests to execute, add, undo, or redo another operation will
	 * result in that operation being added to the open operation. Once the
	 * operation is closed, the composite will be considered an atomic
	 * operation.
	 * </p>
	 * <p>
	 * When a composite is open, operations that are added to the history will
	 * be considered part of the open operation instead. Operations that are
	 * executed while a composite is open will first be executed and then added
	 * to the composite.
	 * </p>
	 * <p>
	 * Open operations cannot be nested. If this method is called when a different
	 * operation is open, it is presumed to be an application coding error and
	 * this method will throw an IllegalStateException.
	 * </p>
	 * @param operation -
	 *            the composite operation to be considered as the parent for all
	 *            subsequent operations.
	 * @param mode -
	 *            the mode the operation is executing in. Can be one of
	 *            <code>EXECUTE</code>, <code>UNDO</code>, or
	 *            <code>REDO</code>. This determines what notifications are
	 *            sent.
	 */
	void openOperation(ICompositeOperation operation, int mode);

	/**
	 * <p>
	 * The specified operation has changed in some way since it was added to the
	 * operation history. Notify listeners with an OPERATION_CHANGED event.
	 * </p>
	 * 
	 * @param operation -
	 *            the operation that has changed.
	 * 
	 */
	void operationChanged(IUndoableOperation operation);

	/**
	 * <p>
	 * Get the operation that will next be undone in the given context.
	 * </p>
	 * 
	 * @param context -
	 *            the context for the undo
	 * @return the operation to be undone or <code>null</code> if there is no
	 *         operation available. There is no guarantee that the available
	 *         operation is valid for the undo.
	 */
	IUndoableOperation getUndoOperation(IUndoContext context);

	/**
	 * <p>
	 * Redo the most recently undone operation in the given context
	 * </p>
	 * 
	 * @param context -
	 *            the context to be redone
	 * @param monitor -
	 *            the progress monitor to be used for the redo, or
	 *            <code>null</code> if no progress monitor is provided.
	 * @param info -
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not
	 *            <code>null</code>, it should minimally contain an adapter
	 *            for the org.eclipse.swt.widgets.Shell.class.
	 * @return the IStatus indicating whether the redo succeeded.
	 * 
	 * <p>
	 * The severity code in the returned status describes whether the operation
	 * succeeded and whether it remains in the history. <code>OK</code>
	 * severity indicates that the redo operation was successful and that the
	 * operation has been placed on the undo history. Listeners will receive the
	 * <code>REDONE</code> notification.
	 * </p>
	 * <p>
	 * <code>CANCEL</code> severity indicates that the user cancelled the
	 * operation and that the operation remains in the redo history.
	 * <code>ERROR</code> severity indicates that the operation could not
	 * successfully be redone and that it has been removed from the history.
	 * Listeners will also be notified that the operation was removed. Any other
	 * severity code is not specifically interpreted by the history, and is
	 * simply passed back to the caller. For all severities other than
	 * <code>OK</code>, listeners will receive the
	 * <code>OPERATION_NOT_OK</code> notification instead of the
	 * <code>REDONE</code> notification.
	 * </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during redo.
	 * 
	 */
	IStatus redo(IUndoContext context, IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * <p>
	 * Redo the specified operation
	 * </p>
	 * 
	 * @param operation -
	 *            the operation to be redone
	 * @param monitor -
	 *            the progress monitor to be used for the redo, or code>null</code>
	 *            if no progress monitor is provided
	 * @param info -
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not <code>null</code>,
	 *            it should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return the IStatus indicating whether the redo succeeded.
	 * 
	 * <p>
	 * The severity code in the returned status describes whether the operation
	 * succeeded and whether it remains in the history. <code>OK</code>
	 * severity indicates that the redo operation was successful and that the
	 * operation has been placed on the undo history. Listeners will receive the
	 * <code>REDONE</code> notification.
	 * </p>
	 * <p>
	 * <code>CANCEL</code> severity indicates that the user cancelled the
	 * operation and that the operation remains in the redo history. <code>ERROR</code>
	 * severity indicates that the operation could not successfully be redone.
	 * The operation will remain at its current location in the history, and
	 * callers must explicitly remove it if desired. Any other severity code is
	 * not interpreted by the history, and is simply passed back to the caller.
	 * For all severities other than <code>OK</code>, listeners will receive
	 * the <code>OPERATION_NOT_OK</code> notification instead of the <code>REDONE</code>
	 * notification.
	 * </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during redo.
	 */
	IStatus redoOperation(IUndoableOperation operation,
			IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * <p>
	 * Remove the specified operation approver from the list of operation
	 * approvers that are consulted before an operation is undone or redone.
	 * </p>
	 * 
	 * @param approver -
	 *            the IOperationApprover to be removed
	 */
	void removeOperationApprover(IOperationApprover approver);

	/**
	 * <p>
	 * Remove the specified listener from the list of operation history
	 * listeners.
	 * </p>
	 * 
	 * @param listener -
	 *            The IOperationHistoryListener to be removed
	 */
	void removeOperationHistoryListener(IOperationHistoryListener listener);

	/**
	 * <p>
	 * Replace the specified operation in the undo or redo history with the
	 * provided list of replacements. This protocol is typically used when a
	 * composite is broken up into its atomic parts. The replacements will be
	 * inserted so that the first replacement will be the first of the
	 * replacements to be undone or redone. Listeners will be notified about the
	 * removal of the replaced element and the addition of each replacement.
	 * </p>
	 * 
	 * @param operation -
	 *            The IUndoableOperation to be replaced
	 * @param replacements -
	 *            the array of IUndoableOperation to replace the first operation
	 */
	void replaceOperation(IUndoableOperation operation,
			IUndoableOperation[] replacements);

	/**
	 * <p>
	 * Set the limit on the undo and redo history for a particular context.
	 * </p>
	 * 
	 * @param context -
	 *            the context whose limit is being set.
	 * 
	 * @param limit -
	 *            the maximum number of operations that should be kept in the
	 *            undo or redo history for the specified context. Must not be
	 *            negative.
	 */
	void setLimit(IUndoContext context, int limit);

	/**
	 * <p>
	 * Undo the most recently undone operation in the given context
	 * </p>
	 * 
	 * @param context -
	 *            the context to be undone
	 * @param monitor -
	 *            the progress monitor to be used for the undo, or
	 *            <code>null</code> if no progress monitor is provided.
	 * @param info -
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not
	 *            <code>null</code>, it should minimally contain an adapter
	 *            for the org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return the IStatus indicating whether the undo succeeded.
	 * 
	 * <p>
	 * The severity code in the returned status describes whether the operation
	 * succeeded and whether it remains in the history. <code>OK</code>
	 * severity indicates that the undo operation was successful and that the
	 * operation has been placed on the redo history. Listeners will receive the
	 * <code>UNDONE</code> notification.
	 * </p>
	 * <p>
	 * <code>CANCEL</code> severity indicates that the user cancelled the
	 * operation and that the operation remains in the undo history.
	 * <code>ERROR</code> severity indicates that the operation could not
	 * successfully be undone and that it has been removed from the history.
	 * Listeners will be notified that the operation was removed. Any other
	 * severity code is not interpreted by the history, and is simply passed
	 * back to the caller. For all severities other than <code>OK</code>,
	 * listeners will receive the <code>OPERATION_NOT_OK</code> notification
	 * instead of the <code>UNDONE</code> notification.
	 * </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during undo.
	 */

	IStatus undo(IUndoContext context, IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * <p>
	 * Undo the specified operation
	 * </p>
	 * 
	 * @param operation -
	 *            the operation to be undone
	 * @param monitor -
	 *            the progress monitor to be used for the undo, or
	 *            <code>null</code> if no progress monitor is provided
	 * @param info -
	 *            the IAdaptable (or <code>null</code>) provided by the
	 *            caller in order to supply UI information for prompting the
	 *            user if necessary. When this parameter is not
	 *            <code>null</code>, it should minimally contain an adapter
	 *            for the org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return the IStatus indicating whether the undo succeeded.
	 * 
	 * <p>
	 * The severity code in the returned status describes whether the operation
	 * succeeded and whether it remains in the history. <code>OK</code>
	 * severity indicates that the undo operation was successful and that the
	 * operation has been placed on the redo history. Listeners will receive the
	 * <code>UNDONE</code> notification.
	 * </p>
	 * <p>
	 * <code>CANCEL</code> severity indicates that the user cancelled the
	 * operation and that the operation remains in the undo history.
	 * <code>ERROR</code> severity indicates that the operation could not
	 * successfully be undone. The operation will remain at its current location
	 * in the history, and callers must explicitly remove it if desired. Any
	 * other severity code is not interpreted by the history, and is simply
	 * passed back to the caller. For all severities other than <code>OK</code>,
	 * listeners will receive the <code>OPERATION_NOT_OK</code> notification
	 * instead of the <code>UNDONE</code> notification.
	 * </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during undo.
	 */
	IStatus undoOperation(IUndoableOperation operation,
			IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

}
