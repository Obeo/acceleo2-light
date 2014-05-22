/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.business.internal.interpreter;

import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;
import org.eclipse.sirius.query.legacy.gen.template.scripts.SpecificScript;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.EvalJavaService;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ContextServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.PropertiesServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.RequestServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ResourceServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.StringServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.SystemServicesFactory;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.XpathServices;

/**
 * Class used to customize the Acceleo internal systems.
 * 
 * @author cbrun
 * 
 */
public class CustomSystemServicesFactory extends SystemServicesFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public void addImports(final IScript script, final boolean isRoot) {
        if (isRoot) {
            script.addImport(new EvalJavaService(new StringServices(), false));
            script.addImport(new EvalJavaService(new EObjectServices(), false));
            script.addImport(new EvalJavaService(new XpathServices(), false));
            script.addImport(new EvalJavaService(new ResourceServices(), false));
            script.addImport(new EvalJavaService(new ContextServices(), false));
        }
        script.addImport(new EvalJavaService(new ENodeServices(script), true));
        script.addImport(new EvalJavaService(new RequestServices(script), true));
        if (script instanceof SpecificScript) {
            script.addImport(new EvalJavaService(new PropertiesServices((SpecificScript) script), true));
        }
    }

}
