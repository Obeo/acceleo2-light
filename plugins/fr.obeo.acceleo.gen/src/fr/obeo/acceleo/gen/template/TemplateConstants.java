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
        TemplateConstants.initConstants(""); //$NON-NLS-1$
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
        if (TemplateConstants.lastFirstChar != null && first == TemplateConstants.lastFirstChar.charValue()) {
            return;
        } else {
            TemplateConstants.lastFirstChar = new Character(first);
        }
        char last = (tab) ? ']' : '>';

        // contains String equal to first + "%" and "%" + last, avoid too much
        // concatenations later on
        final String tagStart = Character.toString(first) + '%';
        final String tagEnd = '%' + Character.toString(last);

        // Update constants
        TemplateConstants.SPEC = "\\\""; //$NON-NLS-1$
        TemplateConstants.COMMENT_BEGIN = tagStart + "--"; //$NON-NLS-1$
        TemplateConstants.COMMENT_END = "--" + tagEnd; //$NON-NLS-1$
        TemplateConstants.COMMENT = new String[] { TemplateConstants.COMMENT_BEGIN, TemplateConstants.COMMENT_END, TextSearch.FORCE_NOT_RECURSIVE };
        TemplateConstants.INHIBS_COMMENT = new String[][] { TemplateConstants.COMMENT };
        TemplateConstants.IMPORT_BEGIN = tagStart;
        TemplateConstants.IMPORT_END = tagEnd;
        TemplateConstants.IMPORT_WORD = "import"; //$NON-NLS-1$
        TemplateConstants.MODELTYPE_WORD = "metamodel"; //$NON-NLS-1$
        TemplateConstants.USER_BEGIN_NAME = "startUserCode"; //$NON-NLS-1$
        TemplateConstants.USER_BEGIN = tagStart + TemplateConstants.USER_BEGIN_NAME + tagEnd;
        TemplateConstants.USER_END_NAME = "endUserCode"; //$NON-NLS-1$
        TemplateConstants.USER_END = tagStart + TemplateConstants.USER_END_NAME + tagEnd;
        TemplateConstants.PARENTH = new String[] { "(", ")" }; //$NON-NLS-1$ //$NON-NLS-2$
        TemplateConstants.BRACKETS = new String[] { "[", "]" }; //$NON-NLS-1$ //$NON-NLS-2$
        TemplateConstants.LITERAL = new String[] { "\"", "\"", TextSearch.FORCE_NOT_RECURSIVE }; //$NON-NLS-1$ //$NON-NLS-2$
        TemplateConstants.LITERAL_SPEC = "\\\""; //$NON-NLS-1$
        TemplateConstants.INHIBS_EXPRESSION = new String[][] { TemplateConstants.PARENTH, TemplateConstants.LITERAL, TemplateConstants.BRACKETS };
        TemplateConstants.NOT = "!"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_OR = "||"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_AND = "&&"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_EQUALS = "=="; //$NON-NLS-1$
        TemplateConstants.OPERATOR_NOT_EQUALS = "!="; //$NON-NLS-1$
        TemplateConstants.OPERATOR_SUP_EQUALS = ">="; //$NON-NLS-1$
        TemplateConstants.OPERATOR_INF_EQUALS = "<="; //$NON-NLS-1$
        TemplateConstants.OPERATOR_SUP = ">"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_INF = "<"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_ADD = "+"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_SUB = "-"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_DIV = "/"; //$NON-NLS-1$
        TemplateConstants.OPERATOR_MUL = "*"; //$NON-NLS-1$
        TemplateConstants.OPERATORS = new String[] { TemplateConstants.OPERATOR_OR, TemplateConstants.OPERATOR_AND, TemplateConstants.OPERATOR_EQUALS, TemplateConstants.OPERATOR_NOT_EQUALS,
                TemplateConstants.OPERATOR_SUP_EQUALS, TemplateConstants.OPERATOR_INF_EQUALS, TemplateConstants.OPERATOR_SUP, TemplateConstants.OPERATOR_INF, TemplateConstants.OPERATOR_ADD,
                TemplateConstants.OPERATOR_SUB, TemplateConstants.OPERATOR_DIV, TemplateConstants.OPERATOR_MUL };
        TemplateConstants.CALL_SEP = "."; //$NON-NLS-1$
        TemplateConstants.ARG_SEP = ","; //$NON-NLS-1$
        TemplateConstants.IF_BEGIN = tagStart + "if "; //$NON-NLS-1$
        TemplateConstants.IF_THEN = '{' + tagEnd;
        TemplateConstants.IF_ELSE = tagStart + "}else{" + tagEnd; //$NON-NLS-1$
        TemplateConstants.IF_ELSE_IF = tagStart + "}else if"; //$NON-NLS-1$
        TemplateConstants.IF_END = tagStart + '}' + tagEnd;
        TemplateConstants.IF = new String[] { TemplateConstants.IF_BEGIN, TemplateConstants.IF_END };
        TemplateConstants.FOR_BEGIN = tagStart + "for "; //$NON-NLS-1$
        TemplateConstants.FOR_THEN = '{' + tagEnd;
        TemplateConstants.FOR_END = tagStart + '}' + tagEnd;
        TemplateConstants.FOR = new String[] { TemplateConstants.FOR_BEGIN, TemplateConstants.FOR_END };
        TemplateConstants.FEATURE_BEGIN = tagStart;
        TemplateConstants.FEATURE_END = tagEnd;
        TemplateConstants.FEATURE = new String[] { TemplateConstants.FEATURE_BEGIN, TemplateConstants.FEATURE_END };
        TemplateConstants.INHIBS_STATEMENT = new String[][] { TemplateConstants.COMMENT, TemplateConstants.IF, TemplateConstants.FOR, TemplateConstants.FEATURE, };
        TemplateConstants.SCRIPT_BEGIN = tagStart + "script "; //$NON-NLS-1$
        TemplateConstants.SCRIPT_TYPE = "type"; //$NON-NLS-1$
        TemplateConstants.SCRIPT_NAME = "name"; //$NON-NLS-1$
        TemplateConstants.SCRIPT_DESC = "description"; //$NON-NLS-1$
        TemplateConstants.SCRIPT_FILE = "file"; //$NON-NLS-1$
        TemplateConstants.SCRIPT_POST = "post"; //$NON-NLS-1$
        TemplateConstants.SCRIPT_PROPERTY_ASSIGN = "="; //$NON-NLS-1$
        TemplateConstants.SCRIPT_END = tagEnd;
        TemplateConstants.SCRIPT_PROPERTIES_SEPARATORS = new String[] { TemplateConstants.SCRIPT_PROPERTY_ASSIGN, " ", "\t", "\r", "\n" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        TemplateConstants.INHIBS_SCRIPT_DECLA = new String[][] { TemplateConstants.LITERAL };
        TemplateConstants.INHIBS_SCRIPT_CONTENT = new String[][] { TemplateConstants.COMMENT, TemplateConstants.FEATURE };
        TemplateConstants.LINK_PREFIX_SCRIPT = "script"; //$NON-NLS-1$
        TemplateConstants.LINK_PREFIX_METAMODEL = "metamodel"; //$NON-NLS-1$
        TemplateConstants.LINK_PREFIX_METAMODEL_SHORT = "m"; //$NON-NLS-1$
        TemplateConstants.LINK_PREFIX_JAVA = "service"; //$NON-NLS-1$
        TemplateConstants.LINK_PREFIX_SEPARATOR = "::"; //$NON-NLS-1$
        TemplateConstants.LINK_NAME_ARGS = "args"; //$NON-NLS-1$
        TemplateConstants.LINK_NAME_INDEX = "i"; //$NON-NLS-1$
        TemplateConstants.SERVICE_SEP = "sep"; //$NON-NLS-1$
        TemplateConstants.SERVICE_SEPSTR = "sepStr"; //$NON-NLS-1$
    }

}
