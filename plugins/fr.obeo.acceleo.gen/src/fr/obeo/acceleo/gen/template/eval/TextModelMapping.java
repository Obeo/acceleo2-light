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

import org.eclipse.emf.ecore.EObject;

import fr.obeo.acceleo.ecore.tools.ETools;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Mapping between the model and the text value.
 * 
 * @author www.obeo.fr
 * 
 */
public class TextModelMapping {

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
     * The element that compares positions (Int2).
     */
    private static class PosComparator implements Comparator {
        /**
         * {@inheritDoc}
         */
        public int compare(Object arg0, Object arg1) {
            Int2 pos0 = ((Int2) arg0);
            Int2 pos1 = ((Int2) arg1);
            int res;
            if (pos0.b() < pos1.b()) {
                res = -1;
            } else if (pos0.b() > pos1.b()) {
                res = 1;
            } else {
                if (pos0.e() < pos1.e()) {
                    res = 1;
                } else if (pos0.e() > pos1.e()) {
                    res = -1;
                } else {
                    res = 0;
                }
            }
            return res;
        }
    }

    /**
     * An object that maps text position to EObject.
     */
    protected Map pos2EObject = new TreeMap(new InversePosComparator());

    /**
     * An object that maps EObject to text position.
     */
    protected Map eObject2Positions = new HashMap();

    /**
     * An object that maps EObject to comment text position.
     */
    protected Map eObject2CommentPositions = new HashMap();

    /**
     * An object that maps text position to an EObject declaration. It is used
     * for an open link action.
     */
    protected Map pos2LinkEObject = new TreeMap(new InversePosComparator());

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
     * Default object for text mapping.
     */
    protected EObject object;

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
     * @param object
     *            is the default object for text mapping
     * @param freeze
     *            is used to freeze the mappings, this helps to improve the
     *            performance
     */
    public TextModelMapping(EObject object, boolean freeze) {
        this.object = object;
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
            pos2EObject.clear();
            index2EObjects.clear();
            eObject2Positions.clear();
            eObject2CommentPositions.clear();
            pos2LinkEObject.clear();
            index2LinkEObject.clear();
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
    public void from(TextModelMapping other) {
        if (!commit && other != null) {
            // mapping pos2EObject, eObject2Pos
            Iterator it = other.pos2EObject.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = shift + ((Int2) entry.getKey()).b();
                int e = shift + ((Int2) entry.getKey()).e();
                EObject object = (EObject) entry.getValue();
                addMapping(object, b, e);
            }
            // mapping eObject2CommentPositions
            it = other.eObject2CommentPositions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    int b = shift + pos.b();
                    int e = shift + pos.e();
                    EObject object = (EObject) entry.getKey();
                    addCommentMapping(object, b, e);
                }
            }
            // mapping pos2LinkEObject
            it = other.pos2LinkEObject.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = shift + ((Int2) entry.getKey()).b();
                int e = shift + ((Int2) entry.getKey()).e();
                EObject object = (EObject) entry.getValue();
                pos2LinkEObject.put(new Int2(b, e), object);
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
     * Returns all used objects.
     * 
     * @return all used objects
     */
    public Set getEObjects() {
        return eObject2Positions.keySet();
    }

    /**
     * Shifts text positions used for the mapping and puts the default
     * highlight.
     * 
     * @param size
     *            is the size of the shift
     */
    public void shift(int size) {
        shift(size, TextModelMapping.HIGHLIGHTED_DEFAULT);
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
            if (linkEObject != null) {
                Int2 pos = new Int2(shift, shift + size);
                pos2LinkEObject.put(pos, linkEObject);
            }
            if (highlightedType == TextModelMapping.HIGHLIGHTED_STATIC_TEXT) {
                Int2 pos = new Int2(shift, shift + size);
                highlightedPos[TextModelMapping.HIGHLIGHTED_STATIC_TEXT].add(pos);
            } else if (highlightedType == TextModelMapping.HIGHLIGHTED_COMMENT) {
                Int2 pos = new Int2(shift, shift + size);
                highlightedPos[TextModelMapping.HIGHLIGHTED_COMMENT].add(pos);
                addCommentMapping(object, pos.b(), pos.e());
            }
            shift += size;
        }
    }

    /**
     * Adds a mapping between the model object and the text begins at the
     * specified begin and extends to the character at index end - 1.
     * 
     * @param object
     *            is the model object
     * @param begin
     *            is the beginning index, inclusive
     * @param end
     *            is the ending index, exclusive
     */
    protected void addMapping(EObject object, int begin, int end) {
        if (begin > -1 && end > -1) {
            // Mapping pos2EObject
            Int2 newPos = new Int2(begin, end);
            if (!pos2EObject.containsKey(newPos)) {
                pos2EObject.put(newPos, object);
            }
            // Mapping eObject2Pos
            List objectPositions = (List) eObject2Positions.get(object);
            if (objectPositions == null) {
                objectPositions = new ArrayList();
                eObject2Positions.put(object, objectPositions);
            }
            add(objectPositions, begin, end);
        }
    }

    /**
     * Adds a comment mapping between the model object and the text begins at
     * the specified begin and extends to the character at index end - 1.
     * 
     * @param object
     *            is the model object
     * @param begin
     *            is the beginning index, inclusive
     * @param end
     *            is the ending index, exclusive
     */
    protected void addCommentMapping(EObject object, int begin, int end) {
        if (begin > -1 && end > -1) {
            List commentPositions = (List) eObject2CommentPositions.get(object);
            if (commentPositions == null) {
                commentPositions = new ArrayList();
                eObject2CommentPositions.put(object, commentPositions);
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
     * @param linkEObject
     *            is the linked object
     */
    public void linkBegin(EObject linkEObject) {
        this.linkEObject = linkEObject;
    }

    /**
     * Marks the end of the current open link action.
     */
    public void linkEnd() {
        this.linkEObject = null;
    }

    /**
     * Open link result for the next added text
     */
    protected EObject linkEObject = null;

    /**
     * Validate the changes.
     * <p>
     * It is impossible to change mappings now.
     */
    protected void commit() {
        if (!commit) {
            commit = true;
            addMapping(object, 0, shift);
        }
    }

    /**
     * Returns the object at the given index.
     * 
     * @param index
     *            is the position in the text
     * @return the object at the index.
     */
    public EObject index2EObject(int index) {
        List eObjects = index2EObjects(index);
        EObject res = null;
        if (eObjects != null && eObjects.size() > 0) {
            res = (EObject) eObjects.get(0);
        }
        return res;
    }

    /**
     * Returns objects at the given index.
     * 
     * @param index
     *            is the position in the text
     * @return objects at the index.
     */
    public List index2EObjects(int index) {
        List eObjects = (List) index2EObjects.get(new Integer(index));
        if (eObjects == null) {
            eObjects = new ArrayList();
            if (!commit) {
                commit();
            }
            Iterator it = pos2EObject.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = ((Int2) entry.getKey()).b();
                if (index >= b) {
                    int e = ((Int2) entry.getKey()).e();
                    if (index < e) {
                        eObjects.add(entry.getValue());
                    }
                }
            }
            index2EObjects.put(new Integer(index), eObjects);
        }
        return eObjects;
    }

    private Map index2EObjects = new HashMap();

    /**
     * Gets a serializable object that maps the ranges of the text and the URIs
     * of the model's objects.
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
        Iterator it = pos2EObject.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Int2 pos = (Int2) entry.getKey();
            EObject object = (EObject) entry.getValue();
            String uriFragment = ETools.getURI(object);
            result.put(pos, uriFragment);
        }
        return result;
    }

    /**
     * Returns the positions of the given object.
     * 
     * @param object
     *            is the model object
     * @return the positions of the object
     */
    public Int2[] eObject2Positions(EObject object) {
        if (object == null) {
            return new Int2[] {};
        }
        if (!commit) {
            commit();
        }
        List positions = (List) eObject2Positions.get(object);
        if (positions != null) {
            return (Int2[]) positions.toArray(new Int2[positions.size()]);
        } else {
            return new Int2[] {};
        }
    }

    /**
     * Gets a serializable object that maps the URIs of the model's objects and
     * the ranges of the text.
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
        Iterator entries = eObject2Positions.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            EObject object = (EObject) entry.getKey();
            String uriFragment = ETools.getURI(object);
            Set positions = new TreeSet(new PosComparator());
            positions.addAll((List) entry.getValue());
            result.put(uriFragment, positions.toArray(new Int2[positions.size()]));
        }
        return result;
    }

    /**
     * Returns the first comment position of the given object.
     * 
     * @param object
     *            is the model object
     * @param limits
     *            delimits the part of the text where the comment can be
     *            searched
     * @return the first comment position of the object
     */
    public Int2 eObject2CommentPositionIn(EObject object, Int2 limits) {
        if (object == null) {
            return Int2.NOT_FOUND;
        }
        if (!commit) {
            commit();
        }
        List positions = (List) eObject2CommentPositions.get(object);
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
     * Returns the linked object at the given index. It is used to make an open
     * link action.
     * 
     * @param index
     *            is the position in the text
     * @return the linked object at the index
     */
    public EObject index2LinkEObject(int index) {
        EObject object = (EObject) index2LinkEObject.get(new Integer(index));
        if (object == null) {
            if (!commit) {
                commit();
            }
            Iterator it = pos2LinkEObject.entrySet().iterator();
            while (object == null && it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                int b = ((Int2) entry.getKey()).b();
                if (index >= b) {
                    int e = ((Int2) entry.getKey()).e();
                    if (index < e) {
                        object = (EObject) entry.getValue();
                    }
                }
            }
            index2LinkEObject.put(new Integer(index), object);
        }
        return object;
    }

    private Map index2LinkEObject = new HashMap();

    /**
     * Returns all positions for the given highlight.
     * 
     * @param highlightedType
     *            is the text highlight
     * @return all positions for the given highlight
     */
    public Int2[] getHighlightedPos(int highlightedType) {
        if (highlightedType == TextModelMapping.HIGHLIGHTED_STATIC_TEXT) {
            return (Int2[]) highlightedPos[TextModelMapping.HIGHLIGHTED_STATIC_TEXT].toArray(new Int2[] {});
        } else if (highlightedType == TextModelMapping.HIGHLIGHTED_COMMENT) {
            return (Int2[]) highlightedPos[TextModelMapping.HIGHLIGHTED_COMMENT].toArray(new Int2[] {});
        } else {
            return new Int2[] {};
        }
    }

    /**
     * Moves the positions into the given range
     * 
     * @param range
     *            are the new bounds
     */
    public void range(Int2 range) {
        if (!commit) {
            shift = range.e() - range.b();
            // mapping pos2EObject
            Iterator it = pos2EObject.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Int2 pos = (Int2) entry.getKey();
                Int2 copy = new Int2(pos.b(), pos.e());
                copy.range(range);
                if (copy.b() == -1 || (!copy.equals(pos) && pos2EObject.containsKey(copy))) {
                    it.remove();
                } else {
                    pos.range(range);
                }
            }
            // mapping eObject2Positions
            it = eObject2Positions.entrySet().iterator();
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
            // mapping eObject2CommentPositions
            it = eObject2CommentPositions.entrySet().iterator();
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
            // mapping pos2LinkEObject
            it = pos2LinkEObject.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Int2 pos = (Int2) entry.getKey();
                Int2 copy = new Int2(pos.b(), pos.e());
                copy.range(range);
                if (copy.b() == -1 || (!copy.equals(pos) && pos2LinkEObject.containsKey(copy))) {
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
            // mapping pos2EObject
            Iterator it = pos2EObject.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ((Int2) entry.getKey()).indent(lines);
            }
            // mapping eObject2Positions
            it = eObject2Positions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    pos.indent(lines);
                }
            }
            // mapping eObject2CommentPositions
            it = eObject2CommentPositions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                List value = (List) entry.getValue();
                Iterator positions = value.iterator();
                while (positions.hasNext()) {
                    Int2 pos = (Int2) positions.next();
                    pos.indent(lines);
                }
            }
            // mapping pos2LinkEObject
            it = pos2LinkEObject.entrySet().iterator();
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
