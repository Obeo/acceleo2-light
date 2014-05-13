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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

import fr.obeo.acceleo.ecore.AcceleoEcoreMessages;
import fr.obeo.acceleo.ecore.AcceleoEcorePlugin;
import fr.obeo.acceleo.ecore.factories.Factories;
import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.tools.classloaders.AcceleoClassLoader;
import fr.obeo.acceleo.tools.classloaders.AcceleoMetaClassLoader;
import fr.obeo.acceleo.tools.plugins.AcceleoMetamodelProvider;
import fr.obeo.acceleo.tools.resources.Resources;

/**
 * It helps to load an instance model of an ecore metamodel.
 * 
 * @author www.obeo.fr
 * 
 */
public class ELoaderUtils {

	/**
	 * Gets the metamodel file for the model.
	 * 
	 * @param xmiFile
	 *            is the model file
	 * @return the metamodel file
	 */
	public static IFile xmi2ecore(IFile xmiFile) {
		if (xmiFile != null) {
			try {
				IPath path = new Path(xmiFile.getFullPath().toString()).removeFileExtension();
				String fileName = path.lastSegment();
				int index$ = fileName.indexOf('.');
				if (index$ > -1)
					fileName = fileName.substring(0, index$);
				// Search the ecore file (it use the prefix) in the containers
				IContainer container = xmiFile.getParent();
				while (container != null && container != ResourcesPlugin.getWorkspace().getRoot()) {
					IFile ecoreFile = Resources.findFile(container.getFullPath().append(new Path(fileName).addFileExtension("ecore"))); //$NON-NLS-1$
					if (ecoreFile != null && ecoreFile.exists()) {
						return ecoreFile;
					}
					container = container.getParent();
				}
				// Search the ecore file (it doesn't use the prefix) in the
				// containers
				container = xmiFile.getParent();
				while (container != null && container != ResourcesPlugin.getWorkspace().getRoot()) {
					IResource[] members = container.members();
					for (int i = 0; i < members.length; i++) {
						IResource member = members[i];
						if (member instanceof IFile && "ecore".equals(member.getFileExtension())) { //$NON-NLS-1$
							return (IFile) member;
						}
					}
					container = container.getParent();
				}
				// Search the ecore file in the referenced projects
				IProject project = xmiFile.getProject();
				try {
					IJavaProject javaProject = JavaCore.create(project);
					IClasspathEntry[] entries;
					try {
						entries = javaProject.getResolvedClasspath(true);
					} catch (JavaModelException e1) {
						entries = new IClasspathEntry[] {};
					}
					for (int i = 0; i < entries.length; i++) {
						IClasspathEntry entry = entries[i];
						if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
							IProject referencedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().toString());
							if (referencedProject.exists()) {
								IFile[] files = Resources.members(referencedProject, new String[] { "ecore" }); //$NON-NLS-1$
								if (files.length > 1) {
									AcceleoEcorePlugin.getDefault().log(AcceleoEcoreMessages.getString("ELoaderUtils.MetamodelConflict", new Object[] { referencedProject.getName(), }), true); //$NON-NLS-1$
								} else if (files.length == 1) {
									return files[0];
								}
							}
						}
					}
				} catch (JavaModelException e) {
					// continue
				}
				String[] requiredPluginIDs = Resources.getRequiredPluginIDs(project);
				for (int i = 0; i < requiredPluginIDs.length; i++) {
					IProject referencedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(requiredPluginIDs[i]);
					if (referencedProject != null && referencedProject.exists()) {
						IFile[] files = Resources.members(referencedProject, new String[] { "ecore" }); //$NON-NLS-1$
						if (files.length > 1) {
							AcceleoEcorePlugin.getDefault().log(AcceleoEcoreMessages.getString("ELoaderUtils.MetamodelConflict", new Object[] { referencedProject.getName(), }), true); //$NON-NLS-1$
						} else if (files.length == 1) {
							return files[0];
						}
					}
				}
			} catch (CoreException e) {
				AcceleoEcorePlugin.getDefault().log(e, false);
			}
		}
		return null;
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
	 * Initialize factories for the given model file.
	 * 
	 * @param model
	 *            is the model file
	 * @param loader
	 *            is the current class loader
	 */
	public static void initModelFactories(IFile model, ClassLoader loader) {
		initMetamodelFactories(xmi2ecore(model), loader);
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
