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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import fr.obeo.acceleo.tools.resources.Resources;

/**
 * To find a module file in the workspace or in the plugins
 * 
 * @author www.obeo.fr
 * 
 */
public class AcceleoModuleProvider {

    /**
     * Gets the sole instance.
     * 
     * @return the sole instance
     */
    public static AcceleoModuleProvider getDefault() {
        if (AcceleoModuleProvider.instance == null) {
            AcceleoModuleProvider.instance = new AcceleoModuleProvider();
        }
        return AcceleoModuleProvider.instance;
    }

    /**
     * The sole instance.
     */
    public final static File NOT_FOUND = new File("");

    /**
     * The sole instance.
     */
    private static AcceleoModuleProvider instance;

    /**
     * Saves the plugin for each file.
     */
    private Map file2plugin = new HashMap();

    /**
     * Saves the relative path for each file.
     */
    private Map file2relativePath = new HashMap();

    /**
     * Saves the file for each absolute path.
     */
    private Map absolutPath2file = new HashMap();

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
     * Gets the relative path of the given file.
     * 
     * @param file
     *            is the file
     * @return the relative path
     */
    public String getRelativePath(File file) {
        return (String) file2relativePath.get(file);
    }

    /**
     * Gets the file for the full name in the given plugin.
     * 
     * @param pluginId
     *            is the plugin
     * @param fullName
     *            is the full name of the resource in the plugin
     * @param extension
     *            is the extension of the file to search
     * @return the file
     */
    public File getFile(String pluginId, String fullName, String extension) {
        IPath fullPath = new Path(fullName.replaceAll("\\.", "/")).addFileExtension(extension); //$NON-NLS-1$ //$NON-NLS-2$
        return getFile(pluginId, fullPath);
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
        File res = null;
        /*
         * FIXME and Analyze if it is needed or if it impact performance in the
         * Sirius case. if (pluginId.indexOf("org.eclipse.") <= -1) { // faster
         * //$NON-NLS-1$
         */
        File cachedRes = (File) absolutPath2file.get(pluginId + relativePath);
        if (cachedRes != null) {
            if (cachedRes != AcceleoModuleProvider.NOT_FOUND) {
                res = cachedRes;
            }
        } else {
            Bundle bundle = Platform.getBundle(pluginId);
            if (bundle != null) {
                URL url = bundle.getEntry(relativePath.toString());
                if (url == null && "mt".equals(relativePath.getFileExtension()) && relativePath.segmentCount() > 1) { //$NON-NLS-1$
                    url = bundle.getEntry(relativePath.removeFirstSegments(1).toString());
                    if (url == null) {
                        url = getRuntimeModeURL(bundle, relativePath);
                    }
                }
                if (url != null) {
                    File file = new File(Resources.transformToAbsolutePath(url));
                    if (file.exists()) {
                        if (!file2plugin.containsKey(file)) {
                            file2plugin.put(file, pluginId);
                            file2relativePath.put(file, relativePath.toString());
                            // Copy the properties in the bundle area
                            Enumeration allProperties = bundle.findEntries(relativePath.removeLastSegments(1).toString(), "*.properties", true); //$NON-NLS-1$
                            while (allProperties != null && allProperties.hasMoreElements()) {
                                URL propertyFileURL = (URL) allProperties.nextElement();
                                if (propertyFileURL != null) {
                                    File propertyFile = new File(Resources.transformToAbsolutePath(propertyFileURL));
                                    if (propertyFile.exists()) {
                                        file2plugin.put(propertyFile, pluginId);
                                    }
                                }
                            }
                        }
                        res = file;
                    }
                } else {
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
                                    File scriptFile = getFile(id, relativePath, true);
                                    if (scriptFile != null) {
                                        res = scriptFile;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (res != null) {
                absolutPath2file.put(pluginId + relativePath, res);
            } else {
                absolutPath2file.put(pluginId + relativePath, AcceleoModuleProvider.NOT_FOUND);
            }
        }
        /*
         * }
         */
        return res;
    }

    private URL getRuntimeModeURL(Bundle bundle, IPath relativePath) {
        Map mtName2mtURLs = (Map) bundleName2mtPaths.get(bundle.getSymbolicName());
        if (mtName2mtURLs == null) {
            mtName2mtURLs = new HashMap();
            bundleName2mtPaths.put(bundle.getSymbolicName(), mtName2mtURLs);
            Enumeration entries = bundle.findEntries("/", "*.mt", true); //$NON-NLS-1$ //$NON-NLS-2$
            if (entries != null) {
                while (entries.hasMoreElements()) {
                    URL entry = (URL) entries.nextElement();
                    if (entry != null) {
                        IPath path = new Path(entry.getPath());
                        if (path.segmentCount() > 0) {
                            String name = path.lastSegment();
                            List mt = (List) mtName2mtURLs.get(name);
                            if (mt == null) {
                                mt = new ArrayList();
                                mtName2mtURLs.put(name, mt);
                            }
                            mt.add(entry);
                        }
                    }
                }
            }
        }
        List URLs = (List) mtName2mtURLs.get(relativePath.lastSegment());
        if (URLs != null) {
            Iterator it = URLs.iterator();
            while (it.hasNext()) {
                URL url = (URL) it.next();
                if (url.getPath().indexOf(relativePath.toString()) > -1) {
                    return url;
                }
            }
        }
        return null;
    }

    private Map bundleName2mtPaths = new HashMap();

}
