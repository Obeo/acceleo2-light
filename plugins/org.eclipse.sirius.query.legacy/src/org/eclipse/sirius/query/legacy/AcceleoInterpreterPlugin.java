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

package org.eclipse.sirius.query.legacy;

import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.sirius.query.legacy.business.internal.interpreter.IAcceleoInterpreterMessages;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.log.EvalFailure;
import org.eclipse.sirius.query.legacy.gen.template.eval.log.EvalLog;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @author ymortier
 */
public class AcceleoInterpreterPlugin extends Plugin {

    /** The plug-in ID . */
    public static final String PLUGIN_ID = "org.eclipse.sirius.query.legacy";

    // The shared instance
    private static AcceleoInterpreterPlugin plugin;

    /**
     * The constructor.
     */
    public AcceleoInterpreterPlugin() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     * 
     * @return the shared instance
     */
    public static AcceleoInterpreterPlugin getDefault() {
        return plugin;
    }

    /**
     * Logs the given message and throwable as an error.
     * 
     * @param message
     *            the message.
     * @param t
     *            the exception.
     */
    public void error(final String message, final Throwable t) {
        final IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, message, t);
        this.getLog().log(status);
    }

    /**
     * Logs the given message and throwable as a warning.
     * 
     * @param message
     *            the message.
     * @param t
     *            the exception.
     */
    public void warning(final String message, final Throwable t) {
        final IStatus status = new Status(IStatus.WARNING, PLUGIN_ID, message, t);
        this.getLog().log(status);
    }

    /**
     * Logs the given message as an information.
     * 
     * @param message
     *            the message.
     */
    public void info(final String message) {
        final IStatus status = new Status(IStatus.INFO, PLUGIN_ID, message);
        this.getLog().log(status);
    }

    /**
     * Log the error of the node.
     * 
     * @param node
     *            the node.
     * @param subStatus
     *            the subStatus
     */
    public void logENode(final ENode node, final IStatus subStatus) {
        final EvalLog evalLog = node.log();
        final MultiStatus bigStatus = new MultiStatus(this.getBundle().getSymbolicName(), IStatus.ERROR, IAcceleoInterpreterMessages.EVALUATION_ERROR, new RuntimeException(evalLog.toString()));
        if (!evalLog.isOk()) {
            @SuppressWarnings("unchecked")
            final Iterator<EvalFailure> iterErrors = evalLog.allOrderedErrorsAndSevereWarnings();
            while (iterErrors.hasNext()) {
                final EvalFailure failure = iterErrors.next();
                final IStatus status = new Status(IStatus.INFO, this.getBundle().getSymbolicName(), failure.getMessage());
                bigStatus.add(status);
            }
            if (subStatus != null) {
                if (subStatus.isMultiStatus()) {
                    bigStatus.addAll(subStatus);
                } else {
                    bigStatus.add(subStatus);
                }
            }
        }
        this.getLog().log(bigStatus);
    }
}
