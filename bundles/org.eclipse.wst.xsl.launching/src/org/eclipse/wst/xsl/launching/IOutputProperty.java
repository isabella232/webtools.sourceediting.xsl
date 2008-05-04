/*******************************************************************************
 * Copyright (c) 2007 Chase Technology Ltd - http://www.chasetechnology.co.uk
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Doug Satchwell (Chase Technology Ltd) - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xsl.launching;

/**
 * An output property supported by an XSLT processor. 
 * 
 * @author Doug Satchwell
 */
public interface IOutputProperty
{
	/**
	 * Get the URI of this output property. 
	 * @return a unique URI
	 */
	String getURI();

	/**
	 * Get a description for this output property. 
	 * @return a description
	 */
	String getDescription();
}