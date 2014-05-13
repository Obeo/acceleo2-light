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

package fr.obeo.acceleo.tools.log;

/**
 * Acceleo Exception.
 * 
 * @author www.obeo.fr
 * 
 */
public class AcceleoException extends Exception {

    private static final long serialVersionUID = 1;

    /**
     * Constructor.
     */
    public AcceleoException() {
    }

    /**
     * Constructor.
     * 
     * @param message
     *            is the message
     */
    public AcceleoException(String message) {
        super(message);
    }

}
