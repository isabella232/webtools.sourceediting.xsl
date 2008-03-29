/*******************************************************************************
 * Copyright (c) 2008 Standards for Technology in Automotive Retail
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Carver - STAR - bug 224197 - initial API and implementation
 *                    based on work from Apache Xalan 2.7.0
 *******************************************************************************/

/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: ProcessorTemplateElem.java,v 1.2 2008/03/28 02:38:16 dacarver Exp $
 */
package org.eclipse.wst.xsl.core.internal.compiler.xslt10.processor;

import javax.xml.transform.TransformerException;

import org.apache.xalan.res.XSLTErrorResources;
import org.eclipse.wst.xsl.core.internal.compiler.xslt10.templates.ElemTemplateElement;

import org.xml.sax.Attributes;

/**
 * This class processes parse events for an XSLT template element.
 * 
 * @see <a href="http://www.w3.org/TR/xslt#dtd">XSLT DTD</a>
 * @see <a
 *      href="http://www.w3.org/TR/xslt#section-Creating-the-Result-Tree">section-Creating-the-Result-Tree
 *      in XSLT Specification</a>
 */
public class ProcessorTemplateElem extends XSLTElementProcessor {
	static final long serialVersionUID = 8344994001943407235L;

	/**
	 * Receive notification of the start of an element.
	 * 
	 * @param handler
	 *            non-null reference to current StylesheetHandler that is
	 *            constructing the Templates.
	 * @param uri
	 *            The Namespace URI, or an empty string.
	 * @param localName
	 *            The local name (without prefix), or empty string if not
	 *            namespace processing.
	 * @param rawName
	 *            The qualified name (with prefix).
	 * @param attributes
	 *            The specified or defaulted attributes.
	 */
	@Override
	public void startElement(StylesheetHandler handler, String uri,
			String localName, String rawName, Attributes attributes)
			throws org.xml.sax.SAXException {

		super.startElement(handler, uri, localName, rawName, attributes);
		try {
			// ElemTemplateElement parent = handler.getElemTemplateElement();
			XSLTElementDef def = getElemDef();
			Class classObject = def.getClassObject();
			ElemTemplateElement elem = null;

			try {
				elem = (ElemTemplateElement) classObject.newInstance();

				elem.setDOMBackPointer(handler.getOriginatingNode());
				elem.setLocaterInfo(handler.getLocator());
				elem.setPrefixes(handler.getNamespaceSupport());
			} catch (InstantiationException ie) {
				handler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMTMPL,
						null, ie);// "Failed creating ElemTemplateElement
									// instance!", ie);
			} catch (IllegalAccessException iae) {
				handler.error(XSLTErrorResources.ER_FAILED_CREATING_ELEMTMPL,
						null, iae);// "Failed creating ElemTemplateElement
									// instance!", iae);
			}

			setPropertiesFromAttributes(handler, rawName, attributes, elem);
			appendAndPush(handler, elem);
		} catch (TransformerException te) {
			throw new org.xml.sax.SAXException(te);
		}
	}

	/**
	 * Append the current template element to the current template element, and
	 * then push it onto the current template element stack.
	 * 
	 * @param handler
	 *            non-null reference to current StylesheetHandler that is
	 *            constructing the Templates.
	 * @param elem
	 *            non-null reference to a the current template element.
	 * 
	 * @throws org.xml.sax.SAXException
	 *             Any SAX exception, possibly wrapping another exception.
	 */
	protected void appendAndPush(StylesheetHandler handler,
			ElemTemplateElement elem) throws org.xml.sax.SAXException {

		ElemTemplateElement parent = handler.getElemTemplateElement();
		if (null != parent) // defensive, for better multiple error reporting.
							// -sb
		{
			parent.appendChild(elem);
			handler.pushElemTemplateElement(elem);
		}
	}

	/**
	 * Receive notification of the end of an element.
	 * 
	 * @param handler
	 *            non-null reference to current StylesheetHandler that is
	 *            constructing the Templates.
	 * @param uri
	 *            The Namespace URI, or an empty string.
	 * @param localName
	 *            The local name (without prefix), or empty string if not
	 *            namespace processing.
	 * @param rawName
	 *            The qualified name (with prefix).
	 */
	@Override
	public void endElement(StylesheetHandler handler, String uri,
			String localName, String rawName) throws org.xml.sax.SAXException {
		super.endElement(handler, uri, localName, rawName);
		handler.popElemTemplateElement()
				.setEndLocaterInfo(handler.getLocator());
	}
}