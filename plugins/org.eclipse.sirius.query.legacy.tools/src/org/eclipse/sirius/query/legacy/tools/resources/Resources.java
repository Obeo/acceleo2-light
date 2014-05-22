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

package org.eclipse.sirius.query.legacy.tools.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Constants;

import org.eclipse.sirius.query.legacy.tools.AcceleoToolsPlugin;

/**
 * It helps to use workspace resources.
 * 
 */
public class Resources {

    private static final String INSTALL_LOCATION_TAG = "INSTALL_LOCATION"; //$NON-NLS-1$

    private static final String WORKSPACE_LOCATION_TAG = "WORKSPACE_LOCATION"; //$NON-NLS-1$

    private static final int BOM_SIZE = 3;

    /**
     * Returns the content of the file.
     * 
     * @param file
     *            is the file
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    public static StringBuffer getFileContent(IFile file) {
        return Resources.getFileContent(file, true);
    }

    /**
     * Returns the content of the file.
     * 
     * @param file
     *            is the file
     * @param report
     *            indicates if an error is reported when the file doesn't exist
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    public static StringBuffer getFileContent(IFile file, boolean report) {
        StringBuffer buffer = Resources.doGetFileContent(file, report);
        if (file != null && Resources.isTemplateFile(file.getName()) && Resources.getEncoding(buffer) != null) {
            buffer = Resources.getEncodedFileContent(file, report, Resources.getEncoding(buffer));
        }
        return buffer;
    }

    /**
     * Returns the content of the file.
     * 
     * @param file
     *            is the file
     * @param report
     *            indicates if an error is reported when the file doesn't exist
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    private static StringBuffer doGetFileContent(IFile file, boolean report) {
        StringBuffer buffer = new StringBuffer();
        if (file != null) {

            UnicodeBOMInputStream ubis = null;
            InputStream content = null;
            try {
                content = file.getContents(false);
                ubis = new UnicodeBOMInputStream(content);
                ubis.skipBOM();
                byte[] readBuffer = new byte[ubis.available()];
                int n = ubis.read(readBuffer);
                while (n > 0) {
                    buffer.append(new String(readBuffer));
                    n = ubis.read(readBuffer);
                }
            } catch (Exception e) {
                if (report) {
                    AcceleoToolsPlugin.getDefault().log(e, true);
                }
            } finally {
                if (ubis != null) {
                    try {
                        ubis.close();
                    } catch (IOException e) {
                        if (report) {
                            AcceleoToolsPlugin.getDefault().log(e, true);
                        }
                    }
                }
                if (content != null) {
                    try {
                        content.close();
                    } catch (IOException e) {
                        if (report) {
                            AcceleoToolsPlugin.getDefault().log(e, true);
                        }
                    }
                }
            }
        }
        return buffer;
    }

    /**
     * Returns the content of the file.
     * 
     * @param file
     *            is the file
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    public static StringBuffer getFileContent(File file) {
        return Resources.getFileContent(file, true);
    }

    /**
     * Returns the content of the file.
     * 
     * @param file
     *            is the file
     * @param report
     *            indicates if an error is reported when the file doesn't exist
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    public static StringBuffer getFileContent(File file, boolean report) {
        StringBuffer buffer = Resources.doGetFileContent(file, report);
        String encoding = Resources.getEncoding(buffer);
        if (file != null && Resources.isTemplateFile(file.getName()) && encoding != null) {
            buffer = Resources.getEncodedFileContent(file, report, encoding);
        }
        return buffer;
    }

    /**
     * @param file
     *            any file (existing or not).
     * @return true if this file is an Acceleo Template file, false otherwise.
     */
    private static boolean isTemplateFile(String filename) {
        if (filename != null) {
            if (filename.toLowerCase().endsWith("mt")) { //$NON-NLS-1$
                return true;
            }
            if (filename.toLowerCase().endsWith("tr")) { //$NON-NLS-1$
                return true;
            }
        }
        return false;
    }

    /**
     * Return the content of an {@link IFile} using the specified encoding.
     * 
     * @param file
     *            file to read.
     * @param report
     *            if it's set to true, any error will get reported, otherwise
     *            some errors may appear silently.
     * @param encodingCode
     *            encodingCode to use in order to read the file.
     * @return an unicode {@link StringBuffer} of the file content.
     */
    public static StringBuffer getEncodedFileContent(File file, boolean report, String encodingCode) {
        StringBuffer buffer = new StringBuffer();
        FileInputStream fiss;
        UnicodeBOMInputStream fis;
        try {
            fiss = new FileInputStream(file);
            fis = new UnicodeBOMInputStream(fiss);
            fis.skipBOM();
            InputStreamReader in = new InputStreamReader(fis, encodingCode);
            try {
                char[] buff = new char[512];
                int size = in.read(buff);
                while (size > 0) {
                    buffer.append(buff, 0, size);
                    size = in.read(buff);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (fiss != null) {
                    fiss.close();
                }
                if (fis != null) {
                    fis.close();
                }
            }
        } catch (IOException e) {
            if (report && !(e instanceof UnsupportedEncodingException)) {
                AcceleoToolsPlugin.getDefault().log(e, true);
            }
        }

        return buffer;
    }

    /**
     * Return the content of an {@link IFile} using the specified encoding.
     * 
     * @param file
     *            file to read.
     * @param report
     *            if it's set to true, any error will get reported, otherwise
     *            some errors may appear silently.
     * @param encodingCode
     *            encodingCode to use in order to read the file.
     * @return an unicode {@link StringBuffer} of the file content.
     */
    public static StringBuffer getEncodedFileContent(IFile file, boolean report, String encodingCode) {
        StringBuffer buffer = new StringBuffer();
        if (file != null) {
            UnicodeBOMInputStream content = null;
            try {
                content = new UnicodeBOMInputStream(file.getContents(false));
                content.skipBOM();
                /*
                 * create an InputStreamReader specifying an encoding.
                 */
                InputStreamReader in = new InputStreamReader(content, encodingCode);
                try {
                    char[] buff = new char[content.available()];
                    int size = in.read(buff);
                    while (size > 0) {
                        buffer.append(buff, 0, size);
                        size = in.read(buff);
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (Exception e) {
                if (report) {
                    AcceleoToolsPlugin.getDefault().log(e, true);
                }
            } finally {
                if (content != null) {
                    try {
                        content.close();
                    } catch (IOException e) {
                        if (report && !(e instanceof UnsupportedEncodingException)) {
                            AcceleoToolsPlugin.getDefault().log(e, true);
                        }
                    }
                }
            }
        }
        return buffer;
    }

    /**
     * Returns the content of the file.
     * 
     * @param file
     *            is the file
     * @param report
     *            indicates if an error is reported when the file doesn't exist
     * @return the content of the file, or an empty buffer if the file doesn't
     *         exist
     */
    private static StringBuffer doGetFileContent(File file, boolean report) {
        StringBuffer buffer = new StringBuffer();
        try {
            FileInputStream fis = new FileInputStream(file);
            UnicodeBOMInputStream ubis = new UnicodeBOMInputStream(fis);
            ubis.skipBOM();
            BufferedReader in = new BufferedReader(new InputStreamReader(ubis));
            try {
                char[] buff = new char[512];
                int size = in.read(buff);
                while (size > 0) {
                    buffer.append(buff, 0, size);
                    size = in.read(buff);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (fis != null) {
                    fis.close();
                }
                if (ubis != null) {
                    ubis.close();
                }
            }
        } catch (IOException e) {
            if (report) {
                AcceleoToolsPlugin.getDefault().log(e, true);
            }
        }
        return buffer;
    }

    /**
     * Gets the default output of the project.
     * 
     * @param project
     *            is the project
     * @return the default output of the project, or null if it doesn't exist
     */
    public static IFolder getOutputFolder(IProject project) {
        final IJavaProject projet = JavaCore.create(project);
        try {
            IPath output = projet.getOutputLocation();
            if (output != null && output.segmentCount() > 1) {
                IFolder folder = project.getWorkspace().getRoot().getFolder(output);
                if (folder.exists()) {
                    return folder;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (JavaModelException e) {
            return null;
        }
    }

    /**
     * Creates and load a persistent EMF document for a resource in the
     * workspace.
     * 
     * @param resource
     *            is the resource in the workspace
     * @param load
     *            indicates if the persistent EMF document is loaded
     * @return the persistent EMF document
     */
    public static Resource getResource(IResource resource, boolean load) {
        ResourceSetImpl resourceSet = new ResourceSetImpl();
        Resource result = resourceSet.getResource(Resources.createPlatformResourceURI(resource.getFullPath().toString()), load);
        // EcoreUtil.resolveAll(resourceSet);
        return result;
    }

    /**
     * Gets the resource in the workspace for a persistent EMF document.
     * 
     * @param resource
     *            is the persistent EMF document
     * @return the resource in the workspace
     */
    public static IFile getIFile(Resource resource) {
        URI uri = resource.getURI();
        if ("cdo".equals(uri.scheme())) { //$NON-NLS-1$
            if (resource.getURI().path() != null) {
                if (resource.getURI().path() != null) {
                    IPath path = Path.fromPortableString(resource.getURI().path());
                    IContainer folder = (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(path.removeLastSegments(1));
                    if (folder != null) {
                        path = path.removeFirstSegments(path.segmentCount() - 1);
                        if ("system".equals(path.getFileExtension())) { //$NON-NLS-1$
                            path = path.removeFileExtension().removeFileExtension().addFileExtension("system").addFileExtension("ost"); //$NON-NLS-1$ //$NON-NLS-2$
                        } else if ("xmi".equals(path.getFileExtension())) { //$NON-NLS-1$
                            path = path.removeFileExtension().addFileExtension("ost"); //$NON-NLS-1$
                        }
                        return folder.getFile(path);
                    }
                }
            }
        } else {
            String path = uri.path();
            if (path != null) {
                if (path.startsWith("/resource")) { //$NON-NLS-1$
                    path = path.substring("/resource".length()); //$NON-NLS-1$
                }
                IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
                if (member instanceof IFile) {
                    return (IFile) member;
                }
            }
        }
        return null;
    }

    /**
     * Finds and returns the member resource identified by the given path in the
     * workspace, or null if no such resource exists.
     * 
     * @param path
     *            is the path of the desired resource
     * @return the member resource, or null if no such resource exists
     */
    public static IResource findResource(IPath path) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace.getRoot().exists(path)) {
            return workspace.getRoot().findMember(path);
        } else {
            return null;
        }
    }

    /**
     * Finds and returns the file identified by the given path in the workspace,
     * or null if no such file exists.
     * 
     * @param path
     *            is the path of the desired resource
     * @return the member file, or null if no such resource exists
     */
    public static IFile findFile(IPath path) {
        IResource resource = Resources.findResource(path);
        if (resource instanceof IFile) {
            return (IFile) resource;
        } else {
            return null;
        }
    }

    /**
     * Creates a platform-relative path URI.
     * 
     * @param path
     *            is the path of the URI to create
     * @return the new URI
     * @see org.eclipse.emf.common.util.URI#createPlatformResourceURI
     */
    public static URI createPlatformResourceURI(String path) {
        if (path.startsWith("platform:/plugin")) { //$NON-NLS-1$
            return URI.createURI(path, true);
        } else {
            return URI.createPlatformResourceURI(path, true);
        }
    }

    /**
     * Creates relative path from absolute. Resolve relative pathnames against
     * workspace directory.
     * 
     * @param absolutePath
     *            absolute file path (directory or file).
     * @return relative path if workspace directory is included in absolutePath,
     *         otherwise return absolute path.
     */
    public static String makeWorkspaceRelativePath(String absolutePath) {
        return Resources.makeRelativePath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString(), absolutePath);
    }

    /**
     * Creates relative path from absolute. Resolve relative pathnames against
     * parent directory.
     * 
     * @param parentPath
     *            parent directory path.
     * @param absolutePath
     *            absolute file path (directory or file).
     * @return relative path if parent directory is included in absolutePath,
     *         otherwise return absolute path.
     */
    public static String makeRelativePath(String parentPath, String absolutePath) {
        String relativePath = null;
        if (parentPath != null && absolutePath != null && parentPath.length() < absolutePath.length()) {
            File absoluteFile = new File(absolutePath);
            if (absoluteFile.isAbsolute()) {
                File parentFile = new File(parentPath);
                if (parentFile.isAbsolute()) {
                    String dirPath = parentFile.getAbsolutePath().replace('\\', '/');
                    absolutePath = absolutePath.replace('\\', '/');
                    int dirLength = dirPath.length();
                    if (absolutePath.substring(0, dirLength).equalsIgnoreCase(dirPath)) {
                        relativePath = absolutePath.substring(dirLength);
                        if (relativePath.startsWith("/")) { //$NON-NLS-1$
                            relativePath = relativePath.substring(1);
                        }
                    }
                } else {
                    relativePath = absoluteFile.getPath().replace('\\', '/');
                }
            } else {
                relativePath = absoluteFile.getPath().replace('\\', '/');
            }
        } else {
            if (absolutePath != null) {
                relativePath = absolutePath.replace('\\', '/');
            }
        }
        return relativePath;
    }

    /**
     * Creates the absolute path.
     * 
     * @param url
     *            is the relative path
     * @return the absolute file path
     */
    public static String transformToAbsolutePath(URL url) {
        String absolutePath;
        try {
            URL transformedUrl = FileLocator.toFileURL(url);
            File file = new File(transformedUrl.getFile());
            absolutePath = file.getAbsolutePath();
        } catch (IOException e) {
            absolutePath = ""; //$NON-NLS-1$
            AcceleoToolsPlugin.getDefault().log(e, true);
        }
        return absolutePath;
    }

    /**
     * Get the IFile for the URI.
     * 
     * @param uri
     *            is the URI
     * @return the IFile if the URI is a project form, or null if not a project
     *         form, OR the project doesn't exist.The IFile returned doesn't
     *         necessarily exist.
     */
    public static IFile getIFile(URI uri) {
        IProject project = Resources.getProject(uri);
        if (project != null) {
            IPath path;
            if (Resources.isPlatformResourceURI(uri)) {
                // remove /resource/project name/
                path = new Path(URI.decode(uri.path())).removeFirstSegments(2);
            } else {
                // remove /project name/
                path = new Path(URI.decode(uri.path())).removeFirstSegments(1);
            }
            return project.getFile(path);
        } else {
            return null;
        }
    }

    private static IProject getProject(URI uri) {
        String projectName;
        if (Resources.isPlatformResourceURI(uri)) {
            projectName = uri.segment(1);
        } else if (uri.scheme() == null) {
            projectName = new Path(uri.path()).segment(0); // project name is
            // first in the URI
        } else {
            return null;
        }
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(URI.decode(projectName));
        if (project != null && project.isAccessible()) {
            return project;
        } else {
            return null;
        }
    }

    private static boolean isPlatformResourceURI(URI uri) {
        return "platform".equals(uri.scheme()) && "resource".equals(uri.segment(0)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Gets the required plugins for the given project.
     * 
     * @param project
     *            is the project
     * @return the IDs of the required plugins
     */
    public static String[] getRequiredPluginIDs(IProject project) {
        List IDs = new ArrayList();
        IFile file = project.getFile(JarFile.MANIFEST_NAME);
        Double cachedTimestamp = (Double) Resources.fileTimestamp.get(file);
        if (cachedTimestamp != null && file.getModificationStamp() + file.getLocalTimeStamp() == cachedTimestamp.longValue()) {
            IDs = (List) Resources.fileRequired.get(file);
        } else {
            if (file.exists()) {
                InputStream manifestStream = null;
                try {
                    manifestStream = new FileInputStream(file.getLocation().toFile());
                    Manifest manifest = new Manifest(manifestStream);
                    Properties prop = Resources.manifestToProperties(manifest.getMainAttributes());
                    String requiredBundles = (String) prop.get(Constants.REQUIRE_BUNDLE);
                    if (requiredBundles != null) {
                        StringTokenizer st = new StringTokenizer(requiredBundles, ","); //$NON-NLS-1$
                        while (st.hasMoreTokens()) {
                            String id = st.nextToken().trim();
                            int iDot = id.indexOf(';');
                            if (iDot > -1) {
                                id = id.substring(0, iDot).trim();
                            }
                            if (id.length() > 0) {
                                IDs.add(id);
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                } finally {
                    try {
                        if (manifestStream != null) {
                            manifestStream.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
            Resources.fileTimestamp.put(file, new Double(file.getModificationStamp() + file.getLocalTimeStamp()));
            Resources.fileRequired.put(file, IDs);
        }
        return (String[]) IDs.toArray(new String[IDs.size()]);
    }

    /**
     * Cache map File -> time stamp.
     */
    private static Map fileTimestamp = new HashMap();

    /**
     * Cache file -> required plug-in.
     */
    private static Map fileRequired = new HashMap();

    private static Properties manifestToProperties(Attributes d) {
        Iterator iter = d.keySet().iterator();
        Properties result = new Properties();
        while (iter.hasNext()) {
            Attributes.Name key = (Attributes.Name) iter.next();
            result.put(key.toString(), d.get(key));
        }
        return result;
    }

    /**
     * Decodes an acceleo path, it replaces the acceleo tags by the workspace
     * location or the platform location.
     * 
     * @param path
     *            is the path to decode
     * @return the absolute path
     * @see encodeAcceleoAbsolutePath
     */
    public static String decodeAcceleoAbsolutePath(String path) {
        if (path == null) {
            return path;
        } else {
            if (path.startsWith(Resources.WORKSPACE_LOCATION_TAG)) {
                String workspaceLocation;
                if (ResourcesPlugin.getWorkspace().getRoot().getLocation() != null) {
                    workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
                } else {
                    workspaceLocation = ""; //$NON-NLS-1$
                }
                return workspaceLocation + path.substring(Resources.WORKSPACE_LOCATION_TAG.length());
            } else if (path.startsWith(Resources.INSTALL_LOCATION_TAG)) {
                String installLocation;
                if (Platform.getInstallLocation() != null) {
                    installLocation = new Path(Resources.transformToAbsolutePath(Platform.getInstallLocation().getURL())).toString();
                } else {
                    installLocation = ""; //$NON-NLS-1$
                }
                return installLocation + path.substring(Resources.INSTALL_LOCATION_TAG.length());
            } else {
                return path;
            }
        }
    }

    /**
     * Encodes an absolute path, it replaces the workspace location or the
     * platform location by an acceleo tag.
     * 
     * @param path
     *            is the path to encode
     * @return the acceleo path
     * @see decodeAcceleoAbsolutePath
     */
    public static String encodeAcceleoAbsolutePath(String path) {
        if (path == null) {
            return path;
        } else {
            path = new Path(path).toString();
            String workspaceLocation = null;
            if (ResourcesPlugin.getWorkspace().getRoot().getLocation() != null) {
                workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
            }
            if (workspaceLocation != null && path.startsWith(workspaceLocation)) {
                return Resources.WORKSPACE_LOCATION_TAG + path.substring(workspaceLocation.length());
            } else {
                String installLocation = null;
                if (Platform.getInstallLocation() != null) {
                    installLocation = new Path(Resources.transformToAbsolutePath(Platform.getInstallLocation().getURL())).toString();
                }
                if (installLocation != null && path.startsWith(installLocation)) {
                    return Resources.INSTALL_LOCATION_TAG + path.substring(installLocation.length());
                } else {
                    return path;
                }
            }
        }
    }

    /**
     * Encoding specification start marker, used in MT templates to indicate to
     * Acceleo in which encoding the file should be loaded.
     */
    private static final String ENCODING_START = "encoding=";//$NON-NLS-1$

    /**
     * @param buffer
     *            buffer in which we want to look for an encoding code.
     * @return a {@link String} having an encoding code as value (be carefull it
     *         may be a wrong code, you'll have to check by yourself). It may
     *         return null if no encoding code is found.
     */
    public static String getEncoding(StringBuffer buffer) {
        String startMarker = "<%--"; //$NON-NLS-1$
        String endMarker = "--%>"; //$NON-NLS-1$
        String result = Resources.doGetEncoding(buffer, startMarker, endMarker);
        if (result == null) {
            startMarker = "[%--"; //$NON-NLS-1$
            endMarker = "--%]"; //$NON-NLS-1$
            result = Resources.doGetEncoding(buffer, startMarker, endMarker);
        }
        return result;
    }

    /**
     * Look for an encoding specification in a buffer using start and end
     * markers.
     * 
     * @param buffer
     *            buffer in which we want to look for an encoding code.
     * @param startMarker
     *            start marker of the language.
     * @param endMarker
     *            end marker of the language.
     * @return the found encoding code or null.
     */
    private static String doGetEncoding(StringBuffer buffer, String startMarker, String endMarker) {
        int start = buffer.indexOf(startMarker + Resources.ENCODING_START);
        if (start != -1) {
            int end = buffer.indexOf(endMarker, start);
            if (end != -1) {
                String encoding = buffer.substring(start + (startMarker + Resources.ENCODING_START).length(), end);
                return encoding.trim().toUpperCase();
            }
        }
        return null;
    }
}
