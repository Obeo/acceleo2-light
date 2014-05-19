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

package fr.obeo.dsl.common.acceleo.business.internal.interpreter;

import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalJavaService;
import fr.obeo.acceleo.gen.template.scripts.imports.services.ContextServices;
import fr.obeo.acceleo.gen.template.scripts.imports.services.PropertiesServices;
import fr.obeo.acceleo.gen.template.scripts.imports.services.RequestServices;
import fr.obeo.acceleo.gen.template.scripts.imports.services.ResourceServices;
import fr.obeo.acceleo.gen.template.scripts.imports.services.StringServices;
import fr.obeo.acceleo.gen.template.scripts.imports.services.SystemServicesFactory;
import fr.obeo.acceleo.gen.template.scripts.imports.services.XpathServices;

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
