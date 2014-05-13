/*******************************************************************************
 * Copyright (c) 2006-2008 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package fr.obeo.acceleo.tools;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class to access externalized Strings for Acceleo's tools.
 * 
 * @author Laurent Goubet <a
 *         href="mailto:laurent.goubet@obeo.fr">laurent.goubet@obeo.fr</a>
 */
public final class AcceleoToolsMessages {
    /** Full qualified path to the properties file in which to seek the keys. */
    private static final String BUNDLE_NAME = "fr.obeo.acceleo.tools.acceleotoolsmessages"; //$NON-NLS-1$

    /** Contains the locale specific {@link String}s needed by this plug-in. */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(AcceleoToolsMessages.BUNDLE_NAME);

    /**
     * Utility classes don't need to (and shouldn't) be instantiated.
     */
    private AcceleoToolsMessages() {
        // prevents instantiation
    }

    /**
     * Returns a specified {@link String} from the resource bundle.
     * 
     * @param key
     *            Key of the String we seek.
     * @return The String from the resource bundle associated with
     *         <code>key</code>.
     */
    public static String getString(String key) {
        try {
            return AcceleoToolsMessages.getString(key, new Object[] {});
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /**
     * Returns a String from the resource bundle binded with the given
     * arguments.
     * 
     * @param key
     *            Key of the String we seek.
     * @param arguments
     *            Arguments for the String formatting.
     * @return formatted {@link String}.
     * @see MessageFormat#format(String, Object[])
     */
    public static String getString(String key, Object[] arguments) {
        if (arguments == null) {
            return MessageFormat.format(AcceleoToolsMessages.RESOURCE_BUNDLE.getString(key), new Object[] {});
        }
        return MessageFormat.format(AcceleoToolsMessages.RESOURCE_BUNDLE.getString(key), arguments);
    }
}
