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

import java.io.File;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.sirius.common.tools.api.interpreter.EvaluationException;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreter;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreterContext;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreterStatus;
import org.eclipse.sirius.ecore.extender.business.api.accessor.ModelAccessor;
import org.eclipse.sirius.ext.base.collect.StackEx;
import org.eclipse.sirius.query.legacy.AcceleoInterpreterPlugin;
import org.eclipse.sirius.query.legacy.gen.AcceleoGenMessages;
import org.eclipse.sirius.query.legacy.gen.template.TemplateSyntaxExceptions;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.log.EvalFailure;
import org.eclipse.sirius.query.legacy.gen.template.scripts.SpecificScript;
import org.eclipse.sirius.query.legacy.tools.classloaders.AcceleoClassLoader;
import org.eclipse.sirius.query.legacy.tools.plugins.AcceleoModuleProvider;
import org.eclipse.sirius.query.legacy.tools.resources.Resources;

import com.google.common.collect.Sets;

/**
 * {@link AcceleoExtendedInterpreter} is able to evaluate Acceleo expression
 * considering the possible metamodel extenders and using variables.
 * 
 * @author cbrun
 */
public class AcceleoExtendedInterpreter extends AcceleoInterpreter {

    private static final boolean LOG_WARNINGS = false;

    private static final String ACCELEO_PLUGIN_NAME = AcceleoInterpreterPlugin.getDefault().getBundle().getSymbolicName();

    /**
     * Class used to access the model data.
     */
    protected ModelAccessor accessor;

    private ECrossReferenceAdapter crossReferencer;

    /**
     * All the representation description files (*.odesign) to to take into
     * account.
     */
    private final List<File> representationDescriptionFiles;

    /**
     * Create a new {@link AcceleoExtendedInterpreter}. Be careful instantiating
     * it this way, you should better use the File parameterized initializer to
     * avoid class loader issues when evaluating your scripts.
     */
    public AcceleoExtendedInterpreter() {
        super();
        representationDescriptionFiles = new ArrayList<File>();
    }

    /**
     * Create a new {@link AcceleoExtendedInterpreter} initialized with an
     * accessor.. Be careful instantiating it this way, you should better use
     * the File parameterized initializer to avoid class loader issues when
     * evaluating your scripts.
     * 
     * @param accessor
     *            a model accessor.
     */
    public AcceleoExtendedInterpreter(final ModelAccessor accessor) {
        this();
        this.accessor = accessor;
    }

    /**
     * Create a new {@link AcceleoExtendedInterpreter} from an .air file.
     * 
     * @param representationDescriptionFile
     *            any .odesign file, this file is used to initialize the class
     *            loaders.
     * @deprecated
     */
    @Deprecated
    public AcceleoExtendedInterpreter(final File representationDescriptionFile) {
        this();
        representationDescriptionFiles.add(representationDescriptionFile);
    }

    /**
     * Factory method for the new {@link SpecificScript} instances. Clients may
     * override this method to provide new behaviors while evaluating Acceleo
     * scripts.
     * 
     * @param current
     *            current {@link EObject} used to create the script.
     * @param mmURI
     *            metamodel URI.
     * @return the new instance of {@link SpecificScript}.
     */
    @Override
    protected SpecificScript createNewScript(final EObject current, final String mmURI) {
        File moreSpecificRepresentationDescriptionFile = representationDescriptionFiles != null && !representationDescriptionFiles.isEmpty() ? representationDescriptionFiles.get(0) : null;
        /*
         * If the interpreter has been created with no AIR file, then let's use
         * the model one but then we will probably encounter issues with the
         * services if the model is not in the same project as the AIR file.
         */
        if (moreSpecificRepresentationDescriptionFile == null && current != null && current.eResource() != null) {
            // if the resource associated to current is a CDOResource
            // we obviously can't get any IFile related to this element
            // to avoid any dependency to CDO, we catch the exception raised by
            // getIFile
            try {

                if (current.eResource().getURI().path() != null && Resources.getIFile(current.eResource()) != null && Resources.getIFile(current.eResource()).getLocation() != null) {
                    moreSpecificRepresentationDescriptionFile = Resources.getIFile(current.eResource()).getLocation().toFile();
                }
            } catch (IllegalArgumentException e) {
                // The assertion in
                // org.eclipse.core.internal.resources.Workspace.newResource()
                // -> case IFIle (line 1801), can raise an error

                // We don't have to treat this exception, the
                // moreSpecificRepresentationDescriptionFile will be left to his
                // default value
            }
        }

        /*
         * We need to set the preferred loader here in order to have Acceleo
         * remember which class loader was used to create the script so as to
         * avoid LinkageErrors. It will be reverted back to null at the end of
         * this method. IMPORTANT : the preferred class loader MUST be set
         * everywhere we instantiate or evaluate a SpecificScript. See
         * AcceleoInterpreter#evaluate(EObject, String) and
         * SiriusInformationAdapter#getAdapter(Object, Class).
         */
        if (current != null) {
            AcceleoClassLoader.setPreferredClassLoader(current.getClass().getClassLoader());
        }
        final SmartSpecificScript newScript = new SmartSpecificScript(moreSpecificRepresentationDescriptionFile, representationDescriptionFiles, this);
        newScript.setCrossReferencer(crossReferencer);
        try {
            final String templateHeader = getScriptHeader(mmURI);
            newScript.reset(templateHeader);
        } catch (final TemplateSyntaxExceptions e) {
            AcceleoInterpreterPlugin.getDefault().error("Syntax error in template while initializing the interpreter.", e);
        }
        AcceleoClassLoader.setPreferredClassLoader(null);
        return newScript;
    }

    /**
     * Evaluate the Acceleo expression.
     * 
     * @param eObj
     *            instance on which the Expression should be evaluated.
     * @param expression
     *            Acceleo expression to evaluate.
     * @throws EvaluationException
     *             on error concerning ENode.
     * @return the result of the Expression.
     */
    @Override
    public Object evaluate(final EObject eObj, final String expression) throws EvaluationException {
        // Save the original output
        final PrintStream originalOut = System.out;
        ENode result = null;
        result = doEvaluate(eObj, expression);
        // Restore the original output (because it is catched by Acceleo)
        if (originalOut != System.out) {
            System.setOut(originalOut);
        }
        return getValue(result, expression);
    }

    @SuppressWarnings("unused")
    private ENode doEvaluate(final EObject eObj, final String expression) throws EvaluationException {
        ENodeException.disableRuntimeMarkersFor(eObj);

        final ENode result = super.evaluateENode(eObj, expression);

        try {
            //
            // log eNode error if the profiler is active.
            if (AcceleoExtendedInterpreter.LOG_WARNINGS && !result.log().isOk()) {
                final MultiStatus status = new MultiStatus(AcceleoExtendedInterpreter.ACCELEO_PLUGIN_NAME, IStatus.INFO, "variables info", null);
                final Iterator<Entry<String, StackEx<ENode>>> iterVariables = variableTables.entrySet().iterator();
                while (iterVariables.hasNext()) {
                    final Entry<String, StackEx<ENode>> entry = iterVariables.next();
                    final String variableName = entry.getKey();
                    final Object variableValue = getVariableValue(variableName);
                    final IStatus subStatus = new Status(IStatus.INFO, AcceleoExtendedInterpreter.ACCELEO_PLUGIN_NAME, variableName + " : " + String.valueOf(variableValue));
                    status.add(subStatus);
                }
                final IStatus evaluationStatus = new Status(IStatus.INFO, AcceleoExtendedInterpreter.ACCELEO_PLUGIN_NAME, "expression : " + expression);
                status.add(evaluationStatus);
                final IStatus contextStatus = new Status(IStatus.INFO, AcceleoExtendedInterpreter.ACCELEO_PLUGIN_NAME, "context : " + eObj);
                status.add(contextStatus);
                AcceleoInterpreterPlugin.getDefault().logENode(result, status);
            }
        } finally {
            ENodeException.enableRuntimeMarkersFor(eObj);
        }
        if (result.log().allErrors().hasNext()) {
            final EvalFailure evalFailure = (EvalFailure) result.log().allErrors().next();
            if (isMessageToThrow(evalFailure.getMessage())) {
                throw new EvaluationException(evalFailure.getMessage());
            }
        }
        return result;
    }

    /**
     * Throw all the message except those corresponding to
     * EmptyFeatureEvaluation
     * 
     * @param message
     *            The message to test
     * @return false if the message corresponds to EmptyFeatureEvaluation, true
     *         otherwise
     */
    private boolean isMessageToThrow(final String message) {
        final String evalFormater = AcceleoGenMessages.getString("EvalFailure.FailureMessage");
        final String evalPrefix = MessageFormat.format(evalFormater, "");
        if (message != null && message.startsWith(evalPrefix)) {
            final String endOfMessage = message.substring(evalPrefix.length());
            if (endOfMessage.startsWith(AcceleoGenMessages.getString("ENodeError.EmptyEvaluation"))) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.query.legacy.business.internal.interpreter.AcceleoInterpreter#setModelAccessor(org.eclipse.sirius.ecore.extender.business.api.accessor.ModelAccessor)
     */
    @Override
    public void setModelAccessor(final ModelAccessor modelAccessor) {
        this.accessor = modelAccessor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.query.legacy.business.internal.interpreter.AcceleoInterpreter#createInterpreter()
     */
    @Override
    public IInterpreter createInterpreter() {
        return new AcceleoExtendedInterpreter();
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.query.legacy.business.internal.interpreter.AcceleoInterpreter#setProperty(java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public void setProperty(final Object key, final Object value) {
        if (IInterpreter.FILES.equals(key)) {
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                final List<String> odesignPaths = (List<String>) value;
                for (String odesignPath : odesignPaths) {
                    // Add this file to the list of odesignFiles
                    File file = AcceleoModuleProvider.getDefault().getFile(new Path(odesignPath));
                    if (file != null) {
                        this.representationDescriptionFiles.add(file);
                    }
                }
            } else if (value == null) {
                this.representationDescriptionFiles.clear();
            }
        }
        super.setProperty(key, value);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void setCrossReferencer(final ECrossReferenceAdapter crossReferencer) {
        this.crossReferencer = crossReferencer;
        if (crossReferencer != null) {
            for (final SpecificScript script : mmToScript.values()) {
                if (script instanceof SmartSpecificScript) {
                    ((SmartSpecificScript) script).setCrossReferencer(crossReferencer);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.query.legacy.business.internal.interpreter.AcceleoInterpreter#getCompletionEntry()
     */
    @Override
    protected AcceleoCompletionEntry getCompletionEntry() {
        return new AcceleoCompletionEntry() {
            @Override
            protected void addModelElements(Object[] proposals, List<Object> contents) {
                if (element != null && accessor != null && accessor.eInstanceOf(element, "tool.ModelOperation")) {
                    /*
                     * if we are on a tool, then we do not need to provide
                     * semantic model element in completion
                     */
                } else {
                    super.addModelElements(proposals, contents);
                }
            }
        };
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#validateExpression(java.lang.String,
     *      java.lang.String)
     */
    public Collection<IInterpreterStatus> validateExpression(IInterpreterContext context, String expression) {
        // Acceleo 2 is not able to analyse this expression if no target is
        // given
        return Sets.newLinkedHashSet();
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreter#supportsValidation()
     */
    public boolean supportsValidation() {
        return false;
    }

}
