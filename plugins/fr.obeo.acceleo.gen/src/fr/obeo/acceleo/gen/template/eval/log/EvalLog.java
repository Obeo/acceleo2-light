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

package fr.obeo.acceleo.gen.template.eval.log;

import java.util.Iterator;

import fr.obeo.acceleo.tools.log.TreeLog;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Template log that contains evaluation failures.
 * 
 * @author www.obeo.fr
 * 
 */
public class EvalLog extends TreeLog {

    /**
     * Constructor.
     */
    public EvalLog() {
        super();
    }

    /**
     * Constructor with one failure.
     * 
     * @param failure
     *            is the first failure
     */
    public EvalLog(EvalFailure failure) {
        super(failure);
    }

    /**
     * Add evaluation error.
     * 
     * @param failure
     *            is a new failure
     */
    public void addError(EvalFailure failure) {
        super.addError(failure);
    }

    /**
     * Add evaluation warning.
     * 
     * @param failure
     *            is a new failure
     */
    public void addSevereWarning(EvalFailure failure) {
        super.addSevereWarning(failure);
    }

    /**
     * Shift position in generated text for all failures.
     * 
     * @param shift
     *            is the size of the shift
     */
    public void shiftPosition(int shift) {
        Iterator errors = this.errors.iterator();
        while (errors.hasNext()) {
            EvalFailure failure = (EvalFailure) errors.next();
            failure.position += shift;
        }
        Iterator severeWarnings = this.severeWarnings.iterator();
        while (severeWarnings.hasNext()) {
            EvalFailure failure = (EvalFailure) severeWarnings.next();
            failure.position += shift;
        }
    }

    /**
     * Moves the bounds of the errors into the given range
     * 
     * @param range
     *            are the new bounds
     */
    public void range(Int2 range) {
        if (range.b() > -1 && range.e() > -1) {
            Iterator errors = this.errors.iterator();
            while (errors.hasNext()) {
                EvalFailure failure = (EvalFailure) errors.next();
                if (failure.position > range.e()) {
                    errors.remove();
                } else {
                    failure.position -= range.b();
                }
            }
            Iterator severeWarnings = this.severeWarnings.iterator();
            while (severeWarnings.hasNext()) {
                EvalFailure failure = (EvalFailure) severeWarnings.next();
                if (failure.position > range.e()) {
                    errors.remove();
                } else {
                    failure.position -= range.b();
                }
            }
        }
    }

    /**
     * Applies the indent strategy to the positions (each line adds one
     * character).
     * 
     * @param lines
     *            are the positions of the lines
     */
    public void indent(Int2[] lines) {
        for (Int2 line : lines) {
            Iterator errors = this.errors.iterator();
            while (errors.hasNext()) {
                EvalFailure failure = (EvalFailure) errors.next();
                if (line.b() < failure.position) {
                    failure.position++;
                }
            }
            Iterator severeWarnings = this.severeWarnings.iterator();
            while (severeWarnings.hasNext()) {
                EvalFailure failure = (EvalFailure) severeWarnings.next();
                if (line.b() < failure.position) {
                    failure.position++;
                }
            }
        }
    }

}
