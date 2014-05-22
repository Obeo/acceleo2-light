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

import java.util.Iterator;
import java.util.List;

import org.eclipse.sirius.query.legacy.gen.AcceleoGenMessages;
import org.eclipse.sirius.query.legacy.tools.log.AcceleoException;

/**
 * List of template syntax exceptions.
 * 
 * 
 */
public class TemplateSyntaxExceptions extends AcceleoException {

    private static final long serialVersionUID = 1;

    /**
     * List of problems (TemplateSyntaxException).
     */
    protected List problems;

    /**
     * Constructor.
     */
    public TemplateSyntaxExceptions(List problems) {
        super();
        this.problems = problems;
    }

    /**
     * @return the list of problems
     */
    public List getProblems() {
        return problems;
    }

    /* (non-Javadoc) */
    @Override
    public String getMessage() {
        StringBuffer report = new StringBuffer(""); //$NON-NLS-1$
        Iterator it = problems.iterator();
        while (it.hasNext()) {
            TemplateSyntaxException e = (TemplateSyntaxException) it.next();
            if (e.getScript() != null && e.getScript().getFile() != null) {
                report.append(AcceleoGenMessages.getString("TemplateSyntaxExceptions.ErrorInFile", new Object[] { e.getScript().getFile().getAbsolutePath().toString(), })); //$NON-NLS-1$
            } else {
                report.append(AcceleoGenMessages.getString("TemplateSyntaxExceptions.ErrorUnknownFile")).append(':'); //$NON-NLS-1$
            }
            report.append('\n');
            report.append(e.getMessage());
            report.append('\n');
        }
        return report.toString();
    }

}
