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
package fr.obeo.acceleo.gen.template.profile;

import fr.obeo.acceleo.gen.profiler.LoopProfileEntry;
import fr.obeo.acceleo.gen.profiler.ProfileEntry;
import fr.obeo.acceleo.gen.profiler.ProfileResource;
import fr.obeo.acceleo.gen.profiler.ProfilerFactory;
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateElement;
import fr.obeo.acceleo.gen.template.TemplateText;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallSetExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateLiteralExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateNotExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateOperatorExpression;
import fr.obeo.acceleo.gen.template.expressions.TemplateParenthesisExpression;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalJavaService;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalModel;
import fr.obeo.acceleo.gen.template.statements.TemplateCommentStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateFeatureStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateForStatement;
import fr.obeo.acceleo.gen.template.statements.TemplateIfStatement;
import fr.obeo.acceleo.tools.resources.Resources;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * The default template profiler.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 * 
 */
public class TemplateProfiler implements ITemplateProfiler {
	private final class Context {
		/**
		 * Cache Object -> Context.
		 */
		private final Map childrenCache = new HashMap();

		private final Context parent;

		/**
		 * Current call entry.
		 */
		private final LoopProfileEntry currentEntry;

		/**
		 * Constructor.
		 * 
		 * @param parent
		 *            parent context if any
		 * @param monitored
		 *            object to profile
		 */
		public Context(Context parent, LoopProfileEntry entry) {
			this.parent = parent;
			currentEntry = entry;
		}

		/**
		 * Getter for the Profiling entry that match the given object.
		 * 
		 * @param monitored
		 *            the object to monitor
		 * @return the Profiling entry
		 */
		public Context getChildContext(Object monitored) {
			Context childContext = (Context) childrenCache.get(monitored);
			if (childContext == null) {
				final LoopProfileEntry entry = ProfilerFactory.eINSTANCE.createLoopProfileEntry();
				entry.setCreateTime(System.currentTimeMillis());
				entry.setMonitored(getString(monitored));
				if (monitored instanceof TemplateElement) {
					entry.setTextBegin(((TemplateElement) monitored).getPos().b());
					entry.setTextEnd(((TemplateElement) monitored).getPos().e());
				}
				childContext = new Context(this, entry);
				childrenCache.put(monitored, childContext);
			}
			return childContext;
		}

		/**
		 * Getter for the current entry.
		 * 
		 * @return the current entry
		 */
		public LoopProfileEntry getcurrentEntry() {
			return currentEntry;
		}

		/**
		 * Getter for the parent of this context.
		 * 
		 * @return the parent context
		 */
		public Context getParent() {
			return parent;
		}
	}

	/**
	 * The profiling currentContext stack.
	 */
	private Context currentContext;

	/**
	 * Profiling resource container.
	 */
	private ProfileResource resource;

	/**
	 * The current loop element to use.
	 */
	private ProfileEntry currentLoopEntry;

	/* (non-Javadoc) */
	public void loop(Object loopElement) {
		final LoopProfileEntry entry = currentContext.getcurrentEntry();

		stopCurrentLoopEntry();
		startCurrentLoopEntry(loopElement);
		entry.getLoopElements().add(currentLoopEntry);
	}

	/* (non-Javadoc) */
	public void loop() {
		currentContext.getcurrentEntry().setCount(currentContext.getcurrentEntry().getCount() + 1);
	}

	/**
	 * Stop the current loop element profiling.
	 */
	private void stopCurrentLoopEntry() {
		if (currentLoopEntry != null) {
			currentLoopEntry.stop();
			currentLoopEntry = null;
		}
	}

	/**
	 * Start the profiling for the new loop element.
	 * 
	 * @param loopElement
	 *            the new loop element to profile
	 */
	private void startCurrentLoopEntry(Object loopElement) {
		currentLoopEntry = ProfilerFactory.eINSTANCE.createProfileEntry();
		currentLoopEntry.setCreateTime(System.currentTimeMillis());
		currentLoopEntry.setMonitored(getString(loopElement));
		currentLoopEntry.start();
	}

	/* (non-Javadoc) */
	public void start(Object monitored) {
		final Context nextContext;
		if (currentContext != null) {
			nextContext = currentContext.getChildContext(monitored);
			nextContext.getcurrentEntry().start();
			currentContext.getcurrentEntry().getCallees().add(nextContext.getcurrentEntry());
		} else {
			final LoopProfileEntry entry = ProfilerFactory.eINSTANCE.createLoopProfileEntry();
			entry.setCreateTime(System.currentTimeMillis());
			entry.setMonitored(getString(monitored));
			if (monitored instanceof TemplateElement) {
				entry.setTextBegin(((TemplateElement) monitored).getPos().b());
				entry.setTextEnd(((TemplateElement) monitored).getPos().e());
			}
			if (resource == null) {
				resource = ProfilerFactory.eINSTANCE.createProfileResource();
			}
			resource.getEntries().add(entry);
			entry.start();
			nextContext = new Context(null, entry);
		}
		currentContext = nextContext;
	}

	/**
	 * get a string representation of the monitored element.
	 * 
	 * @param monitored
	 *            the monitored element
	 * @return a string representation of the monitored element
	 */
	protected String getString(Object monitored) {
		if (monitored instanceof String) {
			return (String) monitored;
		} else if (monitored instanceof SpecificScript) {
			final IWorkspace workspace = ResourcesPlugin.getWorkspace();
			final IPath location = Path.fromOSString(((SpecificScript) monitored).getFile().getAbsolutePath());
			final IFile file = workspace.getRoot().getFileForLocation(location);
			if (file != null) {
				return "Template : " + file.getFullPath(); //$NON-NLS-1$
			} else {
				return "Template : " + location.toOSString(); //$NON-NLS-1$
			}
		} else if (monitored instanceof EvalJavaService) {
			return "Service : " + ((EvalJavaService) monitored).getInstance().getClass().getName(); //$NON-NLS-1$
		} else if (monitored instanceof Method) {
			return "Service : " + ((Method) monitored).getName() + "()"; //$NON-NLS-1$ //$NON-NLS-2$
		} else if (monitored instanceof EvalModel) {
			return "Model loading : " + ((EvalModel) monitored).getUri(); //$NON-NLS-1$
		} else if (monitored instanceof Template) {
			if (((Template) monitored).getDescriptor() != null) {
				return "Script : " + ((Template) monitored).getDescriptor().getName(); //$NON-NLS-1$
			} else {
				return "Script : "; //$NON-NLS-1$
			}
		} else if (monitored instanceof TemplateCallExpression) {
			return "Call : " + ((TemplateCallExpression) monitored).getLink(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateCallSetExpression) {
			return "CallSet : " + monitored.toString(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateLiteralExpression) {
			return "Literal : " + ((TemplateLiteralExpression) monitored).getValue(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateNotExpression) {
			return "Not : " + monitored.toString(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateOperatorExpression) {
			return "Operator : " + ((TemplateOperatorExpression) monitored).getOperator(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateParenthesisExpression) {
			return "Parenthesis : " + monitored.toString(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateCommentStatement) {
			return "Comment : "; //$NON-NLS-1$
		} else if (monitored instanceof TemplateFeatureStatement) {
			return "Feature : " + monitored.toString(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateForStatement) {
			return "For : " + ((TemplateForStatement) monitored).getCondition().toString(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateIfStatement) {
			return "If : " + ((TemplateIfStatement) monitored).getCondition().toString(); //$NON-NLS-1$
		} else if (monitored instanceof TemplateText) {
			return "Text : "; //$NON-NLS-1$
		} else if (monitored instanceof IFile) {
			return ((IFile) monitored).getFullPath().toString();
		} else if (monitored instanceof EObject) {
			int pos = monitored.toString().indexOf('(');
			if (pos < 0) {
				pos = 0;
			}
			return ((EObject) monitored).eClass().getName() + " : " + monitored.toString().substring(pos); //$NON-NLS-1$
		} else if (monitored == null) {
			return "Null"; //$NON-NLS-1$
		} else {
			return "Unknown : " + monitored.toString(); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc) */
	public void reset() {
		currentContext = null;
		resource = null;
	}

	/* (non-Javadoc) */
	public void stop() {
		currentContext.getcurrentEntry().stop();
		stopCurrentLoopEntry();
		currentContext = currentContext.getParent();
	}

	/* (non-Javadoc) */
	public void save(String modelURI) throws IOException {
		addDefaultNodes();
		computePercentage();
		save(resource, modelURI);
	}

	private void computePercentage() {
		final Iterator itRoots = resource.getEntries().iterator();
		while (itRoots.hasNext()) {
			final ProfileEntry root = (ProfileEntry) itRoots.next();
			final long baseTime = root.getDuration();
			root.setPercentage(100.0);
			final Iterator itContent = root.eAllContents();
			while (itContent.hasNext()) {
				final ProfileEntry node = (ProfileEntry) itContent.next();
				node.setPercentage(node.getDuration() * 100.0 / baseTime);
			}
		}
	}

	/**
	 * Add defult entries to reach 100%.
	 */
	protected void addDefaultNodes() {
		final Iterator itRoots = resource.getEntries().iterator();
		while (itRoots.hasNext()) {
			final ProfileEntry root = (ProfileEntry) itRoots.next();
			addDefaultNodes(root);
		}
	}

	/**
	 * Add defult entries to reach 100%.
	 * 
	 * @param entry
	 *            the root node
	 * @param totalDuration
	 *            the duration corresponding to 100%.
	 */
	private void addDefaultNodes(ProfileEntry entry) {
		long childrenDuration = 0;
		Iterator children = entry.getCallees().iterator();
		while (children.hasNext()) {
			ProfileEntry child = (ProfileEntry) children.next();
			childrenDuration += child.getDuration();
			addDefaultNodes(child);
		}
		if (entry.getCallees().size() > 0 && entry.getDuration() - childrenDuration > 0) {
			ProfileEntry def = ProfilerFactory.eINSTANCE.createProfileEntry();
			def.setCreateTime(System.currentTimeMillis());
			def.setDuration(entry.getDuration() - childrenDuration);
			def.setMonitored("Internal"); //$NON-NLS-1$
			entry.getCallees().add(def);
		}
	}

	/**
	 * Serialize the current profiling data.
	 * 
	 * @param result
	 *            root element for profiling data
	 * @param modelURI
	 *            URI where to save data
	 * @throws IOException
	 *             if save fail
	 */
	private void save(EObject result, String modelURI) throws IOException {
		final ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		final Resource newModelResource = resourceSet.createResource(Resources.createPlatformResourceURI(modelURI));
		newModelResource.getContents().add(result);
		final Map options = new HashMap();
		newModelResource.save(options);
		newModelResource.unload();
	}
}
