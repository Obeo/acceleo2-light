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

package fr.obeo.acceleo.gen.template.scripts;

import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * It describes a template in a specific script : name, type...
 * <p>
 * Sample : script type="EClass" name="template" description=""
 * 
 * @author www.obeo.fr
 * 
 */
public class ScriptDescriptor {

    /**
     * The type.
     */
    protected String type;

    /**
     * The name.
     */
    protected String name;

    /**
     * The documentation.
     */
    protected String description;

    /**
     * The position of the file template.
     */
    protected Int2 fileTemplate;

    /**
     * The position of the post template expression.
     */
    protected Int2 postExpression;

    /**
     * The position of the script descriptor.
     */
    protected Int2 pos;

    /**
     * Constructor.
     * 
     * @param type
     *            is the type
     * @param name
     *            is the name
     */
    public ScriptDescriptor(String type, String name) {
        this(type, name, "", null, null); //$NON-NLS-1$
    }

    /**
     * Constructor.
     * 
     * @param type
     *            is the type
     * @param name
     *            is the name
     * @param description
     *            is the documentation
     * @param fileTemplate
     *            is the position of the file template
     * @param postExpression
     *            is the position of the post template expression
     */
    public ScriptDescriptor(String type, String name, String description, Int2 fileTemplate, Int2 postExpression) {
        this.type = type;
        this.name = name;
        this.description = description != null ? description : ""; //$NON-NLS-1$
        this.fileTemplate = fileTemplate;
        this.postExpression = postExpression;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the documentation
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the position of the file template
     */
    public Int2 getFileTemplate() {
        return fileTemplate;
    }

    /**
     * @return the position of the post template expression
     */
    public Int2 getPostExpression() {
        return postExpression;
    }

    /* (non-Javadoc) */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ScriptDescriptor) {
            return type.equals(((ScriptDescriptor) other).type) && name.equals(((ScriptDescriptor) other).name);
        } else {
            return false;
        }
    }

    /* (non-Javadoc) */
    @Override
    public int hashCode() {
        StringBuffer key = new StringBuffer();
        key.append(type);
        key.append(' ');
        key.append(name);
        return key.toString().hashCode();
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        String type = this.type;
        int dot = type.lastIndexOf("."); //$NON-NLS-1$
        dot = dot > -1 ? type.substring(0, dot).lastIndexOf(".") : -1; //$NON-NLS-1$
        if (dot > -1) {
            type = type.substring(dot + 1);
        }
        StringBuffer buffer = new StringBuffer(""); //$NON-NLS-1$
        buffer.append(TemplateConstants.SCRIPT_TYPE);
        buffer.append(TemplateConstants.SCRIPT_PROPERTY_ASSIGN);
        buffer.append(TemplateConstants.LITERAL[0]);
        buffer.append(type);
        buffer.append(TemplateConstants.LITERAL[1]);
        buffer.append(' ');
        buffer.append(TemplateConstants.SCRIPT_NAME);
        buffer.append(TemplateConstants.SCRIPT_PROPERTY_ASSIGN);
        buffer.append(TemplateConstants.LITERAL[0]);
        buffer.append(name);
        buffer.append(TemplateConstants.LITERAL[1]);
        if (description != null && description.length() > 0) {
            buffer.append(' ');
            buffer.append(TemplateConstants.SCRIPT_DESC);
            buffer.append(TemplateConstants.SCRIPT_PROPERTY_ASSIGN);
            buffer.append(TemplateConstants.LITERAL[0]);
            buffer.append(description);
            buffer.append(TemplateConstants.LITERAL[1]);
        }
        return buffer.toString();
    }

    /**
     * Returns the text to put in an outline view.
     * 
     * @return the text to put in an outline view
     */
    public String getOutlineText() {
        StringBuffer buffer = new StringBuffer(""); //$NON-NLS-1$
        String type = this.type;
        int dot = type.lastIndexOf("."); //$NON-NLS-1$
        dot = dot > -1 ? type.substring(0, dot).lastIndexOf(".") : -1; //$NON-NLS-1$
        if (dot > -1) {
            type = type.substring(dot + 1);
        }
        buffer.append(type);
        buffer.append(" : "); //$NON-NLS-1$
        buffer.append(name);
        return buffer.toString();
    }

    /**
     * Getter for the descriptor position.
     * 
     * @return the descriptor position
     */
    public Int2 getPos() {
        return pos;
    }

    /**
     * Setter for the descriptor position.
     * 
     * @param pos
     *            descriptor position
     */
    public void setPos(Int2 pos) {
        this.pos = pos;
    }
}
