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

/**
 * A script loader that converts the script's content before loading.
 * 
 * @author www.obeo.fr
 * 
 */
public interface IScriptLoader {

    /**
     * The method to convert the script's content.
     * 
     * @param content
     *            is the content to convert
     * @return the converted string
     */
    public String load(String content);

    /**
     * Gets the scripts of the specific strategy.
     * 
     * @param script
     *            is the current script
     */
    public IScript[] goToSpecifics(IScript script);

}
