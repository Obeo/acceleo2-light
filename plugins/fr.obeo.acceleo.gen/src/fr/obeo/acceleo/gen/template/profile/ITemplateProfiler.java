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
package fr.obeo.acceleo.gen.template.profile;

import java.io.IOException;

/**
 * Interface for a template evaluation profiler.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 * 
 */
public interface ITemplateProfiler {
	/**
	 * Reset the statistics of the profiler.
	 */
	public void reset();

	/**
	 * Start monitoring the given object.
	 * 
	 * @param monitored
	 *            the object to monitor
	 */
	public void start(Object monitored);

	/**
	 * Stop monitoring the current object.
	 */
	public void stop();

	/**
	 * Set the given element as a loop element of the current monitored element.
	 * 
	 * @param loopElement
	 *            the current loop element to consider.
	 */
	public void loop(Object loopElement);

	/**
	 * Set loop on the current monitored element without tracking the the
	 * element.
	 * 
	 * @param loopElement
	 *            the current loop element to consider.
	 */
	public void loop();

	/**
	 * Save profiling results to the given URI.
	 * 
	 * @param modelURI
	 *            the URI where to save results.
	 */
	public void save(String modelURI) throws IOException;
}
