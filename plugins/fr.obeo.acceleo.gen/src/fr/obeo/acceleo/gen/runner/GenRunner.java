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

package fr.obeo.acceleo.gen.runner;

import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.tools.resources.Resources;

import java.io.File;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Platform runnables represent executable entry points into plug-ins. This
 * class can be used to launch a module outside of eclipse.
 * <p>
 * The command to launch one generator : java -cp startup.jar
 * org.eclipse.core.launcher.Main -application fr.obeo.acceleo.gen.launch -gen
 * ./acceleo.ini
 * <p>
 * 
 * @author <a href="mailto:jonathan.musset@obeo.fr">Jonathan Musset</a>
 * 
 */
public class GenRunner implements IPlatformRunnable {

	/**
	 * Temporary project name. A temporary project is created to simulate a
	 * workspace resource in the standalone context
	 */
	private static final String projectName = "AcceleoGenerations"; //$NON-NLS-1$

	/**
	 * Temporary target folder name.
	 */
	private static final String targetName = "src-gen"; //$NON-NLS-1$

	/**
	 * Temporary model folder name.
	 */
	private static final String modelFolderName = "model"; //$NON-NLS-1$

	/**
	 * Report file name. It is often created in the target folder.
	 */
	private static final String reportName = "report.txt"; //$NON-NLS-1$

	/**
	 * Argument to declare a new configuration file in the command line. It is
	 * an optional argument. We can use this argument several times in the
	 * command line.
	 */
	private static final String acceleoIniCmd = "-gen"; //$NON-NLS-1$

	/**
	 * Default name of the configuration file.
	 */
	private static final String acceleoIniDefaultName = "acceleo.ini"; //$NON-NLS-1$

	/**
	 * Declaration of the model in the configuration file. Each "model" line
	 * starts with this tag.
	 */
	private static final String acceleoIniLineModel = "model="; //$NON-NLS-1$

	/**
	 * Declaration of the target folder in the configuration file. Each "target
	 * folder" line starts with this tag.
	 */
	private static final String acceleoIniLineTarget = "folder="; //$NON-NLS-1$

	/**
	 * Declaration of the properties container in the configuration file. Each
	 * "properties container" line starts with this tag.
	 */
	private static final String acceleoIniLineProperties = "properties="; //$NON-NLS-1$

	/**
	 * Declaration of the generate action in the configuration file. Each
	 * "generate action" line starts with this tag.
	 */
	private static final String acceleoIniLineGenerate = "generate="; //$NON-NLS-1$

	/**
	 * Plugin relative path of the template file to generate.
	 */
	private String modulePluginPath = ""; //$NON-NLS-1$

	/**
	 * Get the generation launcher for each template file.
	 */
	private Map module2AcceleoGenerate = new WeakHashMap();

	/**
	 * The absolute path in the local file system to the model resource.
	 */
	private String modelLocation = ""; //$NON-NLS-1$

	/**
	 * Loaded model.
	 */
	private EObject loadedModel;

	/**
	 * The absolute path in the local file system to the loaded model.
	 */
	private String loadedModelLocation = ""; //$NON-NLS-1$

	/**
	 * The absolute path in the local file system to the target resource.
	 */
	private String targetLocation = ""; //$NON-NLS-1$

	/**
	 * The absolute path in the local file system to the properties container.
	 */
	private String propertiesLocation = ""; //$NON-NLS-1$

	/* (non-Javadoc) */
	public Object run(Object args) throws Exception {
		try {
			final String[] arguments = (String[]) args;
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IProject project = Resources.createSimpleProject(projectName);
					try {
						String installLocation = Platform.getInstallLocation().getURL().getPath();
						String acceleoIniDefaultPath = new Path(installLocation).append(acceleoIniDefaultName).toString();
						String acceleoIniPath = null;
						for (int i = 0; i < arguments.length - 1; i++) {
							if (arguments[i].equalsIgnoreCase(acceleoIniCmd)) {
								acceleoIniPath = arguments[++i];
								acceleoIniPath = convertPath(acceleoIniDefaultPath, acceleoIniPath);
								runAcceleoIni(project, acceleoIniPath, monitor);
							}
						}
						if (acceleoIniPath == null) {
							acceleoIniPath = acceleoIniDefaultPath;
							runAcceleoIni(project, acceleoIniPath, monitor);
						}
					} finally {
						loadedModel = null;
						loadedModelLocation = ""; //$NON-NLS-1$
						project.delete(true, monitor);
					}
				}
			};
			workspace.run(runnable, new NullProgressMonitor());
			return IPlatformRunnable.EXIT_OK;
		} catch (CoreException e) {
			e.printStackTrace();
			AcceleoEcoreGenPlugin.getDefault().log(e, false);
			return new Integer(1);
		}
	}

	/**
	 * Creates the configuration settings by reading each line of the
	 * 'acceleo.ini' file.
	 * 
	 * @param project
	 *            is the project
	 * @param acceleoIniPath
	 *            is the acceleo configuration file
	 * @param monitor
	 *            is the monitor
	 * @throws CoreException
	 */
	private void runAcceleoIni(IProject project, String acceleoIniPath, IProgressMonitor monitor) throws CoreException {
		File acceleoIniFile = new Path(acceleoIniPath).toFile();
		if (!acceleoIniFile.exists()) {
			System.err.println(AcceleoGenMessages.getString("AcceleoGenerate.MissingAcceleoIni", new Object[] { acceleoIniPath, })); //$NON-NLS-1$
		} else {
			String acceleoIniContent = Resources.getFileContent(acceleoIniFile).toString();
			StringTokenizer st = new StringTokenizer(acceleoIniContent, "\n"); //$NON-NLS-1$
			int lineCount = 0;
			while (st.hasMoreTokens()) {
				String line = st.nextToken().trim();
				lineCount++;
				if (line.length() > 0 && !line.startsWith("#")) { //$NON-NLS-1$
					System.out.println(line);
					if (line.startsWith(acceleoIniLineModel)) {
						modelLocation = convertPath(acceleoIniPath, line.substring(acceleoIniLineModel.length()).trim());
					} else if (line.startsWith(acceleoIniLineTarget)) {
						targetLocation = convertPath(acceleoIniPath, line.substring(acceleoIniLineTarget.length()).trim());
					} else if (line.startsWith(acceleoIniLineProperties)) {
						propertiesLocation = convertPath(acceleoIniPath, line.substring(acceleoIniLineProperties.length()).trim());
					} else if (line.startsWith(acceleoIniLineGenerate)) {
						modulePluginPath = line.substring(acceleoIniLineGenerate.length()).trim();
						if (!project.isAccessible() || targetLocation == null || targetLocation.length() == 0) {
							System.err.println(AcceleoGenMessages.getString("AcceleoGenerate.MissingTarget", new Object[] { targetLocation, })); //$NON-NLS-1$
						} else {
							runGenerateLine(project, monitor);
						}
					} else {
						System.err.println(AcceleoGenMessages.getString("AcceleoGenerate.AcceleoIniLineIssue", new Object[] { new Integer(lineCount), acceleoIniPath, })); //$NON-NLS-1$						
					}
				}
			}
		}
	}

	/**
	 * Computes an absolute path by replacing '..' and '.' prefixes.
	 * 
	 * @param acceleoIniPath
	 *            is the base folder path
	 * @param path
	 *            is the path to convert
	 * @return an absolute path
	 */
	private String convertPath(String acceleoIniPath, String path) {
		if (path != null) {
			if (path.startsWith("..")) { //$NON-NLS-1$
				IPath newPath = new Path(acceleoIniPath).removeLastSegments(1);
				String[] segments = new Path(path).segments();
				for (int i = 0; i < segments.length; i++) {
					if ("..".equals(segments[i])) { //$NON-NLS-1$
						newPath = newPath.removeLastSegments(1);
					} else {
						newPath = newPath.append(segments[i]);
					}
				}
				path = newPath.toString();
			} else if (path.equals(".")) { //$NON-NLS-1$
				path = new Path(acceleoIniPath).removeLastSegments(1).toString();
			} else if (path.startsWith(".")) { //$NON-NLS-1$
				path = new Path(acceleoIniPath).removeLastSegments(1).append(path.substring(1)).toString();
			}
		}
		return path;
	}

	private void runGenerateLine(IProject project, IProgressMonitor monitor) throws CoreException {
		File targetLocationFile = new Path(targetLocation).toFile();
		if (!targetLocationFile.exists()) {
			targetLocationFile.mkdirs();
		}
		if (targetLocationFile.exists()) {
			IFolder targetFolder = project.getFolder(targetName);
			if (targetFolder.exists()) {
				targetFolder.delete(true, monitor);
			}
			targetFolder.createLink(new Path(targetLocation), IResource.ALLOW_MISSING_LOCAL, monitor);
			StringBuffer report = new StringBuffer();
			try {
				if (modelLocation == null || modelLocation.length() == 0 || (!new Path(modelLocation).toFile().exists())) {
					report.append(AcceleoGenMessages.getString("AcceleoGenerate.MissingModel", new Object[] { modelLocation, })); //$NON-NLS-1$
					report.append('\n');
				} else if (modulePluginPath == null || modulePluginPath.length() == 0) {
					report.append(AcceleoGenMessages.getString("AcceleoGenerate.MissingTemplate", new Object[] { modulePluginPath, })); //$NON-NLS-1$
					report.append('\n');
				} else {
					EObject model;
					if (modelLocation.equals(loadedModelLocation)) {
						model = loadedModel;
					} else {
						IFolder modelFolder = project.getFolder(modelFolderName);
						if (modelFolder.exists()) {
							modelFolder.delete(true, monitor);
						}
						modelFolder.createLink(new Path(modelLocation).removeLastSegments(1), IResource.ALLOW_MISSING_LOCAL, monitor);
						IFile modelFile = modelFolder.getFile(new Path(modelLocation).lastSegment());
						model = loadXMI(modelFile.getFullPath().toString());
						if (model == null) {
							report.append(AcceleoGenMessages.getString("AcceleoGenerate.MissingModel", new Object[] { modelLocation, })); //$NON-NLS-1$
							report.append('\n');
						} else {
							loadedModelLocation = modelLocation;
							loadedModel = model;
						}
					}
					if (model != null) {
						File propertiesContainer;
						if (propertiesLocation != null && propertiesLocation.length() > 0) {
							propertiesContainer = new Path(propertiesLocation).toFile();
							if (!propertiesContainer.exists()) {
								propertiesContainer = null;
								report.append(AcceleoGenMessages.getString("AcceleoGenerate.UnresolvedProperties", new Object[] { propertiesLocation, })); //$NON-NLS-1$
								report.append('\n');
							}
						} else {
							propertiesContainer = null;
						}
						AcceleoGenerate gen = (AcceleoGenerate) module2AcceleoGenerate.get(modulePluginPath);
						if (gen == null) {
							gen = createGenerator(modulePluginPath);
							module2AcceleoGenerate.put(modulePluginPath, gen);
						}
						gen.generate(model, targetFolder, propertiesContainer, report, monitor);
					}
				}
			} catch (CoreException e) {
				report.append(e.getMessage());
				report.append('\n');
				throw e;
			} finally {
				report(targetFolder, report, monitor);
			}
		}
	}

	/**
	 * Creates a generation launcher for the given template file.
	 * 
	 * @return the plugin relative path of the generator
	 */
	protected AcceleoGenerate createGenerator(String templateID) {
		return new AcceleoGenerate(templateID);
	}

	/**
	 * Loads an EMF model, using a specific resource factory (using UML resource
	 * for an XMI file)
	 * 
	 * @param path
	 *            is the absolute path of the model to load
	 * 
	 * @return the root element of the model
	 */
	private EObject loadXMI(String path) {
		URI modelURI = Resources.createPlatformResourceURI(path);
		String fileExtension = modelURI.fileExtension();
		if (fileExtension == null || fileExtension.length() == 0) {
			fileExtension = Resource.Factory.Registry.DEFAULT_EXTENSION;
		}
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Object resourceFactory = reg.getExtensionToFactoryMap().get(fileExtension);
		if (resourceFactory != null) {
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension, resourceFactory);
		} else {
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(fileExtension, new XMIResourceFactoryImpl());
		}
		Resource modelResource = resourceSet.getResource(modelURI, true);
		// EcoreUtil.resolveAll(resourceSet);
		return (EObject) ((modelResource.getContents().size() > 0) ? modelResource.getContents().get(0) : null);
	}

	/**
	 * Reports an error in the log.
	 * 
	 * @param targetFolder
	 *            is the generation target folder
	 * @param report
	 *            is the text to report
	 * @param monitor
	 *            is the monitor
	 * @throws CoreException
	 */
	private void report(IFolder targetFolder, StringBuffer report, IProgressMonitor monitor) throws CoreException {
		if (report.length() > 0) {
			Resources.appendFile(targetFolder, new Path(reportName), report.toString(), monitor);
			System.err.println(report.toString());
		}
	}

}
