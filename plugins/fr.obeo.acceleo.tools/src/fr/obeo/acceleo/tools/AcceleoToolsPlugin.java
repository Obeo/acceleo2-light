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

package fr.obeo.acceleo.tools;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.BundleContext;

import fr.obeo.acceleo.tools.resources.AcceleoPlugin;

/**
 * The main plugin class to be used in the desktop.
 * 
 * @author www.obeo.fr
 * 
 */
public class AcceleoToolsPlugin extends AcceleoPlugin {

    /* (non-Javadoc) */
    @Override
    public String getID() {
        return "fr.obeo.acceleo.tools"; //$NON-NLS-1$
    }

    /**
     * The shared instance.
     */
    private static AcceleoToolsPlugin plugin;

    /**
     * Resource bundle.
     */
    private ResourceBundle resourceBundle;

    /**
     * Acceleo log errors count.
     */
    private int errorsCount = 0;

    /**
     * Acceleo log warnings count.
     */
    private int warningsCount = 0;

    /**
     * The constructor.
     */
    public AcceleoToolsPlugin() {
        super();
        AcceleoToolsPlugin.plugin = this;
    }

    /**
     * Returns the Acceleo log count.
     * 
     * @param lowestSeverity
     *            is the lowest severity (IStatus.ERROR, IStatus.WARNING)
     * @return the Acceleo log count
     */
    public int getAcceleoLogCount(int lowestSeverity) {
        if (lowestSeverity == IStatus.ERROR) {
            return errorsCount;
        } else if (lowestSeverity == IStatus.WARNING) {
            return warningsCount + errorsCount;
        } else {
            return 0;
        }
    }

    /**
     * Adds an Acceleo log.
     * 
     * @param status
     *            is the status of the log
     */
    public void newAcceleoLog(IStatus status) {
        if (status.getSeverity() == IStatus.ERROR) {
            errorsCount++;
        } else if (status.getSeverity() == IStatus.WARNING) {
            warningsCount++;
        }
    }

    /**
     * Adds a warning.
     */
    public void newWarning() {
        warningsCount++;
    }

    /* (non-Javadoc) */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    /* (non-Javadoc) */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        AcceleoToolsPlugin.plugin = null;
        resourceBundle = null;
    }

    /**
     * @return the shared instance
     */
    public static AcceleoToolsPlugin getDefault() {
        return AcceleoToolsPlugin.plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     * 
     * @param key
     *            identifies the string
     * @return the string from the plugin's resource bundle, or 'key' if not
     *         found
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = AcceleoToolsPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle.
     * 
     * @return the plugin's resource bundle
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (resourceBundle == null) {
                resourceBundle = ResourceBundle.getBundle("fr.obeo.acceleo.tools.AcceleoToolsPluginResources"); //$NON-NLS-1$
            }
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        return resourceBundle;
    }

}
