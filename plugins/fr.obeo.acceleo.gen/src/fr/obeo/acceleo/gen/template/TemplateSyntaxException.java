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

package fr.obeo.acceleo.gen.template;

import org.eclipse.core.resources.IMarker;

import fr.obeo.acceleo.gen.template.scripts.IScript;
import fr.obeo.acceleo.tools.log.AcceleoException;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Template Syntax Exception.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateSyntaxException extends AcceleoException {

    private static final long serialVersionUID = 1;

    /**
     * The script.
     */
    protected IScript script;

    /**
     * Position of the syntax error.
     */
    protected Int2 pos;

    /**
     * The severity of this exception. The default is IMarker.SEVERITY_ERROR.
     */
    private int severity = IMarker.SEVERITY_ERROR;

    /**
     * Constructor.
     * 
     * @param message
     *            is the message
     * @param script
     *            is the script
     * @param pos
     *            is the position of the syntax error
     */
    public TemplateSyntaxException(String message, IScript script, Int2 pos) {
        super(message);
        this.script = script;
        this.pos = pos;
    }

    /**
     * Constructor.
     * 
     * @param message
     *            is the message
     * @param script
     *            is the script
     * @param pos
     *            is the position of the syntax error
     */
    public TemplateSyntaxException(String message, IScript script, int pos) {
        super(message);
        this.script = script;
        this.pos = new Int2(pos, pos + 1);
    }

    /**
     * @return the position of the syntax error
     */
    public Int2 getPos() {
        return pos;
    }

    /**
     * @return the script
     */
    public IScript getScript() {
        return script;
    }

    /* (non-Javadoc) */
    @Override
    public String getMessage() {
        return super.getMessage();
    }

    /**
     * Set the severity of this syntaxe exception. {@see IMarker}
     * 
     * @param severity
     *            the severity
     */
    public void setSeverity(int severity) {
        this.severity = severity;
    }

    /**
     * Get the severity of this exception. {@see IMarker}
     * 
     * @return
     */
    public int getSeverity() {
        return severity;
    }
}
