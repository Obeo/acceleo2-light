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

import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.eval.TextModelMapping;
import fr.obeo.acceleo.gen.template.scripts.IScript;

/**
 * This is an element for the generation tool, used to generate static text.
 * 
 * @author www.obeo.fr
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
