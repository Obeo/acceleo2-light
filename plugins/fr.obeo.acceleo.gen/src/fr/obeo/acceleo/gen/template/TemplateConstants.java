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

import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * Constants for the generation tool.
 * 
 * @author www.obeo.fr
 * 
 */
public class TemplateConstants {

    // Template's global constants.

    public static String SPEC;

    // Template's comments.

    public static String COMMENT_BEGIN;

    public static String COMMENT_END;

    public static String[] COMMENT;

    public static String[][] INHIBS_COMMENT;

    // Template's imports.

    public static String IMPORT_BEGIN;

    public static String IMPORT_END;

    public static String IMPORT_WORD;

    public static String MODELTYPE_WORD;

    // Template's user code.

    public static String USER_BEGIN_NAME;

    public static String USER_BEGIN;

    public static String USER_END_NAME;

    public static String USER_END;

    // Template's expressions.

    public static String[] PARENTH;

    public static String[] BRACKETS;

    public static String[] LITERAL;

    public static String LITERAL_SPEC;

    public static String[][] INHIBS_EXPRESSION;

    public static String NOT;

    public static String OPERATOR_OR;

    public static String OPERATOR_AND;

    public static String OPERATOR_EQUALS;

    public static String OPERATOR_NOT_EQUALS;

    public static String OPERATOR_SUP_EQUALS;

    public static String OPERATOR_INF_EQUALS;

    public static String OPERATOR_SUP;

    public static String OPERATOR_INF;

    public static String OPERATOR_ADD;

    public static String OPERATOR_SUB;

    public static String OPERATOR_DIV;

    public static String OPERATOR_MUL;

    public static String[] OPERATORS;

    public static String CALL_SEP;

    public static String ARG_SEP;

    // Template's statements.

    public static String IF_BEGIN;

    public static String IF_THEN;

    public static String IF_ELSE;

    public static String IF_ELSE_IF;

    public static String IF_END;

    public static String[] IF;

    public static String FOR_BEGIN;

    public static String FOR_THEN;

    public static String FOR_END;

    public static String[] FOR;

    public static String FEATURE_BEGIN;

    public static String FEATURE_END;

    public static String[] FEATURE;

    public static String[][] INHIBS_STATEMENT;

    // Template's declaration line.

    public static String SCRIPT_BEGIN;

    public static String SCRIPT_TYPE;

    public static String SCRIPT_NAME;

    public static String SCRIPT_DESC;

    public static String SCRIPT_FILE;

    public static String SCRIPT_POST;

    public static String SCRIPT_PROPERTY_ASSIGN;

    public static String SCRIPT_END;

    public static String[] SCRIPT_PROPERTIES_SEPARATORS;

    public static String[][] INHIBS_SCRIPT_DECLA;

    public static String[][] INHIBS_SCRIPT_CONTENT;

    // Link prefix

    public static String LINK_PREFIX_SCRIPT;

    public static String LINK_PREFIX_METAMODEL;

    public static String LINK_PREFIX_METAMODEL_SHORT;

    public static String LINK_PREFIX_JAVA;

    public static String LINK_PREFIX_SEPARATOR;

    // Predefined links

    public static String LINK_NAME_ARGS;

    public static String LINK_NAME_INDEX;

    public static String SERVICE_SEP;

    public static String SERVICE_SEPSTR;

    private static Character lastFirstChar = null;

    /**
     * Initializes all the constants for the generation tool.
     */
    public static void initConstants() {
        initConstants(""); //$NON-NLS-1$
    }

    /**
     * Initializes all the constants for the generation tool.
     * 
     * @param content
     *            is the text that is used to determine the first and the last
     *            character of the tags
     */
    public static void initConstants(String content) {
        // Choose tag
        if (content == null) {
            content = ""; //$NON-NLS-1$
        } else {
            content = content.trim();
        }
        boolean tab = content.length() > 0 && content.charAt(0) == '[';
        char first = (tab) ? '[' : '<';
        if (lastFirstChar != null && first == lastFirstChar.charValue()) {
            return;
        } else {
            lastFirstChar = new Character(first);
        }
        char last = (tab) ? ']' : '>';

        // contains String equal to first + "%" and "%" + last, avoid too much
        // concatenations later on
        final String tagStart = Character.toString(first) + '%';
        final String tagEnd = '%' + Character.toString(last);

        // Update constants
        SPEC = "\\\""; //$NON-NLS-1$
        COMMENT_BEGIN = tagStart + "--"; //$NON-NLS-1$
        COMMENT_END = "--" + tagEnd; //$NON-NLS-1$
        COMMENT = new String[] { COMMENT_BEGIN, COMMENT_END, TextSearch.FORCE_NOT_RECURSIVE };
        INHIBS_COMMENT = new String[][] { COMMENT };
        IMPORT_BEGIN = tagStart;
        IMPORT_END = tagEnd;
        IMPORT_WORD = "import"; //$NON-NLS-1$
        MODELTYPE_WORD = "metamodel"; //$NON-NLS-1$
        USER_BEGIN_NAME = "startUserCode"; //$NON-NLS-1$
        USER_BEGIN = tagStart + USER_BEGIN_NAME + tagEnd;
        USER_END_NAME = "endUserCode"; //$NON-NLS-1$
        USER_END = tagStart + USER_END_NAME + tagEnd;
        PARENTH = new String[] { "(", ")" }; //$NON-NLS-1$ //$NON-NLS-2$
        BRACKETS = new String[] { "[", "]" }; //$NON-NLS-1$ //$NON-NLS-2$
        LITERAL = new String[] { "\"", "\"", TextSearch.FORCE_NOT_RECURSIVE }; //$NON-NLS-1$ //$NON-NLS-2$
        LITERAL_SPEC = "\\\""; //$NON-NLS-1$
        INHIBS_EXPRESSION = new String[][] { PARENTH, LITERAL, BRACKETS };
        NOT = "!"; //$NON-NLS-1$
        OPERATOR_OR = "||"; //$NON-NLS-1$
        OPERATOR_AND = "&&"; //$NON-NLS-1$
        OPERATOR_EQUALS = "=="; //$NON-NLS-1$
        OPERATOR_NOT_EQUALS = "!="; //$NON-NLS-1$
        OPERATOR_SUP_EQUALS = ">="; //$NON-NLS-1$
        OPERATOR_INF_EQUALS = "<="; //$NON-NLS-1$
        OPERATOR_SUP = ">"; //$NON-NLS-1$
        OPERATOR_INF = "<"; //$NON-NLS-1$
        OPERATOR_ADD = "+"; //$NON-NLS-1$
        OPERATOR_SUB = "-"; //$NON-NLS-1$
        OPERATOR_DIV = "/"; //$NON-NLS-1$
        OPERATOR_MUL = "*"; //$NON-NLS-1$
        OPERATORS = new String[] { OPERATOR_OR, OPERATOR_AND, OPERATOR_EQUALS, OPERATOR_NOT_EQUALS, OPERATOR_SUP_EQUALS, OPERATOR_INF_EQUALS, OPERATOR_SUP, OPERATOR_INF, OPERATOR_ADD, OPERATOR_SUB,
                OPERATOR_DIV, OPERATOR_MUL };
        CALL_SEP = "."; //$NON-NLS-1$
        ARG_SEP = ","; //$NON-NLS-1$
        IF_BEGIN = tagStart + "if "; //$NON-NLS-1$
        IF_THEN = '{' + tagEnd;
        IF_ELSE = tagStart + "}else{" + tagEnd; //$NON-NLS-1$
        IF_ELSE_IF = tagStart + "}else if"; //$NON-NLS-1$
        IF_END = tagStart + '}' + tagEnd;
        IF = new String[] { IF_BEGIN, IF_END };
        FOR_BEGIN = tagStart + "for "; //$NON-NLS-1$
        FOR_THEN = '{' + tagEnd;
        FOR_END = tagStart + '}' + tagEnd;
        FOR = new String[] { FOR_BEGIN, FOR_END };
        FEATURE_BEGIN = tagStart;
        FEATURE_END = tagEnd;
        FEATURE = new String[] { FEATURE_BEGIN, FEATURE_END };
        INHIBS_STATEMENT = new String[][] { COMMENT, IF, FOR, FEATURE, };
        SCRIPT_BEGIN = tagStart + "script "; //$NON-NLS-1$
        SCRIPT_TYPE = "type"; //$NON-NLS-1$
        SCRIPT_NAME = "name"; //$NON-NLS-1$
        SCRIPT_DESC = "description"; //$NON-NLS-1$
        SCRIPT_FILE = "file"; //$NON-NLS-1$
        SCRIPT_POST = "post"; //$NON-NLS-1$
        SCRIPT_PROPERTY_ASSIGN = "="; //$NON-NLS-1$
        SCRIPT_END = tagEnd;
        SCRIPT_PROPERTIES_SEPARATORS = new String[] { SCRIPT_PROPERTY_ASSIGN, " ", "\t", "\r", "\n" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        INHIBS_SCRIPT_DECLA = new String[][] { LITERAL };
        INHIBS_SCRIPT_CONTENT = new String[][] { COMMENT, FEATURE };
        LINK_PREFIX_SCRIPT = "script"; //$NON-NLS-1$
        LINK_PREFIX_METAMODEL = "metamodel"; //$NON-NLS-1$
        LINK_PREFIX_METAMODEL_SHORT = "m"; //$NON-NLS-1$
        LINK_PREFIX_JAVA = "service"; //$NON-NLS-1$
        LINK_PREFIX_SEPARATOR = "::"; //$NON-NLS-1$
        LINK_NAME_ARGS = "args"; //$NON-NLS-1$
        LINK_NAME_INDEX = "i"; //$NON-NLS-1$
        SERVICE_SEP = "sep"; //$NON-NLS-1$
        SERVICE_SEPSTR = "sepStr"; //$NON-NLS-1$
    }

}
