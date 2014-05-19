/*****************************************************************************************
 * Copyright (c) 2007, 2008, 2009 THALES CORPORATE SERVICE
 * All rights reserved.
 *
 * Contributors:
 *      Cedric Brun      (Obeo) <cedric.brun@obeo.fr>      - Initial API and implementation 
 *****************************************************************************************/

package fr.obeo.dsl.common.acceleo.business.internal.interpreter;

import org.eclipse.sirius.common.tools.api.interpreter.IInterpreter;
import org.eclipse.sirius.common.tools.api.interpreter.IInterpreterProvider;

/**
 * Provides Acceleo interpreter.
 * 
 * @author ymortier
 */
public class AcceleoInterpreterProvider implements IInterpreterProvider {

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreterProvider#createInterpreter()
     */
    public IInterpreter createInterpreter() {
        return new AcceleoExtendedInterpreter();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.sirius.common.tools.api.interpreter.IInterpreterProvider#provides(java.lang.String)
     */
    public boolean provides(final String expression) {
        return expression != null && expression.indexOf(AcceleoInterpreter.PREFIX_KEYWORD) > 0;
    }

}
