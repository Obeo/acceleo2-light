/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    David Bonneau and Obeo - initial API and implementation
 */

package fr.obeo.acceleo.gen.template.debug;

import java.io.File;

import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Interface for a template evaluation debugger.
 * 
 * @author David Bonneau
 * 
 */
public interface ITemplateDebugger {

	/**
	 * Resumed state.
	 */
	public static final int RESUMED = 0;

	/**
	 * Suspended state.
	 */
	public static final int SUSPENDED = 1;

	/**
	 * To resume the evaluation of the current template.
	 */
	void resume();

	/**
	 * To suspend the evaluation of the current template.
	 */
	void suspend();

	/**
	 * Go into the next evaluation step.
	 */
	void stepInto();

	/**
	 * Go over the next evaluation step.
	 */
	void stepOver();

	/**
	 * Go to the parent evaluation step.
	 */
	void stepReturn();

	/**
	 * Terminate the evaluation.
	 */
	void terminate();

	/**
	 * Adds a breakpoint.
	 * 
	 * @param aFile
	 *            is the template file
	 * @param aLine
	 *            is the line of the breakpoint in the file
	 */
	void addBreakpoint(File aFile, int aLine);

	/**
	 * Removes a breakpoint.
	 * 
	 * @param aFile
	 *            is the template file
	 * @param aLine
	 *            is the line of the breakpoint in the file
	 */
	void removeBreakpoint(File aFile, int aLine);

	/**
	 * Returns the state of the debugger.
	 * 
	 * @return the state of the debugger
	 */
	int getState();

	/**
	 * Adds a template listener.
	 * 
	 * @param aListener
	 *            is the listener to add
	 */
	void addListener(ITemplateDebuggerListener aListener);

	/**
	 * Removes a template listener.
	 * 
	 * @param aListener
	 *            is the listener to remove
	 */
	void removeListener(ITemplateDebuggerListener aListener);

	/**
	 * To initialize the debugger.
	 */
	void init();

	/**
	 * To start the debugger.
	 */
	void start();

	/**
	 * To finish the debugger.
	 */
	void end();

	/**
	 * Returns the execution stack.
	 * 
	 * @return the execution stack
	 */
	StackInfo[] getStack();

	/**
	 * Pushes a new execution context.
	 * 
	 * @param aFile
	 *            is the template file
	 * @param aLine
	 *            is the line of the context in the file
	 * @param position
	 *            is the position of the context in the file
	 */
	void pushStack(File aFile, int aLine, Int2 position);

	/**
	 * Pops an execution context.
	 */
	void popStack();

}
