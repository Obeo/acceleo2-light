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

package fr.obeo.acceleo.gen.phantom;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.tools.resources.FileContentMap;
import fr.obeo.acceleo.tools.resources.Resources;

/**
 * The .launcher files provider.
 * 
 * @author www.obeo.fr
 * 
 */
public class ChainPhantomProvider {

	/**
	 * The phantom path relative to the project.
	 */
	private static final String PHANTOM_CHAIN_FILE = PhantomResources.getRootName() + "/.launcher"; //$NON-NLS-1$

	/**
	 * Gets the sole instance.
	 * 
	 * @return the sole instance
	 */
	public static ChainPhantomProvider getDefault() {
		if (instance == null) {
			instance = new ChainPhantomProvider();
		}
		return instance;
	}

	/**
	 * The sole instance.
	 */
	private static ChainPhantomProvider instance;

	/**
	 * Gets the phantom of the container.
	 */
	private FileContentMap project2Phantom = new FileContentMap();

	/**
	 * Constructor.
	 */
	private ChainPhantomProvider() {
	}

	/**
	 * Gets the phantom of the given project.
	 * 
	 * @param project
	 *            is the project
	 * @return the phantom or an empty phantom if it doesn't exist
	 * @throws CoreException
	 */
	private ChainPhantom getPhantom(IProject project) throws CoreException {
		IPath path = new Path(PHANTOM_CHAIN_FILE);
		IFile phantomChainFile = project.getFile(path);
		ChainPhantom phantom = (ChainPhantom) project2Phantom.get(phantomChainFile);
		if (phantom == null) {
			phantom = readPhantom(phantomChainFile);
			if (phantom == null) {
				phantom = new ChainPhantom();
			}
			project2Phantom.put(phantomChainFile, phantom);
		}
		return phantom;
	}

	/**
	 * Gets the phantom in the given file.
	 * 
	 * @param file
	 *            is the phantom file
	 * @return the phantom or null if it doesn't exist
	 * @throws CoreException
	 */
	private ChainPhantom readPhantom(IFile file) throws CoreException {
		if (file != null && file.exists()) {
			FileInputStream fis = null;
			ObjectInputStream in = null;
			try {
				fis = new FileInputStream(file.getLocation().toString());
				in = new ObjectInputStream(fis);
				ChainPhantom result = (ChainPhantom) in.readObject();
				in.close();
				return result;
			} catch (IOException e) {
				return null;
			} catch (ClassNotFoundException e) {
				throw new CoreException(new Status(IStatus.ERROR, AcceleoEcoreGenPlugin.getDefault().getID(), -1, AcceleoGenMessages.getString("ChainPhantomProvider.InvalidFile"), e)); //$NON-NLS-1$
			}
		} else {
			return null;
		}
	}

	/**
	 * Saves the phantom in the given project.
	 * 
	 * @param project
	 *            is the project
	 * @param phantom
	 *            is the phantom to save
	 * @param monitor
	 *            is the progress monitor
	 * @throws CoreException
	 */
	private void savePhantom(IProject project, ChainPhantom phantom, IProgressMonitor monitor) throws CoreException {
		boolean projectIsValid = false;
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(SyncPhantom.SYNC_PHANTOM_EXTENSION_ID);
		if (extensionPoint != null) {
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length && !projectIsValid; i++) {
				IExtension extension = extensions[i];
				IConfigurationElement[] members = extension.getConfigurationElements();
				for (int j = 0; j < members.length && !projectIsValid; j++) {
					IConfigurationElement member = members[j];
					String projectNature = member.getAttribute("projectNature"); //$NON-NLS-1$
					if (projectNature != null && project.hasNature(projectNature)) {
						projectIsValid = true;
					}
				}
			}
		}
		if (projectIsValid) {
			Resources.createFile(project, new Path(PHANTOM_CHAIN_FILE), phantom, monitor);
		}
	}

	/**
	 * Adds files generated by the given chain.
	 * 
	 * @param chain
	 *            is the chain
	 * @param generated
	 *            are the generated files
	 * @param monitor
	 *            is the progress monitor
	 * @throws CoreException
	 */
	public void add(IFile chain, IFile[] generated, IProgressMonitor monitor) throws CoreException {
		IProject project = null;
		ChainPhantom phantom = null;
		for (int i = 0; i < generated.length; i++) {
			if (project == null) {
				project = generated[i].getProject();
				phantom = getPhantom(project);
			} else if (!project.equals(generated[i].getProject())) {
				savePhantom(project, phantom, monitor);
				project = generated[i].getProject();
				phantom = getPhantom(project);
			}
			phantom.add(chain, generated[i]);
		}
		if (project != null) {
			savePhantom(project, phantom, monitor);
		}
	}

	/**
	 * Clears the context for the next update operation of the given chain.
	 * 
	 * @param chain
	 *            is the chain
	 */
	public void clearForUpdate(IFile chain) {
		projects.clear();
		lastChain = chain;
	}

	private List projects = new ArrayList();

	private IFile lastChain = null;

	/**
	 * Operation that replaces the generated files in the phantom.
	 * 
	 * @param chain
	 *            is the chain
	 * @param generated
	 *            are the generated files
	 * @param monitor
	 *            is the progress monitor
	 * @throws CoreException
	 */
	public void update(IFile chain, IFile[] generated, IProgressMonitor monitor) throws CoreException {
		if (chain != null && chain.equals(lastChain)) {
			for (int i = 0; i < generated.length; i++) {
				IProject project = generated[i].getProject();
				if (!projects.contains(project)) {
					projects.add(project);
					clear(project, chain, monitor);
				}
			}
			add(chain, generated, monitor);
		}
	}

	private void clear(IProject project, IFile chain, IProgressMonitor monitor) throws CoreException {
		ChainPhantom phantom = getPhantom(project);
		phantom.clear(chain);
		savePhantom(project, phantom, monitor);
	}

	/**
	 * Indicates if the given file has been generated.
	 * 
	 * @param generated
	 *            is the file to test
	 * @return true if the given file has been generated
	 * @throws CoreException
	 */
	public boolean isGenerated(IFile generated) throws CoreException {
		ChainPhantom phantom = getPhantom(generated.getProject());
		return phantom.isGenerated(generated);
	}

}