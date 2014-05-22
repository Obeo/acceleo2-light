/*******************************************************************************
 * Copyright (c) 2005-2014 Obeo
 *  
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.gen.template;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.sirius.query.legacy.ecore.factories.FactoryException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.LaunchManager;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;

/**
 * This is a template's node element for the generation tool. This element can
 * be applied to model's objects to allow dynamic generation.
 * 
 * 
 */
public abstract class TemplateNodeElement extends TemplateElement {

    /**
     * Constructor.
     * 
     * @param script
     *            is the script
     */
    public TemplateNodeElement(IScript script) {
        super(script);
    }

    /* (non-Javadoc) */
    @Override
    public abstract String toString();

    /**
     * Evaluates this template element on an EObject. The result node of
     * generation is an ENode.
     * 
     * @param object
     *            is the model's object
     * @param mode
     *            is the mode in which to launch, one of the mode constants
     *            defined - RUN_MODE or DEBUG_MODE
     * @return the result node of generation
     * @throws ENodeException
     * @throws FactoryException
     * @see ENode
     */
    public abstract ENode evaluate(EObject object, LaunchManager mode) throws ENodeException, FactoryException;

    /**
     * Returns the text to put in an outline view.
     * 
     * @return the text to put in an outline view
     */
    public abstract String getOutlineText();

}
