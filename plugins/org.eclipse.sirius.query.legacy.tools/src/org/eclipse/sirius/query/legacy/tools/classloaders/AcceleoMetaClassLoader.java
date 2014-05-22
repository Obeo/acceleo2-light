/*******************************************************************************
 * Copyright (c) 2005-2014 Obeo
 *  
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.tools.classloaders;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

import org.eclipse.sirius.query.legacy.tools.resources.Resources;

/**
 * This is a classloader for a metamodel project.
 * 
 */
public class AcceleoMetaClassLoader extends AcceleoClassLoader {

    /**
     * Constructor.
     * 
     * @param project
     *            is the project that contains the code of the metamodel
     * @param parent
     *            is the parent classloader
     */
    public AcceleoMetaClassLoader(IProject project, ClassLoader parent) {
        super(AcceleoMetaClassLoader.resource2URLs(project), parent);
    }

    /**
     * Constructor.
     * 
     * @param bundle
     *            is the optional bundle that contains the generator
     * @param parent
     *            is the parent classloader
     */
    public AcceleoMetaClassLoader(Bundle bundle, ClassLoader parent) {
        super(bundle, parent);
    }

    /**
     * Gets the URLs of the project.
     * 
     * @param project
     *            is the project
     * @return the URLs of the project
     */
    private static URL[] resource2URLs(IProject project) {
        try {
            IFolder binFolder = Resources.getOutputFolder(project);
            if (binFolder != null) {
                String location = binFolder.getLocation().toString();
                if (location.startsWith("/")) { //$NON-NLS-1$
                    location = '/' + location;
                }
                return new URL[] { new URL("file:/" + location + '/') }; //$NON-NLS-1$
            } else {
                return new URL[] {};
            }
        } catch (MalformedURLException e) {
            return new URL[] {};
        }
    }

}
