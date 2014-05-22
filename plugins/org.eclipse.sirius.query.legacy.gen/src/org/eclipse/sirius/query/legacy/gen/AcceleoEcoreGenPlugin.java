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

package org.eclipse.sirius.query.legacy.gen;

import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.osgi.framework.BundleContext;

import org.eclipse.sirius.query.legacy.gen.template.TemplateConstants;
import org.eclipse.sirius.query.legacy.gen.template.scripts.ISpecificScriptContext;
import org.eclipse.sirius.query.legacy.gen.template.scripts.SpecificScript;
import org.eclipse.sirius.query.legacy.tools.resources.AcceleoPlugin;
import org.eclipse.sirius.query.legacy.tools.resources.FileContentMap;

/**
 * The main plugin class to be used in the desktop.
 * 
 * 
 */
public class AcceleoEcoreGenPlugin extends AcceleoPlugin {

    /* (non-Javadoc) */
    @Override
    public String getID() {
        return "org.eclipse.sirius.query.legacy.gen"; //$NON-NLS-1$
    }

    /**
     * The shared instance.
     */
    private static AcceleoEcoreGenPlugin plugin;

    /**
     * Resource bundle.
     */
    private ResourceBundle resourceBundle;

    /**
     * The global script context.
     */
    private SpecificScriptContext globalScriptContext;

    private class SpecificScriptContext implements ISpecificScriptContext {

        private int maxLevel;

        private FileContentMap contextFile2Script = new FileContentMap(true);

        /**
         * Constructor.
         * 
         * @param maxLevel
         *            is the maximum level of the context
         */
        public SpecificScriptContext(int maxLevel) {
            this.maxLevel = maxLevel;
        }

        /* (non-Javadoc) */
        public SpecificScript getScript(File file, File chainFile) {
            SpecificScript result = (SpecificScript) contextFile2Script.get(file);
            if (result != null) {
                result.setChainFile(chainFile);
                return result;
            } else {
                return null;
            }
        }

        /* (non-Javadoc) */
        public void setScript(File file, SpecificScript script) {
            if (script != null) {
                contextFile2Script.put(file, script);
            }
        }

        /* (non-Javadoc) */
        public int getMaxLevel() {
            return maxLevel;
        }

    };

    /**
     * The constructor.
     */
    public AcceleoEcoreGenPlugin() {
        super();
        AcceleoEcoreGenPlugin.plugin = this;
    }

    /* (non-Javadoc) */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        TemplateConstants.initConstants();
    }

    /* (non-Javadoc) */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        AcceleoEcoreGenPlugin.plugin = null;
        resourceBundle = null;
        globalScriptContext = null;
    }

    /**
     * @return the shared instance
     */
    public static AcceleoEcoreGenPlugin getDefault() {
        return AcceleoEcoreGenPlugin.plugin;
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
        ResourceBundle bundle = AcceleoEcoreGenPlugin.getDefault().getResourceBundle();
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
                resourceBundle = ResourceBundle.getBundle("org.eclipse.sirius.query.legacy.gen.AcceleoEcoreGenPluginResources"); //$NON-NLS-1$
            }
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        return resourceBundle;
    }

    /**
     * @return the global script context
     */
    public ISpecificScriptContext getGlobalScriptContext() {
        if (globalScriptContext == null) {
            globalScriptContext = new SpecificScriptContext(2);
        }
        return globalScriptContext;
    }

}
