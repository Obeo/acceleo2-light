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

package fr.obeo.acceleo.gen.template.scripts.imports.services;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;

import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;

/**
 * System services for the properties files.
 * 
 * @author www.obeo.fr
 * 
 */
public class PropertiesServices {

    /**
     * The script.
     */
    protected SpecificScript script;

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public PropertiesServices(SpecificScript script) {
        this.script = script;
    }

    /**
     * Gets the value for the key in all the property files.
     * 
     * @param current
     *            is the current node
     * @param key
     *            is the key
     * @return the value for the given key
     * @throws CoreException
     * @throws IOException
     */
    public ENode getProperty(ENode current, String key) throws CoreException, IOException {
        String result = script.getProperty(key);
        return new ENode(result, current);
    }

    /**
     * Gets the property for the key and the property file (without extension).
     * 
     * @param current
     *            is the current node
     * @param name
     *            is the name of the property file (without ".properties"
     *            extension)
     * @param key
     *            is the key
     * @return the value for the given key
     * @throws CoreException
     * @throws IOException
     */
    public ENode getProperty(ENode current, String name, String key) throws CoreException, IOException {
        String result = script.getProperty(name, key);
        return new ENode(result, current);
    }

    /**
     * Gets the best value for the key in all the property files.
     * <p>
     * Sample : key == "aa.bb.cc.dd"
     * <p>
     * It returns getProperty("aa.bb.cc.dd") || getProperty("aa.bb.cc") ||
     * getProperty("aa.bb") || getProperty("aa").
     * 
     * @param current
     *            is the current node
     * @param key
     *            is the key
     * @return the value for the given key
     * @throws CoreException
     * @throws IOException
     */
    public ENode getBestProperty(ENode current, String key) throws CoreException, IOException {
        String result = null;
        while (result == null && key != null && key.length() > 0) {
            result = script.getProperty(key);
            if (result == null) {
                int i = key.lastIndexOf("."); //$NON-NLS-1$
                if (i == -1) {
                    key = null;
                } else {
                    key = key.substring(0, i);
                }
            }
        }
        return new ENode(result, current);
    }

}
