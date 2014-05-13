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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateSyntaxExceptions;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.merge.MergeTools;
import fr.obeo.acceleo.gen.template.scripts.AbstractScript;
import fr.obeo.acceleo.gen.template.scripts.EmptyScript;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.tools.classloaders.AcceleoClassLoader;
import fr.obeo.acceleo.tools.plugins.AcceleoModuleProvider;
import fr.obeo.acceleo.tools.resources.Resources;

/**
 * This class is used to generate files for a single template.
 * 
 * @author <a href="mailto:jonathan.musset@obeo.fr">Jonathan Musset</a>
 * 
 */
public class AcceleoGenerate {

	/**
	 * Plugin relative path of the template.
	 */
	private String templateID;

	/**
	 * Constructor.
	 * 
	 * @param templateID
	 *            the plugin relative path of the template
	 */
	public AcceleoGenerate(String templateID) {
		this.templateID = templateID;
	}

	/**
	 * Generates files from a model by using the current RCP product.
	 * 
	 * @param model
	 *            is the model
	 * @param target
	 *            is the target container where the files will be generated
	 * @param propertiesContainer
	 *            is the 'properties' files container
	 * @param report
	 *            is the error log
	 * @param monitor
	 *            is the monitor
	 * @throws CoreException
	 */
	public void generate(EObject model, IContainer target, final File propertiesContainer, StringBuffer report, IProgressMonitor monitor) throws CoreException {
		try {
			AcceleoClassLoader.setPreferredLoader(model);
			try {
				AbstractScript defaultGenSettings = createDefaultScript();
				IScript aScript = defaultGenSettings;
				File script = AcceleoModuleProvider.getDefault().getFile(new Path(templateID));
				if (script == null || !script.exists()) {
					String projectName = new Path(templateID).segment(0);
					IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					IPath newProjectLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(projectName);
					if (!newProject.exists() && newProjectLocation.toFile().exists()) {
						IProject project = Resources.createSimpleProject(projectName);
						project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						script = AcceleoModuleProvider.getDefault().getFile(new Path(templateID));
					}
					if (script == null || !script.exists()) {
						report.append(AcceleoGenMessages.getString("AcceleoGenerate.MissingTemplate", new Object[] { templateID, })); //$NON-NLS-1$
						report.append('\n');
					}
				} else {
					if (propertiesContainer != null) {
						aScript = new SpecificScript(script, null, null) {
							protected List getPropertyContainers() {
								List folders = new ArrayList();
								folders.add(propertiesContainer);
								folders.addAll(super.getPropertyContainers());
								return folders;
							}
						};
					} else {
						aScript = new SpecificScript(script, null, null);
					}
					aScript.reset();
					aScript.addImport(defaultGenSettings);
					defaultGenSettings.setSpecific(aScript);
				}
				if (aScript.isDefault() || aScript.isSpecific()) {
					generate(aScript, model, target, report, monitor, LaunchManager.create("run", false)); //$NON-NLS-1$
				} else {
					report.append(AcceleoGenMessages.getString("AcceleoGenerate.UnresolvedTemplate")); //$NON-NLS-1$
					report.append('\n');
				}
			} finally {
				AcceleoClassLoader.setPreferredLoader(null);
			}
		} catch (TemplateSyntaxExceptions e) {
			report.append(e.getMessage());
		} catch (FactoryException e) {
			report.append(e.getMessage());
		} catch (ENodeException e) {
			report.append(e.getMessage());
		}
		monitor.worked(10);
	}

	/**
	 * Creates a default script.
	 * 
	 * @return a default script
	 */
	private AbstractScript createDefaultScript() {
		return new EmptyScript();
	}

	/**
	 * Generates files from a model object by using the current RCP product.
	 * 
	 * @param aScript
	 *            is the current template file
	 * @param object
	 *            is the current model object to generate
	 * @param target
	 *            is the target container where the files will be generated
	 * @param report
	 *            is the error log
	 * @param monitor
	 *            is the monitor
	 * @param mode
	 *            is the generation mode (RUN_MODE or DEBUG_MODE)
	 * @throws CoreException
	 */
	private void generate(IScript aScript, EObject object, IContainer target, StringBuffer report, IProgressMonitor monitor, LaunchManager mode) throws FactoryException, ENodeException, CoreException {
		if (monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		if (aScript.isGenerated(object)) {
			IPath path = aScript.getFilePath(object, true);
			// path != null => Generate file
			if (path != null) {
				monitor.subTask(AcceleoGenMessages.getString("AcceleoGenerate.Task.Generate", new Object[] { target.getFile(path).getFullPath().toString(), })); //$NON-NLS-1$
				if (mode.getMode() == LaunchManager.DEBUG_MODE) {
					System.out.println(AcceleoGenMessages.getString("AcceleoGenerate.Task.Generate", new Object[] { target.getFile(path).getFullPath().toString(), })); //$NON-NLS-1$
				}
				ENode eval = evaluate(aScript, object, mode);
				if (eval.log().hasError()) {
					report.append("-> "); //$NON-NLS-1$
					report.append(AcceleoGenMessages.getString("AcceleoGenerate.ErrorReport", new Object[] { path, })); //$NON-NLS-1$
					report.append('\n');
					report.append(eval.log().toString());
					report.append('\n');
				}
				StringBuffer buffer = new StringBuffer(eval.asString());
				StringBuffer oldBuffer = Resources.getFileContent(target.getFile(path), false);
				if (oldBuffer.length() > 0) {
					String lostCode = MergeTools.merge(target.getFile(path), buffer, oldBuffer, MergeTools.DEFAULT_USER_BEGIN, MergeTools.DEFAULT_USER_END);
					if (lostCode.length() > 0) {
						lostCode = '[' + AcceleoGenMessages.getString("AcceleoGenerate.LostCode") + "] " + new Date().toString() + '\n' + lostCode + '\n'; //$NON-NLS-1$ //$NON-NLS-2$
						Resources.appendFile(target, path.addFileExtension(MergeTools.LOST_FILE_EXTENSION), lostCode, monitor).setDerived(true);
					}
				}
				Resources.createFile(target, path, buffer.toString(), monitor);
			}
		}
		generateSub(aScript, target, object, report, monitor, mode);
	}

	private void generateSub(IScript aScript, IContainer target, EObject object, StringBuffer report, IProgressMonitor monitor, LaunchManager mode) throws FactoryException, ENodeException,
			CoreException {
		Iterator contents = object.eContents().iterator();
		while (contents.hasNext()) {
			EObject content = (EObject) contents.next();
			generate(aScript, content, target, report, monitor, mode);
		}
	}

	/**
	 * Evaluates the template on the given EObject (but not on its children).
	 * The generation result is an ENode.
	 * 
	 * @param aScript
	 *            is the current template file
	 * @param object
	 *            is the input object of the generation
	 * @param mode
	 *            is the generation mode (RUN_MODE or DEBUG_MODE)
	 * @return the result node of generation
	 * @throws ENodeException
	 * @throws FactoryException
	 * @see ENode
	 */
	protected ENode evaluate(IScript aScript, EObject object, LaunchManager mode) throws FactoryException, ENodeException {
		Template template = aScript.getRootTemplate(object, true);
		if (template != null) {
			boolean withComment = aScript.isDefault() || !aScript.hasFileTemplate();
			if (withComment) {
				return template.evaluateWithComment(object, mode);
			} else {
				return template.evaluate(object, mode);
			}
		} else {
			return new ENode(ENode.EMPTY, object, Template.EMPTY, mode.isSynchronize());
		}
	}

}
