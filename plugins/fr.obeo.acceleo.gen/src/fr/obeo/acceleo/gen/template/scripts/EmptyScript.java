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

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateSyntaxExceptions;
import fr.obeo.acceleo.gen.template.eval.ENode;
import fr.obeo.acceleo.gen.template.eval.ENodeException;
import fr.obeo.acceleo.gen.template.eval.LaunchManager;
import fr.obeo.acceleo.gen.template.expressions.TemplateCallExpression;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Empty Generator configuration. <li>isDefault() == false</li> <li>isSpecific()
 * == false</li> <li>hasFileTemplate() == false</li> <li>isGenerated(EObject) ==
 * false</li> <li>hasError(EObject) == false</li>
 * 
 * @author www.obeo.fr
 * 
 */
public class EmptyScript extends AbstractScript {

    /**
     * Constructor.
     */
    public EmptyScript() {
        super();
    }

    /* (non-Javadoc) */
    public boolean isDefault() {
        return false;
    }

    /* (non-Javadoc) */
    public boolean isSpecific() {
        return false;
    }

    /* (non-Javadoc) */
    public void reset() throws TemplateSyntaxExceptions {
    }

    /* (non-Javadoc) */
    public boolean hasFileTemplate() {
        return false;
    }

    /* (non-Javadoc) */
    public boolean isGenerated(EObject object) {
        return false;
    }

    /* (non-Javadoc) */
    public IPath getFilePath(EObject object, boolean recursive) throws FactoryException {
        return null;
    }

    /* (non-Javadoc) */
    public boolean hasError(EObject object) {
        return false;
    }

    /* (non-Javadoc) */
    public Template getTextTemplateForEObject(EObject object, String key) throws FactoryException, ENodeException {
        throw new ENodeException(AcceleoGenMessages.getString("ENodeError.UnresolvedTemplate", new Object[] { key, }), new Int2(0, 0), this, object, false); //$NON-NLS-1$
    }

    /* (non-Javadoc) */
    public Template getRootTemplate(EObject object, boolean recursive) throws FactoryException, ENodeException {
        throw new ENodeException(AcceleoGenMessages.getString("ENodeError.UnresolvedRoot"), new Int2(0, 0), this, object, false); //$NON-NLS-1$
    }

    /* (non-Javadoc) */
    @Override
    public ENode eGetTemplate(ENode node, String name, ENode[] args, LaunchManager mode) throws ENodeException, FactoryException {
        return null;
    }

    /* (non-Javadoc) */
    public boolean validateCall(TemplateCallExpression call) {
        return false;
    }

}
