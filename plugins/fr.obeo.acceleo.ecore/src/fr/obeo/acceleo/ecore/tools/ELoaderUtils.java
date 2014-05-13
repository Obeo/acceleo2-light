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

package fr.obeo.acceleo.ecore.tools;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import fr.obeo.acceleo.ecore.AcceleoEcorePlugin;
import fr.obeo.acceleo.ecore.factories.Factories;
import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.tools.classloaders.AcceleoClassLoader;
import fr.obeo.acceleo.tools.classloaders.AcceleoMetaClassLoader;
import fr.obeo.acceleo.tools.plugins.AcceleoMetamodelProvider;

/**
 * It helps to load an instance model of an ecore metamodel.
 * 
 * @author www.obeo.fr
 * 
 */
public class ELoaderUtils {



	/**
	 * Loading of the jar file corresponding to the metamodel.
	 * 
	 * @param metaFile
	 *            is the metamodel file
	 * @param loader
	 *            is the current class loader
	 * @return the new classloader
	 */
	public static AcceleoClassLoader loadJarForMeta(IFile metaFile, ClassLoader loader) {
		AcceleoClassLoader newLoader = new AcceleoMetaClassLoader(metaFile.getProject(), loader);
		return newLoader;
	}

	/**
	 * Loading of the jar file corresponding to the metamodel.
	 * 
	 * @param metaFile
	 *            is the metamodel file
	 * @param loader
	 *            is the current class loader
	 * @return the new classloader
	 */
	public static AcceleoClassLoader loadJarForMeta(File metaFile, ClassLoader loader) {
		IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(metaFile.getAbsolutePath()));
		if (workspaceFile != null && workspaceFile.isAccessible()) {
			return loadJarForMeta(workspaceFile, loader);
		} else {
			String pluginId = AcceleoMetamodelProvider.getDefault().getPluginId(metaFile);
			if (pluginId != null) {
				final Bundle bundle = Platform.getBundle(pluginId);
				if (bundle != null) {
					return new AcceleoClassLoader(bundle, loader);
				}
			}
			return new AcceleoClassLoader(new URL[] {}, loader);
		}
	}

	/**
	 * Initialize factories for the given metamodel file.
	 * 
	 * @param metamodel
	 *            is the metamodel file (ecore)
	 * @param loader
	 *            is the current class loader
	 */
	public static void initMetamodelFactories(IFile metamodel, ClassLoader loader) {
		if (metamodel != null && metamodel.exists()) {
			AcceleoClassLoader newLoader = loadJarForMeta(metamodel, loader);
			try {
				new Factories(metamodel.getFullPath().toString(), newLoader);
			} catch (FactoryException e) {
				AcceleoEcorePlugin.getDefault().log(e, false);
			}
		}
	}

}
