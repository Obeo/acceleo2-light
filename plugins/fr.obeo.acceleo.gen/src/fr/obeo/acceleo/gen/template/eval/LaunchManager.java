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

package fr.obeo.acceleo.gen.template.eval;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The launch manager : the mode in which to launch - RUN_MODE, DEBUG_MODE, or
 * PREVIEW_MODE.
 * 
 * @author www.obeo.fr
 * 
 */
public class LaunchManager {

    /**
     * The mode in which to launch : RUN_MODE.
     */
    public static final int RUN_MODE = 1;

    /**
     * The mode in which to launch : DEBUG_MODE.
     */
    public static final int DEBUG_MODE = 2;

    /**
     * The mode in which to launch : PREVIEW_MODE.
     */
    public static final int PREVIEW_MODE = 3;

    /**
     * The mode in which to launch : PHANTOM_MODE.
     */
    public static final int PHANTOM_MODE = 4;

    /**
     * The mode in which to launch.
     */
    private final int mode;

    /**
     * Activates text/model synchronization.
     */
    private boolean synchronize;

    /**
     * The progress monitor;
     */
    private IProgressMonitor monitor;

    private boolean profiling;

    private boolean logEObject;

    /**
     * Constructor.
     * 
     * @param mode
     *            is mode in which to launch
     * @param synchronize
     *            activates text/model synchronization
     */
    private LaunchManager(int mode, boolean synchronize) {
        this.mode = mode;
        this.synchronize = synchronize;
    }

    /**
     * Creates a new manager.
     * 
     * @param mode
     *            is a string - "run", "debug", "preview", "phantom", "profile"
     * @param synchronize
     *            indicates if the text/model synchronization is activated
     * @return the manager or null
     */
    public static LaunchManager create(String mode, boolean synchronize) {
        if (mode == null) {
            return null;
        } else if (mode.equals("run")) { //$NON-NLS-1$
            return new LaunchManager(LaunchManager.RUN_MODE, synchronize);
        } else if (mode.equals("debug")) { //$NON-NLS-1$
            return new LaunchManager(LaunchManager.DEBUG_MODE, synchronize);
        } else if (mode.equals("preview")) { //$NON-NLS-1$
            return new LaunchManager(LaunchManager.PREVIEW_MODE, synchronize);
        } else if (mode.equals("phantom")) { //$NON-NLS-1$
            return new LaunchManager(LaunchManager.PHANTOM_MODE, synchronize);
        } else {
            return null;
        }
    }

    /**
     * @return the mode in which to launch
     */
    public int getMode() {
        return mode;
    }

    /**
     * @return true if the text/model synchronization is activated
     */
    public boolean isSynchronize() {
        return synchronize;
    }

    /**
     * @param synchronize
     *            indicates if the text/model synchronization is activated
     */
    public void setSynchronize(boolean synchronize) {
        this.synchronize = synchronize;
    }

    /**
     * @return the monitor
     */
    public IProgressMonitor getMonitor() {
        return monitor;
    }

    /**
     * @param monitor
     *            is the monitor to set
     */
    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * @return true if the profiling is activated
     */
    public boolean isProfiling() {
        return profiling;
    }

    /**
     * @return true if the profiling of EObjects is activated
     */
    public boolean isLoggingEObject() {
        return logEObject;
    }

    /**
     * Turn EObjects profiling on.
     */
    public void logEObjects() {
        logEObject = true;
    }
}
