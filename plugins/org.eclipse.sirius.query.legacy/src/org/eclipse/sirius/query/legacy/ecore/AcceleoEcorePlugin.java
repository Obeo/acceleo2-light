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

package org.eclipse.sirius.query.legacy.ecore;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.sirius.query.legacy.tools.resources.AcceleoPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 * 
 * 
 */
public class AcceleoEcorePlugin extends AcceleoPlugin {

    /* (non-Javadoc) */
    @Override
    public String getID() {
        return "org.eclipse.sirius.query.legacy.ecore"; //$NON-NLS-1$
    }

    /**
     * The shared instance.
     */
    private static AcceleoEcorePlugin plugin;

    /**
     * Resource bundle.
     */
    private ResourceBundle resourceBundle;

    /**
     * The constructor.
     */
    public AcceleoEcorePlugin() {
        super();
        AcceleoEcorePlugin.plugin = this;
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
        AcceleoEcorePlugin.plugin = null;
        resourceBundle = null;
    }

    /**
     * @return the shared instance
     */
    public static AcceleoEcorePlugin getDefault() {
        return AcceleoEcorePlugin.plugin;
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
        ResourceBundle bundle = AcceleoEcorePlugin.getDefault().getResourceBundle();
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
                resourceBundle = ResourceBundle.getBundle("org.eclipse.sirius.query.legacy.ecore.AcceleoEcorePluginResources"); //$NON-NLS-1$
            }
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        return resourceBundle;
    }

}
