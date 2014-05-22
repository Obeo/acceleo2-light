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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.sirius.common.tools.api.resource.FileProvider;

/**
 * Workspace resource listener.
 * 
 * @author cnotot
 */
public class AcceleoTemplateWorkspaceListener implements IResourceChangeListener {

    private AcceleoInterpreter interpreter;

    /**
     * Creates a new instance.
     * 
     * @param interpreter
     *            the interpreter
     */
    public AcceleoTemplateWorkspaceListener(final AcceleoInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * resourceChanged.
     * 
     * @param event
     *            {@link IResourceChangeListener}
     */
    public void resourceChanged(final IResourceChangeEvent event) {

        final IResourceDelta delta = event.getDelta();

        if (delta != null && hasAtLeastOneModifiedTemplate(delta)) {
            processDelta(delta);
        }

    }

    private boolean hasAtLeastOneModifiedTemplate(IResourceDelta delta) {
        for (IResourceDelta deltaChild : delta.getAffectedChildren()) {
            if (isAboutFolderDerived(deltaChild)) {
                break;
            } else if (isAboutTemplateChange(deltaChild) || hasAtLeastOneModifiedTemplate(deltaChild)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAboutFolderDerived(final IResourceDelta delta) {
        return delta.getResource().getType() == IResource.FOLDER && delta.getResource().isDerived();
    }

    private boolean isAboutTemplateChange(final IResourceDelta delta) {
        return delta.getResource().getType() == IResource.FILE && delta.getKind() == IResourceDelta.CHANGED && isTemplate(delta);
    }

    private boolean isTemplate(final IResourceDelta delta) {
        if (delta.getFullPath() != null) {
            IFile file = FileProvider.findFile(delta.getFullPath());
            return "mt".equals(file.getFileExtension());
        }
        return false;
    }

    /**
     * Re-Initialize the interpreters for the Sessions
     * 
     * @param delta
     */
    private void processDelta(final IResourceDelta delta) {
        interpreter.reconsiderTemplateFiles();
    }

}
