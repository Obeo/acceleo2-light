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

package fr.obeo.acceleo.gen.template.scripts.imports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import fr.obeo.acceleo.ecore.factories.EFactory;
import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.ecore.tools.ETools;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeCastException;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.log.EvalFailure;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;
import fr.obeo.acceleo.gen.template.scripts.IEvalSettings;

/**
 * Metamodel element that can be used during a code generation.
 * 
 * @author www.obeo.fr
 * 
 */
public class EvalModel implements IEvalSettings {

    /**
     * URI of the metamodel.
     */
    protected String uri;

    /**
     * Root element of the metamodel.
     */
    protected EPackage metamodel;

    /**
     * Indicates if the errors are shown.
     */
    private final boolean logErrors;

    /**
     * Constructor.
     * 
     * @param uri
     *            is the URI of the metamodel
     */
    public EvalModel(String uri) {
        this(uri, true);
    }

    /**
     * Constructor.
     * 
     * @param uri
     *            is the URI of the metamodel
     * @param logErrors
     *            indicates if the errors are shown
     */
    public EvalModel(String uri, boolean logErrors) {
        this.uri = uri;
        this.logErrors = logErrors;
        this.metamodel = ETools.uri2EPackage(uri, false);
    }

    /**
     * @return the URI of the metamodel
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the root element of the metamodel
     */
    public EPackage getMetamodel() {
        return metamodel;
    }

    /**
     * {@inheritDoc}
     */
    public ENode eGet(TemplateCallExpression call, ENode node, ENode[] args, LaunchManager mode, boolean recursiveSearch) throws FactoryException, ENodeException {
        if (args.length == 0) {
            return eGetSub(call, node, args, mode);
        } else {
            return null;
        }
    }

    private ENode eGetSub(TemplateCallExpression call, ENode node, ENode[] args, LaunchManager mode) throws FactoryException, ENodeException {
        EObject eObject = null;
        if (node.isEObject()) {
            try {
                eObject = node.getEObject();
                node = new ENode(eObject, call, mode.isSynchronize()); // for
                // text
                // to model
            } catch (final ENodeCastException e) {
                eObject = null;
            }
        }
        ENode eval;
        if (eObject != null && EFactory.eValid(eObject, call.getLink()) && call.countArguments() == 0) {
            boolean hasMetaPrefix = TemplateConstants.LINK_PREFIX_METAMODEL.equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_METAMODEL_SHORT.equals(call.getPrefix());
            final Object object = EFactory.eGet(eObject, call.getLink(), !hasMetaPrefix);
            eval = eGetHolder(object, node);
            final EStructuralFeature feature = eObject.eClass().getEStructuralFeature(call.getLink());
            if (feature != null) {
                final boolean containment = feature == null || !(feature instanceof EReference) || ((EReference) feature).isContainment();
                eval.setContainment(containment);
                final boolean optional = feature != null && feature instanceof EReference && ((EReference) feature).getLowerBound() == 0;
                eval.setOptional(optional);
            } else {
                // nt templateCallExpression called is an eoperation
                // with no parameter, this step is skipped
            }
        } else {
            eval = null;
        }
        boolean isNaming;
        try {
            isNaming = call.getNextCall() != null && call.getNextCall().getLink().equals("naming") && call.countArguments() == 0 && call.getNextCall().countArguments() == 0 && node.isEObject() //$NON-NLS-1$
                    && (eval == null || eval.isNull() || eval.isEObject() && eval.getEObject().eContainer() == null);
        } catch (final ENodeCastException e1) {
            isNaming = false;
        }
        if (isNaming) {
            try {
                // Sample : 'link.naming' is empty => try 'linkValue'
                final String otherLink = call.getLink() + "Value"; //$NON-NLS-1$
                String result;
                result = EFactory.eGetAsString(eObject, otherLink);
                if (result != null) {
                    final ENode otherEval = new ENode(result, node);
                    if (logErrors) {
                        otherEval.log().addError(
                                new EvalFailure(AcceleoGenMessages.getString("EvalModel.EvaluationEmptyNaming", new Object[] { otherLink, node.getEObject().eClass().getName(), call.getLink(), }))); //$NON-NLS-1$
                    }
                    call.getNextCall().ignoreNextEval();
                    return otherEval;
                }
            } catch (final ENodeCastException e) {
                // Never catch
            } catch (final FactoryException e) {
                // Never catch
            }
        }
        return eval;
    }

    private ENode eGetHolder(Object object, ENode node) throws ENodeException {
        ENode result = ENode.createTry(object, node);
        if (result == null) {
            result = new ENode(ENode.EMPTY, node);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object resolveType(Object type, TemplateCallExpression call, int depth) {
        if (type != null) {
            // Remark : type == GENERIC_TYPE => type != null
            if (type instanceof EClass) {
                Object result = null;
                if (call.countArguments() == 0) {
                    result = checkingForFeature((EClass) type, call.getLink());
                    if (result == null) {
                        result = checkingForOperation((EClass) type, call.getLink(), call.countArguments());
                    }
                } else {
                    result = checkingForOperation((EClass) type, call.getLink(), call.countArguments());
                }
                return result;
            } else if (type == IEvalSettings.GENERIC_TYPE) {
                return type;
            }
        }
        return null;
    }

    /**
     * 
     * @param eClass
     *            EClass where the feature should be
     * @param link
     *            name of the feature
     * @return type of the feature or null
     */
    private Object checkingForFeature(EClass eClass, String link) {
        final EStructuralFeature feature = eClass.getEStructuralFeature(link);
        if (feature != null) {
            final EClassifier result = feature.getEType();
            if (!"http://www.eclipse.org/emf/2002/Ecore".equals(uri) && result == EcorePackage.eINSTANCE.getEObject()) { //$NON-NLS-1$
                return IEvalSettings.GENERIC_TYPE;
            } else {
                return result;
            }
        }
        return null;
    }

    /**
     * 
     * @param eClass
     *            EClass where the operation should be
     * @param link
     *            name of the feature
     * @param argCount
     * @return type of the feature or null
     */
    private Object checkingForOperation(EClass eClass, String link, int argCount) {
        EOperation operation = null;
        final Iterator it = eClass.getEAllOperations().iterator();
        while (it.hasNext()) {
            final EOperation currentOp = (EOperation) it.next();
            if (currentOp.getName().equals(link) && currentOp.getEParameters().size() == argCount) {
                operation = currentOp;
                break;
            }
        }
        if (operation != null) {
            final EClassifier result = operation.getEType();
            // !"http://www.eclipse.org/emf/2002/Ecore".equals(uri) &&
            if (result == EcorePackage.eINSTANCE.getEObject()) {
                return IEvalSettings.GENERIC_TYPE;
            } else {
                return result;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getCompletionProposals(Object type, int depth) {
        final Collection result = new ArrayList();
        if (type != null && type instanceof EClass) {
            result.addAll(getStructuralFeaturesProposals((EClass) type));
            result.addAll(getOperationProposals((EClass) type));
            return result.toArray();
        } else {
            return new Object[] {};
        }
    }

    /**
     * pre : eClass must be set
     * 
     * @param eClass
     *            the EClass from witch we want the completion proposals
     * @return all structural features of eClass ordered by name
     */
    private Collection getStructuralFeaturesProposals(EClass eClass) {
        final Collection result = new TreeSet(new Comparator() {
            public int compare(Object arg0, Object arg1) {
                EStructuralFeature f0 = (EStructuralFeature) arg0;
                EStructuralFeature f1 = (EStructuralFeature) arg1;
                return f0.getName().compareTo(f1.getName());
            }
        });
        result.addAll(eClass.getEAllStructuralFeatures());

        return result;
    }

    /**
     * pre : eClass must be set
     * 
     * @param eClass
     *            the EClass from witch we want the completion proposals
     * @return all operations of eClass ordered by name
     */
    private Collection getOperationProposals(EClass eClass) {
        final Collection result = new TreeSet(new Comparator() {
            public int compare(Object arg0, Object arg1) {
                EOperation o0 = (EOperation) arg0;
                EOperation o1 = (EOperation) arg1;
                String o0tag = o0.getName();
                String o1tag = o1.getName();
                Iterator it = o0.getEParameters().iterator();
                while (it.hasNext()) {
                    o0tag += ((EParameter) it.next()).getEType().getName();
                }
                it = o1.getEParameters().iterator();
                while (it.hasNext()) {
                    o1tag += ((EParameter) it.next()).getEType().getName();
                }

                return o0tag.compareTo(o1tag);
            }
        });

        final Iterator op = eClass.getEAllOperations().iterator();

        while (op.hasNext()) {
            final EOperation operation = (EOperation) op.next();
            // filter to keep only EOperation without arguments and with a
            // usable return type that is not declared by EObject
            if (operation.getEType() != null && ENode.getAdapterType(operation.getEType().getClass()) != null && operation.getEParameters().size() == 0
                    && !"EObject".equals(operation.getEContainingClass().getName())) {
                result.add(operation);
            }
        }

        return result;
    }

    /* (non-Javadoc) */
    public boolean validateCall(TemplateCallExpression call) {
        return "".equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_METAMODEL.equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_METAMODEL_SHORT.equals(call.getPrefix()); //$NON-NLS-1$
    }

}
