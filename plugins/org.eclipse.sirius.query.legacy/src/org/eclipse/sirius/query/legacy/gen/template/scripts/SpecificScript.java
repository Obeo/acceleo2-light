/*******************************************************************************
 * Copyright (c) 2005-2014 Obeo
 *  
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.gen.template.scripts;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

import org.eclipse.sirius.query.legacy.ecore.factories.FactoryException;
import org.eclipse.sirius.query.legacy.ecore.tools.ETools;
import org.eclipse.sirius.query.legacy.gen.AcceleoGenMessages;
import org.eclipse.sirius.query.legacy.gen.template.Template;
import org.eclipse.sirius.query.legacy.gen.template.TemplateConstants;
import org.eclipse.sirius.query.legacy.gen.template.TemplateSyntaxException;
import org.eclipse.sirius.query.legacy.gen.template.TemplateSyntaxExceptions;
import org.eclipse.sirius.query.legacy.gen.template.TemplateText;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeCastException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.LaunchManager;
import org.eclipse.sirius.query.legacy.gen.template.expressions.TemplateCallExpression;
import org.eclipse.sirius.query.legacy.gen.template.expressions.TemplateCallSetExpression;
import org.eclipse.sirius.query.legacy.gen.template.expressions.TemplateExpression;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.EvalJavaService;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.EvalModel;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.JavaServiceNotFoundException;
import org.eclipse.sirius.query.legacy.gen.template.statements.TemplateFeatureStatement;
import org.eclipse.sirius.query.legacy.tools.plugins.AcceleoMetamodelProvider;
import org.eclipse.sirius.query.legacy.tools.plugins.AcceleoModuleProvider;
import org.eclipse.sirius.query.legacy.tools.resources.Resources;
import org.eclipse.sirius.query.legacy.tools.strings.Int2;
import org.eclipse.sirius.query.legacy.tools.strings.TextSearch;

/**
 * Model specific generator configuration. A script file contains this
 * configuration. <li>isDefault() == false</li> <li>isSpecific() == true</li>
 * <li>hasFileTemplate() == true if more than one file template is defined in
 * the configuration</li> <li>isGenerated(EObject) == true if one file template
 * is defined for the object</li> <li>hasError(EObject) == false</li>
 * 
 * 
 */
public class SpecificScript extends AbstractScript {

    /**
     * Generator file extension.
     */
    public static final String GENERATORS_EXTENSION = "mt"; //$NON-NLS-1$

    /**
     * Should we profile the initialization of the script.
     */
    private boolean initProfiling = false;

    /**
     * Map of text templates.
     */
    protected Map textTemplates = new TreeMap(new Comparator() {
        public int compare(Object arg0, Object arg1) {
            if (arg0.equals(arg1)) {
                return 0;
            } else {
                ScriptDescriptor a0 = (ScriptDescriptor) arg0;
                ScriptDescriptor a1 = (ScriptDescriptor) arg1;
                int result = a0.type.compareTo(a1.type);
                if (result != 0) {
                    return result;
                } else {
                    return a0.name.compareTo(a1.name);
                }
            }
        }
    });

    /**
     * The name of the templates.
     */
    protected List textTemplateNames = new ArrayList();

    /**
     * Map of file templates.
     */
    protected Map fileTemplates = new HashMap();

    /**
     * Script file that contains the specific configuration.
     */
    protected File file;

    /**
     * Root element of the metamodel.
     */
    protected EPackage metamodel = null;

    /**
     * Gets the text template that corresponds to a file template.
     */
    protected Map file2TextTemplate = new HashMap();

    /**
     * Gets the file template that corresponds to a text template.
     */
    protected Map text2FileTemplate = new HashMap();

    /**
     * The chain file.
     */
    protected File chainFile = null;

    /**
     * The context of the script.
     */
    protected ISpecificScriptContext scriptContext = null;

    /**
     * Constructor.
     */
    public SpecificScript() {
        super();
        this.file = null;
        try {
            init(new ArrayList(), "", false); //$NON-NLS-1$
        } catch (final TemplateSyntaxExceptions e) {
            // Never catch
        }
    }

    /**
     * Constructor.
     * 
     * @param file
     *            is the script file that contains the specific configuration
     */
    public SpecificScript(File file) {
        this(file, null);
    }

    /**
     * Constructor.
     * 
     * @param file
     *            is the script file that contains the specific configuration
     * @param chainFile
     *            is the optional chain file
     */
    public SpecificScript(File file, File chainFile) {
        this();
        this.file = file;
        this.chainFile = chainFile;
        this.scriptContext = null;
    }

    /**
     * Constructor.
     * 
     * @param file
     *            is the script file that contains the specific configuration
     * @param chainFile
     *            is the optional chain file
     * @param scriptContext
     *            is the context of the script
     */
    public SpecificScript(File file, File chainFile, ISpecificScriptContext scriptContext) {
        this();
        this.file = file;
        this.chainFile = chainFile;
        this.scriptContext = scriptContext;
        if (scriptContext != null) {
            scriptContext.setScript(file, this);
        }
    }

    /**
     * @return the root element of the metamodel or null
     */
    public EPackage getMetamodel() {
        return metamodel;
    }

    /**
     * @return the map of text templates
     */
    public Map getTextTemplates() {
        return textTemplates;
    }

    /* (non-Javadoc) */
    @Override
    public File getFile() {
        return file;
    }

    /**
     * @return the chain file
     */
    public File getChainFile() {
        return chainFile;
    }

    /**
     * @param chainFile
     *            is the chain file
     */
    public void setChainFile(File chainFile) {
        this.chainFile = chainFile;
    }

    /* (non-Javadoc) */
    public boolean isDefault() {
        return false;
    }

    /* (non-Javadoc) */
    public boolean isSpecific() {
        return true;
    }

    /* (non-Javadoc) */
    public void reset() throws TemplateSyntaxExceptions {
        reset(new ArrayList());
    }

    private void reset(List fileHierarchy) throws TemplateSyntaxExceptions {
        if (file != null) {
            final String content = Resources.getFileContent(file).toString();
            reset(fileHierarchy, content);
        }
    }

    /**
     * Reset the generator.
     * 
     * @param content
     *            is the new content to be parsed
     * @throws TemplateSyntaxExceptions
     */
    public void reset(String content) throws TemplateSyntaxExceptions {
        reset(new ArrayList(), content);
    }

    /**
     * Reset the generator.
     * 
     * @param fileHierarchy
     *            is the imported scripts hierarchy
     * @param content
     *            is the new content to be parsed
     * @throws TemplateSyntaxExceptions
     */
    private synchronized void reset(List fileHierarchy, String content) throws TemplateSyntaxExceptions {
        if (file != null) {
            if (fileHierarchy.contains(file.getAbsolutePath())) {
                final List problems = new ArrayList();
                problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.RecursiveDependency"), this, 0)); //$NON-NLS-1$
                throw new TemplateSyntaxExceptions(problems);
            } else {
                fileHierarchy.add(file.getAbsolutePath());
            }
        }
        boolean checkOnly;
        if (oldContent == null || !oldContent.equals(content)) {
            oldContent = content;
            checkOnly = false;
            textTemplates.clear();
            textTemplateNames.clear();
            fileTemplates.clear();
            id2TextTemplate.clear();
            id2FileTemplate.clear();
            file2TextTemplate.clear();
            text2FileTemplate.clear();
            quickTemplates.clear();
        } else {
            checkOnly = true;
        }
        clearFoundProperties();
        current = null;
        init(fileHierarchy, content, checkOnly);

    }

    private String oldContent = null;

    /**
     * Gets the file template for the given text template.
     * 
     * @param textTemplate
     *            is a text template
     * @return the file template
     */
    public Template getFileTemplate(Template textTemplate) {
        return (Template) text2FileTemplate.get(textTemplate);

    }

    /**
     * Gets the text template for the given file template.
     * 
     * @param fileTemplate
     *            is a file template
     * @return the text template
     */
    public Template getTextTemplate(Template fileTemplate) {
        return (Template) file2TextTemplate.get(fileTemplate);
    }

    /* (non-Javadoc) */
    public Template getTextTemplateForEObject(EObject object, String name) throws FactoryException, ENodeException {
        return getTextTemplateForEClass(object.eClass(), name);
    }

    private Template getTextTemplateForEClass(EClass eClass, String name) throws FactoryException, ENodeException {
        Template template = getTextTemplateForEClassifier(eClass, name);
        if (template == null) {
            if (eClass.getESuperTypes().isEmpty() && eClass != EcorePackage.eINSTANCE.getEObject()) {
                /*
                 * We want to add an "artificial" specialization on EObject
                 */
                template = getTextTemplateForEClass(EcorePackage.eINSTANCE.getEObject(), name);
            }
            final Iterator superTypes = eClass.getESuperTypes().iterator();
            while (template == null && superTypes.hasNext()) {
                final EClassifier superType = (EClassifier) superTypes.next();
                if (superType instanceof EClass) {
                    template = getTextTemplateForEClass((EClass) superType, name);
                } else {
                    template = getTextTemplateForEClassifier(superType, name);
                }
            }
        }
        return template;
    }

    private Template getTextTemplateForEClassifier(EClassifier eClass, String name) throws FactoryException, ENodeException {
        final ScriptDescriptor key = new ScriptDescriptor(ETools.getEClassifierPath(eClass), name);
        Template template = (Template) id2TextTemplate.get(key);
        if (template == null) {
            template = (Template) textTemplates.get(key);
            if (template == null) {
                final Iterator it = textTemplates.entrySet().iterator();
                while (template == null && it.hasNext()) {
                    final Map.Entry entry = (Map.Entry) it.next();
                    if (name.equals(((ScriptDescriptor) entry.getKey()).name) && ('.' + key.type).endsWith('.' + ((ScriptDescriptor) entry.getKey()).type)) {
                        template = (Template) entry.getValue();
                    }
                }
            }
            if (template != null) {
                id2TextTemplate.put(key, template);
            }
        }
        return template;
    }

    /* (non-Javadoc) */
    public Template getRootTemplate(EObject object, boolean recursive) throws FactoryException, ENodeException {
        Template textTemplate = null;
        final Template fileTemplate = getFileTemplateForEObject(object);
        if (fileTemplate != null) {
            textTemplate = (Template) file2TextTemplate.get(fileTemplate);
        }
        if (recursive && !hasFileTemplate()) {
            if (textTemplate == null) {
                final Iterator imports = this.imports.iterator();
                while (textTemplate == null && imports.hasNext()) {
                    final IEvalSettings anImport = (IEvalSettings) imports.next();
                    if (anImport instanceof IScript) {
                        try {
                            textTemplate = ((IScript) anImport).getRootTemplate(object, false);
                        } catch (final ENodeException e) {
                            textTemplate = null;
                        }
                    }
                }
            }
        }
        if (textTemplate != null) {
            return textTemplate;
        } else {
            throw new ENodeException(AcceleoGenMessages.getString("ENodeError.UnresolvedRoot"), new Int2(0, 0), this, object, true); //$NON-NLS-1$
        }
    }

    /**
     * Gets the text template that corresponds to an identifiant.
     */
    protected Map id2TextTemplate = new HashMap();

    /**
     * Creates a new empty text template.
     * 
     * @param descriptor
     *            is the descriptor of the template
     * @return the new text template
     * @throws TemplateSyntaxException
     */
    public Template createTextTemplate(ScriptDescriptor descriptor) throws TemplateSyntaxException {
        return createTextTemplate(descriptor, "", new Int2(0, 0)); //$NON-NLS-1$
    }

    /**
     * Creates a new text template.
     * 
     * @param descriptor
     *            is the descriptor of the template
     * @param text
     *            is the content of the file
     * @param pos
     *            is the position of the template in the file
     * @return the new text template
     * @throws TemplateSyntaxException
     *             if the text has syntax errors
     */
    public Template createTextTemplate(ScriptDescriptor descriptor, String text, Int2 pos) throws TemplateSyntaxException {
        TemplateSyntaxException failure = null;
        Template template = (Template) textTemplates.get(descriptor);
        if (template == null) {
            // Template has been created before?
            final String quickEntry = descriptor.getType() + '\n' + descriptor.getName() + '\n' + text.substring(pos.b(), pos.e());
            template = (Template) quickTemplates.get(quickEntry);
            if (template == null) {
                // Create template
                try {
                    template = newTextTemplate(descriptor, text, pos);
                } catch (final TemplateSyntaxException e) {
                    template = newTextTemplate(descriptor, null, pos);
                    failure = e;
                }
                quickTemplates.put(quickEntry, template);
            }
            // Update the position of this template in the script file
            template.setPos(pos);
            textTemplates.put(descriptor, template);
            if (!textTemplateNames.contains(descriptor.getName())) {
                textTemplateNames.add(descriptor.getName());
            }
            current = template;
        } else {
            failure = new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.DuplicateEntry.Text", new Object[] { descriptor.toString(), }), this, pos); //$NON-NLS-1$
        }
        if (failure != null) {
            throw failure;
        }
        return template;
    }

    private final Map quickTemplates = new HashMap();

    /**
     * It checks the syntax and creates a template for the given part of the
     * text. The part of the text to be parsed is delimited by the given limits.
     * 
     * @param descriptor
     *            is the descriptor of the template
     * @param text
     *            is the textual representation of the script that contains
     *            templates
     * @param pos
     *            delimits the part of the text to be parsed for this template
     * @return the new template
     * @throws TemplateSyntaxException
     */
    protected Template newTextTemplate(ScriptDescriptor descriptor, String text, Int2 pos) throws TemplateSyntaxException {
        final Template template = Template.read(text, pos, this);
        template.setDescriptor(descriptor);
        return template;
    }

    /**
     * Last template created.
     */
    protected Template current = null;

    /**
     * @return last template created
     */
    public Template getCurrent() {
        return current;
    }

    /**
     * @param current
     *            is the last template created
     */
    public void setCurrent(Template current) {
        this.current = current;
    }

    /* (non-Javadoc) */
    public boolean hasFileTemplate() {
        return fileTemplates.size() > 0;
    }

    /* (non-Javadoc) */
    public boolean isGenerated(EObject object) {
        try {
            return getFilePath(object, true) != null;
        } catch (final FactoryException e) {
            return false;
        }
    }

    /* (non-Javadoc) */
    public IPath getFilePath(EObject object, boolean recursive) throws FactoryException {
        if (hasFileTemplate()) {
            try {
                final Template fileTemplate = getFileTemplateForEObject(object);
                if (fileTemplate != null) {
                    final String path = fileTemplate.evaluateAsString(object, LaunchManager.create("run", false)).trim(); //$NON-NLS-1$
                    if (path.length() > 0) {
                        return new Path(path);
                    }
                }
            } catch (final ENodeException e) {
            }
        } else if (recursive) {
            final Iterator imports = this.imports.iterator();
            while (imports.hasNext()) {
                final IEvalSettings anImport = (IEvalSettings) imports.next();
                if (anImport instanceof IScript) {
                    final IPath path = ((IScript) anImport).getFilePath(object, false);
                    if (path != null) {
                        return path;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the file template for the given object of the model.
     * 
     * @param object
     *            is an object of the model
     * @return the file template
     */
    public Template getFileTemplateForEObject(EObject object) {
        Template res = getFileTemplateForEClass(object.eClass());
        // try the default EObject type
        if (res == null) {
            res = getFileTemplateForEClass(EcorePackage.eINSTANCE.getEObject());
        }
        return res;

    }

    private Template getFileTemplateForEClass(EClass eClass) {
        Template template = getFileTemplateForEClassifier(eClass);
        if (template == null) {
            final Iterator superTypes = eClass.getESuperTypes().iterator();
            while (template == null && superTypes.hasNext()) {
                final EClassifier superType = (EClassifier) superTypes.next();
                if (superType instanceof EClass) {
                    template = getFileTemplateForEClass((EClass) superType);
                } else {
                    template = getFileTemplateForEClassifier(superType);
                }
            }
        }
        return template;
    }

    private Template getFileTemplateForEClassifier(EClassifier eClass) {
        final String typeID = ETools.getEClassifierPath(eClass);
        Template template = (Template) id2FileTemplate.get(typeID);
        if (template == null) {
            template = (Template) fileTemplates.get(typeID);
            if (template == null) {
                final Iterator it = fileTemplates.entrySet().iterator();
                while (template == null && it.hasNext()) {
                    final Map.Entry entry = (Map.Entry) it.next();
                    if (('.' + typeID).endsWith("." + entry.getKey())) { //$NON-NLS-1$
                        template = (Template) entry.getValue();
                    }
                }
            }
            if (template != null) {
                id2FileTemplate.put(typeID, template);
            }
        }
        return template;
    }

    /**
     * Gets the file template that corresponds to an identifiant.
     */
    protected Map id2FileTemplate = new HashMap();

    /**
     * Creates a new file template.
     * 
     * @param typeID
     *            is the identifiant
     * @param text
     *            is the content of the template
     * @param textTemplate
     *            is the text template that corresponds to the file template to
     *            be created
     * @throws TemplateSyntaxException
     */
    public void createFileTemplate(String typeID, String text, Template textTemplate) throws TemplateSyntaxException {
        createFileTemplate(typeID, text, new Int2(0, text.length()), textTemplate);
    }

    /**
     * Creates a new file template.
     * 
     * @param typeID
     *            is the identifiant
     * @param text
     *            is the content of the file
     * @param pos
     *            is the position of the template in the file
     * @param textTemplate
     *            is the text template that corresponds to the file template to
     *            be created
     * @throws TemplateSyntaxException
     */
    public void createFileTemplate(String typeID, String text, Int2 pos, Template textTemplate) throws TemplateSyntaxException {
        TemplateSyntaxException failure = null;
        Template template = (Template) fileTemplates.get(typeID);
        if (template == null) {
            try {
                template = Template.read(text, pos, this);
            } catch (final TemplateSyntaxException e) {
                template = new Template(this);
                failure = e;
            }
            fileTemplates.put(typeID, template);
            file2TextTemplate.put(template, textTemplate);
            text2FileTemplate.put(textTemplate, template);
        } else {
            failure = new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.DuplicateEntry.File", new Object[] { typeID, }), this, pos); //$NON-NLS-1$
        }
        if (failure != null) {
            throw failure;
        }
    }

    /* (non-Javadoc) */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer(super.toString());
        final Iterator it = textTemplates.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry entry = (Map.Entry) it.next();
            final ScriptDescriptor key = (ScriptDescriptor) entry.getKey();
            buffer.append(TemplateConstants.SCRIPT_BEGIN); // TemplateConstants.
            // SCRIPT_BEGIN
            // ends with " "
            buffer.append(key.toString());
            // File template?
            final Template fileTemplate = (Template) fileTemplates.get(key.type);
            if (fileTemplate != null) {
                buffer.append(' ');
                buffer.append(TemplateConstants.SCRIPT_FILE);
                buffer.append(TemplateConstants.SCRIPT_PROPERTY_ASSIGN);
                buffer.append(TemplateConstants.LITERAL[0]);
                buffer.append(fileTemplate.toString());
                buffer.append(TemplateConstants.LITERAL[1]);
            }
            buffer.append(TemplateConstants.SCRIPT_END);
            buffer.append('\n');
            final String text = ((Template) entry.getValue()).toString();
            buffer.append(text);
            buffer.append("\n\n"); //$NON-NLS-1$
        }
        return buffer.toString();
    }

    /**
     * It checks the syntax and creates templates for the given text.
     * 
     * @param fileHierarchy
     *            is the imported scripts hierarchy
     * @param text
     *            is the text to be parsed
     * @param checkOnly
     *            indicates if it checks the syntax only
     * @throws TemplateSyntaxExceptions
     */
    protected void init(List fileHierarchy, String text, boolean checkOnly) throws TemplateSyntaxExceptions {
        final boolean isRoot = fileHierarchy.size() <= 1;
        if (AbstractScript.getScriptLoader() != null) {
            text = AbstractScript.getScriptLoader().load(text);
            if (text != null) {
                final List problems = new ArrayList();
                TemplateConstants.initConstants(text);
                oldImports = new ArrayList(imports);
                clearImports();
                if (text != null && text.length() > 0) {
                    // Get imports
                    if (file != null) {
                        try {
                            addImport(new EvalJavaService(file));
                        } catch (final JavaServiceNotFoundException e) {
                        }
                    }
                    parseImports(fileHierarchy, text, problems);
                    getSystemServicesFactory().addImports(this, isRoot);
                    // Parse scripts
                    if (!checkOnly) {
                        syntaxErrors.clear();
                        parseScripts(text, syntaxErrors);
                    }
                    problems.addAll(syntaxErrors);
                }
                // Check all expressions of the generator
                if (file != null) {
                    final IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
                    if (workspaceFile != null && workspaceFile.exists()) {
                        if (isRoot) {
                            checkAllExpressions(problems);
                        }
                    }
                }
                // check only if the compilation result is not for runtime use
                if (scriptContext != null && scriptContext.getMaxLevel() != -1) {
                    checkOverride(problems);
                }
                // Throw TemplateSyntaxExceptions if problems are detected
                if (problems.size() > 0) {
                    throw new TemplateSyntaxExceptions(problems);
                }
            }
        }
    }

    /**
     * Check for masked overrides.
     * 
     * @param problems
     *            the problem list
     */
    private void checkOverride(List problems) {
        Map overrides = new HashMap();

        Iterator it = getImports().iterator();
        while (it.hasNext()) {
            Map localOverrides = new HashMap();
            IEvalSettings imp = (IEvalSettings) it.next();
            if (imp instanceof SpecificScript) {
                SpecificScript script = (SpecificScript) imp;
                final Iterator entries = script.textTemplates.entrySet().iterator();
                while (entries.hasNext()) {
                    final Map.Entry entry = (Map.Entry) entries.next();
                    final EClassifier type = ETools.getEClassifier(script.getMetamodel(), ((ScriptDescriptor) entry.getKey()).getType());
                    if (type instanceof EClass) {
                        final Template template = (Template) entry.getValue();
                        final String name = template.getDescriptor().getName();
                        List typeList = (List) overrides.get(name);
                        if (typeList == null) {
                            typeList = new ArrayList();
                            localOverrides.put(name, typeList);
                        } else {
                            if (isMaskedOverride((EClass) type, typeList)) {
                                TemplateSyntaxException problem = new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.BadOverride",
                                        new Object[] { fileScriptToImportString(script.getFile()), }), this, 1);
                                problem.setSeverity(IMarker.SEVERITY_WARNING);
                                problems.add(problem);
                            }
                        }
                        typeList.add(type);
                    }
                }
            }
            // merge localOverrides and overrides
            Iterator localIt = localOverrides.entrySet().iterator();
            while (localIt.hasNext()) {
                final Map.Entry entry = (Map.Entry) localIt.next();
                String name = (String) entry.getKey();
                List types = (List) overrides.get(name);
                if (types == null) {
                    overrides.put(name, entry.getValue());
                } else {
                    types.addAll((List) entry.getValue());
                }
            }
        }
    }

    private boolean isMaskedOverride(EClass type, List typeList) {
        boolean res = false;
        List supertypes = type.getEAllSuperTypes();
        Iterator it = typeList.iterator();
        while (!res && it.hasNext()) {
            res = supertypes.contains(it.next());
        }
        return res;
    }

    private final List syntaxErrors = new ArrayList();

    /**
     * It checks the syntax and creates the scripts for the given text.
     * 
     * @param text
     *            is the text to be parsed
     * @param problems
     *            are the syntax problems detected during this parsing
     */
    private void parseScripts(String text, List problems) {
        TemplateConstants.initConstants(text);
        ScriptDescriptor descriptor = null;
        Int2 end = new Int2(0, 0);
        while (end.e() > -1 && end.e() < text.length()) {
            final Int2 begin = TextSearch.getDefaultSearch().indexOf(text, TemplateConstants.SCRIPT_BEGIN, end.e(), null, TemplateConstants.INHIBS_SCRIPT_CONTENT);
            if (begin.b() > -1) {
                // Create a new script
                try {
                    newScript(descriptor, text, new Int2(end.e(), begin.b()));
                } catch (final TemplateSyntaxException e) {
                    problems.add(e);
                }
                // Clear informations
                descriptor = null;
                // Prepare new script
                end = TextSearch.getDefaultSearch().blockIndexEndOf(text, TemplateConstants.SCRIPT_BEGIN, TemplateConstants.SCRIPT_END, begin.b(), false, TemplateConstants.SPEC,
                        TemplateConstants.INHIBS_SCRIPT_DECLA);
                if (end.e() == -1) {
                    problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingScriptEndTag"), this, begin.b())); //$NON-NLS-1$
                } else {
                    try {
                        descriptor = getDescriptorFactory().createScriptDescriptor(text, this, new Int2(begin.e(), end.b()));
                    } catch (final TemplateSyntaxException e) {
                        problems.add(e);
                        descriptor = null;
                    }
                }
            } else { // -1
                // Create a new script
                try {
                    newScript(descriptor, text, new Int2(end.e(), text.length()));
                } catch (final TemplateSyntaxException e) {
                    problems.add(e);
                }
                end = new Int2(text.length(), text.length());
            }
        }
    }

    /**
     * Creates an instance of the descriptor factory. This new factory will be
     * used to create template's descriptors.
     * 
     * @return the new factory
     */
    protected ScriptDescriptorFactory createDescriptorFactory() {
        return new ScriptDescriptorFactory();
    }

    /**
     * Gets the instance of the descriptor factory. This factory is used to
     * create template's descriptors.
     * 
     * @return the descriptor factory
     */
    private ScriptDescriptorFactory getDescriptorFactory() {
        if (descriptorFactory == null) {
            descriptorFactory = createDescriptorFactory();
        }
        return descriptorFactory;
    }

    private ScriptDescriptorFactory descriptorFactory = null;

    /**
     * It checks the syntax and creates the imports for the given text.
     * 
     * @param fileHierarchy
     *            is the imported scripts hierarchy
     * @param text
     *            is the text to be parsed
     * @param problems
     *            are the syntax problems detected during this parsing
     */
    protected void parseImports(List fileHierarchy, String text, List problems) {
        metamodel = null;
        final List importValues = new ArrayList();
        int end = TextSearch.getDefaultSearch().indexOf(text, TemplateConstants.SCRIPT_BEGIN, 0, null, TemplateConstants.INHIBS_SCRIPT_CONTENT).b();
        if (end == -1) {
            end = text.length();
        }
        int pos = 0;
        while (pos > -1 && pos < end) {
            final Int2 bComment = TextSearch.getDefaultSearch().indexIn(text, TemplateConstants.COMMENT_BEGIN, pos, end);
            final Int2 bImports = TextSearch.getDefaultSearch().indexIn(text, TemplateConstants.IMPORT_BEGIN, pos, end);
            if (bComment.b() > -1 && (bImports.b() == -1 || bComment.b() <= bImports.b())) {
                final Int2 eComment = TextSearch.getDefaultSearch().blockIndexEndIn(text, TemplateConstants.COMMENT_BEGIN, TemplateConstants.COMMENT_END, bComment.b(), end, false);
                if (eComment.b() > -1) {
                    pos = eComment.e();
                } else {
                    problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingCommentEndTag"), this, bComment.b())); //$NON-NLS-1$
                    pos = end;
                }
            } else if (bImports.b() > -1) {
                final Int2 eImports = TextSearch.getDefaultSearch().indexIn(text, TemplateConstants.IMPORT_END, bImports.e(), end);
                if (eImports.b() > -1) {
                    final Int2[] imports = TextSearch.getDefaultSearch().splitPositionsIn(text, bImports.e(), eImports.b(), new String[] { "\n" }, false); //$NON-NLS-1$
                    for (Int2 import1 : imports) {
                        Int2 importPos = import1;
                        importPos = TextSearch.getDefaultSearch().trim(text, importPos.b(), importPos.e());
                        if (importPos.b() > -1) {
                            if (importPos.e() > importPos.b()) {
                                if (TextSearch.getDefaultSearch().indexIn(text, TemplateConstants.IMPORT_WORD, importPos.b(), importPos.e()).b() == importPos.b()) {
                                    final Int2 valuePos = TextSearch.getDefaultSearch().trim(text, importPos.b() + TemplateConstants.IMPORT_WORD.length(), importPos.e());
                                    if (valuePos.b() > -1) {
                                        final String value = text.substring(valuePos.b(), valuePos.e()).trim();
                                        if (importValues.contains(value)) {
                                            problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.DuplicateValue", new Object[] { "import", }), this, valuePos)); //$NON-NLS-1$ //$NON-NLS-2$
                                        } else {
                                            importValues.add(value);
                                            try {
                                                newImport(fileHierarchy, value, valuePos, importValues.size());
                                            } catch (final TemplateSyntaxException e) {
                                                problems.add(e);
                                            }
                                        }
                                    } else {
                                        problems.add(new TemplateSyntaxException(
                                                AcceleoGenMessages.getString("TemplateSyntaxError.EmptyImport"), this, importPos.b() + TemplateConstants.IMPORT_WORD.length())); //$NON-NLS-1$
                                    }
                                } else if (TextSearch.getDefaultSearch().indexIn(text, TemplateConstants.MODELTYPE_WORD, importPos.b(), importPos.e()).b() == importPos.b()) {
                                    final Int2 valuePos = TextSearch.getDefaultSearch().trim(text, importPos.b() + TemplateConstants.MODELTYPE_WORD.length(), importPos.e());
                                    if (valuePos.b() > -1) {
                                        final String value = text.substring(valuePos.b(), valuePos.e()).trim();
                                        if (importValues.contains(value)) {
                                            problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.DuplicateValue", new Object[] { "metamodel", }), this, valuePos)); //$NON-NLS-1$ //$NON-NLS-2$
                                        } else {
                                            importValues.add(value);
                                            try {
                                                newImport(fileHierarchy, value, valuePos, importValues.size());
                                            } catch (final TemplateSyntaxException e) {
                                                problems.add(e);
                                            }
                                        }
                                    } else {
                                        problems.add(new TemplateSyntaxException(
                                                AcceleoGenMessages.getString("TemplateSyntaxError.EmptyValue", new Object[] { "metamodel", }), this, importPos.b() + TemplateConstants.MODELTYPE_WORD.length())); //$NON-NLS-1$ //$NON-NLS-2$
                                    }
                                } else {
                                    problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.MissingKeyWord", new Object[] { "import", }), this, importPos.b())); //$NON-NLS-1$ //$NON-NLS-2$
                                }
                            }
                        }
                    }
                    pos = eImports.e();
                } else {
                    problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidImportSequence"), this, bImports.b())); //$NON-NLS-1$
                    pos = end;
                }
            } else {
                pos = end;
            }
        }
    }

    /**
     * Creates an import.
     * 
     * @param fileHierarchy
     *            is the imported scripts hierarchy
     * @param value
     *            is the import value
     * @param valuePos
     *            is the position of the value in the script
     * @param num
     *            is the number of the import
     * @throws TemplateSyntaxException
     */
    private void newImport(List fileHierarchy, String value, Int2 valuePos, int num) throws TemplateSyntaxException {
        boolean isMetamodelImport = parseMetamodelImport(value, valuePos, num);
        if (scriptContext == null || scriptContext.getMaxLevel() == -1 || fileHierarchy.size() < scriptContext.getMaxLevel()) {
            if (!isMetamodelImport) {
                if (file != null) {
                    boolean service = false;
                    boolean specificExists = false;
                    // Specific script file
                    final String[] specificExtensions = getSpecificImportExtensions();
                    for (String specificExtension : specificExtensions) {
                        final File specificFile = resolveScriptFile(file, value, specificExtension);
                        if (specificFile != null && specificFile.exists()) {
                            // Is recursive import?
                            IScript genSpecific = this;
                            do {
                                if (genSpecific.getFile() != null && genSpecific.getFile().equals(specificFile)) {
                                    throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.RecursiveImport"), this, valuePos); //$NON-NLS-1$
                                }
                                genSpecific = genSpecific.getSpecific();
                            } while (genSpecific != null);
                            // Add import
                            try {
                                if (!tryOptimizedImport(fileHierarchy, specificFile)) {
                                    final SpecificScript newImport = createSpecificImport(specificFile);
                                    newImport.setInitProfiling(initProfiling);
                                    newImport.reset(new ArrayList(fileHierarchy));
                                    newImport.setSpecific(this);
                                    addImport(newImport);
                                }
                            } catch (final TemplateSyntaxExceptions e) {
                                String message = AcceleoGenMessages.getString("SpecificScript.ErroneousTemplate"); //$NON-NLS-1$
                                if (e.getProblems().size() == 1) {
                                    message += " : " + ((TemplateSyntaxException) e.getProblems().get(0)).getMessage(); //$NON-NLS-1$
                                }
                                throw new TemplateSyntaxException(message, this, valuePos);
                            }
                            specificExists = true;
                        }
                    }
                    // Java service
                    try {

                        addImportForJavaService(file, value);
                        service = true;
                    } catch (final JavaServiceNotFoundException e) {
                        // throw new
                        // TemplateSyntaxException(e.getMessage(),this,valuePos);
                    }
                    if (!service && !specificExists) {
                        throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.UnresolvedImport", new Object[] { value, }), this, valuePos); //$NON-NLS-1$
                    }
                }
            }
        }
    }

    /**
     * Add a java service to import list.
     * 
     * @param file
     *            the current script file
     * @param value
     *            the class name
     * @throws JavaServiceNotFoundException
     *             if the class designed by value doesn't exists
     */
    protected void addImportForJavaService(final File file, final String value) throws JavaServiceNotFoundException {
        addImport(new EvalJavaService(file, value));
    }

    protected File resolveScriptFile(File script, String importValue, String extension) {

        File res = null;
        final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(script.getAbsolutePath()));
        if (file != null && file.isAccessible()) {
            final IPath importPath = new Path(importValue.replaceAll("\\.", "/")).addFileExtension(extension); //$NON-NLS-1$ //$NON-NLS-2$
            res = getScriptFileInProject(file.getProject(), importPath);
        } else {
            final String pluginId = AcceleoModuleProvider.getDefault().getPluginId(script);
            if (pluginId != null) {
                final Bundle bundle = Platform.getBundle(pluginId);
                if (bundle != null) {
                    res = AcceleoModuleProvider.getDefault().getFile(bundle.getSymbolicName(), importValue, extension);
                }
            }
        }

        return res;
    }

    private String fileScriptToImportString(File script) {
        String res = "";
        final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(script.getAbsolutePath()));
        String[] segments = file.getProjectRelativePath().segments();
        for (int i = 1; i < segments.length; ++i) {
            res += segments[i];
            if (i < segments.length - 1) {
                res += "."; //$NON-NLS-1$
            }
        }
        return res;
    }

    private File getScriptFileInProject(IProject project, IPath importPath) {
        File result = null;
        if (project.exists()) {
            try {
                final IJavaProject javaProject = JavaCore.create(project);
                final IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
                for (int i = 0; i < entries.length && result == null; i++) {
                    final IClasspathEntry entry = entries[i];
                    if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getPath().segmentCount() > 1) {
                        final IFile test = ResourcesPlugin.getWorkspace().getRoot().getFile(entry.getPath().append(importPath));
                        if (test.exists()) {
                            result = test.getLocation().toFile();
                        }
                    }
                }
                for (int i = 0; i < entries.length && result == null; i++) {
                    final IClasspathEntry entry = entries[i];
                    if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT && entry.getPath().segmentCount() == 1) {
                        final IProject entryProject = ResourcesPlugin.getWorkspace().getRoot().getProject(entry.getPath().segment(0));
                        if (entryProject != null && entryProject.exists()) {
                            result = getScriptFileInProject(entryProject, importPath);
                        }
                    }
                }
            } catch (final JavaModelException e) {
                result = null;
            }
            if (result == null) {
                final String[] requiredPluginIDs = Resources.getRequiredPluginIDs(project);
                for (int i = 0; i < requiredPluginIDs.length && result == null; i++) {
                    final IProject bundleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(requiredPluginIDs[i]);
                    if (bundleProject != null && bundleProject.exists()) {
                        result = getScriptFileInProject(bundleProject, importPath);
                    } else if (Platform.getBundle(requiredPluginIDs[i]) != null) {
                        result = AcceleoModuleProvider.getDefault().getFile(requiredPluginIDs[i], importPath);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Tries to parse the import value and creates a metamodel import.
     * 
     * @param value
     *            is the import value
     * @param valuePos
     *            is the position of the value in the script
     * @param num
     *            is the number of the import
     * @return true if a metamodel import has been created
     * @throws TemplateSyntaxException
     */
    protected boolean parseMetamodelImport(String value, Int2 valuePos, int num) throws TemplateSyntaxException {
        if (num == 1) {
            value = value.trim();
            final EPackage regValue = EPackage.Registry.INSTANCE.getEPackage(value);
            if (regValue != null) {
                final EvalModel anImport = new EvalModel(value);
                if (anImport.getMetamodel() == null) {
                    throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.UnresolvedMetamodel"), this, valuePos); //$NON-NLS-1$
                }
                metamodel = anImport.getMetamodel();
                addImport(anImport);
                return true;
            } else {
                IPath ecorePath = new Path(value);
                if (ecorePath.segmentCount() >= 2) {
                    ecorePath = ecorePath.removeFileExtension().addFileExtension("ecore"); //$NON-NLS-1$
                }
                final File ecoreFile = AcceleoMetamodelProvider.getDefault().getFile(ecorePath);
                if (ecoreFile != null && ecoreFile.exists()) {
                    final EvalModel anImport = new EvalModel(value);
                    if (anImport.getMetamodel() == null) {
                        throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.UnresolvedMetamodel"), this, valuePos); //$NON-NLS-1$
                    }
                    metamodel = anImport.getMetamodel();
                    addImport(anImport);
                    return true;
                } else {
                    throw new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.UnresolvedMetamodel"), this, valuePos); //$NON-NLS-1$
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Gets all the file extensions that are accepted in the imports.
     * 
     * @return a table of extensions
     */
    protected String[] getSpecificImportExtensions() {
        return new String[] { SpecificScript.GENERATORS_EXTENSION };
    }

    /**
     * Creates a specific import for the given file.
     * 
     * @param specificFile
     *            is the file to import
     * @return the new specific import
     */
    protected SpecificScript createSpecificImport(File specificFile) {
        if (scriptContext != null) {
            final SpecificScript result = scriptContext.getScript(specificFile, chainFile);
            if (result != null) {
                return result;
            }
        }
        final SpecificScript result = new SpecificScript(specificFile, chainFile, scriptContext);
        return result;
    }

    /**
     * Tries a quick import with the file modification stamp.
     * 
     * @param fileHierarchy
     *            is the imported scripts hierarchy
     * @param file
     *            is the file to import
     * @return true if the quick import is done
     * @throws TemplateSyntaxExceptions
     */
    private boolean tryOptimizedImport(List fileHierarchy, File file) throws TemplateSyntaxExceptions {
        boolean tryOptimizedImport;
        final IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
        if (workspaceFile != null && workspaceFile.isAccessible()) {
            final Double newModificationStamp = new Double(workspaceFile.getModificationStamp());
            final Double oldModificationStamp = (Double) mt2OldModificationStamp.get(workspaceFile);
            if (oldModificationStamp != null && oldModificationStamp.doubleValue() == newModificationStamp.doubleValue()) {
                tryOptimizedImport = true;
            } else {
                tryOptimizedImport = false;
            }
            mt2OldModificationStamp.put(workspaceFile, newModificationStamp);
        } else {
            tryOptimizedImport = true;
        }
        if (tryOptimizedImport) {
            final Iterator it = oldImports.iterator();
            while (it.hasNext()) {
                final Object oldImportObject = it.next();
                if (oldImportObject instanceof SpecificScript) {
                    final SpecificScript oldImport = (SpecificScript) oldImportObject;
                    if (oldImport.getFile().equals(file)) {
                        oldImport.clearFoundProperties();
                        oldImport.reset(new ArrayList(fileHierarchy));
                        addImport(oldImport);
                        oldImport.setSpecific(this);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* (non-Javadoc) */
    @Override
    public void addImport(IEvalSettings element) {
        super.addImport(element);
    }

    /**
     * Quick imports : old script modification stamp.
     */
    private final Map mt2OldModificationStamp = new HashMap();

    /**
     * Quick imports : old imports.
     */
    private List oldImports = new ArrayList();

    /**
     * It checks the semantic of the links.
     * 
     * @param problems
     *            are the semantic problems detected during this checking.
     */
    protected void checkAllExpressions(List problems) {

        if (metamodel == null) {
            problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.InvalidImportSequence"), this, 0)); //$NON-NLS-1$
        } else {
            final Iterator textTemplates = this.textTemplates.entrySet().iterator();
            while (textTemplates.hasNext()) {
                final Map.Entry entry = (Map.Entry) textTemplates.next();
                final String type = ((ScriptDescriptor) entry.getKey()).type;
                final Template template = (Template) entry.getValue();
                checkAllExpressions(problems, type, template);
            }
            final Iterator fileTemplates = this.fileTemplates.entrySet().iterator();
            while (fileTemplates.hasNext()) {
                final Map.Entry entry = (Map.Entry) fileTemplates.next();
                final String type = (String) entry.getKey();
                final Template template = (Template) entry.getValue();
                checkAllExpressions(problems, type, template);
            }
        }

    }

    private void checkAllExpressions(List problems, String type, Template template) {
        // Get the EClassifier for the identifier
        final EClassifier classifier = ETools.getEClassifier(metamodel, type);
        if (classifier == null) {
            problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.UnresolvedClassifier", new Object[] { type, }), this, template.getPos())); //$NON-NLS-1$
        } else {
            // Check the elements of the template
            final List allElements = template.getAllElements(TemplateCallSetExpression.class);
            if (template.getPostExpression() != null) {
                allElements.addAll(template.getPostExpression().getAllElements(TemplateCallSetExpression.class));
            }
            final Iterator elements = allElements.iterator();
            while (elements.hasNext()) {
                final TemplateCallSetExpression callSet = (TemplateCallSetExpression) elements.next();
                if (callSet != null) {
                    if (!callSet.isPredefined()) {
                        // Check not predefined links
                        Object resolvedType = callSet.getRootResolver(classifier, this);
                        final Iterator calls = callSet.iterator();
                        TemplateCallExpression call = null;
                        while (resolvedType != null && calls.hasNext()) {
                            // Remark : resolvedType == GENERIC_TYPE
                            // => resolvedType != null
                            call = (TemplateCallExpression) calls.next();
                            if (resolvedType == IEvalSettings.GENERIC_TYPE && call.getLink().length() > 0) {
                                final char[] array = call.getLink().toCharArray();
                                for (int i = 0; i < array.length; i++) {
                                    if (!Character.isJavaIdentifierPart(array[i])) {
                                        problems.add(new TemplateSyntaxException(
                                                AcceleoGenMessages.getString("TemplateSyntaxError.InvalidCharacter", new Object[] { Character.toString(array[i]), }), this, new Int2(call.getPos().b() + i, call.getPos().b() + i + 1))); //$NON-NLS-1$
                                    }
                                }
                            }
                            resolvedType = resolveType(resolvedType, call);
                        }
                        if (call != null) {
                            if (resolvedType == null) {
                                problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.UnresolvedCall", new Object[] { call, }), this, call.getPos())); //$NON-NLS-1$
                            }
                        }
                    } else {
                        // Check the user tags
                        if (callSet.getFirst().getLink().equals(TemplateConstants.USER_BEGIN_NAME) && callSet.getFirst().countArguments() == 0) {
                            // Remark : isPredefined() &&
                            // USER_BEGIN_NAME => getParent() is
                            // instance of TemplateFeatureStatement
                            final TemplateFeatureStatement feature = (TemplateFeatureStatement) callSet.getParent();
                            boolean valid = false;
                            if (feature.getNext() != null && feature.getNext() instanceof TemplateText) {
                                final String nextString = ((TemplateText) feature.getNext()).toString();
                                final int iNewLine = nextString.indexOf("\n"); //$NON-NLS-1$
                                if (iNewLine == -1 || nextString.substring(0, iNewLine).trim().length() > 0) {
                                    valid = true;
                                }
                            }
                            if (!valid && feature.getPrevious() != null && feature.getPrevious() instanceof TemplateText) {
                                final String previousString = ((TemplateText) feature.getPrevious()).toString();
                                final int iNewLine = previousString.lastIndexOf("\n"); //$NON-NLS-1$
                                if (iNewLine == -1 || previousString.substring(iNewLine).trim().length() > 0) {
                                    valid = true;
                                }
                            }
                            if (!valid) {
                                problems.add(new TemplateSyntaxException(AcceleoGenMessages.getString("TemplateSyntaxError.EmptyUserTag"), this, callSet.getPos())); //$NON-NLS-1$
                            }
                        }
                    }
                }
            }
        }
    }

    private void newScript(ScriptDescriptor descriptor, String text, Int2 limitsTextTemplate) throws TemplateSyntaxException {
        if (descriptor != null && text != null) {
            limitsTextTemplate = Template.formatTemplate(text, limitsTextTemplate, 2);
            final Template textTemplate = createTextTemplate(descriptor, text, limitsTextTemplate);
            if (descriptor.getFileTemplate() != null) {
                createFileTemplate(descriptor.getType(), text, descriptor.getFileTemplate(), textTemplate);
            }
            if (descriptor.getPostExpression() != null) {
                final TemplateExpression postExpression = TemplateExpression.fromString(text, descriptor.getPostExpression(), this);
                textTemplate.setPostExpression(postExpression);
            }
        }
    }

    /* (non-Javadoc) */
    public boolean hasError(EObject object) {
        return false;
    }

    /* (non-Javadoc) */
    @Override
    public ENode eGetTemplate(ENode node, String name, ENode[] args, LaunchManager mode) throws ENodeException, FactoryException {

        return eGetTemplateSub(node, name, args, mode);
    }

    public ENode eGetTemplateSub(ENode node, String name, ENode[] args, LaunchManager mode) throws ENodeException, FactoryException {
        if (textTemplateNames.contains(name)) {
            if (node.isEObject()) {
                try {
                    final EObject object = node.getEObject();
                    final Template template = getTextTemplateForEObject(object, name);
                    if (template != null) {
                        contextPush(IScript.TEMPLATE_ARGS, args);
                        ENode result;
                        try {
                            final boolean withComment = !hasFileTemplate() && name.equals("write"); //$NON-NLS-1$
                            if (withComment) {
                                result = template.evaluateWithComment(object, mode);
                            } else {
                                result = template.evaluate(object, mode);
                            }
                            if (result.isNull()) {
                                final List children = template.getSignificantStatements();
                                if (children.size() > 1 || children.size() == 1 && !(children.get(0) instanceof TemplateFeatureStatement)) {
                                    // isNull() => ""
                                    result.asString();
                                }
                            }
                            if (withComment && name.equals("write") && template.isEmptyEvaluation()) { //$NON-NLS-1$
                                return null;
                            }
                        } finally {
                            contextPop(IScript.TEMPLATE_ARGS);
                        }
                        return result;
                    }
                } catch (final ENodeCastException e) {
                    // Never catch
                }
            }
        }
        return null;
    }

    /* (non-Javadoc) */
    @Override
    public Object resolveType(Object type, TemplateCallExpression call, int depth) {
        if ("".equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_SCRIPT.equals(call.getPrefix())) { //$NON-NLS-1$
            if (call.getLink().length() == 0) {
                return IEvalSettings.GENERIC_TYPE;
            }
            if (type instanceof EClassifier) {
                final Iterator textTemplates = this.textTemplates.entrySet().iterator();
                while (textTemplates.hasNext()) {
                    final Map.Entry entry = (Map.Entry) textTemplates.next();
                    final ScriptDescriptor key = (ScriptDescriptor) entry.getKey();
                    if (ETools.ofType((EClassifier) type, key.type) && key.name.equals(call.getLink())) {
                        return IEvalSettings.GENERIC_TYPE;
                    }
                }
            } else if (type == IEvalSettings.GENERIC_TYPE) {
                final Iterator textTemplates = this.textTemplates.entrySet().iterator();
                while (textTemplates.hasNext()) {
                    final Map.Entry entry = (Map.Entry) textTemplates.next();
                    final ScriptDescriptor key = (ScriptDescriptor) entry.getKey();
                    if (key.name.equals(call.getLink())) {
                        return IEvalSettings.GENERIC_TYPE;
                    }
                }
            }
        }
        return super.resolveType(type, call, depth);
    }

    /* (non-Javadoc) */
    @Override
    public Object[] getCompletionProposals(Object type, int depth) {
        final List result = new ArrayList();
        // Templates proposals
        if (type instanceof EClassifier) {
            final Iterator textTemplates = this.textTemplates.entrySet().iterator();
            while (textTemplates.hasNext()) {
                final Map.Entry entry = (Map.Entry) textTemplates.next();
                if (ETools.ofType((EClassifier) type, ((ScriptDescriptor) entry.getKey()).type)) {
                    result.add(entry);
                }
            }
        }
        // Other proposals
        result.addAll(Arrays.asList(super.getCompletionProposals(type, depth)));
        return result.toArray();
    }

    /**
     * Clears the found properties.
     */
    private void clearFoundProperties() {
        name2Properties.clear();
        allProperties = null;
    }

    /**
     * Gets the property for the key and the property file (without extension).
     * 
     * @param name
     *            is the name of the property file (without ".properties"
     *            extension)
     * @param key
     *            is the key
     * @return the value for the given key
     * @throws CoreException
     * @throws IOException
     */
    public String getProperty(String name, String key) throws CoreException, IOException {
        final Properties properties = getOrCreateProperties(name);
        if (properties != null) {
            return properties.getProperty(key);
        } else {
            return null;
        }
    }

    private Properties getOrCreateProperties(String name) throws CoreException, IOException {
        Properties properties = (Properties) name2Properties.get(name);
        if (properties == null) {
            boolean found = false;
            final List containers = getPropertyContainers();
            final Iterator it = containers.iterator();
            while (it.hasNext() && !found) {
                final File container = (File) it.next();
                if (container.exists() && container.isDirectory()) {
                    final File[] members = container.listFiles();
                    for (int i = 0; i < members.length && !found; i++) {
                        if (members[i].isFile() && (name + ".properties").equals(members[i].getName())) { //$NON-NLS-1$
                            properties = new Properties();
                            properties.load(members[i].toURL().openStream());
                            name2Properties.put(name, properties);
                            found = true;
                        }
                    }
                }
            }
        }
        return properties;
    }

    private final Map name2Properties = new HashMap();

    /**
     * Gets the value for the key in all the properties files.
     * 
     * @param key
     *            is the key
     * @return the value for the given key
     * @throws CoreException
     * @throws IOException
     */
    public String getProperty(String key) throws CoreException, IOException {
        final Properties[] properties = getOrCreateProperties();
        for (Properties propertie : properties) {
            if (propertie != null) {
                final String value = propertie.getProperty(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private Properties[] getOrCreateProperties() throws CoreException, IOException {
        if (allProperties == null) {
            final List allPropertiesList = new ArrayList();
            final List containers = getPropertyContainers();
            final Iterator it = containers.iterator();
            while (it.hasNext()) {
                final File container = (File) it.next();
                if (container.exists() && container.isDirectory()) {
                    final File[] members = container.listFiles();
                    for (File member : members) {
                        if (member.isFile() && member.getName() != null && member.getName().endsWith(".properties")) { //$NON-NLS-1$
                            final Properties properties = new Properties();
                            properties.load(member.toURL().openStream());
                            name2Properties.put(new Path(member.getName()).removeFileExtension().lastSegment(), properties);
                            allPropertiesList.add(properties);
                        }
                    }
                }
            }
            allProperties = (Properties[]) allPropertiesList.toArray(new Properties[allPropertiesList.size()]);
        }
        return allProperties;
    }

    private Properties[] allProperties = null;

    /**
     * Gets the properties files containers.
     * 
     * @return the properties files containers
     */
    protected List getPropertyContainers() {
        final List result = new ArrayList();
        if (chainFile != null && chainFile.getParentFile() != null) {
            result.add(chainFile.getParentFile());
        }
        if (file != null && file.getParentFile() != null) {
            result.add(file.getParentFile());
            File project = null;
            final IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
            if (workspaceFile != null && workspaceFile.isAccessible()) {
                project = workspaceFile.getProject().getLocation().toFile();
            }
            final String pluginId = AcceleoModuleProvider.getDefault().getPluginId(file);
            if (pluginId != null) {
                final Bundle bundle = Platform.getBundle(pluginId);
                if (bundle != null) {
                    final URL url = bundle.getEntry("/"); //$NON-NLS-1$
                    if (url != null) {
                        final File file = new File(Resources.transformToAbsolutePath(url));
                        if (file.exists()) {
                            project = file;
                        }
                    }
                }
            }
            if (project != null && project.exists()) {
                File parent = file.getParentFile().getParentFile();
                while (parent != null && parent.exists()) {
                    result.add(parent);
                    if (parent.equals(project)) {
                        break;
                    } else {
                        parent = parent.getParentFile();
                    }
                }
            }
        }
        return result;
    }

    /* (non-Javadoc) */
    public boolean validateCall(TemplateCallExpression call) {
        return "".equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_SCRIPT.equals(call.getPrefix()); //$NON-NLS-1$
    }

    /**
     * Sets initProfiling value.
     * 
     * @param initProfiling
     */
    public void setInitProfiling(boolean initProfiling) {
        this.initProfiling = initProfiling;
    }
}
