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

package fr.obeo.acceleo.gen.template;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.profile.ITemplateProfiler;
import fr.obeo.acceleo.gen.template.profile.TemplateProfiler;
import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.statements.TemplateFeatureStatement;
import fr.obeo.acceleo.tools.resources.MarkerUtilities;
import fr.obeo.acceleo.tools.resources.Resources;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * This is a template's element for the generation tool.
 * 
 * @author www.obeo.fr
 * 
 */
public abstract class TemplateElement {

	/**
	 * The script.
	 */
	protected IScript script;

	/**
	 * The parent element.
	 */
	protected TemplateElement parent = null;

	/**
	 * The children.
	 */
	protected List children = new ArrayList();

	/**
	 * The next element.
	 */
	protected TemplateElement next = null;

	/**
	 * The previous element.
	 */
	protected TemplateElement previous = null;

	/**
	 * The position in the script file.
	 */
	protected Int2 pos = Int2.NOT_FOUND;

	/**
	 * The line in the script file.
	 */
	protected Integer line = null;

	/**
	 * Profiler for all template evaluation.
	 */
	private static ITemplateProfiler profiler;

	/**
	 * Constructor.
	 * 
	 * @param script
	 *            is the script
	 */
	public TemplateElement(IScript script) {
		this.script = script;
	}

	/**
	 * @return the script
	 */
	public IScript getScript() {
		return script;
	}

	/**
	 * @return the position of this expression
	 */
	public Int2 getPos() {
		return this.pos;
	}

	/**
	 * @param pos
	 *            is the position of this expression
	 */
	public void setPos(Int2 pos) {
		this.pos = pos;
		line = null;
	}

	/**
	 * @return the line of this expression
	 */
	public int getLine() {
		if (line == null) {
			if (pos.b() == -1 || getScript() == null
					|| getScript().getFile() == null) {
				line = new Integer(0);
			} else {
				line = new Integer(TextSearch.getDefaultSearch().lineNumber(
						getScript().getFile(), pos.b()));
			}
		}
		return line.intValue();
	}

	/**
	 * Sets the parent element. This element is a child for the parent.
	 * 
	 * @param parent
	 *            is the parent element
	 */
	public void setParent(TemplateElement parent) {
		// ASSERT this.parent == null
		this.parent = parent;
		if (parent != null) {
			if (parent.children.size() > 0) {
				final TemplateElement previous = (TemplateElement) parent.children
						.get(parent.children.size() - 1);
				this.previous = previous;
				previous.next = this;
			}
			parent.children.add(this);
		}
	}

	/**
	 * @return the parent element
	 */
	public TemplateElement getParent() {
		return parent;
	}

	/**
	 * @return the previous element
	 */
	public TemplateElement getPrevious() {
		return previous;
	}

	/**
	 * @return the next element
	 */
	public TemplateElement getNext() {
		return next;
	}

	/**
	 * Returns a table of children.
	 * 
	 * @return a table of children
	 */
	public TemplateElement[] getChildren() {
		return (TemplateElement[]) children
				.toArray(new TemplateElement[children.size()]);
	}

	/**
	 * Returns a table of the children that are instance of the given class.
	 * 
	 * @param c
	 *            is a class
	 * @return a table of the children that are instance of the given class
	 */
	public TemplateElement[] getChildren(Class c) {
		final List result = new ArrayList();
		final Iterator it = children.iterator();
		while (it.hasNext()) {
			final TemplateElement element = (TemplateElement) it.next();
			if (c.isInstance(element)) {
				result.add(element);
			}
		}
		return (TemplateElement[]) result.toArray(new TemplateElement[result
				.size()]);
	}

	/**
	 * Returns a table of the children and the gateway children that are
	 * instance of the given classes.
	 * 
	 * @param classes
	 *            are the kept children
	 * @param gatewayClasses
	 *            are the gateway nodes that aren't kept, but their children are
	 *            tested
	 * @return a table of the children that are instance of the given classes
	 */
	public TemplateElement[] getChildren(Class[] classes, Class[] gatewayClasses) {
		final List result = new ArrayList();
		final Iterator it = children.iterator();
		while (it.hasNext()) {
			final TemplateElement element = (TemplateElement) it.next();
			boolean instance = false;
			for (int i = 0; i < gatewayClasses.length && !instance; i++) {
				final Class c = gatewayClasses[i];
				if (c.isInstance(element)) {
					result.addAll(Arrays.asList(element.getChildren(classes,
							gatewayClasses)));
					instance = true;
				}
			}
			for (int i = 0; i < classes.length && !instance; i++) {
				final Class c = classes[i];
				if (c.isInstance(element)) {
					result.add(element);
					instance = true;
				}
			}
		}
		return (TemplateElement[]) result.toArray(new TemplateElement[result
				.size()]);
	}

	/**
	 * Returns a list of all the children that are instance of the given class.
	 * The result contains also the children of the children...
	 * 
	 * @param c
	 *            is a class
	 * @return a list of all the children that are instance of the given class
	 */
	public List getAllElements(Class c) {
		final List result = new ArrayList();
		if (c.isInstance(this)) {
			result.add(this);
		}
		final Iterator it = children.iterator();
		while (it.hasNext()) {
			final TemplateElement element = (TemplateElement) it.next();
			result.addAll(element.getAllElements(c));
		}
		return result;
	}

	/**
	 * Returns the URI fragment of this template element.
	 * 
	 * @return the URI fragment of this template element
	 */
	public String getURIFragment() {
		final StringBuffer fragment = new StringBuffer(""); //$NON-NLS-1$
		if (script != null && script.getFile() != null) {
			fragment.append(Resources.encodeAcceleoAbsolutePath(script
					.getFile().getAbsolutePath()));
		}
		fragment.append(" //pos="); //$NON-NLS-1$
		fragment.append(pos.b());
		fragment.append(',');
		fragment.append(pos.e());
		return fragment.toString();
	}

	/**
	 * Gets the file that corresponds to the given fragment.
	 * 
	 * @param uriFragment
	 *            is the URI of the template element
	 * @return the file, or null if it doesn't exist
	 */
	public static File getTemplateFileFromURIFragment(String uriFragment) {
		if (uriFragment != null) {
			String path;
			final int i = uriFragment.indexOf(" //pos="); //$NON-NLS-1$
			if (i > -1) {
				path = uriFragment.substring(0, i).trim();
			} else {
				path = uriFragment;
			}
			final File file = new File(
					Resources.decodeAcceleoAbsolutePath(path));
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Gets the position that corresponds to the given fragment.
	 * 
	 * @param uriFragment
	 *            is the URI of the template element
	 * @return the position in the template file, or Int2.NotFound if it doesn't
	 *         exist
	 */
	public static Int2 getPositionFromURIFragment(String uriFragment) {
		if (uriFragment != null) {
			final String search = " //pos="; //$NON-NLS-1$
			int i = uriFragment.indexOf(search);
			if (i > -1) {
				final String pos = uriFragment.substring(i + search.length())
						.trim();
				i = pos.indexOf(","); //$NON-NLS-1$
				if (i > -1) {
					final String b = pos.substring(0, i).trim();
					final String e = pos.substring(i + 1).trim();
					return new Int2(Integer.parseInt(b), Integer.parseInt(e));
				}
			}
		}
		return Int2.NOT_FOUND;
	}

	private Map getInputVariables(Object input) {
		final Map map = new HashMap();
		map.put("input", input); //$NON-NLS-1$
		if (script != null) {
			map.put("i", script.contextPeek(IScript.WHILE_INDEX)); //$NON-NLS-1$
			final List currents = new ArrayList();
			int i = 0;
			Object iObject = script.contextAt(IScript.TEMPLATE_NODE, i);
			while (iObject != null) {
				currents.add(iObject);
				i++;
				iObject = script.contextAt(IScript.TEMPLATE_NODE, i);
			}
			map.put("current(i)", currents); //$NON-NLS-1$
		}
		return map;
	}

	private boolean conditionOK(File file, int line, Object input) {
		final IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot()
				.getFileForLocation(new Path(file.getAbsolutePath()));
		if (workspaceFile != null && workspaceFile.isAccessible()) {
			String condition = null;
			try {
				final IMarker[] markers = workspaceFile.findMarkers(
						"org.eclipse.debug.core.lineBreakpointMarker", true, 1); //$NON-NLS-1$
				for (int i = 0; condition == null && i < markers.length; i++) {
					if (MarkerUtilities.getLineNumber(markers[i]) == line) {
						condition = markers[i].getAttribute("condition", ""); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			} catch (final CoreException e) {
				AcceleoEcoreGenPlugin.getDefault().log(e, false);
			}
			if (condition != null && condition.trim().length() > 0) {
				final Template template = new Template(getScript());
				try {
					template.append(TemplateFeatureStatement.fromString(
							condition, new Int2(0, condition.length()),
							getScript()));
				} catch (final TemplateSyntaxException e) {
					AcceleoEcoreGenPlugin.getDefault().log(e, true);
					return false;
				}
				ENodeException.disableRuntimeMarkersFor(this);
				try {
					if (input instanceof ENode) {
						input = ((ENode) input).getAdapterValue(EObject.class);
					}
					if (input instanceof EObject) {
						final ENode result = template.evaluate((EObject) input,
								LaunchManager.create("run", false)); //$NON-NLS-1$
						return ((Boolean) result.getAdapterValue(boolean.class))
								.booleanValue();
					} else {
						return false;
					}
				} catch (final ENodeException e) {
					return false;
				} catch (final FactoryException e) {
					return false;
				} catch (final ENodeCastException e) {
					return false;
				} finally {
					ENodeException.enableRuntimeMarkersFor(this);
				}
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

}