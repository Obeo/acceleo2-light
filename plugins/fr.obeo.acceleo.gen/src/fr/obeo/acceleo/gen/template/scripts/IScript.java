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

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.factories.FactoryException;
import fr.obeo.acceleo.gen.template.Template;
import fr.obeo.acceleo.gen.template.TemplateSyntaxExceptions;
import fr.obeo.acceleo.gen.template.eval.ENodeException;

/**
 * Generator configuration.
 * 
 * @author www.obeo.fr
 * 
 */
public interface IScript extends IEvalSettings {

    /**
     * Indicates if it's a default generator.
     * 
     * @return true if it's a default generator
     */
    public boolean isDefault();

    /**
     * Indicates if it's a specific generator.
     * 
     * @return true if it's a specific generator
     */
    public boolean isSpecific();

    /**
     * Reset the generator.
     * 
     * @throws TemplateSyntaxExceptions
     */
    public void reset() throws TemplateSyntaxExceptions;

    /**
     * Indicates if this generator has file template.
     * 
     * @return true if this generator has file template
     */
    public boolean hasFileTemplate();

    /**
     * Indicates if the given object has to be generated.
     * 
     * @param object
     *            is the tested object
     * @return true if the given object has to be generated
     */
    public boolean isGenerated(EObject object);

    /**
     * Returns the path of generation where the given object has to be
     * generated.
     * 
     * @param object
     *            is the tested object
     * @param recursive
     *            indicates that we use imported scripts
     * @return the path of generation, or null if the given object isn't
     *         generated
     * @throws FactoryException
     */
    public IPath getFilePath(EObject object, boolean recursive) throws FactoryException;

    /**
     * Indicates if the generation of the given object contains errors.
     * 
     * @param object
     *            is the tested object
     * @return true if the generation of the given object contains errors
     */
    public boolean hasError(EObject object);

    /**
     * Gets the template with the given name for the given object of the model.
     * In this generator, the resulting template will be applied on the given
     * object.
     * 
     * @param object
     *            is an object of the model
     * @param key
     *            is the name of the wanted template
     * @return the text template
     * @throws FactoryException
     * @throws ENodeException
     */
    public Template getTextTemplateForEObject(EObject object, String key) throws FactoryException, ENodeException;

    /**
     * Gets the root template of the script for the given object.
     * 
     * @param object
     *            is an object
     * @param recursive
     *            indicates that we use imported scripts
     * @return the root template
     * @throws FactoryException
     * @throws ENodeException
     */
    public Template getRootTemplate(EObject object, boolean recursive) throws FactoryException, ENodeException;

    /**
     * Adds a new import. The imported elements are used, in order, after this
     * one during generation.
     * 
     * @param element
     *            to add
     */
    public void addImport(IEvalSettings element);

    /**
     * Removes the given import. The imported elements are used, in order, after
     * this one during generation.
     * 
     * @param element
     *            to remove
     */
    public void removeImport(IEvalSettings element);

    /**
     * Removes all imports. The imported elements are used, in order, after this
     * one during generation.
     */
    public void clearImports();

    /**
     * Gets the main script of the specific strategy.
     */
    public IScript[] goToSpecifics();

    /**
     * Gets the more specific script. The specific script is used before this
     * one to resolve the links.
     * 
     * @return the more specific script
     */
    public IScript getSpecific();

    /**
     * Sets the more specific script. The specific script is used before this
     * one to resolve the links.
     * 
     * @param specific
     *            is the more specific script
     */
    public void setSpecific(IScript specific);

    /**
     * Gets the script file.
     */
    public File getFile();

    /**
     * Returns the value to which the context of this generator maps the
     * specified key.
     * 
     * @param key
     *            whose associated value is to be returned
     * @return the value to which the context of this generator maps the
     *         specified key
     */
    public Object contextPeek(Object key);

    /**
     * Returns the value to which the context of this generator maps the
     * specified key at the given index.
     * 
     * @param key
     *            is the key
     * @param index
     *            is the index
     * @return the value to which the context of this generator maps the
     *         specified key
     */
    public Object contextAt(Object key, int index);

    /**
     * Associates the specified value with the specified key in the context of
     * this generator.
     * 
     * @param key
     *            with which the specified value is to be associated
     * @param value
     *            to be associated with the specified key
     */
    public void contextPush(Object key, Object value);

    /**
     * Deletes the last specified value for the specified key in the context of
     * this generator.
     * 
     * @param key
     *            is the key
     */
    public void contextPop(Object key);

    // Context key for while index
    public static final Integer WHILE_INDEX = new Integer(0);

    // Context key for template arguments
    public static final Integer TEMPLATE_ARGS = new Integer(1);

    // Context key for service position
    public static final Integer ARGUMENT_POSITION = new Integer(2);

    // Context key for current node
    public static final Integer CURRENT_NODE = new Integer(3);

    // Context key for current node
    public static final Integer TEMPLATE_NODE = new Integer(4);

}
