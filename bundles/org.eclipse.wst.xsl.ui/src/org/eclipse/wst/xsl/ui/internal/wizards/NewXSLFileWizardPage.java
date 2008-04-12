/*******************************************************************************
 * Copyright (c) 2008 Chase Technology Ltd - http://www.chasetechnology.co.uk
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Doug Satchwell (Chase Technology Ltd) - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xsl.ui.internal.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.wst.xsl.core.XSLCore;

class NewXSLFileWizardPage extends WizardNewFileCreationPage
{
	private IContentType fContentType;
	private List fValidExtensions = null;

	public NewXSLFileWizardPage(String pageName, IStructuredSelection selection)
	{
		super(pageName, selection);
		setFileName("NewFile.xsl");
	}

	protected boolean validatePage()
	{
		setMessage(null);
		setErrorMessage(null);

		if (!super.validatePage())
		{
			return false;
		}

		String fileName = getFileName();
		IPath fullPath = getContainerFullPath();
		if ((fullPath != null) && (fullPath.isEmpty() == false) && (fileName != null))
		{
			// check that filename does not contain invalid extension
			if (!extensionValidForContentType(fileName))
			{
				setErrorMessage(NLS.bind("The file name must end in one of the following extensions {0}.", getValidExtensions().toString()));
				return false;
			}
			// no file extension specified so check adding default
			// extension doesn't equal a file that already exists
			if (fileName.lastIndexOf('.') == -1)
			{
				String newFileName = addDefaultExtension(fileName);
				IPath resourcePath = fullPath.append(newFileName);

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IStatus result = workspace.validatePath(resourcePath.toString(), IResource.FOLDER);
				if (!result.isOK())
				{
					// path invalid
					setErrorMessage(result.getMessage());
					return false;
				}

				if ((workspace.getRoot().getFolder(resourcePath).exists() || workspace.getRoot().getFile(resourcePath).exists()))
				{
					setErrorMessage("The same name already exists.");
					return false;
				}
			}
		}
		return true;
	}

	String addDefaultExtension(String filename)
	{
		StringBuffer newFileName = new StringBuffer(filename);

		// Preferences preference =
		// XSLUIPlugin.getDefault().getPluginPreferences();
		// String ext =
		// preference.getString(JSPCorePreferenceNames.DEFAULT_EXTENSION);

		String ext = "xsl";
		newFileName.append(".");
		newFileName.append(ext);

		return newFileName.toString();
	}

	/**
	 * Get content type associated with this new file wizard
	 * 
	 * @return IContentType
	 */
	private IContentType getContentType()
	{
		if (fContentType == null)
			fContentType = Platform.getContentTypeManager().getContentType(XSLCore.XSL_CONTENT_TYPE);
		return fContentType;
	}

	private List getValidExtensions()
	{
		if (fValidExtensions == null)
		{
			IContentType type = getContentType();
			fValidExtensions = new ArrayList(Arrays.asList(type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC)));
		}
		return fValidExtensions;
	}

	private boolean extensionValidForContentType(String fileName)
	{
		boolean valid = false;

		IContentType type = getContentType();
		// there is currently an extension
		if (fileName.lastIndexOf('.') != -1)
		{
			// check what content types are associated with current extension
			IContentType[] types = Platform.getContentTypeManager().findContentTypesFor(fileName);
			int i = 0;
			while (i < types.length && !valid)
			{
				valid = types[i].isKindOf(type);
				++i;
			}
		}
		else
			valid = true; // no extension so valid
		return valid;
	}
}