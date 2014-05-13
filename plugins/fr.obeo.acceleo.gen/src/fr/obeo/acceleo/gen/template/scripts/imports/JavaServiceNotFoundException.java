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

import fr.obeo.acceleo.tools.log.AcceleoException;

/**
 * Java service not found.
 * 
 * @author www.obeo.fr
 * 
 */
public class JavaServiceNotFoundException extends AcceleoException {

    private static final long serialVersionUID = 1;

    /**
     * Constructor.
     * 
     * @param message
     *            is the message
     */
    public JavaServiceNotFoundException(String message) {
        super(message);
    }

    /* (non-Javadoc) */
    @Override
    public String getMessage() {
        return super.getMessage();
    }

}
