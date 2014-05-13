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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debugger execution context.
 * 
 * @author David Bonneau
 * 
 */
public class StackInfo {

	private int line;

	private File file;

	private int charStart;

	private int charEnd;

	private Map variables = new LinkedHashMap();

	/**
	 * @return the template file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file
	 *            is the template file
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * @return the line in the template file
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @param line
	 *            is the line in the template file
	 */
	public void setLine(int line) {
		this.line = line;
	}

	/**
	 * @return the index of the last character
	 */
	public int getCharEnd() {
		return charEnd;
	}

	/**
	 * @param charEnd
	 *            is the index of the last character
	 */
	public void setCharEnd(int charEnd) {
		this.charEnd = charEnd;
	}

	/**
	 * @return the index of the first character
	 */
	public int getCharStart() {
		return charStart;
	}

	/**
	 * @param charStart
	 *            is the index of the first character
	 */
	public void setCharStart(int charStart) {
		this.charStart = charStart;
	}

	/**
	 * Adds a new variable for the given name and the givan value.
	 * 
	 * @param aName
	 *            is the name
	 * @param aValue
	 *            is the value
	 */
	public void addVariable(String aName, Object aValue) {
		variables.put(aName, aValue);
	}

	/**
	 * @return the variables of the execution context
	 */
	public Map getVariables() {
		return variables;
	}
}
