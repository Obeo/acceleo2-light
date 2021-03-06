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

package org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;
import org.eclipse.sirius.query.legacy.gen.template.scripts.SpecificScript;
import org.eclipse.sirius.query.legacy.tools.resources.Resources;

/**
 * Eclipse resource services.
 * 
 * 
 */
public class ResourceServices {

    /**
     * Returns the content of the given file.
     * 
     * @param node
     *            is the current node
     * @param path
     *            is the path of the file in the workspace
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    public String getFileContent(ENode node, String path) {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
        if (file.exists()) {
            return Resources.getFileContent(file).toString();
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public String getChainPath(ENode node) {
        String res = ""; //$NON-NLS-1$
        IScript script = node.getContainerTemplateElement().getScript();
        if (script instanceof SpecificScript) {
            File chain = ((SpecificScript) script).getChainFile();
            if (chain != null) {
                res = Resources.makeWorkspaceRelativePath(chain.getAbsolutePath());
            }
        }
        return res;
    }
}
