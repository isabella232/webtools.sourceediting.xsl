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
package org.eclipse.wst.xsl.core.internal.model;

/**
 * TODO: Add JavaDoc
 * @author Doug Satchwell
 */
public class Parameter extends Variable
{
	boolean value;

	public Parameter(SourceFile sf, String name, String select)
	{
		super(sf,name,select);
	}

	public boolean isValue()
	{
		return select != null;
	}
	
	@Override
	public String toString() {
		return "Parameter "+super.toString()+" name="+name+", select="+select;
	}
}