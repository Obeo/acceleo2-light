/*******************************************************************************
 * Copyright (c) 2007, 2008, 2009 THALES GLOBAL SERVICES.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Obeo - initial API and implementation
 *******************************************************************************/

package org.eclipse.sirius.query.legacy.business.internal.interpreter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.sirius.common.tools.api.contentassist.ContentContext;
import org.eclipse.sirius.common.tools.api.contentassist.ContentInstanceContext;
import org.eclipse.sirius.common.tools.api.contentassist.ContentProposal;
import org.eclipse.sirius.query.legacy.ecore.tools.ETools;
import org.eclipse.sirius.query.legacy.gen.template.TemplateConstants;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.EvalJavaService;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ContextServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ENodeServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.EObjectServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.PropertiesServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.RequestServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.ResourceServices;
import org.eclipse.sirius.query.legacy.gen.template.scripts.imports.services.StringServices;
import org.eclipse.sirius.query.legacy.tools.strings.Int2;
import org.eclipse.sirius.query.legacy.tools.strings.TextSearch;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * This utility class evaluates the proposals of the Content Assist.
 * 
 * @author ggebhart
 * 
 */
public class AcceleoCompletionEntry {

    /**
     * proposal contents key of the tools map.
     * 
     */
    public static final Integer PROPOSAL = Integer.valueOf(0);

    /**
     * display contents key of the tools map.
     * 
     */
    public static final Integer DISPLAY = Integer.valueOf(1);

    /**
     * information contents key of the tools map.
     * 
     */
    public static final Integer INFORMATION = Integer.valueOf(2);

    /**
     * The activation characters for completion proposal.
     */
    protected static final char[] ACTIVATION_CHARACTERS = new char[] { ' ', '\t', '\n', '|', '&', '=', '(', ',', '-', '+', '*', '/', '!', '{', '[' };

    /**
     * The parenthesis characters.
     */
    protected static final char[] PARENTHESIS = new char[] { '(', ')' };

    /**
     * The brackets characters.
     */
    protected static final char[] BRACKETS = new char[] { '[', ']' };

    /**
     * The comma and space characters.
     */
    protected static final String COMMA = ", ";

    /**
     * The current context element.
     */
    protected EObject element;

    /**
     * Semantic element of the mapping or tools description.
     */
    private String metaClass = "";

    private String qualifier = "";

    private EPackage.Registry runtimeTypeRegistry = EPackage.Registry.INSTANCE;

    /**
     * Constructor.
     * 
     */
    public AcceleoCompletionEntry() {

    }

    /**
     * Evaluates the content proposals for a given expression and returns the
     * result as an List.
     * 
     * @param context
     *            the context.
     * @return the content proposals list.
     */
    public List<ContentProposal> computeProposals(final ContentContext context) {

        final Collection<String> targetTypes = context.getInterpreterContext().getTargetTypes();
        if (!context.getInterpreterContext().requiresTargetType()) {
            metaClass = "ecore.EObject";
        } else if (targetTypes.size() == 1) {
            metaClass = targetTypes.iterator().next();
        }
        element = context.getInterpreterContext().getElement();
        final EClassifier[] proposals = computeMetamodelTypesProposals(getElementClassifierList(context.getInterpreterContext().getElement(), context.getContents()), context.getContents(), false);
        final List<ContentProposal> contentsMap = computeResolvedTypesProposals(proposals, context.getContents(), context.getPosition());

        // Delete duplicated proposals
        return deleteDuplicatedProposals(contentsMap);
    }

    /**
     * Evaluates the content proposals for a given expression and returns the
     * result as an List.
     * 
     * @param context
     *            the context.
     * @return the content proposals list.
     */
    public List<ContentProposal> computeProposals(final ContentInstanceContext context) {

        final EClassifier[] proposals = computeMetamodelTypesProposals(getElementClassifierList(context.getCurrentSelected(), context.getTextSoFar()), context.getTextSoFar(), false);
        final List<ContentProposal> contentsMap = computeResolvedTypesProposals(proposals, context.getTextSoFar(), context.getCursorPosition());

        // Delete duplicated proposals
        return deleteDuplicatedProposals(contentsMap);
    }

    /**
     * Returns the available ePackages according to the given EObject.
     * 
     * @param eObject
     *            the currently selected {@link EObject}
     * @param textSoFar
     *            the typed expression
     * 
     * @return the available ePackages according to the given {@link EClass}
     */
    private List<EClassifier> getElementClassifierList(EObject eObject, String textSoFar) {
        List<EClassifier> eClassifiers = new ArrayList<EClassifier>();
        // If selection is not null and if expression is focusing on currently
        // selected element, getting all EClassifiers extended by this
        // element
        if (eObject != null && textSoFar.replace(" ", "").equals("<%%>") || textSoFar.replace(" ", "").equals("<%self%>")) {
            eClassifiers.addAll(getElementClassifierList(eObject.eClass()));
        } else {
            // Otherwise, get package registry

            final Collection<Object> values = new ArrayList<Object>(runtimeTypeRegistry.values());
            for (final Object value : values) {
                try {
                    if (value instanceof EPackage) {
                        eClassifiers.addAll(((EPackage) value).getEClassifiers());
                    } else if (value instanceof EPackage.Descriptor) {
                        eClassifiers.addAll((((EPackage.Descriptor) value).getEPackage()).getEClassifiers());
                    }
                    // CHECKSTYLE:OFF
                } catch (Throwable e) {
                    /*
                     * anything might happen here depending on the other Eclipse
                     * tools, and we've seen many time tools (like XText for
                     * instance) breaking all the others .
                     */
                    // CHECKSTYLE:ON
                }
            }
        }
        return eClassifiers;
    }

    /**
     * Returns the available {@link EClassifier}s according to the given
     * {@link EClass}.
     * 
     * @param eClass
     *            the currently selected {@link EObject}'s {@link EClass}
     * 
     * @return the available {@link EClassifier}s according to the given
     *         {@link EClass}
     */
    private Set<EClass> getElementClassifierList(EClass eClass) {

        final Set<EClass> eClassifiers = new LinkedHashSet<EClass>();
        // Adding the current EObject's EClass's package
        eClassifiers.add(eClass);
        // Also adding the EPackages holding the super classes of the current
        // EObject
        for (EClass superClass : eClass.getESuperTypes()) {
            if (!eClassifiers.contains(superClass)) {
                eClassifiers.addAll(getElementClassifierList(superClass));
            }
        }
        return eClassifiers;
    }

    /**
     * Removes the duplicated proposals.
     * 
     * @param matches
     *            are the initial proposals
     * @return the valid proposals
     */
    private List<ContentProposal> deleteDuplicatedProposals(final List<ContentProposal> contents) {

        final List<ContentProposal> resultProposals = new ArrayList<ContentProposal>(contents.size());
        final Iterator<ContentProposal> it = contents.iterator();
        while (it.hasNext()) {
            final ContentProposal entry = it.next();
            if (!resultProposals.contains(entry)) {
                resultProposals.add(entry);
            }
        }
        Collections.sort(resultProposals);
        return resultProposals;
    }

    /**
     * Computes valid metamodel types proposals.
     * 
     * @param classes
     *            the available {@link EClass}es
     * @param start
     *            is the start of the proposal
     * @param offset
     *            is the offset within the text for which completions should be
     *            computed
     * @param classOnly
     *            indicates that only the classes are kept
     * @return the proposals
     */
    private EClassifier[] computeMetamodelTypesProposals(final List<EClassifier> classes, final String start, final boolean classOnly) {

        final Collection<EClassifier> classifiers = new TreeSet<EClassifier>(new Comparator<EClassifier>() {
            public int compare(final EClassifier c0, final EClassifier c1) {
                return ETools.getEClassifierShortPath(c0).compareTo(ETools.getEClassifierShortPath(c1));
            }
        });
        Iterables.addAll(classifiers, classes);

        /* remove classifier with null name */
        final Iterator<EClassifier> it = classifiers.iterator();
        while (it.hasNext()) {
            final EClassifier eClassifier = it.next();
            if (eClassifier.getName() == null) {
                it.remove();
            }
        }

        /*
         * Add EObject as a default type
         */
        classifiers.add(EcorePackage.eINSTANCE.getEObject());

        final EClassifier[] proposals = classifiers.toArray(new EClassifier[classifiers.size()]);
        final String[] replacementStrings = new String[proposals.length * 2];
        final String[] displayStrings = new String[proposals.length * 2];
        final String[] informationStrings = new String[proposals.length * 2];
        final int[] cursorPositions = new int[proposals.length * 2];

        for (int i = 0; i < proposals.length; i++) {
            // Name only
            final EClassifier currentProposal = proposals[i];
            replacementStrings[i] = currentProposal.getName();
            cursorPositions[i] = replacementStrings[i].length();
            displayStrings[i] = currentProposal.getName();
            informationStrings[i] = AcceleoCompletionEntry.getDescription(currentProposal);

            // Short path
            final int j = proposals.length + i;
            replacementStrings[j] = ETools.getEClassifierShortPath(currentProposal);
            cursorPositions[j] = replacementStrings[j].length();
            displayStrings[j] = replacementStrings[j];
            informationStrings[j] = informationStrings[i];

        }
        return proposals;
    }

    /**
     * Computes valid proposals for the objects detected.
     * 
     * @param proposals
     *            are the objects detected for the current resolved type
     * @param text
     *            is the text
     * @param offset
     *            is the offset within the text for which completions should be
     *            computed
     * @return the proposals
     */
    private List<ContentProposal> computeResolvedTypesProposals(final Object[] proposal, final String text, final int offset) {
        final String endStart = AcceleoCompletionEntry.extractEndStart(text, offset);
        final Object[] proposals = getAllContents(proposal);

        final String[] replacementStrings = new String[proposals.length];
        final String[] displayStrings = new String[proposals.length];
        final String[] informationStrings = new String[proposals.length];
        final int[] cursorPositions = new int[proposals.length];

        for (int i = 0; i < replacementStrings.length; i++) {
            replacementStrings[i] = null;
            if (proposals[i] instanceof Method) {

                AcceleoCompletionEntry.computeForMethod(proposals, replacementStrings, displayStrings, informationStrings, cursorPositions, i);

            } else if (proposals[i] instanceof EOperation) {

                AcceleoCompletionEntry.computeForEOperation(proposals, replacementStrings, displayStrings, informationStrings, cursorPositions, i);

            } else if (proposals[i] instanceof EAttribute) {

                AcceleoCompletionEntry.computeForEAttribute(proposals, replacementStrings, displayStrings, informationStrings, cursorPositions, i);

            } else if (proposals[i] instanceof EReference) {

                AcceleoCompletionEntry.computeForEReference(proposals, replacementStrings, displayStrings, informationStrings, cursorPositions, i);

            } else {
                replacementStrings[i] = proposals[i].toString();
                displayStrings[i] = proposals[i].toString();
                informationStrings[i] = proposals[i].toString();
                cursorPositions[i] = replacementStrings[i].length();

            }
            if (endStart.startsWith(TemplateConstants.FEATURE_BEGIN) && replacementStrings[i] != null) {
                // For '<%link' proposals
                replacementStrings[i] = TemplateConstants.FEATURE_BEGIN + replacementStrings[i];
                cursorPositions[i] = cursorPositions[i] + TemplateConstants.FEATURE_BEGIN.length();
            }
        }
        return computeValidProposals(proposals, replacementStrings, displayStrings, informationStrings, cursorPositions, endStart, offset);
    }

    private static void computeForMethod(final Object[] proposals, final String[] replacementStrings, final String[] displayStrings, final String[] informationStrings, final int[] cursorPositions,
            final int i) {
        final Method method = (Method) proposals[i];
        StringBuilder replacementString = new StringBuilder(method.getName());
        replacementString.append('(');
        cursorPositions[i] = replacementString.length();
        StringBuilder displayString = new StringBuilder(method.getName());
        displayString.append(" ("); //$NON-NLS-1$
        final Class<?>[] paramTypes = method.getParameterTypes();
        for (int j = 1; j < paramTypes.length; j++) { // The first
            // parameter is
            // ignored
            final Class<?> paramType = paramTypes[j];
            replacementString.append(EvalJavaService.getSimpleName(paramType)); // add
            // args
            // to
            // proposals
            // definition
            displayString.append(EvalJavaService.getSimpleName(paramType));
            if (j + 1 < paramTypes.length) {
                replacementString.append(AcceleoCompletionEntry.COMMA);
                displayString.append(AcceleoCompletionEntry.COMMA);
            }
        }
        replacementString.append(')');
        displayString.append(')');
        if (method.getReturnType() != null) {
            displayString.append(' ');
            displayString.append(EvalJavaService.getSimpleName(method.getReturnType()));
        }
        displayString.append(" - ");
        displayString.append(EvalJavaService.getSimpleName(method.getDeclaringClass())); //$NON-NLS-1$
        replacementStrings[i] = replacementString.toString();
        displayStrings[i] = displayString.toString();
        informationStrings[i] = method.toString();
    }

    private static void computeForEAttribute(final Object[] proposals, final String[] replacementStrings, final String[] displayStrings, final String[] informationStrings,
            final int[] cursorPositions, final int i) {
        final EAttribute attribute = (EAttribute) proposals[i];
        replacementStrings[i] = attribute.getName();
        if (replacementStrings[i] != null) {
            cursorPositions[i] = replacementStrings[i].length();
        } else {
            cursorPositions[i] = 0;
        }
        String displayString = attribute.getName();
        if (attribute.getEType() != null) {
            displayString += " : " + attribute.getEType().getName(); //$NON-NLS-1$
        }
        displayStrings[i] = displayString;
        informationStrings[i] = displayString;
    }

    private static void computeForEReference(final Object[] proposals, final String[] replacementStrings, final String[] displayStrings, final String[] informationStrings,
            final int[] cursorPositions, final int i) {
        final EReference reference = (EReference) proposals[i];

        replacementStrings[i] = reference.getName();
        if (replacementStrings[i] != null) {
            cursorPositions[i] = replacementStrings[i].length();
        } else {
            cursorPositions[i] = 0;
        }
        String displayString = reference.getName();
        if (reference.getEType() != null) {

            final int lowerBound = reference.getLowerBound();
            final int upperBound = reference.getUpperBound();
            String bounds;
            if (lowerBound == upperBound) {
                bounds = String.valueOf(lowerBound);
            } else {
                bounds = lowerBound + (upperBound != -1 ? ".." + upperBound : "..*"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (reference.isContainment()) {
                bounds = '[' + bounds + ']';
            } else {
                bounds = '{' + bounds + '}';
            }
            displayString += " : " + reference.getEType().getName() + ' ' + bounds;

            displayStrings[i] = displayString;
            informationStrings[i] = displayString;
        }
    }

    private static void computeForEOperation(final Object[] proposals, final String[] replacementStrings, final String[] displayStrings, final String[] informationStrings,
            final int[] cursorPositions, final int i) {
        final EOperation method = (EOperation) proposals[i];
        StringBuilder replacementString;

        replacementString = new StringBuilder(method.getName());
        replacementString.append('(');
        cursorPositions[i] = replacementString.length();
        StringBuilder displayString = new StringBuilder(method.getName());
        displayString.append(" ("); //$NON-NLS-1$
        final EList<ETypeParameter> paramTypes = method.getETypeParameters();

        for (int j = 1; j < paramTypes.size(); j++) { // The first
            // parameter is
            // ignored
            final ETypeParameter paramType = paramTypes.get(j);
            displayString.append(paramType.getName());
            if (j + 1 < paramTypes.size()) {
                replacementString.append(AcceleoCompletionEntry.COMMA);
                displayString.append(AcceleoCompletionEntry.COMMA);
            }
        }

        replacementString.append(')');
        displayString.append(')');
        replacementStrings[i] = replacementString.toString();
        displayStrings[i] = displayString.toString();
        informationStrings[i] = method.toString();
    }

    /**
     * Computes valid proposals (CompletionProposal).
     * 
     * @param objects
     *            are the optional objects
     * @param replacementStrings
     *            are the replacement strings
     * @param displayStrings
     *            are the display strings
     * @param informationStrings
     *            are the information strings
     * @param cursorPositions
     *            are the cursor positions after the replacement
     * @param start
     *            is the start of the proposal
     * @param offset
     *            is the offset within the text for which completions should be
     *            computed
     * @param images
     *            are the icons
     * @return the proposals (CompletionProposal)
     */
    private List<ContentProposal> computeValidProposals(final Object[] objects, final String[] replacementStrings, final String[] displayStrings, final String[] informationStrings,
            final int[] cursorPositions, final String start, final int offset) {
        final ArrayList<ContentProposal> proposals = new ArrayList<ContentProposal>();
        final String startToLowerCase = start.toLowerCase();

        for (int i = 0; i < replacementStrings.length; i++) {
            final String replacementString = replacementStrings[i];
            if (replacementString != null) {
                final String displayString = displayStrings[i];
                final String informationString = informationStrings[i];
                final String replacementStringL = replacementString.toLowerCase();
                if (start.length() == 0
                        || replacementStringL.startsWith(startToLowerCase)
                        || replacementStringL.indexOf(TemplateConstants.LINK_PREFIX_SEPARATOR
                                + (startToLowerCase.startsWith(TemplateConstants.FEATURE_BEGIN) ? startToLowerCase.substring(TemplateConstants.FEATURE_BEGIN.length()) : startToLowerCase)) > -1) {

                    proposals.add(new ContentProposal(replacementString.replaceFirst(TemplateConstants.FEATURE_BEGIN, ""), displayString, informationString));
                }
            }
        }

        proposals.trimToSize();

        return proposals;
    }

    /**
     * Computes proposals for the objects detected.
     * 
     * @param proposals
     *            are the objects detected for the current resolved type
     * @return the proposals
     */
    private Object[] getAllContents(final Object[] proposals) {
        Object[] newProposals = null;
        Method[] allMethodsTab = null;
        List<Object> contents = Lists.newArrayList();

        addModelElements(proposals, contents);

        /* add all acceleo and EMF services */
        allMethodsTab = EObjectServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);
        }

        allMethodsTab = ContextServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);
        }

        allMethodsTab = ENodeServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);
        }

        allMethodsTab = PropertiesServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);
        }

        allMethodsTab = RequestServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);
        }

        allMethodsTab = StringServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);

        }

        allMethodsTab = ResourceServices.class.getDeclaredMethods();
        newProposals = getPublicMethod(allMethodsTab);
        for (Object newProposal : newProposals) {
            contents.add(newProposal);
        }
        newProposals = contents.toArray();
        return newProposals;
    }

    /**
     * Add model elements (structural features) to completion.
     * 
     * @param proposals
     *            the initial proposals
     * @param contents
     *            the modified proposals
     */
    protected void addModelElements(final Object[] proposals, List<Object> contents) {
        EClassifier classifier = null;
        EClass current = null;
        for (Object proposal2 : proposals) {
            classifier = (EClassifier) proposal2;
            if (classifier instanceof EClass) {
                current = (EClass) classifier;
                if (metaClass != null && classifier.getName().equals(metaClass) && (qualifier == null || current.getEPackage().getNsURI().contains(qualifier))) {
                    contents.clear();
                    contents.addAll(current.getEAllAttributes());
                    contents.addAll(current.getEAllReferences());
                    break;

                } else {
                    contents.addAll(current.getEAllAttributes());
                    contents.addAll(current.getEAllReferences());
                }
            }
        }
    }

    /**
     * Computes public methods
     * 
     * 
     * @param tab
     *            array of API's methods
     * @return array of public method contained in a API
     */
    private Method[] getPublicMethod(final Method[] tab) {
        final ArrayList<Method> publicMethods = new ArrayList<Method>();

        for (Method element2 : tab) {
            if (element2.getModifiers() == Modifier.PUBLIC) {
                publicMethods.add(element2);
            }
        }
        publicMethods.trimToSize();

        final Method[] methods = new Method[publicMethods.size()];
        for (int i = 0; i < methods.length; i++) {
            methods[i] = publicMethods.get(i);
        }
        return methods;
    }

    /**
     * Gets the completion's start, after the last dot.
     * <p>
     * Sample : "a.b.c.DDD" -> "DDD"
     * 
     * @param text
     *            is the text
     * @param offset
     *            is the offset within the text for which completions should be
     *            computed
     * @return the completion's start, after the last dot
     */
    private static String extractEndStart(final String text, final int offset) {
        final String extractStart = AcceleoCompletionEntry.extractStart(text, offset);
        final Int2 iSep = TextSearch.getDefaultSearch().lastIndexOf(extractStart, TemplateConstants.CALL_SEP, TemplateConstants.SPEC, TemplateConstants.INHIBS_EXPRESSION);
        final Int2 iBracket = AcceleoCompletionEntry.getLastIndexOfOpenBracket(extractStart, iSep.e() == -1 ? 0 : iSep.e());

        String endStart;

        if (iBracket.e() > -1) {
            endStart = extractStart.substring(iBracket.e());
        } else if (iSep.e() > -1) {
            endStart = extractStart.substring(iSep.e());
        } else {
            endStart = extractStart;
        }
        return endStart;
    }

    private static Int2 getLastIndexOfOpenBracket(final String extractStart, final int start) {
        final Int2 end = TextSearch.getDefaultSearch().lastIndexOf(extractStart, TemplateConstants.BRACKETS[1], start, TemplateConstants.SPEC, new String[][] { TemplateConstants.LITERAL });
        final Int2 begin = TextSearch.getDefaultSearch().lastIndexOf(extractStart, TemplateConstants.BRACKETS[0], end.e() == -1 ? start : end.e(), TemplateConstants.SPEC,
                new String[][] { TemplateConstants.LITERAL });
        if (begin.b() > -1) {
            return begin;
        }
        return Int2.NOT_FOUND;
    }

    /**
     * Gets the completion's start.
     * <p>
     * Sample : "a.b.c.DDD" -> "a.b.c.DDD"
     * 
     * @param text
     *            is the text
     * @param originalOffset
     *            is the offset within the text for which completions should be
     *            computed
     * @return the completion's start
     */
    // CHECKSTYLE:OFF this method comes from acceleo
    private static String extractStart(final String originalText, final int originalOffset) {
        if (originalOffset <= originalText.length()) {
            final String prefix = "  "; //$NON-NLS-1$
            final String text = prefix + originalText;
            final int offset = prefix.length() + originalOffset;
            int i = offset;
            while (i >= 2) {
                char c = text.charAt(i - 1);
                if (TemplateConstants.FEATURE_BEGIN.equals(text.substring(i - TemplateConstants.FEATURE_BEGIN.length(), i))) {
                    return TemplateConstants.FEATURE_BEGIN + text.substring(i, offset);
                } else if (c == AcceleoCompletionEntry.PARENTHESIS[1]) { // (
                                                                         // ...
                                                                         // ) is
                                                                         // a
                                                                         // block
                                                                         // to
                    // ignore
                    int level = 0;
                    do {
                        i--;
                        c = text.charAt(i - 1);
                        if (c == '"') {
                            do {
                                i--;
                                c = text.charAt(i - 1);
                                if (c == '"' && (i < 2 || text.charAt(i - 2) != '\\')) {
                                    break;
                                }
                            } while (i > 1);
                        } else if (c == AcceleoCompletionEntry.PARENTHESIS[1]) {
                            level++;
                        } else if (c == AcceleoCompletionEntry.PARENTHESIS[0]) {
                            if (level == 0) {
                                break;
                            } else {
                                level--;
                            }
                        }
                    } while (i > 1);
                } else if (c == AcceleoCompletionEntry.BRACKETS[1]) { // [ ... ]
                                                                      // is a
                                                                      // block
                                                                      // to
                    int level = 0;
                    do {
                        i--;
                        c = text.charAt(i - 1);
                        if (c == '"') {
                            do {
                                i--;
                                c = text.charAt(i - 1);
                                if (c == '"' && (i < 2 || text.charAt(i - 2) != '\\')) {
                                    break;
                                }
                            } while (i > 1);
                        } else if (c == AcceleoCompletionEntry.BRACKETS[1]) {
                            level++;
                        } else if (c == AcceleoCompletionEntry.BRACKETS[0]) {
                            if (level == 0) {
                                break;
                            } else {
                                level--;
                            }
                        }
                    } while (i > 1);
                } else {
                    for (char element2 : AcceleoCompletionEntry.ACTIVATION_CHARACTERS) {
                        if (c == element2) {
                            return text.substring(i, offset);
                        }
                    }
                }
                i--;
            }
        }
        return ""; //$NON-NLS-1$
        // CHECKSTYLE:ON
    }

    private static String getDescription(final EClassifier eClassifier) {
        String desc = null;
        // Information come from GenModel.
        // As the metamodel object of Acceleo loose this information, it doesn't
        // do anything. Need to be fix.
        final EAnnotation eAnno = eClassifier.getEAnnotation("http://www.eclipse.org/emf/2002/GenModel");
        if (eAnno != null) {
            desc = eAnno.getDetails().get("documentation"); //$NON-NLS-1$
        }
        if (desc == null) {
            desc = eClassifier.getName();
        }
        return desc;
    }
}
