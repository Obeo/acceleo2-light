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
 * This is a failure for a TreeLog.
 * <p>
 * It's about an error having a message and a position.
 * 
 * @author www.obeo.fr
 * 
 */
public interface IFailure {

    /**
     * @return the failure message
     */
    public String getMessage();

    /**
     * @return the position in the text
     */
    public int position();

}
