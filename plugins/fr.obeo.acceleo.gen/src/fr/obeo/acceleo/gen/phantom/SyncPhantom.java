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

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.tools.resources.Resources;

/**
 * This phantom structure is used with the synchronization tools of acceleo.
 * This structure implements the interface Serializable, so we can save it in a
 * stream.
 * 
 * @author www.obeo.fr
 * 
 */
public abstract class SyncPhantom implements Externalizable {

	/**
	 * The identifier of the internal extension point specifying the
	 * implementation to use with acceleo synchronization tools.
	 */
	public static final String SYNC_PHANTOM_EXTENSION_ID = "fr.obeo.acceleo.gen.syncphantom"; //$NON-NLS-1$

	/**
	 * The folder to put the generated phantoms.
	 */
	public static final String PHANTOM_GENERATED_FOLDER = PhantomResources.getRootName() + "/generated"; //$NON-NLS-1$

	/**
	 * The extension of the phantoms.
	 */
	public static final String PHANTOM_SYNC_FILE_EXTENSION = "sync"; //$NON-NLS-1$

	/**
	 * The project relative path of this file.
	 */
	protected String projectRelativePath;

	/**
	 * The model path.
	 */
	protected String model;

	/**
	 * The script path.
	 */
	protected String script;

	/**
	 * The target path.
	 */
	protected String target;

	/**
	 * The chain path.
	 */
	protected String chain;

	/**
	 * The generation time.
	 */
	protected long time;

	/**
	 * @return the chain.
	 */
	public String getChain() {
		return chain;
	}

	/**
	 * @param chain
	 *            is the chain to set.
	 */
	public void setChain(String chain) {
		this.chain = chain;
	}

	/**
	 * Puts the given structure in the phantom.
	 * 
	 * @param syncElement
	 *            is the structure to put in the phantom
	 */
	public abstract void setEval(SyncElement syncElement);

	/**
	 * @return the model.
	 */
	public String getModel() {
		return model;
	}

	/**
	 * @param model
	 *            is the model to set.
	 */
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * @return the projectRelativePath.
	 */
	public String getProjectRelativePath() {
		return projectRelativePath;
	}

	/**
	 * @param projectRelativePath
	 *            is the project relative path to set.
	 */
	public void setProjectRelativePath(String projectRelativePath) {
		this.projectRelativePath = projectRelativePath;
	}

	/**
	 * @return the script.
	 */
	public String getScript() {
		return Resources.decodeAcceleoAbsolutePath(script);
	}

	/**
	 * @param script
	 *            is the script to set.
	 */
	public void setScript(String script) {
		this.script = Resources.encodeAcceleoAbsolutePath(script);
	}

	/**
	 * @return the target.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target
	 *            is the target to set.
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * @return the generation time.
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param time
	 *            is the generation time
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * It uses the extension points to create phantom.
	 * 
	 * @param file
	 *            is the generated file
	 * @param model
	 *            is the model file
	 * @param script
	 *            is the script file
	 * @param target
	 *            is the target container
	 * @param chain
	 *            is the chain file
	 * @param eval
	 *            is the node to synchronize
	 * @param monitor
	 *            is the progress monitor
	 * @throws CoreException
	 */
	public static void createPhantomExtensionPoint(IFile file, IFile model, File script, IContainer target, IFile chain, SyncElement eval, IProgressMonitor monitor) throws CoreException {
		createPhantomExtensionPoint(file, model, script, target, chain, eval, 0, monitor);
	}

	/**
	 * It uses the extension points to create phantom.
	 * 
	 * @param file
	 *            is the generated file
	 * @param model
	 *            is the model file
	 * @param script
	 *            is the script file
	 * @param target
	 *            is the target container
	 * @param chain
	 *            is the chain file
	 * @param eval
	 *            is the node to synchronize
	 * @param time
	 *            is the time to generate the file
	 * @param monitor
	 *            is the progress monitor
	 * @throws CoreException
	 */
	public static void createPhantomExtensionPoint(IFile file, IFile model, File script, IContainer target, IFile chain, SyncElement eval, long time, IProgressMonitor monitor) throws CoreException {
		IPath path = new Path(PHANTOM_GENERATED_FOLDER).append(file.getProjectRelativePath()).addFileExtension(PHANTOM_SYNC_FILE_EXTENSION);
		SyncPhantom phantom = null;
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(SYNC_PHANTOM_EXTENSION_ID);
		if (extensionPoint == null) {
			AcceleoEcoreGenPlugin.getDefault().log(AcceleoGenMessages.getString("UnresolvedExtensionPoint", new Object[] { SYNC_PHANTOM_EXTENSION_ID, }), true); //$NON-NLS-1$
		} else {
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IExtension extension = extensions[i];
				IConfigurationElement[] members = extension.getConfigurationElements();
				for (int j = 0; j < members.length; j++) {
					IConfigurationElement member = members[j];
					String projectNature = member.getAttribute("projectNature"); //$NON-NLS-1$
					if (projectNature != null && file.getProject().hasNature(projectNature)) {
						String syncClass = member.getAttribute("syncClass"); //$NON-NLS-1$
						if (syncClass != null && syncClass.equals(eval.getClass().getName())) {
							String phantomClass = member.getAttribute("phantomClass"); //$NON-NLS-1$
							if (phantomClass != null) {
								Bundle bundle = Platform.getBundle(member.getNamespace());
								if (bundle == null) {
									bundle = AcceleoEcoreGenPlugin.getDefault().getBundle();
								}
								try {
									Class c = bundle.loadClass(phantomClass);
									Object instance = c.newInstance();
									if (instance instanceof SyncPhantom) {
										phantom = (SyncPhantom) instance;
										phantom.setProjectRelativePath(path.toString());
										phantom.setModel((model != null) ? model.getFullPath().toString() : ""); //$NON-NLS-1$
										phantom.setScript((script != null) ? script.getAbsolutePath() : ""); //$NON-NLS-1$
										phantom.setTarget((target != null) ? target.getFullPath().toString() : ""); //$NON-NLS-1$
										phantom.setChain((chain != null) ? chain.getFullPath().toString() : ""); //$NON-NLS-1$
										phantom.setTime(time);
										phantom.setEval(eval);
									}
								} catch (ClassNotFoundException e) {
									AcceleoEcoreGenPlugin.getDefault().log(e, true);
								} catch (InstantiationException e) {
									AcceleoEcoreGenPlugin.getDefault().log(e, true);
								} catch (IllegalAccessException e) {
									AcceleoEcoreGenPlugin.getDefault().log(e, true);
								}
							}
						}
					}
				}
			}
		}
		if (phantom != null) {
			Resources.createFile(file.getProject(), path, phantom, monitor);
		}
	}

	/* (non-Javadoc) */
	public void readExternal(ObjectInput arg) throws IOException, ClassNotFoundException {
		projectRelativePath = arg.readUTF();
		model = arg.readUTF();
		script = arg.readUTF();
		target = arg.readUTF();
		chain = arg.readUTF();
		time = arg.readLong();
	}

	/* (non-Javadoc) */
	public void writeExternal(ObjectOutput arg) throws IOException {
		arg.writeUTF(projectRelativePath);
		arg.writeUTF(model);
		arg.writeUTF(script);
		arg.writeUTF(target);
		arg.writeUTF(chain);
		arg.writeLong(time);
	}

}
