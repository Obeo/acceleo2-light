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

package org.eclipse.sirius.query.legacy.gen.template.eval;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

import org.eclipse.sirius.query.legacy.ecore.tools.ETools;
import org.eclipse.sirius.query.legacy.gen.AcceleoGenMessages;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;
import org.eclipse.sirius.query.legacy.tools.log.AcceleoException;
import org.eclipse.sirius.query.legacy.tools.log.Trace;
import org.eclipse.sirius.query.legacy.tools.plugins.AcceleoModuleProvider;
import org.eclipse.sirius.query.legacy.tools.resources.Resources;
import org.eclipse.sirius.query.legacy.tools.strings.Int2;
import org.eclipse.sirius.query.legacy.tools.strings.TextSearch;

/**
 * ENode Exception.
 * 
 */
public class ENodeException extends AcceleoException {

    private static final long serialVersionUID = 1;

    /**
     * The identifier of the runtime error marker.
     */
    public static final String RUNTIME_ERROR_MARKER_ID = "org.eclipse.sirius.query.legacy.gen.ui.problem"; //$NON-NLS-1$

    /**
     * The model attribute for the runtime error marker.
     */
    public static final String RUNTIME_ERROR_MARKER_MODEL = "runmodel"; //$NON-NLS-1$

    /**
     * The fragment attribute for the runtime error marker.
     */
    public static final String RUNTIME_ERROR_MARKER_FRAGMENT = "runfragment"; //$NON-NLS-1$

    /**
     * Position of the evaluation error in the script.
     */
    protected Int2 pos;

    /**
     * Script that launch the evaluation error.
     */
    protected IScript script;

    /**
     * The current object of the model (EObject or ENode).
     */
    protected Object object;

    /**
     * Optional runtime exception.
     */
    protected Throwable exception;

    /**
     * Indicates if the runtime markers are activated.
     */
    private static boolean runtimeMarker = true;

    /**
     * The trigger to disable markers.
     */
    private static Object disabledTrigger = null;

    /**
     * The runtime markers are disabled.
     * 
     * @param source
     *            is the trigger object
     */
    public static void disableRuntimeMarkersFor(Object source) {
        if (ENodeException.runtimeMarker && ENodeException.disabledTrigger == null) {
            ENodeException.runtimeMarker = false;
            ENodeException.disabledTrigger = source;
        }
    }

    /**
     * The runtime markers are enabled.
     * 
     * @param source
     *            is the trigger object
     */
    public static void enableRuntimeMarkersFor(Object source) {
        if (!ENodeException.runtimeMarker && source == ENodeException.disabledTrigger) {
            ENodeException.runtimeMarker = true;
            ENodeException.disabledTrigger = null;
        }
    }

    /**
     * Constructor.
     * 
     * @param message
     *            is the message
     * @param pos
     *            is the position of the evaluation error in the script
     * @param script
     *            is the script
     * @param object
     *            is the current object of the model (EObject or ENode)
     * @param report
     *            indicates if this exception has to be reported in the error
     *            log
     */
    public ENodeException(String message, Int2 pos, IScript script, Object object, boolean report) {
        this(message, pos, script, object, report, null);
    }

    /**
     * Constructor.
     * 
     * @param message
     *            is the message
     * @param pos
     *            is the position of the evaluation error in the script
     * @param script
     *            is the script
     * @param object
     *            is the current object of the model (EObject or ENode)
     * @param report
     *            indicates if this exception has to be reported in the error
     *            log
     * @param exception
     *            is an optional runtime exception
     */
    public ENodeException(String message, Int2 pos, IScript script, Object object, boolean report, Throwable exception) {
        super(message);
        this.pos = pos;
        this.script = script;
        this.object = object;
        this.exception = exception;
        if (report && ENodeException.runtimeMarker) {
            getErrorMessage(true);
        }
    }

    /* (non-Javadoc) */
    @Override
    public String getMessage() {
        return getErrorMessage(false);
    }

    private String getErrorMessage(boolean report) {
        // FIXME NLS should unit test this method to ensure messages are still
        // accurate
        String errorMessage = super.getMessage();
        String extendedErrorMessage = new String();
        if (script.getFile() != null) {
            String path = script.getFile().getAbsolutePath();
            IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(path));
            if (workspaceFile == null || !workspaceFile.isAccessible()) {
                String relativePath = AcceleoModuleProvider.getDefault().getRelativePath(script.getFile());
                if (relativePath != null) {
                    String pluginId = AcceleoModuleProvider.getDefault().getPluginId(script.getFile());
                    if (pluginId != null) {
                        path = new Path('/' + pluginId).append(relativePath).toString();
                    }
                }
            } else {
                path = workspaceFile.getFullPath().toString();
            }

            Int2[] newLines = TextSearch.getDefaultSearch().allIndexIn(Resources.getFileContent(script.getFile()).toString(), "\n", 0, pos.b()); //$NON-NLS-1$
            int line = newLines.length + 1;
            int column = pos.b();
            if (newLines.length > 0) {
                column = column - newLines[newLines.length - 1].e();
            }
            extendedErrorMessage = AcceleoGenMessages
                    .getString("ENodeException.ExtendedErrorMessage.KnownFile", new Object[] { errorMessage, path, Integer.toString(line), Integer.toString(column), }); //$NON-NLS-1$		
        } else {
            if (pos.b() > 0) {
                extendedErrorMessage = AcceleoGenMessages.getString(
                        "ENodeException.ExtendedErrorMessage.UnknownFile", new Object[] { errorMessage, Integer.toString(pos.b()), Integer.toString(pos.e()), }); //$NON-NLS-1$
            }
        }
        String modelURI = ""; //$NON-NLS-1$
        String fragmentURI = ""; //$NON-NLS-1$
        if (object != null) {
            Object object = this.object;
            if (object instanceof ENode) {
                if (((ENode) object).isEObject()) {
                    try {
                        object = ((ENode) object).getEObject();
                    } catch (ENodeCastException e) {
                        // Never catch
                    }
                } else if (((ENode) object).isList()) {
                    extendedErrorMessage += AcceleoGenMessages.getString("ENodeException.ExtendedErrorMessage.NodeFragment", new Object[] { ((ENode) object).getType(), }); //$NON-NLS-1$ 
                    object = ((ENode) object).getContainerEObject();
                } else {
                    extendedErrorMessage += AcceleoGenMessages.getString("ENodeException.ExtendedErrorMessage.NodeFragment", new Object[] { ((ENode) object).getType(), }); //$NON-NLS-1$
                    extendedErrorMessage += " : " + ((ENode) object).toString(); //$NON-NLS-1$
                    object = ((ENode) object).getContainerEObject();
                }
            }
            if (object instanceof EObject) {
                try {
                    fragmentURI = ETools.getURI((EObject) object);
                } catch (UnsupportedOperationException e) {
                    // we know some ResourceImpl will throw this Exception if
                    // the getURIFragment is not
                    // supported, (like CDOResource for instance). We don't want
                    // to process to fail just for
                    // an error during the message creation.
                }
                if (((EObject) object).eResource() != null) {
                    URI uri = ((EObject) object).eResource().getURI();
                    modelURI = (uri != null) ? uri.path() : ""; //$NON-NLS-1$
                    extendedErrorMessage += AcceleoGenMessages.getString("ENodeException.ExtendedErrorMessage.ObjectFragmentKnownResource", new Object[] { fragmentURI, modelURI, }); //$NON-NLS-1$
                } else {
                    extendedErrorMessage += AcceleoGenMessages.getString(
                            "ENodeException.ExtendedErrorMessage.ObjectFragmentUnknownResource", new Object[] { fragmentURI, ((EObject) object).eClass().getName(), object.toString(), }); //$NON-NLS-1$
                }
            }
        }
        if (exception != null) {
            extendedErrorMessage += '\n' + Trace.getStackTrace(exception);
        }

        return extendedErrorMessage;
    }

}
