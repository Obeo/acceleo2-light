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

import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.gen.template.TemplateConstants;
import fr.obeo.acceleo.gen.template.TemplateSyntaxException;
import fr.obeo.acceleo.tools.strings.Int2;
import fr.obeo.acceleo.tools.strings.TextSearch;

/**
 * It is used to create a template descriptor for the given part of the text.
 * <p>
 * Sample : script type="EClass" name="template" description=""
 * 
 * @author www.obeo.fr
 * 
 */
public class ScriptDescriptorFactory {

    /**
     * It checks the syntax and creates a descriptor for the given part of the
     * text. The part of the text to be parsed is delimited by the given limits.
     * 
     * @param text
     *            is the textual representation of the templates
     * @param script
     *            is the script
     * @param limits
     *            delimits the part of the text to be parsed for this descriptor
     * @return the new descriptor
     * @throws TemplateSyntaxException
     */
    public ScriptDescriptor createScriptDescriptor(String text, IScript script, Int2 limits) throws TemplateSyntaxException {
        String templateType = null;
        String templateName = null;
        String templateDescription = null;
        Int2 limitsFileTemplate = null;
        Int2 limitsPostExpression = null;
        Int2[] properties = TextSearch.getDefaultSearch().splitPositionsIn(text, limits.b(), limits.e(), TemplateConstants.SCRIPT_PROPERTIES_SEPARATORS, false, TemplateConstants.SPEC,
                TemplateConstants.INHIBS_SCRIPT_DECLA);
        for (int i = 0; i < properties.length; i += 2) {
            String key = text.substring(properties[i].b(), properties[i].e());
            if ((i + 1) < properties.length) {
                String value = text.substring(properties[i + 1].b(), properties[i + 1].e());
                if (value.length() >= 2 && value.startsWith(TemplateConstants.LITERAL[0]) && value.endsWith(TemplateConstants.LITERAL[1])) {
                    value = value.substring(1, value.length() - 1);
                    if (key.equals(TemplateConstants.SCRIPT_TYPE)) {
                        templateType = value;
                    } else if (key.equals(TemplateConstants.SCRIPT_NAME)) {
                        templateName = value;
                    } else if (key.equals(TemplateConstants.SCRIPT_DESC)) {
                        templateDescription = value;
                    } else if (key.equals(TemplateConstants.SCRIPT_FILE)) {
                        limitsFileTemplate = new Int2(properties[i + 1].b() + 1, properties[i + 1].e() - 1);
                    } else if (key.equals(TemplateConstants.SCRIPT_POST)) {
                        limitsPostExpression = new Int2(properties[i + 1].b() + 1, properties[i + 1].e() - 1);
                    } else {
                        throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidKey", new Object[] { key, }), script, properties[i].b()); //$NON-NLS-1$
                    }
                } else {
                    throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidKeyValue", new Object[] { key, }), script, properties[i].e()); //$NON-NLS-1$
                }
            } else {
                throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingKeyValue", new Object[] { key, }), script, properties[i].e()); //$NON-NLS-1$
            }
        }
        if (templateType == null) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingType"), script, limits.b()); //$NON-NLS-1$
        }
        if (templateName == null) {
            throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingName"), script, limits.b()); //$NON-NLS-1$
        }
        ScriptDescriptor res = new ScriptDescriptor(templateType, templateName, templateDescription, limitsFileTemplate, limitsPostExpression);
        res.setPos(limits);
        return res;
    }

    /**
     * Searches the region of the template in the text.
     * 
     * @param typeToSearch
     *            is the type of the template to search
     * @param nameToSearch
     *            is the name of the template to search
     * @return the region of the template in the text
     */
    public Int2 searchScriptRegion(String text, String typeToSearch, String nameToSearch) {
        TemplateConstants.initConstants(text);
        ScriptDescriptor descriptor = null;
        int b = 0;
        Int2 end = new Int2(0, 0);
        while (end.e() > -1 && end.e() < text.length()) {
            Int2 begin = TextSearch.getDefaultSearch().indexOf(text, TemplateConstants.SCRIPT_BEGIN, end.e(), null, TemplateConstants.INHIBS_SCRIPT_CONTENT);
            if (begin.b() > -1) {
                // A script
                if (descriptor != null && typeToSearch.equals(descriptor.getType()) && nameToSearch.equals(descriptor.getName())) {
                    return new Int2(b, begin.b());
                }
                // Prepare new script
                end = TextSearch.getDefaultSearch().blockIndexEndOf(text, TemplateConstants.SCRIPT_BEGIN, TemplateConstants.SCRIPT_END, begin.b(), false, TemplateConstants.SPEC,
                        TemplateConstants.INHIBS_SCRIPT_DECLA);
                if (end.e() == -1) {
                    descriptor = null;
                } else {
                    try {
                        descriptor = createScriptDescriptor(text, null, new Int2(begin.e(), end.b()));
                        b = begin.b();
                    } catch (TemplateSyntaxException e) {
                        descriptor = null;
                    }
                }
            } else { // -1
                // A script
                if (descriptor != null && typeToSearch.equals(descriptor.getType()) && nameToSearch.equals(descriptor.getName())) {
                    return new Int2(b, text.length());
                }
                end = new Int2(text.length(), text.length());
            }
        }
        return Int2.NOT_FOUND;
    }

}
