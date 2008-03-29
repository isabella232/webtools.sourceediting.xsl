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
 * $Id: ElemWhen.java,v 1.3 2008/03/28 02:38:15 dacarver Exp $
 */
package org.eclipse.wst.xsl.core.internal.compiler.xslt10.templates;

import org.eclipse.wst.xsl.core.internal.compiler.xslt10.xpath.XPath;

/**
 * Implement xsl:when.
 * 
 * <pre>
 * &lt;!ELEMENT xsl:when %template;&gt;
 * &lt;!ATTLIST xsl:when
 *   test %expr; #REQUIRED
 *   %space-att;
 * &gt;
 * </pre>
 * 
 * @see <a
 *      href="http://www.w3.org/TR/xslt#section-Conditional-Processing-with-xsl:choose">XXX
 *      in XSLT Specification</a>
 * @xsl.usage advanced
 */
public class ElemWhen extends ElemTemplateElement {
	static final long serialVersionUID = 5984065730262071360L;

	/**
	 * Each xsl:when element has a single attribute, test, which specifies an
	 * expression.
	 * 
	 * @serial
	 */
	private XPath m_test;

	/**
	 * Set the "test" attribute. Each xsl:when element has a single attribute,
	 * test, which specifies an expression.
	 * 
	 * @param v
	 *            Value to set for the "test" attribute.
	 */
	public void setTest(XPath v) {
		m_test = v;
	}

	/**
	 * Get the "test" attribute. Each xsl:when element has a single attribute,
	 * test, which specifies an expression.
	 * 
	 * @return Value of the "test" attribute.
	 */
	public XPath getTest() {
		return m_test;
	}

	/**
	 * Get an integer representation of the element type.
	 * 
	 * @return An integer representation of the element, defined in the
	 *         Constants class.
	 * @see org.apache.xalan.templates.Constants
	 */
	@Override
	public int getXSLToken() {
		return Constants.ELEMNAME_WHEN;
	}

	/**
	 * This function is called after everything else has been recomposed, and
	 * allows the template to set remaining values that may be based on some
	 * other property that depends on recomposition.
	 */
	@Override
	public void compose(StylesheetRoot sroot)
			throws javax.xml.transform.TransformerException {
		super.compose(sroot);
		java.util.Vector vnames = sroot.getComposeState().getVariableNames();
		if (null != m_test)
			m_test.fixupVariables(vnames, sroot.getComposeState()
					.getGlobalsSize());
	}

	/**
	 * Return the node name.
	 * 
	 * @return The node name
	 */
	@Override
	public String getNodeName() {
		return Constants.ELEMNAME_WHEN_STRING;
	}

	/**
	 * Constructor ElemWhen
	 * 
	 */
	public ElemWhen() {
	}

	/**
	 * Call the children visitors.
	 * 
	 * @param visitor
	 *            The visitor whose appropriate method will be called.
	 */
	@Override
	protected void callChildVisitors(XSLTVisitor visitor, boolean callAttrs) {
		if (callAttrs)
			m_test.getExpression().callVisitors(m_test, visitor);
		super.callChildVisitors(visitor, callAttrs);
	}

}