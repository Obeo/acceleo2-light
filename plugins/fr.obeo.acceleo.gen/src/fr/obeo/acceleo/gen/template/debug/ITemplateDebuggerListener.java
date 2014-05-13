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

/**
 * Event on a template evaluation debugger.
 * 
 * @author David Bonneau
 * 
 */
public interface ITemplateDebuggerListener {

	/**
	 * Resume after client action.
	 * 
	 */
	void resumeClient();

	/**
	 * resume after a step.
	 * 
	 */
	void resumeStep();

	/**
	 * Suspend on a breakpoint.
	 * 
	 */
	void suspendBreakpoint();

	/**
	 * Suspend after a step.
	 * 
	 */
	void suspendStep();

	/**
	 * On "start" action.
	 */
	void start();

	/**
	 * On "end" action.
	 */
	void end();

}
