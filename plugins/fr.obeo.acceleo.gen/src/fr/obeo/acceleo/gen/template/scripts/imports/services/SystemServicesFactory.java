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

package fr.obeo.acceleo.gen.template.scripts.imports.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;

import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.gen.template.scripts.SpecificScript;
import fr.obeo.acceleo.gen.template.scripts.imports.EvalJavaService;

/**
 * Factory that imports the system services for the given script.
 * 
 * @author www.obeo.fr
 * 
 */
public class SystemServicesFactory {

    /**
     * The instance of the root EObjectServices.
     */
    private EObjectServices eObjectServices;

    private List metamodels;

    /**
     * Adds system imports to the given script.
     * 
     * @param script
     *            is the script
     * @param isRoot
     *            indicates if the script is the root
     */
    public void addImports(IScript script, boolean isRoot) {
        if (isRoot) {
            script.addImport(new EvalJavaService(new StringServices(), false));
            eObjectServices = new EObjectServices(script);
            script.addImport(new EvalJavaService(eObjectServices, false));
            if (metamodels != null) {
                Iterator it = metamodels.iterator();
                while (it.hasNext()) {
                    eObjectServices.addMetamodel((EPackage) it.next());
                }
                metamodels = null;
            }
            script.addImport(new EvalJavaService(new XpathServices(), false));
            script.addImport(new EvalJavaService(new ResourceServices(), false));
            script.addImport(new EvalJavaService(new ContextServices(), false));
        }
        script.addImport(new EvalJavaService(new ENodeServices(script), true));
        script.addImport(new EvalJavaService(new RequestServices(script), true));
        if (script instanceof SpecificScript) {
            if (eObjectServices != null) {
                eObjectServices.addMetamodel(((SpecificScript) script).getMetamodel());
            } else {
                if (metamodels == null) {
                    metamodels = new ArrayList();
                }
                metamodels.add(((SpecificScript) script).getMetamodel());
            }
            script.addImport(new EvalJavaService(new PropertiesServices((SpecificScript) script), true));
            addExternalSystemServices(script); // registre external services as
                                               // system services
        }
    }

    /**
     * allows to add user services as System Services
     * 
     * @param script
     *            is the script
     */
    private void addExternalSystemServices(IScript script) {

        final ExternalServices service = new ExternalServices();
        final List services = service.getAllExternalServices();

        for (int index = 0; index < services.size(); index++) {
            script.addImport(new EvalJavaService(services.get(index), true));
        }

    }
}
