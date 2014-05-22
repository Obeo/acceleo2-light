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
package org.eclipse.sirius.query.legacy.tools.resources;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

import org.eclipse.sirius.query.legacy.tools.AcceleoToolsMessages;
import org.eclipse.sirius.query.legacy.tools.AcceleoToolsPlugin;
import org.eclipse.sirius.query.legacy.tools.log.AcceleoException;

/**
 * Abstract acceleo plugin class (not UI). All plugins extend this class that
 * contains ID and log facilities.
 */
public abstract class AcceleoPlugin extends Plugin {

    /**
     * @return the identifier of the plugin
     */
    public abstract String getID();

    /**
     * Puts the given status in the error log view.
     * 
     * @param status
     *            is the status
     */
    public void log(IStatus status) {
        getLog().log(status);
        AcceleoToolsPlugin.getDefault().newAcceleoLog(status);
    }

    /**
     * Puts the given exception in the error log view, as error or warning.
     * 
     * @param e
     *            is the exception to put in the error log view
     * @param blocker
     *            is the severity : (blocker)? IStatus.ERROR : IStatus.WARNING
     */
    public void log(Throwable e, boolean blocker) {
        if (e instanceof CoreException) {
            IStatus status = ((CoreException) e).getStatus();
            log(new Status(status.getSeverity(), getID(), status.getCode(), status.getMessage(), status.getException()));
        } else if (e instanceof InvocationTargetException) {
            log(((InvocationTargetException) e).getTargetException(), blocker);
        } else if (e instanceof AcceleoException) {
            int severity = (blocker) ? IStatus.ERROR : IStatus.WARNING;
            log(new Status(severity, getID(), severity, ((e.getMessage() != null) ? e.getMessage() : AcceleoToolsMessages.getString("AcceleoPlugin.UnknownError")), null)); //$NON-NLS-1$
        } else if (e instanceof NullPointerException) {
            int severity = (blocker) ? IStatus.ERROR : IStatus.WARNING;
            log(new Status(severity, getID(), severity, AcceleoToolsMessages.getString("AcceleoPlugin.MissingElement"), e)); //$NON-NLS-1$
        } else {
            int severity = (blocker) ? IStatus.ERROR : IStatus.WARNING;
            log(new Status(severity, getID(), severity, AcceleoToolsMessages.getString("AcceleoPlugin.UnexpectedException"), e)); //$NON-NLS-1$
        }
    }

    /**
     * Puts the given message in the error log view, as error or warning.
     * 
     * @param message
     *            is the message to put in the error log view
     * @param blocker
     *            is the severity : (blocker)? IStatus.ERROR : IStatus.WARNING
     */
    public void log(String message, boolean blocker) {
        int severity = (blocker) ? IStatus.ERROR : IStatus.WARNING;
        log(new Status(severity, getID(), severity, ((message != null) ? message.trim().replaceFirst("\n", ";\n") : AcceleoToolsMessages.getString("AcceleoPlugin.UnknownError")), null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
