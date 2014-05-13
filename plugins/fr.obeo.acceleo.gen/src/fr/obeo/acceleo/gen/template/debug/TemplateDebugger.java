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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Template evaluation debugger.
 * 
 * @author David Bonneau
 * 
 */
public class TemplateDebugger implements ITemplateDebugger {

	private int state = ITemplateDebugger.RESUMED;

	private Map breakpoints = new HashMap();

	private Set listeners = new HashSet();

	private boolean stepInto = true;

	private boolean stepOver = true;

	private boolean stepReturn = true;

	private boolean terminated = false;

	private Stack stackDebugger = new Stack();

	/* (non-Javadoc) */
	public void init() {
		breakpoints.clear();
		state = ITemplateDebugger.RESUMED;
		stepInto = false;
		stepOver = false;
		stepReturn = false;
		terminated = false;
		stackDebugger.clear();
	}

	/* (non-Javadoc) */
	public void addBreakpoint(File aFile, int aLine) {
		if (breakpoints.get(aFile) == null) {
			breakpoints.put(aFile, new HashSet());
		}
		((Set) (breakpoints.get(aFile))).add(new Integer(aLine));
	}

	/* (non-Javadoc) */
	public void removeBreakpoint(File aFile, int aLine) {
		if (breakpoints.get(aFile) != null) {
			((Set) (breakpoints.get(aFile))).remove(new Integer(aLine));
		}
	}

	/* (non-Javadoc) */
	public void addListener(ITemplateDebuggerListener aListener) {
		listeners.add(aListener);
	}

	/* (non-Javadoc) */
	public int getState() {
		return state;
	}

	/* (non-Javadoc) */
	public void removeListener(ITemplateDebuggerListener aListener) {
		listeners.remove(aListener);

	}

	/* (non-Javadoc) */
	public void resume() {
		stepInto = false;
		stepOver = false;
		stepReturn = false;
		state = ITemplateDebugger.RESUMED;
	}

	/* (non-Javadoc) */
	public void stepInto() {
		stepInto = true;
		stepOver = false;
		stepReturn = false;
		state = ITemplateDebugger.RESUMED;
	}

	/* (non-Javadoc) */
	public void stepOver() {
		stepInto = false;
		stepOver = true;
		stepReturn = false;
		state = ITemplateDebugger.RESUMED;
		stackDebuggerSize = stackDebugger.size();
	}

	/* (non-Javadoc) */
	public void stepReturn() {
		stepInto = false;
		stepOver = false;
		stepReturn = true;
		state = ITemplateDebugger.RESUMED;
		stackDebuggerSize = stackDebugger.size();
	}

	private int stackDebuggerSize = 0;

	/* (non-Javadoc) */
	public void suspend() {
		stepInto = false;
		stepOver = false;
		stepReturn = false;
		state = ITemplateDebugger.SUSPENDED;

	}

	/* (non-Javadoc) */
	public void start() {
		fireStartEvent();
	}

	/* (non-Javadoc) */
	public void end() {
		fireEndEvent();
	}

	private File getFile(File aFile) {
		File ret = null;
		for (Iterator iterator = breakpoints.keySet().iterator(); iterator.hasNext() && ret == null;) {
			File file = (File) iterator.next();
			if (file.equals(aFile)) {
				ret = file;
			}
		}
		return ret;
	}

	/**
	 * Indicates if there is a breakpoint at the given position.
	 * 
	 * @param aFile
	 *            is the template
	 * @param aLine
	 *            is the line
	 * @param position
	 *            is the position
	 * @return true if there is a breakpoint at the given position.
	 */
	public boolean isBreakpoint(File aFile, int aLine, Int2 position) {
		if (terminated) {
			return false;
		} else {
			boolean ret = false;
			if (stepInto) {
				ret = true;
			} else if (stepOver) {
				if (stackDebugger.size() <= stackDebuggerSize) {
					ret = true;
					stackDebuggerSize = stackDebugger.size();
				} else {
					ret = false;
				}
			} else if (stepReturn) {
				if (stackDebugger.size() < stackDebuggerSize) {
					ret = true;
					stackDebuggerSize = stackDebugger.size();
				} else {
					ret = false;
				}
			} else {
				File file = getFile(aFile);
				if (file != null) {
					if (breakpoints.get(file) != null) {
						Set setLine = (Set) (breakpoints.get(file));
						ret = setLine.contains(new Integer(aLine));
					}
				}
			}
			if (ret) {
				state = SUSPENDED;
			}
			return ret;
		}
	}

	private void fireSuspendedBreakpointEvent() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ITemplateDebuggerListener debugListener = (ITemplateDebuggerListener) iterator.next();
			debugListener.suspendBreakpoint();
		}
	}

	private void fireResumedClientEvent() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ITemplateDebuggerListener debugListener = (ITemplateDebuggerListener) iterator.next();
			debugListener.resumeClient();
		}
	}

	private void fireResumedStepEvent() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ITemplateDebuggerListener debugListener = (ITemplateDebuggerListener) iterator.next();
			debugListener.resumeStep();
		}
	}

	private void fireSuspendedStepEvent() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ITemplateDebuggerListener debugListener = (ITemplateDebuggerListener) iterator.next();
			debugListener.suspendStep();
		}
	}

	private void fireStartEvent() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ITemplateDebuggerListener debugListener = (ITemplateDebuggerListener) iterator.next();
			debugListener.start();
		}
	}

	private void fireEndEvent() {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			ITemplateDebuggerListener debugListener = (ITemplateDebuggerListener) iterator.next();
			debugListener.end();
		}
	}

	/**
	 * It shows the given variables and it waits a user event.
	 * 
	 * @param variables
	 *            are the variables to show
	 */
	public void waitForEvent(Map variables) {
		if (state == SUSPENDED) {
			// Compute stack infos
			if (variables != null) {
				StackInfo stackInfo = (StackInfo) stackDebugger.peek();
				for (Iterator iterator = variables.keySet().iterator(); iterator.hasNext();) {
					String name = (String) iterator.next();
					Object value = variables.get(name);
					stackInfo.addVariable(name, value);
				}
			}
			// we are stoped, we must notify the listeners
			if (stepInto || stepOver || stepReturn) {
				fireSuspendedStepEvent();
			} else {
				fireSuspendedBreakpointEvent();
			}

			// We wait until we are not in suspended state
			while (state == SUSPENDED) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					AcceleoEcoreGenPlugin.getDefault().log(e, true);
				}
			}
			// we run, we must notify the listeners
			if (stepInto || stepOver || stepReturn) {
				fireResumedClientEvent();
			} else {
				fireResumedClientEvent();
				fireResumedStepEvent();
			}
			// We sleep a litte time
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				AcceleoEcoreGenPlugin.getDefault().log(e, true);
			}
		}
	}

	/**
	 * It updates the given variables.
	 * 
	 * @param variables
	 *            are the variables to show
	 */
	public void updateVariables(Map variables) {
		// Compute stack infos
		if (variables != null && !stackDebugger.isEmpty()) {
			StackInfo stackInfo = (StackInfo) stackDebugger.peek();
			for (Iterator iterator = variables.keySet().iterator(); iterator.hasNext();) {
				String name = (String) iterator.next();
				Object value = variables.get(name);
				stackInfo.addVariable(name, value);
			}
		}
	}

	/* (non-Javadoc) */
	public StackInfo[] getStack() {
		StackInfo[] ret = new StackInfo[stackDebugger.size()];
		int i = 0;
		for (Iterator iterator = stackDebugger.iterator(); iterator.hasNext();) {
			StackInfo current = (StackInfo) iterator.next();
			ret[i] = current;
			++i;
		}
		return ret;
	}

	/* (non-Javadoc) */
	public void popStack() {
		stackDebugger.pop();
	}

	/* (non-Javadoc) */
	public void pushStack(File aFile, int aLine, Int2 position) {
		StackInfo stackInfo = new StackInfo();
		stackInfo.setFile(aFile);
		stackInfo.setLine(aLine);
		stackInfo.setCharStart(position.b());
		stackInfo.setCharEnd(position.e());
		stackDebugger.push(stackInfo);
	}

	/* (non-Javadoc) */
	public void terminate() {
		terminated = true;
		state = RESUMED;
	}

}
