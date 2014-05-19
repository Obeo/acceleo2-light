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

package fr.obeo.dsl.common.acceleo.business.internal.interpreter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.sirius.ecore.extender.business.api.accessor.exception.FeatureNotFoundException;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.TemplateSyntaxExceptions;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalJavaService;
import fr.obeo.acceleo.gen.template.scripts.imports.JavaServiceNotFoundException;
import fr.obeo.acceleo.gen.template.scripts.imports.services.SystemServicesFactory;
import fr.obeo.dsl.common.acceleo.AcceleoInterpreterPlugin;

/**
 * A {@link SpecificScript} subcall optimized for performance and which support
 * metamodel extensions.
 * 
 * @author mchauvin
 */
public class SmartSpecificScript extends SpecificScript {
    /**
     * Keyword used for the inverse references of an element.
     */
    protected static final String INVERSE_KEYWORD = "\u007E";

    private final AcceleoExtendedInterpreter interpreter;

    private ECrossReferenceAdapter crossReferencer;

    private final List<File> representationDescriptionFiles;

    /**
     * Constructor.
     * 
     * @param file
     *            is the script file that contains the specific configuration
     * @param interpreter
     *            is the acceleo interpreter to use
     */
    public SmartSpecificScript(final File file, final AcceleoExtendedInterpreter interpreter) {
        super(file);
        this.interpreter = interpreter;
        representationDescriptionFiles = new ArrayList<File>();
    }

    /**
     * Constructor.
     * 
     * @param moreSpecificRepresentationDescriptionFile
     *            the more specific representation description file
     * @param representationDescriptionFiles
     *            is the list of files to use as Acceleo template
     * @param interpreter
     *            is the acceleo interpreter to use
     */
    public SmartSpecificScript(final File moreSpecificRepresentationDescriptionFile, final List<File> representationDescriptionFiles, final AcceleoExtendedInterpreter interpreter) {
        super(moreSpecificRepresentationDescriptionFile);
        this.interpreter = interpreter;
        this.representationDescriptionFiles = representationDescriptionFiles;
    }

    /**
     * {@inheritDoc}
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.AbstractScript#createSystemServicesFactory()
     */
    @Override
    protected SystemServicesFactory createSystemServicesFactory() {
        return new CustomSystemServicesFactory();
    }

    /**
     * {@inheritDoc}
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.AbstractScript#eGet(fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression,
     *      fr.obeo.acceleo.gen.template.eval.ENode,
     *      fr.obeo.acceleo.gen.template.eval.ENode[],
     *      fr.obeo.acceleo.gen.template.eval.LaunchManager, boolean)
     */
    @Override
    public ENode eGet(final TemplateCallExpression call, final ENode node, final ENode[] args, final LaunchManager mode, final boolean recursiveSearch) throws FactoryException, ENodeException {
        final String name = call.getLink();
        ENode result = interpreter.getFromCache(node, name, args);
        if (result == null) {
            result = super.eGet(call, node, args, mode, recursiveSearch);
            if (result != null) {
                interpreter.cache(node, name, args, result);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.SpecificScript#eGetTemplate(fr.obeo.acceleo.gen.template.eval.ENode,
     *      java.lang.String, fr.obeo.acceleo.gen.template.eval.ENode[],
     *      fr.obeo.acceleo.gen.template.eval.LaunchManager)
     */
    @Override
    public ENode eGetTemplate(final ENode node, final String name, final ENode[] args, final LaunchManager mode) throws ENodeException, FactoryException {
        ENode result = null;
        if (name.startsWith(AcceleoInterpreter.VARIABLE_KEYWORD)) {
            if (interpreter.isVariableSet(name.substring(1))) {
                return interpreter.getVariableValue(name.substring(1));
            }
            throw new FactoryException("Variable " + name.substring(1) + " not found");
        } else {
            try {
                if (name.startsWith(INVERSE_KEYWORD) && node.isEObject()) {
                    result = getInverseReference(node, name);
                }
            } catch (final ENodeCastException e1) {
                AcceleoInterpreterPlugin.getDefault().error("Error trying to resolve inverse references", e1);
            }
        }
        /* Metamodel extension */
        if (result == null && node.isEObject()) {
            try {
                final EObject cur = node.getEObject();
                if (this.interpreter.accessor != null && this.interpreter.accessor.eValid(cur, name)) {
                    final Object res = this.interpreter.accessor.eGet(cur, name);
                    result = ENode.createTry(res, node);
                }
            } catch (final ENodeCastException e) {
                AcceleoInterpreterPlugin.getDefault().error("Error trying to get metamodel extensions.", e);
            } catch (final FeatureNotFoundException e) {
                AcceleoInterpreterPlugin.getDefault().error("Error accessing metamodel extensions.", e);
            }
        }
        if (result == null) {
            result = super.eGetTemplate(node, name, args, mode);
        }
        return result;
    }

    private ENode getInverseReference(final ENode node, final String name) throws ENodeCastException {
        ENode result = null;
        if (crossReferencer != null) {
            final Collection<EStructuralFeature.Setting> settings = crossReferencer.getInverseReferences(node.getEObject());
            String inverseName = null;
            if (name.length() > INVERSE_KEYWORD.length()) {
                inverseName = name.substring(INVERSE_KEYWORD.length());
            }
            final Collection<EObject> referencing = new HashSet<EObject>(settings.size());
            for (final EStructuralFeature.Setting setting : settings) {
                if (inverseName == null) {
                    referencing.add(setting.getEObject());
                } else if (setting.getEStructuralFeature().getName().equals(inverseName)) {
                    referencing.add(setting.getEObject());
                }
            }
            result = ENode.createTry(referencing, node);
        }
        return result;
    }

    /**
     * Set the cross referencer used to evaluate the "tilde" syntax.
     * 
     * @param referencer
     *            referencer to use for evaluation.
     */
    public void setCrossReferencer(final ECrossReferenceAdapter referencer) {
        this.crossReferencer = referencer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.SpecificScript#reset()
     */
    @Override
    public void reset() throws TemplateSyntaxExceptions {
        super.reset();
        /*
         * we set the file as null as we don't want Acceleo to read it on every
         * ENodeException.getMessage() or we'll loose performances for nothing!
         */
        file = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.SpecificScript#reset(java.lang.String)
     */
    @Override
    public void reset(final String content) throws TemplateSyntaxExceptions {
        super.reset(content);
        /*
         * we set the file as null as we don't want Acceleo to read it on every
         * ENodeException.getMessage() or we'll loose performances for nothing!
         */
        file = null;
    }

    /**
     * {@inheritDoc} <BR>
     * Search in all representation description files if the import can be
     * resolved.
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.SpecificScript#resolveScriptFile(java.io.File,
     *      java.lang.String, java.lang.String)
     */
    @Override
    protected File resolveScriptFile(final File script, final String importValue, final String extension) {
        File result = null;
        for (File odesignFile : representationDescriptionFiles) {
            result = super.resolveScriptFile(odesignFile, importValue, extension);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}<BR>
     * Search in all representation description files if the import can be
     * resolved.
     * 
     * @see fr.obeo.acceleo.gen.template.scripts.SpecificScript#addImportForJavaService(java.io.File,
     *      java.lang.String)
     */
    @Override
    protected void addImportForJavaService(final File file, final String value) throws JavaServiceNotFoundException {
        boolean javaServiceFound = false;
        final StringBuffer errorMessage = new StringBuffer("");

        for (File odesignFile : representationDescriptionFiles) {
            try {
                addImport(new EvalJavaService(odesignFile, value));
                javaServiceFound = true;
            } catch (final JavaServiceNotFoundException e) {
                errorMessage.append(e.getMessage());
                errorMessage.append("\n");
            }
            if (javaServiceFound) {
                break;
            }
        }
        if (!javaServiceFound) {
            if (errorMessage.length() == 0) {
                throw new JavaServiceNotFoundException("no representation description files found so " + value + "could not be de found");
            } else {
                throw new JavaServiceNotFoundException(errorMessage.substring(0, errorMessage.length() - 1));
            }
        }
    }
}
