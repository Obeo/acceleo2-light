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

package fr.obeo.acceleo.tools.plugins;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import fr.obeo.acceleo.tools.resources.Resources;

/**
 * To find a metamodel file in the workspace or in the plugins
 * 
 * @author www.obeo.fr
 * 
 */
public class AcceleoMetamodelProvider {

    /**
     * Gets the sole instance.
     * 
     * @return the sole instance
     */
    public static AcceleoMetamodelProvider getDefault() {
        if (AcceleoMetamodelProvider.instance == null) {
            AcceleoMetamodelProvider.instance = new AcceleoMetamodelProvider();
        }
        return AcceleoMetamodelProvider.instance;
    }

    /**
     * The sole instance.
     */
    private static AcceleoMetamodelProvider instance;

    /**
     * Saves the plugin for each file.
     */
    private Map file2plugin = new HashMap();

    /**
     * Gets the identifier of the plugin that contains the given file.
     * 
     * @param file
     *            is the file
     * @return the plugin ID
     */
    public String getPluginId(File file) {
        return (String) file2plugin.get(file);
    }

    /**
     * Gets the file for the given full path in the workspace or in the plugins.
     * 
     * @param fullPath
     *            is the full path of the file
     * @return the file
     */
    public File getFile(IPath fullPath) {
        if (fullPath != null && fullPath.segmentCount() > 0) {
            IFile file = Resources.findFile(fullPath);
            if (file != null) {
                return file.getLocation().toFile();
            } else {
                String pluginId = fullPath.segment(0);
                return getFile(pluginId, fullPath.removeFirstSegments(1)); // remove
                // '/Project'
            }
        } else {
            return null;
        }
    }

    /**
     * Gets the file for the relative path in the given plugin.
     * 
     * @param pluginId
     *            is the plugin
     * @param relativePath
     *            is the relative path in the plugin
     * @return the file
     */
    public File getFile(String pluginId, IPath relativePath) {
        return getFile(pluginId, relativePath, true);
    }

    /**
     * Gets the file for the relative path in the given plugin.
     * 
     * @param pluginId
     *            is the plugin
     * @param relativePath
     *            is the relative path in the plugin
     * @param requiredSearch
     *            true to search in the required bundles
     * @return the file
     */
    private File getFile(String pluginId, IPath relativePath, boolean requiredSearch) {
        if (pluginId.indexOf("org.eclipse.") > -1) { // faster //$NON-NLS-1$
            return null;
        }
        Bundle bundle = Platform.getBundle(pluginId);
        if (bundle != null) {
            URL url = bundle.getEntry(relativePath.toString());
            if (url != null) {
                File file = new File(Resources.transformToAbsolutePath(url));
                if (file.exists()) {
                    if (!file2plugin.containsKey(file)) {
                        file2plugin.put(file, pluginId);
                        // Copy the sibling in the bundle area
                        Enumeration all = bundle.findEntries(relativePath.removeLastSegments(1).toString(), "*", false); //$NON-NLS-1$
                        while (all != null && all.hasMoreElements()) {
                            URL fileURL = (URL) all.nextElement();
                            if (fileURL != null) {
                                File ecoreFile = new File(Resources.transformToAbsolutePath(fileURL));
                                if (ecoreFile.exists()) {
                                    file2plugin.put(ecoreFile, pluginId);
                                }
                            }
                        }
                    }
                    return file;
                }
            }
            if (requiredSearch) {
                String requiredBundles = bundle.getHeaders().get(Constants.REQUIRE_BUNDLE);
                if (requiredBundles != null) {
                    StringTokenizer st = new StringTokenizer(requiredBundles, ","); //$NON-NLS-1$
                    while (st.hasMoreTokens()) {
                        String id = st.nextToken().trim();
                        int iDot = id.indexOf(';');
                        if (iDot > -1) {
                            id = id.substring(0, iDot).trim();
                        }
                        if (id.length() > 0) {
                            File scriptFile = getFile(id, relativePath, false);
                            if (scriptFile != null) {
                                return scriptFile;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}
