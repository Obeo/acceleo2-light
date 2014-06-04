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

package org.eclipse.sirius.query.legacy.gen.template.eval;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A string comparator.
 * 
 * 
 */
public class StringComparator implements Comparator, Serializable {

    private static final long serialVersionUID = 1L;

    /* (non-Javadoc) */
    public int compare(Object arg0, Object arg1) {
        return ((String) arg0).compareTo((String) arg1);
    }

}
