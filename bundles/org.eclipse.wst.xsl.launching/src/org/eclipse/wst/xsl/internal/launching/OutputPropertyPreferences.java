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
package org.eclipse.wst.xsl.internal.launching;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OutputPropertyPreferences
{
	private final Map<String, Properties> typeProperties = new HashMap<String, Properties>();

	public Properties getOutputPropertyValues(String typeId)
	{
		return (Properties) typeProperties.get(typeId);
	}

	public void setOutputPropertyValues(String typeId, Properties properties)
	{
		typeProperties.put(typeId, properties);
	}

	public String getAsXML() throws ParserConfigurationException, IOException, TransformerException
	{
		Document doc = PreferenceUtil.getDocument();
		Element config = doc.createElement("outputPropertyPreferences"); //$NON-NLS-1$
		doc.appendChild(config);

		for (String type : typeProperties.keySet())
		{
			Element processorTypeElement = typeAsElement(doc, type);
			Properties propertyValues = (Properties) typeProperties.get(type);
			featureValuesAsElement(doc, processorTypeElement, propertyValues);
			config.appendChild(processorTypeElement);
		}

		// Serialize the Document and return the resulting String
		return PreferenceUtil.serializeDocument(doc);
	}

	public static OutputPropertyPreferences fromXML(InputStream inputStream) throws CoreException
	{
		OutputPropertyPreferences prefs = new OutputPropertyPreferences();

		// Do the parsing and obtain the top-level node
		Document doc = PreferenceUtil.getDocument(inputStream);
		Element config = doc.getDocumentElement();

		Element[] processorTypeEls = PreferenceUtil.getChildElements(config, "processorType"); //$NON-NLS-1$
		for (int i = 0; i < processorTypeEls.length; ++i)
		{
			Element processorTypeEl = processorTypeEls[i];
			String type = elementAsType(processorTypeEl);
			Properties featureValues = elementAsPropertyValues(processorTypeEl);
			prefs.setOutputPropertyValues(type, featureValues);
		}

		return prefs;
	}

	private static String elementAsType(Element parent)
	{
		String id = parent.getAttribute("id"); //$NON-NLS-1$
		return id;
	}

	private static Element typeAsElement(Document doc, String type)
	{
		Element element = doc.createElement("processorType"); //$NON-NLS-1$
		element.setAttribute("id", type); //$NON-NLS-1$
		return element;
	}

	private static Properties elementAsPropertyValues(Element element)
	{
		Element[] propertyEls = PreferenceUtil.getChildElements(element, "property"); //$NON-NLS-1$
		Properties propertyValues = new Properties();
		for (Element featureEl : propertyEls)
		{
			String name = featureEl.getAttribute("name"); //$NON-NLS-1$
			String value = featureEl.getAttribute("value"); //$NON-NLS-1$
			propertyValues.put(name, value);
		}
		return propertyValues;
	}

	private static void featureValuesAsElement(Document doc, Element featuresEl, Properties propertyValues)
	{
		if (propertyValues != null)
		{
			for (Map.Entry<Object,Object> entry2 : propertyValues.entrySet())
			{
				String name = (String) entry2.getKey();
				String value = (String) entry2.getValue();
				Element element = doc.createElement("property"); //$NON-NLS-1$
				element.setAttribute("name", name); //$NON-NLS-1$
				element.setAttribute("value", value); //$NON-NLS-1$
				featuresEl.appendChild(element);
			}
		}
	}
}