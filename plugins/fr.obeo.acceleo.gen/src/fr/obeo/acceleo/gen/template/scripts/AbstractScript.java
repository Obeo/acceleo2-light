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

package fr.obeo.acceleo.gen.template.scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.ENodeIterator;
import fr.obeo.acceleo.gen.template.eval.ENodeList;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalJavaService;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalModel;
import fr.obeo.acceleo.gen.template.scripts.imports.services.SystemServicesFactory;

/**
 * Abstract generator configuration.
 * 
 * @author www.obeo.fr
 * 
 */
public abstract class AbstractScript implements IScript {

    /**
     * The identifier of the internal extension point specifying the
     * implementation to use with an acceleo script loader.
     */
    public static final String SCRIPT_LOADER_EXTENSION_ID = "fr.obeo.acceleo.gen.scriptloader"; //$NON-NLS-1$

    /* (non-Javadoc) */
    public ENode eGet(TemplateCallExpression call, ENode node, ENode[] args, LaunchManager mode, boolean recursiveSearch) throws FactoryException, ENodeException {
        // Services to apply on an ENode or an ENodeList
        Iterator imports = this.imports.iterator();
        while (imports.hasNext()) {
            final IEvalSettings anImport = (IEvalSettings) imports.next();
            if (anImport instanceof EvalJavaService) {
                if ((recursiveSearch || !((EvalJavaService) anImport).hasScriptContext()) && anImport.validateCall(call)) {
                    ((EvalJavaService) anImport).setMode(EvalJavaService.MODE_ENODE);
                    ENode sub = anImport.eGet(call, node, args, mode, false);
                    if (sub != null) {
                        return sub;
                    }
                    ((EvalJavaService) anImport).setMode(EvalJavaService.MODE_LIST);
                    sub = anImport.eGet(call, node, args, mode, false);
                    if (sub != null) {
                        return sub;
                    }
                }
            }
        }
        // Templates to apply on an EObject or an ENodeList
        ENode eval;
        if (validateCall(call)) {
            if (node.isEObject()) {
                eval = eGetTemplate(node, call.getLink(), args, mode);
            } else if (node.isList()) {
                // Iterate on the list
                boolean found = false;
                final ENodeList res = new ENodeList();
                try {
                    if (node.getList().size() == 0 && recursiveSearch /*
                                                                       * ENode
                                                                       * services
                                                                       * OK
                                                                       */) {
                        found = true;
                    }
                    final ENodeIterator it = node.getList().iterator();
                    while (it.hasNext()) {
                        final ENode child = eGet(call, it.next(), args, mode, recursiveSearch);
                        if (child != null) {
                            found = true;
                            res.add(child);
                        }
                    }
                } catch (final ENodeCastException e) {
                    // Never catch
                }
                if (found) {
                    eval = new ENode(res, node);
                } else {
                    eval = null;
                }
            } else {
                eval = null;
            }
        } else {
            eval = null;
        }
        if (eval == null) {
            if (recursiveSearch) {
                // Other imports
                imports = this.imports.iterator();
                while (imports.hasNext()) {
                    final IEvalSettings anImport = (IEvalSettings) imports.next();
                    if (anImport.validateCall(call)) {
                        if (anImport instanceof SpecificScript) {
                            final ENode sub = ((SpecificScript) anImport).eGetTemplate(node, call.getLink(), args, mode);
                            if (sub != null) {
                                return sub;
                            }
                        } else if (anImport instanceof EvalJavaService) {
                            ((EvalJavaService) anImport).setMode(EvalJavaService.MODE_DEFAULT);
                            final ENode sub = anImport.eGet(call, node, args, mode, false);
                            if (sub != null) {
                                return sub;
                            }
                        } else if (anImport instanceof EvalModel
                                && (TemplateConstants.LINK_PREFIX_METAMODEL.equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_METAMODEL_SHORT.equals(call.getPrefix()))) {
                            if (node.isEObject()) {
                                final ENode sub = anImport.eGet(call, node, args, mode, false);
                                if (sub != null) {
                                    return sub;
                                }
                            } else if (node.isList()) {
                                // Iterate on the list
                                boolean found = false;
                                final ENodeList res = new ENodeList();
                                try {
                                    if (node.getList().size() == 0 && recursiveSearch) {
                                        found = true;
                                    }
                                    final ENodeIterator it = node.getList().iterator();
                                    while (it.hasNext()) {
                                        final ENode next = it.next();
                                        final ENode child = anImport.eGet(call, next, args, mode, false);
                                        if (child != null) {
                                            found = true;
                                            res.add(child);
                                        }
                                    }
                                } catch (final ENodeCastException e) {
                                    // Never catch
                                }
                                if (found) {
                                    return new ENode(res, node);
                                }
                            } else {
                                final ENode sub = anImport.eGet(call, node, args, mode, false);
                                if (sub != null) {
                                    return sub;
                                }
                            }
                        } else {
                            final ENode sub = anImport.eGet(call, node, args, mode, false);
                            if (sub != null) {
                                return sub;
                            }
                        }
                    }
                }
            } else {
                // Other imports
                imports = this.imports.iterator();
                while (imports.hasNext()) {
                    final IEvalSettings anImport = (IEvalSettings) imports.next();
                    if (anImport instanceof AbstractScript && !(anImport instanceof SpecificScript) && !(anImport instanceof EmptyScript) && anImport.validateCall(call)) {
                        final ENode sub = anImport.eGet(call, node, args, mode, false);
                        if (sub != null) {
                            return sub;
                        }
                    } else if (anImport instanceof EvalJavaService && !((EvalJavaService) anImport).hasScriptContext() && anImport.validateCall(call)) {
                        ((EvalJavaService) anImport).setMode(EvalJavaService.MODE_DEFAULT);
                        final ENode sub = anImport.eGet(call, node, args, mode, false);
                        if (sub != null) {
                            return sub;
                        }
                    }
                }
            }
        }
        if (eval == null && node.isNull()) {
            return node;
        } else {
            return eval;
        }
    }

    /**
     * Computes a child node of generation for a node and a template name. The
     * template is evaluated on an EObject, so the given node must be an
     * EObject.
     * 
     * @param node
     *            is the parent node of generation
     * @param name
     *            is the template name
     * @param args
     *            is the list of arguments
     * @param mode
     *            is the mode in which to launch, one of the mode constants
     *            defined - RUN_MODE or DEBUG_MODE
     * @return the child node of generation, or null if the template doesn't
     *         exist, or null if the type of the node isn't valid
     * @throws ENodeException
     * @throws FactoryException
     */
    public abstract ENode eGetTemplate(ENode node, String name, ENode[] args, LaunchManager mode) throws ENodeException, FactoryException;

    /* (non-Javadoc) */
    public void addImport(IEvalSettings element) {
        if (element != null) {
            this.imports.add(element);
        }
    }

    /* (non-Javadoc) */
    public void removeImport(IEvalSettings element) {
        if (element != null) {
            this.imports.remove(element);
        }
    }

    /* (non-Javadoc) */
    public void clearImports() {
        imports.clear();
    }

    /**
     * All the elements that are used after this one during generation.
     */
    protected Set imports = new TreeSet(new Comparator() {
        public int compare(Object arg0, Object arg1) {
            // Remark : Low priority for EvalModel
            if (!(arg0 instanceof EvalModel) && arg1 instanceof EvalModel) {
                return -1;
            } else {
                return 1;
            }
        }
    });

    /**
     * @return the imports
     */
    public Set getImports() {
        return imports;
    }

    /**
     * Returns the factory that produces the system services for this script.
     * 
     * @return the factory that produces the system services for this script
     */
    protected SystemServicesFactory getSystemServicesFactory() {
        if (systemServicesFactory == null) {
            systemServicesFactory = createSystemServicesFactory();
        }
        return systemServicesFactory;
    }

    private SystemServicesFactory systemServicesFactory = null;

    /**
     * Creates the factory that produces the system services for this script.
     * 
     * @return the new factory
     */
    protected SystemServicesFactory createSystemServicesFactory() {
        return new SystemServicesFactory();
    }

    /* (non-Javadoc) */
    public IScript[] goToSpecifics() {
        if (goToSpecifics == null) {
            if (AbstractScript.getScriptLoader() != null) {
                goToSpecifics = AbstractScript.getScriptLoader().goToSpecifics(this);
            }
        }
        return goToSpecifics;
    }

    private IScript[] goToSpecifics = null;

    /* (non-Javadoc) */
    public IScript getSpecific() {
        return specific;
    }

    /* (non-Javadoc) */
    public void setSpecific(IScript specific) {
        this.specific = specific;
        this.goToSpecifics = null;
    }

    /**
     * The more specific script. The specific script is used before this one to
     * resolve the links.
     */
    protected IScript specific = null;

    /* (non-Javadoc) */
    public File getFile() {
        return null;
    }

    /* (non-Javadoc) */
    public Object contextPeek(Object key) {
        final Stack stack = (Stack) context.get(key);
        if (stack != null && !stack.isEmpty()) {
            final Object result = stack.peek();
            if (result instanceof ENode) {
                return ((ENode) result).copy();
            } else if (result instanceof ENode[]) {
                final ENode[] copy = new ENode[((ENode[]) result).length];
                for (int i = 0; i < copy.length; i++) {
                    copy[i] = ((ENode[]) result)[i].copy();
                }
                return copy;
            } else {
                return result;
            }
        } else {
            return null;
        }
    }

    /* (non-Javadoc) */
    public Object contextAt(Object key, int index) {
        final Stack stack = (Stack) context.get(key);
        if (stack != null) {
            if (index >= 0 && index < stack.size()) {
                final Object result = stack.elementAt(stack.size() - index - 1);
                if (result instanceof ENode) {
                    return ((ENode) result).copy();
                } else if (result instanceof ENode[]) {
                    final ENode[] copy = new ENode[((ENode[]) result).length];
                    for (int i = 0; i < copy.length; i++) {
                        copy[i] = ((ENode[]) result)[i].copy();
                    }
                    return copy;
                } else {
                    return result;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /* (non-Javadoc) */
    public void contextPush(Object key, Object value) {
        Stack stack = (Stack) context.get(key);
        if (stack == null) {
            stack = new Stack();
            context.put(key, stack);
        }
        stack.push(value);
    }

    /* (non-Javadoc) */
    public void contextPop(Object key) {
        final Stack stack = (Stack) context.get(key);
        if (stack != null) {
            stack.pop();
        }
    }

    /**
     * Context that is used during generation.
     */
    private final Map context = new HashMap();

    /**
     * Resolves the type of the next step for the type of the previous node, and
     * the new link.
     * 
     * @param type
     *            is the type of the previous node
     * @param call
     *            is the new link
     * @return the type of the next step
     */
    public Object resolveType(Object type, TemplateCallExpression call) {
        return resolveType(type, call, 0);
    }

    /* (non-Javadoc) */
    public Object resolveType(Object type, TemplateCallExpression call, int depth) {
        if (depth < 1) {
            final Iterator imports = this.imports.iterator();
            while (imports.hasNext()) {
                final IEvalSettings anImport = (IEvalSettings) imports.next();
                if (anImport.validateCall(call)) {
                    final Object resolvedType = anImport.resolveType(type, call, 1);
                    if (resolvedType != null) {
                        return resolvedType;
                    }
                }
            }
        }
        if (call.getLink().equals(TemplateConstants.LINK_NAME_INDEX) && call.getFirstArgument() == null && "".equals(call.getPrefix())) {
            return IEvalSettings.GENERIC_TYPE;
        } else if (call.getLink().equals(TemplateConstants.LINK_NAME_ARGS) && call.getArguments() != null && call.getArguments().size() == 1 && "".equals(call.getPrefix())) {
            return IEvalSettings.GENERIC_TYPE;
        }
        return null;
    }

    /**
     * Gets the proposals of the next step for the type of the previous node.
     * 
     * @param type
     *            is the type of the previous node
     * @return the proposals of the next step
     */
    public Object[] getCompletionProposals(Object type) {
        return getCompletionProposals(type, 0);
    }

    /* (non-Javadoc) */
    public Object[] getCompletionProposals(Object type, int depth) {
        final List result = new ArrayList();
        if (depth < 1) {
            final TreeSet orderedImports = new TreeSet(new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    // Remark : High priority for EvalModel
                    if (arg0 instanceof EvalModel && !(arg1 instanceof EvalModel)) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
            orderedImports.addAll(this.imports);
            final Iterator imports = orderedImports.iterator();
            while (imports.hasNext()) {
                final IEvalSettings anImport = (IEvalSettings) imports.next();
                result.addAll(Arrays.asList(anImport.getCompletionProposals(type, 1)));
            }
        }
        return result.toArray();
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        final StringBuffer text = new StringBuffer(""); //$NON-NLS-1$
        text.append(TemplateConstants.IMPORT_BEGIN);
        text.append('\n');
        final Iterator imports = this.imports.iterator();
        while (imports.hasNext()) {
            final IEvalSettings anImport = (IEvalSettings) imports.next();
            // Remark : toString -> Keep EvalModel only
            if (anImport instanceof EvalModel) {
                final String importValue = ((EvalModel) anImport).getUri();
                if (importValue.length() > 0) {
                    text.append(TemplateConstants.MODELTYPE_WORD);
                    text.append(' ');
                    text.append(importValue);
                    text.append('\n');
                }
            }
        }
        text.append(TemplateConstants.IMPORT_END);
        text.append("\n\n"); //$NON-NLS-1$
        return text.toString();
    }

    /**
     * @return the script loader that converts the script's content before
     *         loading
     */
    protected static IScriptLoader getScriptLoader() {
        if (AbstractScript.scriptLoader == null) {
            return new DefaultScriptLoader();
        }
        return AbstractScript.scriptLoader;
    }

    private static IScriptLoader scriptLoader = null;

}
