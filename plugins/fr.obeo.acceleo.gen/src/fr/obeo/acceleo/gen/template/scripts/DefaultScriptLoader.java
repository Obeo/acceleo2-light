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

import java.util.ArrayList;
import java.util.List;

/**
 * A script loader that converts the script's content before loading. The
 * default implementation returns the initial content.
 * 
 * @author www.obeo.fr
 * 
 */
public class DefaultScriptLoader implements IScriptLoader {

    /* (non-Javadoc) */
    public String load(String content) {
        return content;
    }

    /* (non-Javadoc) */
    public IScript[] goToSpecifics(IScript script) {
        List specifics = new ArrayList();
        IScript specific = script.getSpecific();
        while (specific != null) {
            if (specific instanceof SpecificScript) {
                specifics.add(0, specific);
            }
            specific = specific.getSpecific();
        }
        return (IScript[]) specifics.toArray(new IScript[specifics.size()]);
    }

}
