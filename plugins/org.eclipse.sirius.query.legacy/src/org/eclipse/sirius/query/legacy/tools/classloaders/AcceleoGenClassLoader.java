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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

import org.eclipse.sirius.query.legacy.tools.resources.Resources;

/**
 * This is a classloader for a generator project.
 * 
 */
public class AcceleoGenClassLoader extends AcceleoClassLoader {

    /**
     * Cache File -> URLs.
     */
    private static Map cacheURL = new HashMap();

    /**
     * Constructor.
     * 
     * @param project
     *            is the project that contains the generator
     * @param parent
     *            is the parent classloader
     */
    public AcceleoGenClassLoader(IProject project, ClassLoader parent) {
        super(AcceleoGenClassLoader.computeURLs(project), parent);
    }

    /**
     * Constructor.
     * 
     * @param bundle
     *            is the bundle that contains the generator
     * @param parent
     *            is the parent classloader
     */
    public AcceleoGenClassLoader(Bundle bundle, ClassLoader parent) {
        super(bundle, parent);
    }

    private static URL[] computeURLs(IProject project) {
        List list = new ArrayList();
        AcceleoGenClassLoader.computeURLs(project, list);
        return (URL[]) list.toArray(new URL[list.size()]);
    }

    private static void computeURLs(IProject project, List URLs) {
        IFolder binFolder = Resources.getOutputFolder(project);
        if (binFolder != null) {
            String location = binFolder.getLocation().toString();
            if (location.startsWith("/")) { //$NON-NLS-1$
                location = '/' + location;
            }
            try {
                URLs.add(new URL("file:/" + location + '/')); //$NON-NLS-1$
            } catch (MalformedURLException e) {
                // continue
            }
        }
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] entries;
        try {
            entries = javaProject.getResolvedClasspath(true);
        } catch (JavaModelException e1) {
            entries = new IClasspathEntry[] {};
        }
        for (IClasspathEntry entry : entries) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                IProject reference = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().toString());
                if (reference.exists()) {
                    AcceleoGenClassLoader.computeURLs(reference, URLs);
                }
            } else if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                try {
                    IFile reference = ResourcesPlugin.getWorkspace().getRoot().getFile(entry.getPath());
                    if (reference.exists()) {
                        URL url = (URL) AcceleoGenClassLoader.cacheURL.get(reference.getLocation().toFile());
                        if (url == null) {
                            url = reference.getLocation().toFile().toURL();
                            AcceleoGenClassLoader.cacheURL.put(reference.getLocation().toFile(), url);
                        }
                        URLs.add(url);
                    } else {
                        URL url = (URL) AcceleoGenClassLoader.cacheURL.get(entry.getPath().toFile());
                        if (url == null) {
                            url = entry.getPath().toFile().toURL();
                            AcceleoGenClassLoader.cacheURL.put(entry.getPath().toFile(), url);
                        }
                        URLs.add(url);
                    }
                } catch (MalformedURLException e) {
                    // continue
                }
            } else {
                try {
                    URL url = (URL) AcceleoGenClassLoader.cacheURL.get(entry.getPath().toFile());
                    if (url == null) {
                        url = entry.getPath().toFile().toURL();
                        AcceleoGenClassLoader.cacheURL.put(entry.getPath().toFile(), url);
                    }
                    URLs.add(url);
                } catch (MalformedURLException e) {
                    // continue
                }
            }
        }
    }
}
