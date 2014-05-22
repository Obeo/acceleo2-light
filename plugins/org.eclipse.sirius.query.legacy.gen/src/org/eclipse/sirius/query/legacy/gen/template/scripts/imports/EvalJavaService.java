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

package org.eclipse.sirius.query.legacy.gen.template.scripts.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EObject;
import org.osgi.framework.Bundle;

import org.eclipse.sirius.query.legacy.ecore.factories.FactoryException;
import org.eclipse.sirius.query.legacy.gen.AcceleoEcoreGenPlugin;
import org.eclipse.sirius.query.legacy.gen.AcceleoGenMessages;
import org.eclipse.sirius.query.legacy.gen.template.TemplateConstants;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENode;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeCastException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeException;
import org.eclipse.sirius.query.legacy.gen.template.eval.ENodeList;
import org.eclipse.sirius.query.legacy.gen.template.eval.LaunchManager;
import org.eclipse.sirius.query.legacy.gen.template.expressions.TemplateCallExpression;
import org.eclipse.sirius.query.legacy.gen.template.expressions.TemplateExpression;
import org.eclipse.sirius.query.legacy.gen.template.expressions.TemplateLiteralExpression;
import org.eclipse.sirius.query.legacy.gen.template.scripts.AbstractScript;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IEvalSettings;
import org.eclipse.sirius.query.legacy.gen.template.scripts.IScript;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ContextServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ENodeServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.EObjectServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.RequestServices;
import org.eclipse.sirius.query.legacy.tools.classloaders.AcceleoClassLoader;
import org.eclipse.sirius.query.legacy.tools.classloaders.AcceleoGenClassLoader;
import org.eclipse.sirius.query.legacy.tools.plugins.AcceleoModuleProvider;

/**
 * Java services element that can be used during a code generation.
 * 
 * 
 */
public class EvalJavaService implements IEvalSettings {

    private static String[] SERVICES_WITH_TYPE_RESOLVE = new String[] { "filter", "cast", "until", "eContainer", "eAllContents", "current" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

    private static String[] SERVICES_WITH_TYPE_BRIDGE = new String[] { "select", "delete", "sep", "nGet", "trace", "debug", "nFirst", "nLast", "sort", "nSort", "minimize", "nMinimize", "reverse", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$
            "nReverse" }; //$NON-NLS-1$

    private static List getTypeResolveList() {
        if (EvalJavaService.typeResolveList == null) {
            EvalJavaService.typeResolveList = new ArrayList();
            for (String element : EvalJavaService.SERVICES_WITH_TYPE_RESOLVE) {
                EvalJavaService.typeResolveList.add(element);
            }
        }
        return EvalJavaService.typeResolveList;
    }

    private static List typeResolveList = null;

    private static List getTypeBridgeList() {
        if (EvalJavaService.typeBridgeList == null) {
            EvalJavaService.typeBridgeList = new ArrayList();
            for (String element : EvalJavaService.SERVICES_WITH_TYPE_BRIDGE) {
                EvalJavaService.typeBridgeList.add(element);
            }
        }
        return EvalJavaService.typeBridgeList;
    }

    private static List typeBridgeList = null;

    /**
     * The default evaluation mode.
     */
    public static final int MODE_DEFAULT = 0;

    /**
     * The 'ENode' evaluation mode : ENode services are available.
     */
    public static final int MODE_ENODE = 1;

    /**
     * The 'ENodeList' evaluation mode : ENodeList services are available.
     */
    public static final int MODE_LIST = 2;

    /**
     * The evaluation mode.
     */
    protected int mode = EvalJavaService.MODE_DEFAULT;

    /**
     * Instance that contains the services.
     */
    protected Object instance;

    /**
     * The class loader.
     */
    private ClassLoader loader = null;

    /**
     * Gets the service for the given name
     */
    private final Map name2service = new HashMap();

    /**
     * Indicates if the services have a script context.
     */
    private boolean hasScriptContext = true;

    /**
     * Constructor.
     * 
     * @param instance
     *            is the instance that contains the services
     * @param hasScriptContext
     *            indicates if the services have a script context
     */
    public EvalJavaService(Object instance, boolean hasScriptContext) {
        this.instance = instance;
        this.hasScriptContext = hasScriptContext;
        initializeName2service();
    }

    /**
     * Constructor.
     * 
     * @param script
     *            is the script that imports the service
     * @param className
     *            is the class to be created
     * @throws JavaServiceNotFoundException
     */
    public EvalJavaService(File script, String className) throws JavaServiceNotFoundException {
        // Remark : script "fr.package.template" -> class "fr.package.Template"
        String shortName = className;
        String path = ""; //$NON-NLS-1$
        final int jDot = className.lastIndexOf("."); //$NON-NLS-1$
        if (jDot > -1) {
            shortName = className.substring(jDot + 1);
            path = className.substring(0, jDot);
        }
        if (shortName.length() > 0) {
            shortName = shortName.substring(0, 1).toUpperCase() + shortName.substring(1);
        }
        className = (path.length() > 0 ? path + '.' : "") + shortName; //$NON-NLS-1$
        initialize(script, className);
        initializeName2service();
    }

    /**
     * Constructor.
     * 
     * @param script
     *            is the script that imports the service
     * @throws JavaServiceNotFoundException
     */
    public EvalJavaService(File script) throws JavaServiceNotFoundException {
        // Remark : script "/fr/package/template.mt" -> class
        // "fr.package.Template"
        String name = new Path(script.getName()).removeFileExtension().lastSegment();
        if (name.length() > 0) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        final String[] segments = getPackagePath(script).append(name).segments();
        // ASSERT (segments.length > 0){
        // Java class name
        final StringBuffer className = new StringBuffer();
        className.append(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            className.append('.');
            className.append(segments[i]);
        }
        initialize(script, className.toString());
        initializeName2service();
    }

    /**
     * @return the evaluation mode
     */
    public int getMode() {
        return mode;
    }

    /**
     * @param mode
     *            is the evaluation mode
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * @return true if the services have a script context
     */
    public boolean hasScriptContext() {
        return hasScriptContext;
    }

    /**
     * @return the instance
     */
    public Object getInstance() {
        return instance;
    }

    private void initialize(File script, String className) throws JavaServiceNotFoundException {
        try {
            loader = createClassLoader(script);
            final Class c = loader.loadClass(className);
            instance = c.newInstance();
        } catch (final Exception e) {
            throw new JavaServiceNotFoundException(AcceleoGenMessages.getString("EvalJavaService.UnresolvedService", new Object[] { e.getMessage(), })); //$NON-NLS-1$
        }
    }

    private void initializeName2service() {
        name2service.clear();
        if (instance != null) {
            final Method[] methods = instance.getClass().getMethods();
            for (Method method : methods) {
                if (method.getDeclaringClass() == instance.getClass() && method.getParameterTypes().length > 0) {
                    final String key = method.getName() + method.getParameterTypes().length;
                    final Method[] name2values = (Method[]) name2service.get(key);
                    if (name2values == null) {
                        name2service.put(key, new Method[] { method });
                    } else {
                        final List name2values_ = new ArrayList(name2values.length + 1);
                        boolean ok = false;
                        for (Method name2value : name2values) {
                            if (name2value.getParameterTypes()[0].isAssignableFrom(method.getParameterTypes()[0]) || name2value.getParameterTypes()[0] == ENode.class) {
                                name2values_.add(method);
                                ok = true;
                            }
                            name2values_.add(name2value);
                        }
                        if (!ok) {
                            name2values_.add(method);
                        }
                        name2service.put(key, name2values_.toArray(new Method[name2values_.size()]));
                    }
                }
            }
        }
    }

    private Method[] getPotentialMethods(String name, int argsCount) {
        final String key = name + argsCount;
        return (Method[]) name2service.get(key);
    }

    /**
     * Gets the package path of the given script
     * 
     * @param script
     *            is the script
     * @return the full path of the package
     */
    private IPath getPackagePath(File script) {
        final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(script.getAbsolutePath()));
        if (file != null && file.isAccessible()) {
            return file.getProjectRelativePath().removeLastSegments(1).removeFirstSegments(1);
        }
        final String path = AcceleoModuleProvider.getDefault().getRelativePath(script);
        if (path != null) {
            return new Path(path).removeLastSegments(1);
        } else {
            return new Path(""); //$NON-NLS-1$
        }
    }

    /**
     * Creates the default class loader for the given script.
     * 
     * @param script
     *            is the script
     * @return the default class loader
     */
    private ClassLoader createClassLoader(File script) {
        final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(script.getAbsolutePath()));
        if (file != null && file.isAccessible()) {
            return new AcceleoGenClassLoader(file.getProject(), EvalJavaService.class.getClassLoader());
        }
        final String pluginId = AcceleoModuleProvider.getDefault().getPluginId(script);
        if (pluginId != null) {
            final Bundle bundle = Platform.getBundle(pluginId);
            if (bundle != null) {
                return new AcceleoGenClassLoader(bundle, EvalJavaService.class.getClassLoader());
            }
        }
        return EvalJavaService.class.getClassLoader();
    }

    /* (non-Javadoc) */
    public ENode eGet(TemplateCallExpression call, ENode node, ENode[] args, LaunchManager mode, boolean recursiveSearch) throws FactoryException, ENodeException {
        if (name2service.containsKey(call.getLink() + (args.length + 1))) {
            if (node.isEObject()) {
                final ClassLoader old = AcceleoClassLoader.getPreferredClassLoader();
                try {
                    AcceleoClassLoader.setPreferredLoader(node.getEObject());
                    return eGetSub(call, node, args, mode);
                } catch (final ENodeCastException e) {
                    // Never catch
                    return null;
                } finally {
                    AcceleoClassLoader.setPreferredClassLoader(old);
                }
            } else {
                return eGetSub(call, node, args, mode);
            }
        } else {
            return null;
        }
    }

    private ENode eGetSub(TemplateCallExpression call, ENode node, ENode[] args, LaunchManager runMode) throws FactoryException, ENodeException {
        // ASSERT (instance != null){
        // Get the type and the value for each argument
        final List argTypesList = new ArrayList(args.length + 1);
        final List argValuesList = new ArrayList(args.length + 1);
        argTypesList.add(null);
        argValuesList.add(null);
        for (final ENode arg : args) {
            argTypesList.add(arg.getTypeClass());
            argValuesList.add(arg.getValue());
        }
        final Class[] argTypes = (Class[]) argTypesList.toArray(new Class[argTypesList.size()]);
        final Object[] argValues = argValuesList.toArray();
        // Get the method
        Method m = null;
        if (mode == EvalJavaService.MODE_DEFAULT && !node.isNull()) {
            argTypes[0] = node.getTypeClass();
            argValues[0] = node.getValue();
            m = eGetMethod(call.getLink(), argTypes, argValues[0]);
        } else if (mode == EvalJavaService.MODE_ENODE) {
            argTypes[0] = ENode.class;
            argValues[0] = node;
            m = eGetMethod(call.getLink(), argTypes, argValues[0]);
        } else if (mode == EvalJavaService.MODE_LIST && node.isList()) {
            try {
                argTypes[0] = ENodeList.class;
                argValues[0] = node.getList();
                m = eGetMethod(call.getLink(), argTypes, argValues[0]);
            } catch (final ENodeCastException e1) {
                m = null;
            }
            if (m == null) {
                try {
                    argTypes[0] = List.class;
                    argValues[0] = node.getList().asList();
                    m = eGetMethod(call.getLink(), argTypes, argValues[0]);
                } catch (final ENodeCastException e2) {
                    m = null;
                }
            }
        }
        if (m == null) {
            // Try to adapt parameters
            if (instance != null) {
                final Method[] methods = getPotentialMethods(call.getLink(), argTypes.length);
                if (methods != null) {
                    for (int i = 0; m == null && i < methods.length; i++) {
                        boolean ok = true;
                        final Class[] parameterTypes = methods[i].getParameterTypes();
                        if (parameterTypes.length > 0) {
                            if (mode == EvalJavaService.MODE_DEFAULT) {
                                final Class parameterType = ENode.getAdapterType(parameterTypes[0]);
                                if (parameterType != null) {
                                    argTypes[0] = parameterType;
                                    try {
                                        argValues[0] = node.getAdapterValue(parameterType);
                                    } catch (final ENodeCastException e) {
                                        ok = false;
                                    }
                                } else {
                                    ok = false;
                                }
                            }
                            if (mode == EvalJavaService.MODE_DEFAULT && ok || mode == EvalJavaService.MODE_ENODE && parameterTypes[0] == ENode.class || mode == EvalJavaService.MODE_LIST
                                    && parameterTypes[0] == ENodeList.class) {
                                for (int j = 1; j < parameterTypes.length; j++) {
                                    final Class parameterType = ENode.getAdapterType(parameterTypes[j]);
                                    if (parameterType != null) {
                                        argTypes[j] = parameterType;
                                        try {
                                            argValues[j] = args[j - 1].getAdapterValue(parameterType);
                                        } catch (final ENodeCastException e) {
                                            ok = false;
                                            break;
                                        }
                                    } else {
                                        ok = false;
                                        break;
                                    }
                                }
                                if (ok) {
                                    m = methods[i];
                                }
                            }
                        }
                    }
                }
            }
        }
        if (m != null) {
            // Display method
            String displayString = m.getName() + " ("; //$NON-NLS-1$
            final Class[] paramTypes = m.getParameterTypes();
            for (int j = 1; j < paramTypes.length; j++) { // The first
                // parameter is
                // ignored
                final Class paramType = paramTypes[j];
                displayString += EvalJavaService.getSimpleName(paramType);
                if (j + 1 < paramTypes.length) {
                    displayString += ", "; //$NON-NLS-1$
                }
            }
            displayString += ')';
            if (m.getReturnType() != null) {
                displayString += ' ' + EvalJavaService.getSimpleName(m.getReturnType());
            }
            displayString += " - " + EvalJavaService.getSimpleName(m.getDeclaringClass()); //$NON-NLS-1$
            // Invoke method on instance
            Object result;
            try {
                if (call.getLink().equals("select") && call.getScript() instanceof AbstractScript && call.countArguments() > 0) { //$NON-NLS-1$
                    ((AbstractScript) call.getScript()).contextPush(IScript.ARGUMENT_POSITION, call.getFirstArgument().getPos());
                }
                try {
                    // void => ""
                    if (m.getReturnType() != null && "void".equals(EvalJavaService.getSimpleName(m.getReturnType()))) { //$NON-NLS-1$
                        m.invoke(instance, argValues);
                        result = ""; //$NON-NLS-1$
                    } else {
                        result = m.invoke(instance, argValues);
                    }
                } catch (final Exception npe) {
                    if (mode == EvalJavaService.MODE_DEFAULT && argValues.length > 0 && argValues[0] == null) {
                        result = null;
                    } else {
                        throw npe;
                    }
                } finally {
                    if (call.getLink().equals("select") && call.getScript() instanceof AbstractScript && call.countArguments() > 0) { //$NON-NLS-1$
                        ((AbstractScript) call.getScript()).contextPop(IScript.ARGUMENT_POSITION);
                    }
                }
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof FactoryException) {
                    throw (FactoryException) e.getTargetException();
                } else if (e.getTargetException() instanceof ENodeException) {
                    throw (ENodeException) e.getTargetException();
                } else {
                    final StringBuffer errorMessage = new StringBuffer("\n"); //$NON-NLS-1$
                    errorMessage.append((e.getTargetException() != null ? e.getTargetException().getClass().getName() + " : " + e.getTargetException().getMessage() : "")); //$NON-NLS-1$ //$NON-NLS-2$
                    throw new ENodeException(
                            AcceleoGenMessages.getString("EvalJavaService.RuntimeException", new Object[] { displayString, errorMessage.toString(), }), call.getPos(), call.getScript(), //$NON-NLS-1$
                            node, true, e.getTargetException());
                }
            } catch (final Exception e) {
                final StringBuffer errorMessage = new StringBuffer("\n"); //$NON-NLS-1$
                errorMessage.append(e.getClass().getName());
                errorMessage.append(" : "); //$NON-NLS-1$
                errorMessage.append(e.getMessage());
                throw new ENodeException(
                        AcceleoGenMessages.getString("EvalJavaService.RuntimeException", new Object[] { displayString, errorMessage.toString(), }), call.getPos(), call.getScript(), node, true, //$NON-NLS-1$
                        e);
            }
            final ENode createTry = ENode.createTry(result, node);
            if (createTry != null) {
                return createTry;
            } else {
                throw new ENodeException(
                        AcceleoGenMessages.getString("EvalJavaService.RuntimeType", new Object[] { displayString, result.getClass().getName(), }), call.getPos(), call.getScript(), node, true); //$NON-NLS-1$
            }
        } else {
            // Service doesn't exist
            return null;
        }
    }

    /**
     * Gets the simple name of the given class.
     * <p>
     * Sample : return 'Object' for 'java.lang.Object'
     * 
     * @param c
     *            is the class
     * @return the name
     */
    public static String getSimpleName(Class c) {
        String name = c.getName();
        final int i = name.lastIndexOf("."); //$NON-NLS-1$
        if (i > -1) {
            name = name.substring(i + 1);
        }
        return name;
    }

    /**
     * Defines if the method belongs to an external service.
     * 
     * @param m
     *            the method
     * @return boolean
     */
    public static boolean isExternalService(Method m) {
        boolean isExternal = false;

        Class[] interfaces = m.getDeclaringClass().getInterfaces();

        if (interfaces != null) {
            for (Class interface1 : interfaces) {
                isExternal = isExternal || EvalJavaService.getSimpleName(interface1).equals("IExternalJavaService"); //$NON-NLS-1$
            }
        }
        return isExternal;
    }

    /**
     * Defines if the method is deprecated.
     * 
     * @param m0
     *            the method
     * @return boolean
     */
    public static boolean isDeprecatedService(Method m0) {
        boolean isDeprecated = false;

        try {
            final Method m2 = m0.getDeclaringClass().getDeclaredMethod("getDeprecatedMethods", null); //$NON-NLS-1$
            if (m2 != null) {
                final Object methods = m2.invoke(m0.getDeclaringClass().newInstance(), null);
                if (methods != null && methods instanceof Method[]) {
                    final Method[] mts = (Method[]) methods;
                    for (final Method m1 : mts) {
                        isDeprecated = isDeprecated || ((m0.getName() + m0.toString()).equals(m1.getName() + m1.toString()));
                    }
                }
            }
        } catch (Exception e) {
            AcceleoEcoreGenPlugin.getDefault().log(e.getMessage(), true);
        }
        return isDeprecated;
    }

    private Method eGetMethod(String name, Class[] argTypes, Object receiver) {
        final Method[] methods = getPotentialMethods(name, argTypes.length);
        if (methods != null) {
            for (Method method : methods) {
                final Class[] parameterTypes = method.getParameterTypes();
                // ASSERT parameterTypes.length > 0
                if (argTypes[0] != EObject.class || parameterTypes[0].isInstance(receiver)) {
                    boolean ok = true;
                    for (int j = 0; j < parameterTypes.length; j++) {
                        if (argTypes[j] == EObject.class && !argTypes[j].isAssignableFrom(parameterTypes[j]) || argTypes[j] != EObject.class && argTypes[j] != parameterTypes[j]) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /* (non-Javadoc) */
    public Object resolveType(Object type, TemplateCallExpression call, int depth) {
        if (instance != null && type != null) { // Remark : type == GENERIC_TYPE
            // => type != null
            final List quickKey = new ArrayList();
            quickKey.add(type);
            quickKey.add(call.toString()); // not getLink() because arguments
            if (quickResolveType.containsKey(quickKey)) {
                return quickResolveType.get(quickKey);
            } else {
                Object resolvedType = null;
                // 'filter' case
                if (instance instanceof ENodeServices || instance instanceof EObjectServices) {
                    if (EvalJavaService.getTypeResolveList().contains(call.getLink()) && call.countArguments() > 0) {
                        final TemplateExpression firstArg = call.getFirstArgument();
                        if (firstArg instanceof TemplateLiteralExpression) {
                            final Object result = ((TemplateLiteralExpression) firstArg).resolveAsEClassifier();
                            if (result != null) {
                                resolvedType = result;
                            }
                        }
                    }
                }
                if (resolvedType == null && (instance instanceof RequestServices || instance instanceof ENodeServices || instance instanceof ContextServices)) {
                    if (EvalJavaService.getTypeBridgeList().contains(call.getLink())) {
                        resolvedType = type;
                    }
                }
                // Use declared methods
                if (resolvedType == null) {
                    final Method[] methods = getPotentialMethods(call.getLink(), call.countArguments() + 1);
                    if (methods != null && methods.length > 0 && methods[0].getDeclaringClass() == instance.getClass()) {
                        resolvedType = IEvalSettings.GENERIC_TYPE;
                    }
                }
                quickResolveType.put(quickKey, resolvedType);
                return resolvedType;
            }
        } else {
            return null;
        }
    }

    private final Map quickResolveType = new HashMap();

    /* (non-Javadoc) */
    public Object[] getCompletionProposals(Object type, int depth) {
        if (instance != null) {
            final Collection result = new TreeSet(new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    Method m0 = (Method) arg0;
                    Method m1 = (Method) arg1;
                    return (m0.getName() + m0.toString()).compareTo(m1.getName() + m1.toString());
                }
            });
            final Method[] methods = instance.getClass().getMethods();
            for (final Method method : methods) {
                if (method.getDeclaringClass() == instance.getClass() && method.getParameterTypes().length >= 1) {
                    result.add(method);
                }
            }
            return result.toArray();
        } else {
            return new Object[] {};
        }
    }

    /* (non-Javadoc) */
    public boolean validateCall(TemplateCallExpression call) {
        return "".equals(call.getPrefix()) || TemplateConstants.LINK_PREFIX_JAVA.equals(call.getPrefix()); //$NON-NLS-1$
    }

}
