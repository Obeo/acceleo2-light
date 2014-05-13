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

package fr.obeo.acceleo.tools.log;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * This is a result log with a value, errors, and warnings.
 * 
 * @author www.obeo.fr
 * 
 */
public abstract class TreeLog {

    /**
     * The value transmitted from a node to another.
     */
    protected Object value = null;

    /**
     * Constructor.
     */
    protected TreeLog() {
    }

    /**
     * Constructor.
     * 
     * @param value
     *            is the value transmitted from a node to another
     */
    protected TreeLog(Object value) {
        this.value = value;
    }

    /**
     * Constructor.
     * 
     * @param failure
     *            is the first failure
     */
    protected TreeLog(IFailure failure) {
        addError(failure);
    }

    /**
     * @return the value transmitted from a node to another
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value
     *            is the value transmitted from a node to another
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Adds the errors and warnings of the other log.
     * 
     * @param otherLog
     *            is the other log
     * @param errorsToSevereWarnings
     *            indicates if the errors of the other log become warnings
     */
    public void getAll(TreeLog otherLog, boolean errorsToSevereWarnings) {
        if (errorsToSevereWarnings) {
            severeWarnings.addAll(otherLog.errors);
        } else {
            errors.addAll(otherLog.errors);
        }
        severeWarnings.addAll(otherLog.severeWarnings);
    }

    /**
     * Indicates if log is valid.
     * 
     * @return true if log is valid
     */
    public boolean isOk() {
        return !hasError();
    }

    /**
     * Indicates if log has errors. The warnings are ignored.
     * 
     * @return true if log has errors
     */
    public boolean hasError() {
        return (errors.size() > 0);
    }

    /**
     * Indicates if log has warnings. The errors are ignored.
     * 
     * @return true if log has warnings
     */
    public boolean hasSevereWarning() {
        return (severeWarnings.size() > 0);
    }

    /**
     * The comparator used to order the sorted set of failures. It guarantees
     * that the sorted set will be in ascending failure order, sorted according
     * to the natural order of the failures (method position).
     */
    protected static Comparator failureComparator = new Comparator() {
        public int compare(Object arg0, Object arg1) {
            IFailure failure0 = ((IFailure) arg0);
            IFailure failure1 = ((IFailure) arg1);
            if (failure0.position() < failure1.position()) {
                return -1;
            } else {
                return 1;
            }
        }
    };

    /**
     * The sorted set of errors.
     */
    protected TreeSet errors = new TreeSet(TreeLog.failureComparator);

    /**
     * Adds a new error.
     * 
     * @param failure
     *            is the new error
     */
    protected void addError(IFailure failure) {
        errors.add(failure);
    }

    /**
     * Iterates on all errors.
     * 
     * @return an iterator on all errors
     */
    public Iterator allErrors() {
        return errors.iterator();
    }

    /**
     * Gets last error.
     * 
     * @return the last error
     */
    public IFailure lastError() {
        if (errors.size() > 0) {
            return (IFailure) errors.last();
        } else {
            return null;
        }
    }

    /**
     * The sorted set of warnings.
     */
    protected TreeSet severeWarnings = new TreeSet(TreeLog.failureComparator);

    /**
     * Adds a new warning.
     * 
     * @param failure
     *            is the new warning
     */
    protected void addSevereWarning(IFailure failure) {
        severeWarnings.add(failure);
    }

    /**
     * Iterates on all warnings.
     * 
     * @return an iterator on all warnings
     */
    public Iterator allSevereWarnings() {
        return severeWarnings.iterator();
    }

    /**
     * Iterates on all errors and warnings.
     * 
     * @return an iterator on all errors and warnings
     */
    public Iterator allOrderedErrorsAndSevereWarnings() {
        TreeSet all = new TreeSet(TreeLog.failureComparator);
        all.addAll(errors);
        all.addAll(severeWarnings);
        return all.iterator();
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        String message = ""; //$NON-NLS-1$
        Iterator errors = allOrderedErrorsAndSevereWarnings();
        while (errors.hasNext()) {
            Object failure = errors.next();
            message += failure.toString() + '\n';
        }
        return message;
    }

}
