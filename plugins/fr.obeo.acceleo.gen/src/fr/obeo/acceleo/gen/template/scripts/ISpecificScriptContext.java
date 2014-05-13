/*
 * Copyright (c) 2005-2008 Obeo
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 */

package fr.obeo.acceleo.gen.template.scripts;

import java.io.File;

/**
 * The compilation context of the script hierarchy.
 * 
 * @author www.obeo.fr
 * 
 */
public interface ISpecificScriptContext {

    /**
     * Gets the script for the given file.
     * 
     * @param file
     *            is the script file
     * @param chainFile
     *            is the running chain
     * @return the script
     */
    public SpecificScript getScript(File file, File chainFile);

    /**
     * Puts the given script in the context
     * 
     * @param file
     *            is the script file
     * @param script
     *            is the script to put in the context
     */
    public void setScript(File file, SpecificScript script);

    /**
     * Gets the maximum level of the context. The scripts are ignored when the
     * level is too high.
     * 
     * @return the maximum level of the context
     */
    public int getMaxLevel();

}
