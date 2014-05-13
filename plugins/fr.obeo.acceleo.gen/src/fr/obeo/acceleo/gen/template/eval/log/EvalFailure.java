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

package fr.obeo.acceleo.gen.template.eval.log;

import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.tools.log.IFailure;

/**
 * Template evaluation failures.
 * 
 * @author www.obeo.fr
 * 
 */
public class EvalFailure implements IFailure {

    /**
     * Failure message.
     */
    protected String message;

    /**
     * Position in the generated text.
     */
    protected int position = 0;

    /**
     * Constructor.
     * 
     * @param message
     *            is the failure message
     */
    public EvalFailure(String message) {
        this.message = message;
    }

    /* (non-Javadoc) */
    public String getMessage() {
        return AcceleoGenMessages.getString("EvalFailure.FailureMessage", new Object[] { message, }); //$NON-NLS-1$
    }

    /* (non-Javadoc) */
    public int position() {
        return position;
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        return AcceleoGenMessages.getString("EvalFailure.FailurePosition", new Object[] { Integer.toString(position()), }) + " : " + getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
    }

}
