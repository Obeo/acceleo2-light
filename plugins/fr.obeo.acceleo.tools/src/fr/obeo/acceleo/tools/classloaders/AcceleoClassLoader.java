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

package fr.obeo.acceleo.tools.classloaders;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * A class loader that combines plugins, acceleo services, and metamodels.
 * 
 * @author www.obeo.fr
 */
public class AcceleoClassLoader extends URLClassLoader {

    /**
     * The preferred class loader.
     */
    protected static ClassLoader preferredClassLoader = null;

    /**
     * The preferred class loader cache class name -> Class.
     */
    protected static Map preferedCache = new HashMap();

    /**
     * Updates the preferred class loader with the given model object.
     * 
     * @param object
     *            is the model object
     */
    public static void setPreferredLoader(EObject object) {
        if (object != null) {
            ClassLoader loader = object.getClass().getClassLoader();
            if (AcceleoClassLoader.preferredClassLoader != loader) {
                AcceleoClassLoader.preferedCache = new HashMap();
            }
            AcceleoClassLoader.preferredClassLoader = loader;
        } else {
            AcceleoClassLoader.preferedCache = new HashMap();
            AcceleoClassLoader.preferredClassLoader = null;
        }
    }

    /**
     * @param loader
     *            is the preferred class loader
     */
    public static void setPreferredClassLoader(ClassLoader loader) {
        if (AcceleoClassLoader.preferredClassLoader != loader) {
            AcceleoClassLoader.preferedCache = new HashMap();
        }
        AcceleoClassLoader.preferredClassLoader = loader;
    }

    /**
     * @return the preferred class loader
     */
    public static ClassLoader getPreferredClassLoader() {
        return AcceleoClassLoader.preferredClassLoader;
    }

    /**
     * The optional bundle that contains the classes.
     */
    private Bundle bundle;

    /**
     * Constructor.
     * 
     * @param urls
     *            are the specific URLs of the classloader
     * @param parent
     *            is the parent classloader
     */
    public AcceleoClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.bundle = null;
    }

    /**
     * Constructor.
     * 
     * @param bundle
     *            is the optional bundle that contains the generator
     * @param parent
     *            is the parent classloader
     */
    public AcceleoClassLoader(Bundle bundle, ClassLoader parent) {
        super(new URL[] {}, parent);
        this.bundle = bundle;
    }

    /* (non-Javadoc) */
    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            if (bundle != null) {
                Class c = loadClassInBundle(bundle, name, true);
                if (c != null) {
                    return c;
                } else {
                    return loadClassInPreferredLoader(name);
                }
            } else {
                return loadClassInPreferredLoader(name);
            }
        } catch (ClassNotFoundException e) {
            return loadClassInPreferredLoader(name);
        } catch (NoClassDefFoundError e) {
            return loadClassInPreferredLoader(name);
        }
    }

    private Class loadClassInBundle(Bundle bundle, String name, boolean requiredSearch) {
        try {
            return bundle.loadClass(name);
        } catch (ClassNotFoundException e) {
            if (requiredSearch) {
                return loadClassInRequiredBundles(bundle, name);
            }
        } catch (NoClassDefFoundError e) {
            if (requiredSearch) {
                return loadClassInRequiredBundles(bundle, name);
            }
        }
        return null;
    }

    private Class loadClassInRequiredBundles(Bundle bundle, String name) {
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
                    Bundle requiredBundle = Platform.getBundle(id);
                    if (requiredBundle != null) {
                        Class c = loadClassInBundle(requiredBundle, name, false);
                        if (c != null) {
                            return c;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Class loadClassInPreferredLoader(String name) throws ClassNotFoundException {
        Class res = (Class) AcceleoClassLoader.preferedCache.get(name);

        if (res == null) {
            try {
                if (AcceleoClassLoader.preferredClassLoader != null && AcceleoClassLoader.preferredClassLoader != this && AcceleoClassLoader.preferredClassLoader != getParent()) {
                    res = AcceleoClassLoader.preferredClassLoader.loadClass(name);
                }
            } catch (ClassNotFoundException e) {
                // continue
            } catch (NoClassDefFoundError e) {
                // continue
            }
            if (res == null) {
                try {
                    res = getParent().loadClass(name);
                } catch (ClassNotFoundException e) {
                    res = super.loadClass(name);
                } catch (NoClassDefFoundError e) {
                    res = super.loadClass(name);
                }
            }
            if (res != null) {
                AcceleoClassLoader.preferedCache.put(name, res);
            }
        }
        return res;
    }
}
