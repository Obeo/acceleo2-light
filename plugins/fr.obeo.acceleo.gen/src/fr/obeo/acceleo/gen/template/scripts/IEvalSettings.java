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

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;

/**
 * An element that can be used during a code generation.
 * 
 * @author www.obeo.fr
 * 
 */
public interface IEvalSettings {

    /**
     * Computes a child node of generation for a node and a link.
     * 
     * @param call
     *            is the link
     * @param node
     *            is the parent node of generation
     * @param args
     *            is the list of arguments
     * @param mode
     *            is the mode in which to launch, one of the mode constants
     *            defined - RUN_MODE or DEBUG_MODE
     * @param recursiveSearch
     *            to search in its imports
     * @return the child node of generation
     * @throws FactoryException
     * @throws ENodeException
     * @see ENode
     */
    public ENode eGet(TemplateCallExpression call, ENode node, ENode[] args, LaunchManager mode, boolean recursiveSearch) throws FactoryException, ENodeException;

    /**
     * Resolves the type of the next step for the type of the previous node, and
     * the new link.
     * 
     * @param type
     *            is the type of the previous node
     * @param call
     *            is the new link
     * @param depth
     *            is the depth of the script
     * @return the type of the next step
     */
    public Object resolveType(Object type, TemplateCallExpression call, int depth);

    /**
     * The pass key. The type that is returned by services and template calls.
     */
    public static final Object GENERIC_TYPE = new Object();

    /**
     * Gets the proposals of the next step for the type of the previous node.
     * 
     * @param type
     *            is the type of the previous node
     * @param depth
     *            is the depth of the script
     * @return the proposals of the next step
     */
    public Object[] getCompletionProposals(Object type, int depth);

    /**
     * Validates the given call (prefix).
     * 
     * @param call
     *            is the call to validate
     * @return true if the call is valid
     */
    public boolean validateCall(TemplateCallExpression call);

}
