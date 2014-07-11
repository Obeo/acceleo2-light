/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.query.legacy.business.internal.interpreter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.sirius.common.tools.api.contentassist.ContentContext;
import org.eclipse.sirius.common.tools.api.contentassist.ContentInstanceContext;
import org.eclipse.sirius.common.tools.api.contentassist.ContentProposal;
import org.eclipse.sirius.common.tools.api.contentassist.IProposalProvider;
import org.eclipse.sirius.common.tools.api.interpreter.EvaluationException;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreter;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreterProvider;
import org.eclipse.sirius.common.tools.api.interpreter.IVariableStatusListener;
import org.eclipse.sirius.ecore.extender.business.api.accessor.MetamodelDescriptor;
import org.eclipse.sirius.ecore.extender.business.api.accessor.ModelAccessor;
import org.eclipse.sirius.ext.base.collect.StackEx;
import org.eclipse.sirius.query.legacy.AcceleoInterpreterPlugin;
import org.eclipse.sirius.query.legacy.ecore.factories.FactoryException;
import org.eclipse.sirius.query.legacy.gen.template.Template;
import org.eclipse.sirius.query.legacy.gen.template.TemplateConstants;
import org.eclipse.sirius.query.legacy.gen.template.TemplateElement;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeCastException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeIterator;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeList;
import org.eclipse.sirius.query.legacy.gen.template.eval.LaunchManager;
import org.eclipse.sirius.query.legacy.gen.template.scripts.SpecificScript;
import org.eclipse.sirius.query.legacy.tools.classloaders.AcceleoClassLoader;

/**
 * This utility class ease the evaluation of Acceleo expressions. A singleton
 * instance is available for pure API comfort but it is advised for performances
 * issues to avoid changing metamodel on consecutive evaluations.
 * 
 * @author cbrun
 * 
 */
// CHECKSTYLE:OFF
public abstract class AcceleoInterpreter implements IInterpreter, IInterpreterProvider, IProposalProvider {
    // CHECKSTYLE:ON
    /**
     * Keyword used to identify a variable.
     */
    protected static final String VARIABLE_KEYWORD = "$";

    /**
     * Keyword used to identify the start of an expression.
     */
    protected static final String PREFIX_KEYWORD = "<%";

    /**
     * Keyword used to identify the end of an expression.
     */
    protected static final String SUFFIX_KEYWORD = "%>";

    /** This will allow us to store the parsed templates for each expressions. */
    protected final Map<SpecificScript, Map<String, Template>> templates = new HashMap<SpecificScript, Map<String, Template>>();

    /**
     * Script instance.
     */
    protected SpecificScript pvScript;

    /**
     * Table keeping track of the variables values.
     */
    protected final Map<String, StackEx<ENode>> variableTables = new HashMap<String, StackEx<ENode>>();

    /**
     * Map used to keep track of the Scripts instances from a metamodel URI.
     */
    protected Map<String, SpecificScript> mmToScript = new WeakHashMap<String, SpecificScript>();

    private final Collection<String> imports = new LinkedHashSet<String>();

    private final Set<IVariableStatusListener> variablesListeners = new HashSet<IVariableStatusListener>();

    /** The optional cache. */
    private Map<CacheKey, ENode> cache;

    private AcceleoTemplateWorkspaceListener templateListener;

    /**
     * Constructor.
     */
    public AcceleoInterpreter() {
        installWorkspaceListener();
        TemplateConstants.initConstants();
    }

    /**
     * Return a script able to evaluate expression on the given EObject.
     * 
     * @param current
     *            current {@link EObject}.
     * @return a script able to evaluate expression on the given EObject.
     */
    protected final SpecificScript getScript(final EObject current) {
        /*
         * we keep one script per metamodel.
         */
        final String mmURI = computeMetamodelURI(current);
        final SpecificScript resultScript = mmToScript.get(mmURI);
        if (resultScript != null)
            return resultScript;
        final SpecificScript newScript = createNewScript(current, mmURI);
        mmToScript.put(mmURI, newScript);
        return newScript;
    }

    /**
     * 
     * {@inheritDoc}
     */
    protected abstract SpecificScript createNewScript(final EObject current, final String mmURI);

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluate(org.eclipse.emf.ecore.EObject,
     *      java.lang.String)
     */
    public Object evaluate(final EObject target, final String expression) throws EvaluationException {
        final ENode node = evaluateENode(target, expression);
        return getValue(node, expression);
    }

    /**
     * Evaluate the given expression and return the result.
     * 
     * @param eObj
     *            : the EObject on which we want to apply the evaluation.
     * @param expression
     *            : the Acceleo expression to evaluate, something like
     *            "<%eContainer()%>" but may also be "name : <%name%>".
     * @return : the {@link ENode} corresponding to this evaluation, and
     *         {@link ENode} wrap any possible type Acceleo may use.
     * @throws EvaluationException
     *             on evaluation errors.
     */
    public ENode evaluateENode(final EObject eObj, final String expression) throws EvaluationException {
        try {
            /*
             * We need to set the preferred loader here in order
             * to have Acceleo use the same class loader to create and evaluate
             * the script so as to avoid LinkageErrors. It will be reverted back
             * to null post-evaluation. IMPORTANT : the preferred class loader
             * MUST be set everywhere we instantiate or evaluate a
             * SpecificScript. See
             * AcceleoExtendedInterpreter#createNewScript(EObject, String) and
             * SiriusInformationAdapter#getAdapter(Object, Class).
             */
            AcceleoClassLoader.setPreferredClassLoader(eObj.getClass().getClassLoader());
            final SpecificScript script = getScript(eObj);
            Template template = null;
            if (expression != null && expression.trim().length() > 0) {
                Map<String, Template> scriptTemplates = templates.get(script);
                if (scriptTemplates != null && scriptTemplates.containsKey(expression)) {
                    template = scriptTemplates.get(expression);
                } else {
                    template = Template.from(expression, script, eObj);
                    if (scriptTemplates == null) {
                        scriptTemplates = new WeakHashMap<String, Template>();
                        templates.put(script, scriptTemplates);
                    }
                    scriptTemplates.put(expression, template);
                }
            }
            if (template != null)
                return template.evaluate(eObj, LaunchManager.create("run", false));
            AcceleoClassLoader.setPreferredClassLoader(null);
            return new ENode(null, (TemplateElement) null, true);
        } catch (final ENodeException e) {
            throw new EvaluationException(e);
        } catch (final FactoryException e) {
            throw new EvaluationException(e);
        }
    }

    /**
     * Evaluate an acceleo condition. The result is <code>true</code> if the
     * evaluation returns a <code>true</code> value or if the evaluation returns
     * a non null object. If the condition is null or empty or only composed
     * with white spaces, the result is <code>true</code>.
     * 
     * @param eObj
     *            the context object.
     * @param condition
     *            the condition to test.
     * @return <code>true</code> if the evaluation returns a <code>true</code>
     *         value or if the evaluation returns a non null object or if the
     *         condition is empty.
     * @throws EvaluationException
     *             if the interpreter encounter a problem while evaluting the
     *             condition.
     * @deprecated use {@link AcceleoInterpreter#evaluate(EObject, String)}
     *             instead
     */
    @Deprecated
    public boolean evaluateCondition(final EObject eObj, final String condition) throws EvaluationException {
        final boolean preconditionAccepted = true;
        return preconditionAccepted;
    }

    /**
     * return the header of the script containing the imports.
     * 
     * @param metamodelURI
     *            the metamodel uri.
     * @return the header of the script containing the imports.
     */
    protected String getScriptHeader(final String metamodelURI) {
        final String carriage = "\n";
        final StringBuffer header = new StringBuffer("");
        header.append(TemplateConstants.IMPORT_BEGIN).append(carriage);
        header.append(TemplateConstants.MODELTYPE_WORD).append(' ').append(metamodelURI).append(" \n");
        final Iterator<String> it = imports.iterator();
        while (it.hasNext()) {
            final String imp = it.next();
            if (!imp.contains("::")) {
                // Don't try to import Acceleo 3 modules.
                header.append(TemplateConstants.IMPORT_WORD).append(' ').append(imp).append(carriage);
            }
        }
        header.append(carriage).append(TemplateConstants.IMPORT_END);
        return header.toString();
    }

    /**
     * Gets the URI of the metamodel that defines the given object.
     * 
     * @param object
     *            is an object of the model
     * @return the URI of the metamodel
     */
    protected String computeMetamodelURI(final EObject object) {
        if (isDynamic(object)) {
            return computeEcoreFileURI(object);
        } else {
            return computeNsURI(object);
        }
    }

    private String computeEcoreFileURI(final EObject object) {
        final Resource eCoreRes = object.eClass().eResource();
        if (eCoreRes != null && eCoreRes.getURI() != null) {
            final URI ecoreURI = eCoreRes.getURI();
            if (ecoreURI.isPlatformResource()) {
                return ecoreURI.toPlatformString(true);
            }
        }
        return null;
    }

    private boolean isDynamic(final EObject object) {
        return object instanceof DynamicEObjectImpl;
    }

    private String computeNsURI(final EObject object) {
        EPackage p = object.eClass().getEPackage();
        String nsURI = p.getNsURI();
        p = p.getESuperPackage();
        while (p != null) {
            final String currentURI = p.getNsURI();
            if (currentURI != null && currentURI.length() > 0 && !currentURI.startsWith("unused://")) {
                nsURI = currentURI;
            }
            p = p.getESuperPackage();
        }
        return nsURI;
    }

    /**
     * Set a variable in the current interpreter. The user will be able to
     * access the variable value using the "$" prefix.
     * 
     * @param name
     *            the variable name.
     * @param value
     *            the variable value.
     */
    public void setVariable(final String name, final EObject value) {
        setVariable(name, new ENode(value, (TemplateElement) null, true));
    }

    /**
     * Set a variable in the current interpreter. The user will be able to
     * access the variable value using the "$" prefix.
     * 
     * @param name
     *            the variable name.
     * @param value
     *            the variable value.
     */
    public void setVariable(final String name, final Object value) {
        final ENode valueNode = ENode.createTry(value, new ENode(null, (TemplateElement) null, true));
        setVariable(name, valueNode);
    }

    /**
     * Set a variable in the current interpreter. The user will be able to
     * access the variable value using the "$" prefix.
     * 
     * @param name
     *            the variable name.
     * @param value
     *            the variable value.
     */
    public void setVariable(final String name, final ENode value) {
        StackEx<ENode> values = variableTables.get(name);
        if (values == null) {
            values = new StackEx<ENode>();
        }
        values.push(value);
        variableTables.put(name, values);
        notifyVariableListeners();
    }

    /**
     * Unset the variable.
     * 
     * @param name
     *            the variable name.
     */
    public void unSetVariable(final String name) {
        final StackEx<ENode> values = variableTables.get(name);
        if (values != null) {
            if (values.size() > 1) {
                /*
                 * There is another value in the stack, let's pop the old one.
                 */
                values.pop();
            } else {
                /*
                 * It was the last value in the stack, let's clear this variable
                 * entry.
                 */
                clearVariable(name);
            }
        }
        notifyVariableListeners();
    }

    /**
     * return the variable value.
     * 
     * @param name
     *            the variable name.
     * @return the variable value.
     */
    protected ENode getVariableValue(final String name) {
        final StackEx<ENode> values = variableTables.get(name);
        if (values != null)
            return values.peek();
        return null;
    }

    /**
     * Check if the variable is set.
     * 
     * @param name
     *            the variable name
     * @return <code>true</code> is the variable is set, <code>false</code>
     *         otherwise
     */
    protected boolean isVariableSet(final String name) {
        return variableTables.containsKey(name);
    }

    /**
     * Clear all the variables of the interpreter.
     */
    public void clearVariables() {
        variableTables.clear();
        notifyVariableListeners();
    }

    /**
     * Clear the variable with the given name.
     * 
     * @param name
     *            : name of the variable to clear.
     */
    public void clearVariable(final String name) {
        if (variableTables.containsKey(name)) {
            variableTables.remove(name);
            notifyVariableListeners();
        }
    }

    /**
     * Clear all the imports.
     */
    public void clearImports() {
        mmToScript.clear();
        templates.clear();
        imports.clear();
    }

    /**
     * Import a Java service or a template in the current Interpreter.
     * 
     * @param path
     *            the import id, for instance
     *            <tt>fr.obeo.my.package.java.StringServices</tt>
     */
    public void addImport(final String path) {
        if (path != null && path.contains(".") && !imports.contains(path)) {
            mmToScript.clear();
            templates.clear();
            imports.add(path);
        }
        templates.clear();
    }

    /**
     * Add a new listener to the current interpreter, the listener will be
     * notified on changes concerning variables.
     * 
     * @param newListener
     *            the new listener to add.
     */
    public void addVariableStatusListener(final IVariableStatusListener newListener) {
        variablesListeners.add(newListener);
    }

    /**
     * Remove the given {@link IVariableStatusListener}.
     * 
     * @param listener
     *            listener to remove.
     */
    public void removeVariableStatusListener(final IVariableStatusListener listener) {
        variablesListeners.remove(listener);
    }

    /**
     * Map with the variables.
     * 
     * @return a map containing the current variables.
     */
    public Map<String, ?> getVariables() {
        final Map<String, ENode> lightVariables = new HashMap<String, ENode>();
        final Iterator<String> it = variableTables.keySet().iterator();
        while (it.hasNext()) {
            final String variableName = it.next();
            lightVariables.put(variableName, getVariableValue(variableName));
        }
        return lightVariables;
    }

    private void notifyVariableListeners() {
        if (variablesListeners.size() > 0) {
            final Map<String, ?> lightVariables = getVariables();
            final Iterator<IVariableStatusListener> it = variablesListeners.iterator();
            while (it.hasNext()) {
                final IVariableStatusListener listener = it.next();
                listener.notifyChanged(lightVariables);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateBoolean(EObject,
     *      String)
     */
    @Deprecated
    public boolean getAsBoolean(final Object evaluate) {
        return internalGetAsBoolean(evaluate);
    }

    private boolean internalGetAsBoolean(final Object evaluate) {
        boolean result = false;
        if (evaluate instanceof ENode) {
            result = ENodeHelper.getAsBoolean((ENode) evaluate);
        } else if (evaluate instanceof Boolean) {
            result = ((Boolean) evaluate).booleanValue();
        } else if (evaluate instanceof String) {
            result = Boolean.parseBoolean((String) evaluate);
        } else if (evaluate instanceof IAdaptable) {
            final Boolean adaptedBoolean = (Boolean) ((IAdaptable) evaluate).getAdapter(Boolean.class);
            if (adaptedBoolean != null)
                result = adaptedBoolean.booleanValue();
        } else if (evaluate != null) {
            // in acceleo, if (something) returns true
            result = true;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateEObject(EObject,
     *      String)
     */
    @Deprecated
    public EObject getAsEObject(final Object result) {
        return internalGetAsEObject(result);
    }

    private EObject internalGetAsEObject(final Object result) {
        EObject eObject = null;
        if (result instanceof ENode) {
            eObject = ENodeHelper.getAsEObject((ENode) result);
        } else if (result instanceof EObject) {
            eObject = (EObject) result;
        } else if (result instanceof IAdaptable) {
            eObject = (EObject) ((IAdaptable) result).getAdapter(EObject.class);
        } else if (result instanceof ENodeList) {
            final ENodeList eNodeList = (ENodeList) result;
            if (eNodeList.size() > 0) {
                eObject = internalGetAsEObject(eNodeList.get(0));
            }
        } else if (result instanceof Collection) {
            final Collection<?> collection = (Collection<?>) result;
            if (!collection.isEmpty()) {
                final Object first = collection.iterator().next();
                eObject = internalGetAsEObject(first);
            }
        }
        return eObject;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateInteger(EObject,
     *      String)
     */
    @Deprecated
    public Integer getAsInteger(final Object result) {
        return internalGetAsInteger(result);
    }

    private Integer internalGetAsInteger(final Object result) {
        Integer integer = null;
        if (result instanceof ENode) {
            integer = ENodeHelper.getAsInteger((ENode) result);
        } else if (result instanceof Integer) {
            integer = (Integer) result;
        } else if (result instanceof IAdaptable) {
            integer = (Integer) ((IAdaptable) result).getAdapter(Integer.class);
        } else if (result instanceof String) {
            try {
                final int res = Integer.parseInt((String) result);
                integer = Integer.valueOf(res);
            } catch (final NumberFormatException e) {
                // silent
            }
        }
        return integer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateBoolean(EObject,
     *      String)
     */
    @Deprecated
    public Collection<EObject> getAsListOfEObjects(final Object object) {
        return internalGetAsListOfEObjects(object);
    }

    private Collection<EObject> internalGetAsListOfEObjects(final Object object) {
        Collection<EObject> result = new LinkedList<EObject>();
        if (object instanceof Collection) {
            final Collection<?> tmp = (Collection<?>) object;
            for (final Object obj : tmp) {
                if (obj instanceof EObject) {
                    result.add((EObject) obj);
                }
            }
        } else if (object instanceof ENode) {
            result = ENodeHelper.getAsListOfEObjects((ENode) object);
        } else if (object instanceof EObject) {
            result.add((EObject) object);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateString(EObject,
     *      String)
     */
    @Deprecated
    public String getAsString(final Object result) {
        return internalGetAsString(result);
    }

    private String internalGetAsString(final Object result) {
        if (result instanceof ENode) {
            return ENodeHelper.getAsString((ENode) result);
        }
        return result != null ? result.toString() : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#getValue(java.lang.Object,
     *      java.lang.String)
     */
    public Object getValue(final Object object, final String s) {
        Object result = object;
        if (object instanceof ENode) {
            result = ENodeHelper.getValue((ENode) result, s);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#provides(java.lang.String)
     */
    public boolean provides(final String expression) {
        return expression != null && expression.indexOf(PREFIX_KEYWORD) >= 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateBoolean(org.eclipse.emf.ecore.EObject,
     *      java.lang.String)
     */
    public boolean evaluateBoolean(final EObject context, final String expression) throws EvaluationException {
        final Object evaluation = evaluate(context, expression);
        return internalGetAsBoolean(evaluation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateCollection(org.eclipse.emf.ecore.EObject,
     *      java.lang.String)
     */
    public Collection<EObject> evaluateCollection(final EObject context, final String expression) throws EvaluationException {
        final Object evaluation = evaluate(context, expression);
        return internalGetAsListOfEObjects(evaluation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateEObject(org.eclipse.emf.ecore.EObject,
     *      java.lang.String)
     */
    public EObject evaluateEObject(final EObject context, final String expression) throws EvaluationException {
        final Object evaluation = evaluate(context, expression);
        return internalGetAsEObject(evaluation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateInteger(org.eclipse.emf.ecore.EObject,
     *      java.lang.String)
     */
    public Integer evaluateInteger(final EObject context, final String expression) throws EvaluationException {
        final Object evaluation = evaluate(context, expression);
        return internalGetAsInteger(evaluation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#evaluateString(org.eclipse.emf.ecore.EObject,
     *      java.lang.String)
     */
    public String evaluateString(final EObject context, final String expression) throws EvaluationException {
        final Object evaluation = evaluate(context, expression);
        return internalGetAsString(evaluation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#getVariable(java.lang.String)
     */
    public Object getVariable(final String name) {
        final ENode node = getVariableValue(name);
        return getValue(node, "");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#setProperty(java.lang.Object,
     *      java.lang.Object)
     */
    public void setProperty(final Object key, final Object value) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#dispose()
     */
    public void dispose() {
        for (final Map.Entry<SpecificScript, Map<String, Template>> child : templates.entrySet())
            child.getValue().clear();
        templates.clear();
        this.variableTables.clear();
        this.variablesListeners.clear();
        this.imports.clear();
        this.deactivateCache();
        uninstallWorkspaceListener();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#setModelAccessor(org.eclipse.sirius.ecore.extender.business.api.accessor.ModelAccessor)
     */
    public void setModelAccessor(final ModelAccessor modelAccessor) {
        // ignore.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreterProvider#createInterpreter()
     */
    public abstract IInterpreter createInterpreter();

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.contentassist.IProposalProvider#getProposals(org.eclipse.sirius.common.tools.api.interpreter.IInterpreter,
     *      org.eclipse.sirius.common.tools.api.contentassist.ContentContext)
     */
    public List<ContentProposal> getProposals(IInterpreter interpreter, ContentContext context) {
        return getCompletionEntry().computeProposals(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#getPrefix()
     */
    public String getPrefix() {
        return PREFIX_KEYWORD;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#getNewEmtpyExpression()
     */
    public ContentProposal getNewEmtpyExpression() {
        return new ContentProposal(PREFIX_KEYWORD + SUFFIX_KEYWORD, PREFIX_KEYWORD + SUFFIX_KEYWORD, "New legacy query language expression.", 2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#getVariablePrefix()
     */
    public String getVariablePrefix() {
        return VARIABLE_KEYWORD;
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void setCrossReferencer(final ECrossReferenceAdapter crossReferencer) {
        // no handling of cross referencer with specific script. (yet ;) )
    }

    /**
     * Returns the result of the evaluation from the cache.
     * 
     * @param context
     *            the context of the evaluation.
     * @param evaluation
     *            the evaluation.
     * @param args
     *            the arguments of the evaluation.
     * @return the result of the evaluation from the cache.
     */
    protected ENode getFromCache(final ENode context, final String evaluation, final ENode[] args) {
        if (cache != null) {
            return cache.get(new CacheKey(context, evaluation, args));
        }
        return null;
    }

    /**
     * Caches an evaluation.
     * 
     * @param context
     *            the context.
     * @param evaluation
     *            the evaluation.
     * @param args
     *            the arguments of the evaluation.
     * @param result
     *            the result of the evaluation.
     */
    protected void cache(final ENode context, final String evaluation, final ENode[] args, final ENode result) {
        if (cache != null)
            cache.put(new CacheKey(context, evaluation, args), result);
    }

    /**
     * Activates the cache.
     */
    public void activateCache() {
        if (this.cache == null)
            this.cache = new HashMap<CacheKey, ENode>();
    }
    
    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#activateMetamodels(java.util.Collection)
     */
    public void activateMetamodels(Collection<MetamodelDescriptor> metamodels) {
        // Nothing to do
    }

    /**
     * Deactivates the cache.
     */
    public void deactivateCache() {
        if (this.cache != null) {
            this.cache.clear();
            this.cache = null;
        }
    }

    /**
     * Inner Class CacheKey
     */
    private static class CacheKey {

        private ENode context;

        private String evaluation;

        private ENode[] args;

        public CacheKey(final ENode context, final String evaluation, final ENode[] args) {
            this.context = context;
            this.evaluation = evaluation;
            this.args = args;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (evaluation == null ? 0 : evaluation.hashCode());
            result = prime * result + (context == null ? 0 : hashCode(context));
            for (ENode node : args) {
                result = prime * result + (node == null ? 0 : hashCode(node));
            }
            return result;
        }

        private int hashCode(final ENode node) {
            int value = 0;
            if (node.getValue() != null) {
                if (!node.isList()) {
                    return node.getValue().hashCode();
                } else {
                    try {
                        final ENodeIterator iterNodes = node.getList().iterator();
                        while (iterNodes.hasNext()) {
                            value += hashCode(iterNodes.next());
                        }
                    } catch (final ENodeCastException e) {
                        AcceleoInterpreterPlugin.getDefault().error(e.getMessage(), e);
                    }
                }
            }
            return value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object obj) {
            boolean result = false;
            if (obj instanceof CacheKey) {
                result = equals((CacheKey) obj);
            }
            return result;
        }

        private boolean equals(final CacheKey cacheKey) {
            final boolean node = (this.context == null && cacheKey.context == null) || (this.context != null && this.context.equals((Object) cacheKey.context));
            final boolean eval = node && ((this.evaluation == null && cacheKey.evaluation == null) || (this.evaluation != null && this.evaluation.equals(cacheKey.evaluation)));
            boolean result = eval && this.args.length == cacheKey.args.length;
            for (int i = 0; i < args.length && result; i++) {
                result = this.args[i].equals((Object) cacheKey.args[i]);
            }
            return result;
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.contentassist.IProposalProvider#getProposals(org.eclipse.sirius.common.tools.api.interpreter.IInterpreter,
     *      org.eclipse.sirius.common.tools.api.contentassist.ContentInstanceContext)
     */
    public List<ContentProposal> getProposals(IInterpreter interpreter, ContentInstanceContext context) {
        final String textSoFar = context.getTextSoFar();
        String evaluationString = textSoFar.substring(0, context.getCursorPosition());

        if (evaluationString.toLowerCase().startsWith(PREFIX_KEYWORD)) {
            // Ask AcceleoCompletionEntry for proposals and process them
            List<ContentProposal> resultList = getCompletionEntry().computeProposals(context);
            if (resultList.size() > 0) {
                return resultList;
            }
        }

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#getImports()
     */
    public Collection<String> getImports() {
        return Collections.<String> unmodifiableCollection(this.imports);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#removeImport(String)
     */
    public void removeImport(String dependency) {
        if (this.imports.contains(dependency)) {
            this.imports.remove(dependency);
            mmToScript.clear();
        }
        templates.clear();
    }

    protected AcceleoCompletionEntry getCompletionEntry() {
        return new AcceleoCompletionEntry();
    }

    /**
     * install workspace listener
     */
    private void installWorkspaceListener() {
        if (templateListener == null) {
            templateListener = new AcceleoTemplateWorkspaceListener(this);
            final IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.addResourceChangeListener(templateListener);
        }
    }

    /**
     * uninstall workspace listener.
     */
    private void uninstallWorkspaceListener() {
        if (templateListener != null) {
            final IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.removeResourceChangeListener(templateListener);
            templateListener = null;
        }
    }

    /**
     * Clear templates stored.
     */
    protected void reconsiderTemplateFiles() {
        for (final Map.Entry<SpecificScript, Map<String, Template>> child : templates.entrySet())
            child.getValue().clear();

        mmToScript.clear();
        templates.clear();
    }

}
