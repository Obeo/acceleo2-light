/*
 * Copyright (c) 2005-2010 Obeo
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

import java.lang.reflect.Method;

/**
 * 
 * An external service
 * @author Guillaume Gebhart<a
 *         href="mailto:guillaume.gebhart@obeo.fr">guillaume.gebhart@obeo.fr</a>
 * 
 */
public interface IExternalJavaService {

	/**
	 * defines the methods deprecated in this service
	 * @return all methods deprecated
	 */
	public Method[] getDeprecatedMethods();
}
