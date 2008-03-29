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
package org.eclipse.wst.xsl.core.internal.validation;

import org.eclipse.wst.xml.core.internal.validation.core.ValidationMessage;
import org.eclipse.wst.xsl.core.internal.model.XSLNode;

public class XSLValidationMessage extends ValidationMessage
{
	private XSLNode node;

	public XSLValidationMessage(String message, int lineNumber, int columnNumber, String string)
	{
		super(message, lineNumber, columnNumber, string);
	}
	
	public void setNode(XSLNode node)
	{
		this.node = node;
	}

	public XSLNode getNode()
	{
		return node;
	}
}