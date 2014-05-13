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

package fr.obeo.acceleo.gen.template.eval.merge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import fr.obeo.acceleo.gen.AcceleoEcoreGenPlugin;
import fr.obeo.acceleo.gen.AcceleoGenMessages;
import fr.obeo.acceleo.tools.strings.Int2;

/**
 * Round-trip support for templates.
 * 
 * @author www.obeo.fr
 * 
 */
public class MergeTools {

	/**
	 * Lost code file extension.
	 */
	public static final String LOST_FILE_EXTENSION = "lost"; //$NON-NLS-1$

	/**
	 * Default tag used to start the user code.
	 */
	public static final String DEFAULT_USER_BEGIN = AcceleoGenMessages.getString("MergeTools.UserCodeStart"); //$NON-NLS-1$

	/**
	 * Default tag used to stop the user code.
	 */
	public static final String DEFAULT_USER_END = AcceleoGenMessages.getString("MergeTools.UserCodeEnd"); //$NON-NLS-1$

	/**
	 * The identifier of the extension point use to overdone the user code after
	 * the generation.
	 */
	private static final String OVERDONE_CODE_EXTENSION_ID = "fr.obeo.acceleo.gen.overdonecode"; //$NON-NLS-1$

	/**
	 * Merges the two buffers. It inserts the user code of the old buffer in the
	 * new buffer.
	 * 
	 * @param file
	 *            is the file to generate
	 * @param newBuffer
	 *            is the new buffer
	 * @param oldBuffer
	 *            is the old buffer
	 * @param beginTag
	 *            is used to start the user code
	 * @param endTag
	 *            is used to stop the user code
	 * @return lost code
	 */
	public static String merge(IFile file, StringBuffer newBuffer, StringBuffer oldBuffer, String beginTag, String endTag) {
		StringBuffer lost = new StringBuffer(""); //$NON-NLS-1$
		Map oldName2Pos = tagName2Pos(oldBuffer, beginTag, endTag, lost);
		Map newPos2Name = tagPos2Name(newBuffer, beginTag, endTag);
		List notFound = new ArrayList();
		Iterator it = newPos2Name.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Int2 newPos = (Int2) entry.getKey();
			String name = (String) entry.getValue();
			Int2 oldPos = (Int2) oldName2Pos.get(name);
			if (oldPos != null) {
				String oldText = oldBuffer.substring(oldPos.b(), oldPos.e());
				newBuffer.delete(newPos.b(), newPos.e());
				newBuffer.insert(newPos.b(), oldText);
				int shift = oldText.length() - (newPos.e() - newPos.b());
				oldName2Pos.remove(name);
				if (notFound.size() > 0) {
					Iterator notFoundIt = notFound.iterator();
					while (notFoundIt.hasNext()) {
						Object[] notFoundEntry = (Object[]) notFoundIt.next();
						int b = ((Int2) notFoundEntry[0]).b() + shift;
						int e = ((Int2) notFoundEntry[0]).e() + shift;
						notFoundEntry[0] = new Int2(b, e);
					}
				}
			} else {
				notFound.add(new Object[] { entry.getKey(), entry.getValue() });
			}
		}
		if (notFound.size() == 1 && oldName2Pos.size() == 1) {
			List toRemove = new ArrayList();
			it = oldName2Pos.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String oldName = (String) entry.getKey();
				Iterator notFoundIt = notFound.iterator();
				while (notFoundIt.hasNext()) {
					Object[] notFoundEntry = (Object[]) notFoundIt.next();
					String newName = (String) notFoundEntry[1];
					if (nameSimilarityMetric(oldName, newName) > 0.5) {
						Int2 oldPos = (Int2) entry.getValue();
						Int2 newPos = (Int2) notFoundEntry[0];
						String oldText = oldBuffer.substring(oldPos.b(), oldPos.e());
						newBuffer.delete(newPos.b(), newPos.e());
						newBuffer.insert(newPos.b(), oldText);
						toRemove.add(oldName);
						notFoundIt.remove();
						lost.append('\n');
						lost.append(AcceleoGenMessages.getString("MergeTools.MovedCode", new Object[] { oldName, newName, })); //$NON-NLS-1$
						lost.append('\n');
						lost.append(oldName);
						lost.append(oldBuffer.substring(oldPos.b(), oldPos.e()));
						lost.append('\n');
						break;
					}
				}
			}
			it = toRemove.iterator();
			while (it.hasNext()) {
				oldName2Pos.remove(it.next());
			}
		}
		it = oldName2Pos.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String name = (String) entry.getKey();
			Int2 oldPos = (Int2) entry.getValue();
			lost.append('\n');
			lost.append(name);
			lost.append(oldBuffer.substring(oldPos.b(), oldPos.e()));
			lost.append('\n');
			int eOld = oldBuffer.indexOf("\n", oldPos.e() + 1); //$NON-NLS-1$
			if (eOld == -1)
				eOld = oldBuffer.length();
			lost.append(oldBuffer.substring(oldPos.e(), eOld).trim());
			lost.append('\n');
		}
		keepOverdoneCode(file, oldBuffer, newBuffer, lost);
		return lost.toString();
	}

	private static Map tagPos2Name(StringBuffer buffer, String beginTag, String endTag) {
		Map tags = new TreeMap(new Comparator() {
			public int compare(Object arg0, Object arg1) {
				return ((((Int2) arg0).b() > ((Int2) arg1).b()) ? -1 : 1);
			}
		});
		int i = 0;
		while (i < buffer.length()) {
			Int2 bUser = nextTag(buffer, beginTag, i);
			Int2 eUser = nextTag(buffer, endTag, bUser.e());
			if (eUser.b() > -1) {
				// pos and name
				Int2 pos = new Int2(bUser.e(), eUser.b());
				String name = buffer.substring(bUser.b(), bUser.e()).trim();
				tags.put(pos, name);
				i = eUser.e();
			} else {
				break;
			}
		}
		return tags;
	}

	private static Map tagName2Pos(StringBuffer buffer, String beginTag, String endTag, StringBuffer lost) {
		Map tags = new HashMap();
		int i = 0;
		while (i < buffer.length()) {
			Int2 bUser = nextTag(buffer, beginTag, i);
			Int2 eUser = nextTag(buffer, endTag, bUser.e());
			if (eUser.b() > -1) {
				// pos and name
				Int2 pos = new Int2(bUser.e(), eUser.b());
				String name = buffer.substring(bUser.b(), bUser.e()).trim();
				// tag exists?
				if (tags.get(name) == null) {
					tags.put(name, pos);
				} else {
					lost.append('\n');
					lost.append(AcceleoGenMessages.getString("MergeTools.DuplicatedTag", new Object[] { name, })); //$NON-NLS-1$
					lost.append(buffer.substring(pos.b(), pos.e()));
					lost.append('\n');
				}
				i = eUser.e();
			} else {
				break;
			}
		}
		return tags;
	}

	private static double nameSimilarityMetric(String str1, String str2) {
		double result = 0;
		if (str1 == null || str2 == null) {
			return 0;
		}
		if (str1.length() == 1 || str2.length() == 1) {
			if (str1.equals(str2))
				return 1.0;
			return 0;
		}
		List pairs1 = pairs(str1);
		List pairs2 = pairs(str2);
		double union = pairs1.size() + pairs2.size();
		if (union == 0) {
			return 0;
		}
		pairs1.retainAll(pairs2);
		int inter = pairs1.size();
		result = (inter * 2.0) / union;
		if (result > 1) {
			result = 1;
		}
		if (result == 1.0 && !str1.equals(str2)) {
			return 0.999999;
		} else {
			return result;
		}
	}

	private static List pairs(String source) {
		List result = new LinkedList();
		if (source != null) {
			for (int i = 0; i < source.length() - 1; i = i + 1) {
				result.add(source.toUpperCase().substring(i, i + 2));
				if (source.length() % 2 == 1 && source.length() > 1) {
					result.add(source.toUpperCase().substring(source.length() - 2, source.length() - 1));
				}
			}
		}
		return result;
	}

	/**
	 * Gets the position of the next tag in the buffer.
	 * 
	 * @param buffer
	 *            is the buffer
	 * @param tag
	 *            is the tag to search
	 * @param pos
	 *            is the beginning index
	 * @return the position of the next tag
	 */
	public static Int2 nextTag(StringBuffer buffer, String tag, int pos) {
		if (pos > -1) {
			int b = buffer.indexOf(tag, pos);
			if (b > -1) {
				// End of the tag
				int eUser = -1;
				String[] eDelimiters = { "\n" }; //$NON-NLS-1$
				for (int i = 0; i < eDelimiters.length; i++) {
					int eUser_ = buffer.indexOf(eDelimiters[i], b + tag.length());
					if (eUser == -1 || (eUser_ > -1 && eUser_ < eUser))
						eUser = eUser_;
				}
				if (eUser == -1)
					eUser = buffer.length();
				// Begin of the tag
				int bUser = -1;
				String[] bDelimiters = { "\n" }; //$NON-NLS-1$
				for (int i = bDelimiters.length - 1; i >= 0; i--) {
					int bUser_ = buffer.lastIndexOf(bDelimiters[i], b);
					if (bUser == -1 || (bUser_ > -1 && bUser_ > bUser))
						bUser = bUser_;
				}
				if (bUser == -1)
					bUser = 0;
				return new Int2(bUser, eUser);
			}
		}
		return Int2.NOT_FOUND;
	}

	/**
	 * Indicates if the offset is between the given tags.
	 * 
	 * @param buffer
	 *            is the buffer
	 * @param beginTag
	 *            is the beginning tag
	 * @param endTag
	 *            is the ending tag
	 * @param index
	 *            is the offset to test
	 * @return true if the offset is between the given tags
	 */
	public static boolean isBetweenTags(StringBuffer buffer, String beginTag, String endTag, int index) {
		index = buffer.substring(0, index).lastIndexOf("\n") + 1; //$NON-NLS-1$
		Int2 bUser = nextTag(buffer, beginTag, index);
		Int2 eUser = nextTag(buffer, endTag, index);
		return (eUser.b() > -1 && (bUser.b() == -1 || eUser.b() < bUser.b()));
	}

	private static void keepOverdoneCode(IFile file, StringBuffer oldBuffer, StringBuffer newBuffer, StringBuffer lost) {
		if (file != null && file.exists()) {
			if (overdoneStrategies == null) {
				overdoneStrategies = new HashMap();
				IExtensionRegistry registry = Platform.getExtensionRegistry();
				IExtensionPoint extensionPoint = registry.getExtensionPoint(OVERDONE_CODE_EXTENSION_ID);
				if (extensionPoint == null) {
					AcceleoEcoreGenPlugin.getDefault().log(AcceleoGenMessages.getString("UnresolvedExtensionPoint", new Object[] { OVERDONE_CODE_EXTENSION_ID, }), true); //$NON-NLS-1$
				} else {
					IExtension[] extensions = extensionPoint.getExtensions();
					for (int i = 0; i < extensions.length; i++) {
						IExtension extension = extensions[i];
						IConfigurationElement[] members = extension.getConfigurationElements();
						for (int j = 0; j < members.length; j++) {
							IConfigurationElement member = members[j];
							String theStrategy = member.getAttribute("strategy"); //$NON-NLS-1$
							String theExtension = member.getAttribute("extension"); //$NON-NLS-1$
							Bundle theBundle = Platform.getBundle(member.getNamespace());
							if (theBundle == null) {
								theBundle = AcceleoEcoreGenPlugin.getDefault().getBundle();
							}
							try {
								Class c = theBundle.loadClass(theStrategy);
								Object instance = c.newInstance();
								if (instance instanceof IOverdoneCodeStrategy) {
									overdoneStrategies.put(theExtension, instance);
								}
							} catch (ClassNotFoundException e) {
								AcceleoEcoreGenPlugin.getDefault().log(e, true);
							} catch (InstantiationException e) {
								AcceleoEcoreGenPlugin.getDefault().log(e, true);
							} catch (IllegalAccessException e) {
								AcceleoEcoreGenPlugin.getDefault().log(e, true);
							}
						}
					}
				}
			}
			IOverdoneCodeStrategy strategy = (IOverdoneCodeStrategy) overdoneStrategies.get(file.getFileExtension());
			if (strategy == null) {
				strategy = (IOverdoneCodeStrategy) overdoneStrategies.get("*"); //$NON-NLS-1$
			}
			if (strategy != null) {
				strategy.keepOverdoneCode(file, oldBuffer, newBuffer, lost);
			}
		}
	}

	private static Map overdoneStrategies = null;

}
