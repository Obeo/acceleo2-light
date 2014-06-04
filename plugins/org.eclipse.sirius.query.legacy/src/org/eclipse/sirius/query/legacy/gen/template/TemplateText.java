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
import org.eclipse.sirius.query.legacy.gen.template.eval.TextModelMapping;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;

/**
 * This is an element for the generation tool, used to generate static text.
 * 
 * 
 */
public class TemplateText extends TemplateNodeElement {

    /**
     * Static text.
     */
    protected String text;

    /**
     * Constructor.
     * 
     * @param text
     *            is the static text
     * @param script
     *            is the script
     */
    public TemplateText(String text, IScript script) {
        super(script);
        this.text = text;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return text;
    }

    /* (non-Javadoc) */
    @Override
    public String getOutlineText() {
        return toString();
    }

    /* (non-Javadoc) */
    @Override
    public ENode evaluate(EObject object, LaunchManager mode) throws ENodeException, FactoryException {
        ENode node = new ENode(ENode.EMPTY, object, this, mode.isSynchronize());
        node.append(text, TextModelMapping.HIGHLIGHTED_STATIC_TEXT);
        return node;
    }

}
