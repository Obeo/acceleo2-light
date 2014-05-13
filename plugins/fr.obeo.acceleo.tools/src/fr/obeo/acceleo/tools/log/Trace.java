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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import fr.obeo.acceleo.tools.format.Conventions;

/**
 * Trace for the acceleo tools.
 * 
 * @author www.obeo.fr
 * 
 */
public class Trace {

    /**
     * Puts in a string the stack trace of an exception.
     * 
     * @param e
     *            is an exception
     * @return the stack transformed into a string
     */
    public static String getStackTrace(Throwable e) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * It Indicates if trace is done.
     * <p>
     * It prints messages in the console.
     */
    public static boolean TRACE = false;

    /**
     * It prints the given text in the console.
     * 
     * @param text
     *            is the message to print
     */
    public static void console(String text) {
        if (Trace.TRACE) {
            text = Conventions.formatString(text);
            System.out.println(Trace.tabs() + text);
        }
    }

    /**
     * It computes correct indentation.
     */
    protected static String tabs() {
        String tabs = ""; //$NON-NLS-1$
        for (int i = 0; i < Trace.nbTab; i++) {
            tabs += "  "; //$NON-NLS-1$
        }
        return tabs;
    }

    /**
     * It adds a tabulation for the indentation.
     */
    public static void tab() {
        Trace.nbTab++;
    }

    /**
     * It withdraws a tabulation for the indentation.
     */
    public static void untab() {
        if (Trace.nbTab > 0) {
            Trace.nbTab--;
        }
    }

    /**
     * Counts the tabulations in the indentation.
     */
    protected static int nbTab = 0;

}
