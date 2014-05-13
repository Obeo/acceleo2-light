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

package fr.obeo.acceleo.tools.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * Gets file's content more faster.
 * 
 * @author www.obeo.fr
 * 
 */
public class FileContentMap {

    /**
     * The max capacity.
     */
    protected int max;

    /**
     * File to content mappings.
     */
    protected Map file2content;

    /**
     * File to modification stamp mappings.
     */
    protected Map file2OldModificationStamp = new HashMap();

    /**
     * Files priorities.
     */
    protected List priority = new ArrayList();

    /**
     * Constructor.
     */
    public FileContentMap() {
        this(-1, true);
    }

    /**
     * Constructor.
     * 
     * @param max
     *            is the max capacity
     */
    public FileContentMap(int max) {
        this(max, true);
    }

    /**
     * Constructor.
     * 
     * @param weak
     *            to use weak hashmap
     */
    public FileContentMap(boolean weak) {
        this(-1, weak);
    }

    /**
     * Constructor.
     * 
     * @param max
     *            is the max capacity
     * @param weak
     *            to use weak hashmap
     */
    public FileContentMap(int max, boolean weak) {
        this.max = max;
        if (weak) {
            file2content = new WeakHashMap();
        } else {
            file2content = new HashMap();
        }
    }

    /**
     * Returns the value to which the specified file is mapped in this identity
     * map, or null if the map contains no mapping for this file, or null if the
     * file has been modified.
     * 
     * @param file
     *            is the file
     * @return the value to which the specified file is mapped
     */
    public Object get(IFile file) {
        if (file != null) {
            String path = file.getFullPath().toString();
            Object content = file2content.get(path);
            Double newModificationStamp = new Double(file.getModificationStamp() + file.getLocalTimeStamp());
            Double oldModificationStamp = (Double) file2OldModificationStamp.get(path);
            if (content != null && oldModificationStamp != null && oldModificationStamp.doubleValue() == newModificationStamp.doubleValue()) {
                if (content instanceof EObject) {
                    return getForEObject((EObject) content);
                } else {
                    return content;
                }
            } else {
                // Remove the expired element
                file2content.remove(path);
                file2OldModificationStamp.remove(path);
                priority.remove(path);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the value to which the specified file is mapped in this identity
     * map, or null if the map contains no mapping for this file, or null if the
     * file has been modified.
     * 
     * @param file
     *            is the file
     * @return the value to which the specified file is mapped
     */
    public Object get(File file) {
        if (file != null) {
            String path = file.getAbsolutePath().toString();
            IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(path));
            if (workspaceFile != null && workspaceFile.isAccessible()) {
                return get(workspaceFile);
            } else {
                Object content = file2content.get(path);
                Double newModificationStamp = new Double(file.lastModified());
                Double oldModificationStamp = (Double) file2OldModificationStamp.get(path);
                if (content != null && oldModificationStamp != null && oldModificationStamp.doubleValue() == newModificationStamp.doubleValue()) {
                    if (content instanceof EObject) {
                        return getForEObject((EObject) content);
                    } else {
                        return content;
                    }
                } else {
                    // Remove the expired element
                    file2content.remove(path);
                    file2OldModificationStamp.remove(path);
                    priority.remove(path);
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private EObject getForEObject(EObject content) {
        if (content.eResource() != null && content.eResource().getResourceSet() != null) {
            ResourceSet resourceSet = content.eResource().getResourceSet();
            Iterator resources = resourceSet.getResources().iterator();
            while (resources.hasNext()) {
                Resource resource = (Resource) resources.next();
                if (resource != content.eResource() && resource.getURI() != null) {
                    IFile loadedFile = Resources.getIFile(resource.getURI());
                    if (loadedFile != null && loadedFile.exists()) {
                        Double newModificationStamp = new Double(loadedFile.getModificationStamp() + loadedFile.getLocalTimeStamp());
                        Double oldModificationStamp = (Double) file2OldModificationStamp.get(loadedFile.getFullPath().toString());
                        if (oldModificationStamp == null || oldModificationStamp.doubleValue() != newModificationStamp.doubleValue()) {
                            return null;
                        }
                    }
                }
            }
        }
        return content;
    }

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for this key, the old value is
     * replaced.
     * 
     * @param file
     *            is the file
     * @param content
     *            is the value to which the specified file is mapped
     */
    public void put(IFile file, Object content) {
        if (file != null) {
            String path = file.getFullPath().toString();
            if (max > -1 && size() >= max && priority.size() > 0) {
                // Remove the oldest element
                Object toRemove = priority.get(0);
                file2content.remove(toRemove);
                file2OldModificationStamp.remove(toRemove);
                priority.remove(0);
            }
            file2content.put(path, content);
            file2OldModificationStamp.put(path, new Double(file.getModificationStamp() + file.getLocalTimeStamp()));
            if (content instanceof EObject) {
                putForEObject((EObject) content);
            }
            priority.add(path);
        }
    }

    /**
     * Associates the specified value with the specified key in this map. If the
     * map previously contained a mapping for this key, the old value is
     * replaced.
     * 
     * @param file
     *            is the file
     * @param content
     *            is the value to which the specified file is mapped
     */
    public void put(File file, Object content) {
        if (file != null) {
            String path = file.getAbsolutePath().toString();
            IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(path));
            if (workspaceFile != null && workspaceFile.isAccessible()) {
                put(workspaceFile, content);
            } else {
                if (max > -1 && size() >= max && priority.size() > 0) {
                    // Remove the oldest element
                    Object toRemove = priority.get(0);
                    file2content.remove(toRemove);
                    file2OldModificationStamp.remove(toRemove);
                    priority.remove(0);
                }
                file2content.put(path, content);
                file2OldModificationStamp.put(path, new Double(file.lastModified()));
                if (content instanceof EObject) {
                    putForEObject((EObject) content);
                }
                priority.add(path);
            }
        }
    }

    private void putForEObject(EObject content) {
        if (content.eResource() != null && content.eResource().getResourceSet() != null) {
            ResourceSet resourceSet = content.eResource().getResourceSet();
            Iterator resources = resourceSet.getResources().iterator();
            while (resources.hasNext()) {
                Resource resource = (Resource) resources.next();
                if (resource != content.eResource() && resource.getURI() != null) {
                    IFile loadedFile = Resources.getIFile(resource.getURI());
                    if (loadedFile != null && loadedFile.exists()) {
                        file2OldModificationStamp.put(loadedFile.getFullPath().toString(), new Double(loadedFile.getModificationStamp() + loadedFile.getLocalTimeStamp()));
                    }
                }
            }
        }
    }

    /**
     * Removes the element for the given file.
     * 
     * @param file
     *            is the file
     */
    public void remove(IFile file) {
        String path = file.getFullPath().toString();
        file2content.remove(path);
        file2OldModificationStamp.remove(path);
        priority.remove(path);
    }

    /**
     * Removes the element for the given file.
     * 
     * @param file
     *            is the file
     */
    public void remove(File file) {
        String path = file.getAbsolutePath().toString();
        IFile workspaceFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(path));
        if (workspaceFile != null && workspaceFile.isAccessible()) {
            remove(workspaceFile);
        } else {
            file2content.remove(path);
            file2OldModificationStamp.remove(path);
            priority.remove(path);
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     * 
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return file2content.size();
    }

    /**
     * Removes all elements.
     */
    public void clear() {
        file2content.clear();
        file2OldModificationStamp.clear();
        priority.clear();
    }

    /**
     * Returns a collection view of the values contained in this map.
     * <p>
     * For (weak == false) only.
     * <p>
     * 
     * @return a collection view of the values contained in this map
     */
    public Collection values() {
        if (!(file2content instanceof WeakHashMap)) {
            return file2content.values();
        } else {
            return new ArrayList();
        }
    }

    /**
     * Returns a set view of the entries contained in this map.
     * <p>
     * 
     * @return a set of the entries contained in this map
     */
    public Set entrySet() {
        return file2content.entrySet();
    }

}
