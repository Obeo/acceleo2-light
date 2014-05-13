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

package fr.obeo.acceleo.gen.template.eval;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.obeo.acceleo.gen.template.TemplateElement;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Mapping between the template and the text value.
 * 
 * @author www.obeo.fr
 * 
 */
public class TextTemplateElementMapping {

    /**
     * The element that compares positions (Int2).
     */
    private static class InversePosComparator implements Comparator, Serializable {

        private static final long serialVersionUID = 1L;

        public int compare(Object arg0, Object arg1) {
            Int2 pos0 = ((Int2) arg0);
            Int2 pos1 = ((Int2) arg1);
            if (pos0.b() < pos1.b()) {
                return 1;
            } else if (pos0.b() > pos1.b()) {
                return -1;
            } else {
                if (pos0.e() < pos1.e()) {
                    return -1;
                } else if (pos0.e() > pos1.e()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Maps text position to EObject.
     */
    protected Map pos2TemplateElement = new TreeMap(new InversePosComparator());

    /**
     * Maps TemplateElement to text position.
     */
    protected Map template2Positions = new HashMap();

    /**
     * Maps TemplateElement to comment text position.
     */
    protected Map template2CommentPositions = new HashMap();

    /**
     * Maps text position to an TemplateElement declaration. It is used for an
     * open link action.
     */
    protected Map pos2LinkTemplateElement = new TreeMap(new Comparator() {
        public int compare(Object arg0, Object arg1) {
            Int2 pos0 = ((Int2) arg0);
            Int2 pos1 = ((Int2) arg1);
            if (pos0.b() < pos1.b()) {
                return 1;
            } else if (pos0.b() > pos1.b()) {
                return -1;
            } else {
                if (pos0.e() < pos1.e()) {
                    return -1;
                } else if (pos0.e() > pos1.e()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    });

    /**
     * Highlight positions.
     * <p>
     * <li>highlightedPos[HIGHLIGHTED_STATIC_TEXT] is a list of positions for
     * the text which is not in the models.</li>
     * <li>highlightedPos[HIGHLIGHTED_COMMENT] is a list of positions for
     * comments.</li>
     */
    protected List[] highlightedPos = new List[2];

    // Default highlight.
    public static final int HIGHLIGHTED_DEFAULT = -1;

    // Highlight for the text which is not in the models.
    public static final int HIGHLIGHTED_STATIC_TEXT = 0;

    // Highlight for comments.
    public static final int HIGHLIGHTED_COMMENT = 1;

    /**
     * Default template for text mapping.
     */
    protected TemplateElement template;

    /**
     * Current size of the text used for the mapping.
     */
    protected int shift;

    /**
     * Indicates if the changes were validated.
     * <p>
     * It is impossible to change mappings when commit is true.
     */
    protected boolean commit = false;

    /**
     * Constructor.
     * 
     * @param template
     *            is the default template for text mapping
     * @param freeze
     *            is used to freeze the mappings, this helps to improve the
     *            performance
     */
    public TextTemplateElementMapping(TemplateElement template, boolean freeze) {
        this.template = template;
        for (int i = 0; i < highlightedPos.length; i++) {
            if (highlightedPos[i] == null) {
                highlightedPos[i] = new ArrayList();
            }
        }
        if (freeze) {
            commit = true;
        } else {
            reset();
        }
    }

    /**
     * Reset all informations.
     */
    protected void reset() {
        if (!commit) {
            this.shift = 0;
            pos2TemplateElement.clear();
            index2TemplateElement.clear();
            template2Positions.clear();
            template2CommentPositions.clear();
            pos2LinkTemplateElement.clear();
            index2LinkTemplateElement.clear();
            for (int i = 0; i < highlightedPos.length; i++) {
                if (highlightedPos[i] != null) {
                    highlightedPos[i].clear();
                } else {
                    highlightedPos[i] = new ArrayList();
                }
            }
        }
    }

    /**
     * Appends other mappings to current mappings.
     * 
     * @param other
     *            is the other mappings between the model and the text value
     */
    public void from(TextTemplateElementMapping other) {
        if (!commit && other != null) {
            // mapping pos2TemplateElement, template2Pos
            Iterator it = other.pos2TemplateElement.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = shift + ((Int2) entry.getKey()).b();
                int e = shift + ((Int2) entry.getKey()).e();
                TemplateElement template = (TemplateElement) entry.getValue();
                addMapping(template, b, e);
            }
            // mapping template2CommentPositions
            it = other.template2CommentPositions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    int b = shift + pos.b();
                    int e = shift + pos.e();
                    TemplateElement template = (TemplateElement) entry.getKey();
                    addCommentMapping(template, b, e);
                }
            }
            // mapping pos2LinkTemplateElement
            it = other.pos2LinkTemplateElement.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = shift + ((Int2) entry.getKey()).b();
                int e = shift + ((Int2) entry.getKey()).e();
                TemplateElement template = (TemplateElement) entry.getValue();
                pos2LinkTemplateElement.put(new Int2(b, e), template);
            }
            // highlightedPos
            for (int i = 0; i < other.highlightedPos.length; i++) {
                it = other.highlightedPos[i].iterator();
                while (it.hasNext()) {
                    Int2 pos = (Int2) it.next();
                    highlightedPos[i].add(new Int2(shift + pos.b(), shift + pos.e()));
                }
            }
            // shift
            shift += other.shift;
        }
    }

    /**
     * Shifts text positions used for the mapping and puts the default
     * highlight.
     * 
     * @param size
     *            is the size of the shift
     */
    public void shift(int size) {
        shift(size, TextTemplateElementMapping.HIGHLIGHTED_DEFAULT);
    }

    /**
     * Shifts text positions used for the mapping and puts the given highlight.
     * 
     * @param size
     *            is the size of the shift
     * @param highlightedType
     *            is the text highlight
     */
    public void shift(int size, int highlightedType) {
        if (!commit) {
            if (linkTemplateElement != null) {
                Int2 pos = new Int2(shift, shift + size);
                pos2LinkTemplateElement.put(pos, linkTemplateElement);
            }
            if (highlightedType == TextTemplateElementMapping.HIGHLIGHTED_STATIC_TEXT) {
                Int2 pos = new Int2(shift, shift + size);
                highlightedPos[TextTemplateElementMapping.HIGHLIGHTED_STATIC_TEXT].add(pos);
            } else if (highlightedType == TextTemplateElementMapping.HIGHLIGHTED_COMMENT) {
                Int2 pos = new Int2(shift, shift + size);
                highlightedPos[TextTemplateElementMapping.HIGHLIGHTED_COMMENT].add(pos);
                addCommentMapping(template, pos.b(), pos.e());
            }
            shift += size;
        }
    }

    /**
     * Adds a mapping between the template and the text begins at the specified
     * begin and extends to the character at index end - 1.
     * 
     * @param template
     *            is the template element
     * @param begin
     *            is the beginning index, inclusive
     * @param end
     *            is the ending index, exclusive
     */
    protected void addMapping(TemplateElement template, int begin, int end) {
        if (begin > -1 && end > -1) {
            // Mapping pos2TemplateElement
            Int2 newPos = new Int2(begin, end);
            if (!pos2TemplateElement.containsKey(newPos)) {
                pos2TemplateElement.put(newPos, template);
            }
            // Mapping template2Pos
            List templatePositions = (List) template2Positions.get(template);
            if (templatePositions == null) {
                templatePositions = new ArrayList();
                template2Positions.put(template, templatePositions);
            }
            add(templatePositions, begin, end);
        }
    }

    /**
     * Adds a comment mapping between the template and the text begins at the
     * specified begin and extends to the character at index end - 1.
     * 
     * @param template
     *            is the template
     * @param begin
     *            is the beginning index, inclusive
     * @param end
     *            is the ending index, exclusive
     */
    protected void addCommentMapping(TemplateElement template, int begin, int end) {
        if (begin > -1 && end > -1) {
            List commentPositions = (List) template2CommentPositions.get(template);
            if (commentPositions == null) {
                commentPositions = new ArrayList();
                template2CommentPositions.put(template, commentPositions);
            }
            add(commentPositions, begin, end);
        }
    }

    /**
     * Amalgamates the new position and the other positions.
     * 
     * @param positions
     *            is the list of positions
     * @param begin
     *            is the beginning index of the new position, inclusive
     * @param end
     *            is the ending index of the new position, exclusive
     */
    private void add(List positions, int begin, int end) {
        boolean insert = false;
        Iterator it = positions.iterator();
        while (!insert && it.hasNext()) {
            Int2 pos = (Int2) it.next();
            if (begin <= pos.e() && begin >= pos.b()) {
                if (end > pos.e()) {
                    positions.remove(pos);
                    add(positions, pos.b(), end);
                }
                insert = true;
            } else if (end >= pos.b() && end <= pos.e()) {
                if (begin < pos.b()) {
                    positions.remove(pos);
                    add(positions, begin, pos.e());
                }
                insert = true;
            } else if (begin < pos.b() && end > pos.e()) {
                positions.remove(pos);
                add(positions, begin, end);
                insert = true;
            }
        }
        if (!insert) {
            positions.add(new Int2(begin, end));
        }
    }

    /**
     * Marks the begin of an open link action.
     * 
     * @param linkTemplateElement
     *            is the linked template
     */
    public void linkBegin(TemplateElement linkTemplateElement) {
        this.linkTemplateElement = linkTemplateElement;
    }

    /**
     * Marks the end of the current open link action.
     */
    public void linkEnd() {
        this.linkTemplateElement = null;
    }

    /**
     * Open link result for the next added text
     */
    protected TemplateElement linkTemplateElement = null;

    /**
     * Validate the changes.
     * <p>
     * It is impossible to change mappings now.
     */
    protected void commit() {
        if (!commit) {
            commit = true;
            addMapping(template, 0, shift);
        }
    }

    /**
     * Returns the template at the given index.
     * 
     * @param index
     *            is the position in the text
     * @return the template at the index.
     */
    public TemplateElement index2TemplateElement(int index) {
        TemplateElement template = (TemplateElement) index2TemplateElement.get(new Integer(index));
        if (template == null) {
            if (!commit) {
                commit();
            }
            Iterator it = pos2TemplateElement.entrySet().iterator();
            while (template == null && it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = ((Int2) entry.getKey()).b();
                if (index >= b) {
                    int e = ((Int2) entry.getKey()).e();
                    if (index < e) {
                        template = (TemplateElement) entry.getValue();
                    }
                }
            }
            index2TemplateElement.put(new Integer(index), template);
        }
        return template;
    }

    private Map index2TemplateElement = new HashMap();

    /**
     * Gets a serializable template that maps the ranges of the text and the
     * URIs of the templates.
     * 
     * @return a serializable map
     * @deprecated
     */
    @Deprecated
    public Map position2uriSerializableMap() {
        Map result = new TreeMap(new InversePosComparator());
        if (!commit) {
            commit();
        }
        Iterator it = pos2TemplateElement.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Int2 pos = (Int2) entry.getKey();
            TemplateElement template = (TemplateElement) entry.getValue();
            String uriFragment = template.getURIFragment();
            result.put(pos, uriFragment);
        }
        return result;
    }

    /**
     * Returns the positions of the given template.
     * 
     * @param template
     *            is the template
     * @return the positions of the template
     */
    public Int2[] template2Positions(TemplateElement template) {
        if (template == null) {
            return new Int2[] {};
        }
        if (!commit) {
            commit();
        }
        List positions = (List) template2Positions.get(template);
        if (positions != null) {
            return (Int2[]) positions.toArray(new Int2[positions.size()]);
        } else {
            return new Int2[] {};
        }
    }

    /**
     * Gets a serializable template that maps the URIs of the templates and the
     * ranges of the text.
     * 
     * @return a serializable map
     * @deprecated
     */
    @Deprecated
    public Map uri2positionsSerializableMap() {
        Map result = new TreeMap(new StringComparator());
        if (!commit) {
            commit();
        }
        Iterator entries = template2Positions.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            TemplateElement template = (TemplateElement) entry.getKey();
            String uriFragment = template.getURIFragment();
            Set positions = new TreeSet(new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    Int2 pos0 = ((Int2) arg0);
                    Int2 pos1 = ((Int2) arg1);
                    if (pos0.b() < pos1.b()) {
                        return -1;
                    } else if (pos0.b() > pos1.b()) {
                        return 1;
                    } else {
                        if (pos0.e() < pos1.e()) {
                            return 1;
                        } else if (pos0.e() > pos1.e()) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                }
            });
            positions.addAll((List) entry.getValue());
            result.put(uriFragment, positions.toArray(new Int2[positions.size()]));
        }
        return result;
    }

    /**
     * Returns the first comment position of the given template.
     * 
     * @param template
     *            is the template
     * @param limits
     *            delimits the part of the text where the comment can be
     *            searched
     * @return the first comment position of the template
     */
    public Int2 template2CommentPositionIn(TemplateElement template, Int2 limits) {
        if (template == null) {
            return Int2.NOT_FOUND;
        }
        if (!commit) {
            commit();
        }
        List positions = (List) template2CommentPositions.get(template);
        if (positions != null) {
            Iterator it = positions.iterator();
            while (it.hasNext()) {
                Int2 pos = (Int2) it.next();
                if (pos.b() >= limits.b() && pos.e() <= limits.e()) {
                    return pos;
                }
            }
        }
        return Int2.NOT_FOUND;
    }

    /**
     * Returns the linked template at the given index. It is used to make an
     * open link action.
     * 
     * @param index
     *            is the position in the text
     * @return the linked template at the index
     */
    public TemplateElement index2LinkTemplateElement(int index) {
        TemplateElement template = (TemplateElement) index2LinkTemplateElement.get(new Integer(index));
        if (template == null) {
            if (!commit) {
                commit();
            }
            Iterator it = pos2LinkTemplateElement.entrySet().iterator();
            while (template == null && it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = ((Int2) entry.getKey()).b();
                if (index >= b) {
                    int e = ((Int2) entry.getKey()).e();
                    if (index < e) {
                        template = (TemplateElement) entry.getValue();
                    }
                }
            }
            index2LinkTemplateElement.put(new Integer(index), template);
        }
        return template;
    }

    private Map index2LinkTemplateElement = new HashMap();

    /**
     * Returns all positions for the given highlight.
     * 
     * @param highlightedType
     *            is the text highlight
     * @return all positions for the given highlight
     */
    public Int2[] getHighlightedPos(int highlightedType) {
        if (highlightedType == TextTemplateElementMapping.HIGHLIGHTED_STATIC_TEXT) {
            return (Int2[]) highlightedPos[TextTemplateElementMapping.HIGHLIGHTED_STATIC_TEXT].toArray(new Int2[] {});
        } else if (highlightedType == TextTemplateElementMapping.HIGHLIGHTED_COMMENT) {
            return (Int2[]) highlightedPos[TextTemplateElementMapping.HIGHLIGHTED_COMMENT].toArray(new Int2[] {});
        } else {
            return new Int2[] {};
        }
    }

    /**
     * Moves the bounds of the errors into the given range
     * 
     * @param range
     *            are the new bounds
     */
    public void range(Int2 range) {
        if (!commit) {
            shift = range.e() - range.b();
            // mapping pos2TemplateElement
            Iterator it = pos2TemplateElement.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Int2 pos = (Int2) entry.getKey();
                Int2 copy = new Int2(pos.b(), pos.e());
                copy.range(range);
                if (copy.b() == -1 || (!copy.equals(pos) && pos2TemplateElement.containsKey(copy))) {
                    it.remove();
                } else {
                    pos.range(range);
                }
            }
            // mapping template2Positions
            it = template2Positions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    pos.range(range);
                    if (pos.b() == -1) {
                        positions.remove();
                    }
                }
            }
            // mapping template2CommentPositions
            it = template2CommentPositions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    pos.range(range);
                    if (pos.b() == -1) {
                        positions.remove();
                    }
                }
            }
            // mapping pos2LinkTemplateElement
            it = pos2LinkTemplateElement.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Int2 pos = (Int2) entry.getKey();
                Int2 copy = new Int2(pos.b(), pos.e());
                copy.range(range);
                if (copy.b() == -1 || (!copy.equals(pos) && pos2LinkTemplateElement.containsKey(copy))) {
                    it.remove();
                } else {
                    pos.range(range);
                }
            }
            // highlightedPos
            for (List highlightedPo : highlightedPos) {
                it = highlightedPo.iterator();
                while (it.hasNext()) {
                    Int2 pos = (Int2) it.next();
                    pos.range(range);
                    if (pos.b() == -1) {
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * Applies the indent strategy to the positions (each line adds one
     * character).
     * 
     * @param lines
     *            are the positions of the lines
     */
    public void indent(Int2[] lines) {
        if (!commit) {
            shift += lines.length;
            // mapping pos2TemplateElement
            Iterator it = pos2TemplateElement.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ((Int2) entry.getKey()).indent(lines);
            }
            // mapping template2Positions
            it = template2Positions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    pos.indent(lines);
                }
            }
            // mapping template2CommentPositions
            it = template2CommentPositions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    pos.indent(lines);
                }
            }
            // mapping pos2LinkTemplateElement
            it = pos2LinkTemplateElement.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ((Int2) entry.getKey()).indent(lines);
            }
            // highlightedPos
            for (List highlightedPo : highlightedPos) {
                it = highlightedPo.iterator();
                while (it.hasNext()) {
                    Int2 pos = (Int2) it.next();
                    pos.indent(lines);
                }
            }
        }
    }

}
