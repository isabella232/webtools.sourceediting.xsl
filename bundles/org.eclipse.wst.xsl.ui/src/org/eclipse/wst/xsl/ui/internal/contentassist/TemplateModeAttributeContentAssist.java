/*******************************************************************************
 *Copyright (c) 2008 Standards for Technology in Automotive Retail and others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *Contributors:
 *    David Carver (STAR) - bug 243578  - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xsl.ui.internal.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;
import org.eclipse.wst.xsl.core.XSLCore;
import org.eclipse.wst.xsl.core.model.StylesheetModel;
import org.eclipse.wst.xsl.core.model.Template;
import org.eclipse.wst.xsl.core.model.XSLAttribute;
import org.eclipse.wst.xsl.ui.internal.XSLUIPlugin;
import org.eclipse.wst.xsl.ui.internal.util.XSLPluginImageHelper;
import org.eclipse.wst.xsl.ui.internal.util.XSLPluginImages;
import org.w3c.dom.Node;

/**
 * TemplateModeAttributeContentAssist provides content assistance proposals for
 * <xsl:templates> with a mode attribute.   It looks at all the modes defined
 * within the xsl model, and pulls out any modes that have been defined.
 * @author dcarver
 * @since 1.0
 */
public class TemplateModeAttributeContentAssist extends
		AbstractXSLContentAssistRequest {

	/**
	 * Constructor for creating the TemplateMode Content Assistance class.
	 * 
	 * @param node
	 * @param parent
	 * @param documentRegion
	 * @param completionRegion
	 * @param begin
	 * @param length
	 * @param filter
	 * @param textViewer
	 */
	public TemplateModeAttributeContentAssist(Node node, Node parent,
			IStructuredDocumentRegion documentRegion,
			ITextRegion completionRegion, int begin, int length, String filter,
			ITextViewer textViewer) {
		super(node, parent, documentRegion, completionRegion, begin, length,
				filter, textViewer);
	}

	/**
	 * The main method that returns an array of proposals. Returns the available
	 * modes that have been defined in the {@link StylesheetModel}.  If no proposals
	 * are found it returns a NULL value.
	 * @return ICompletionPropsal[] 
	 * @see org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest#getCompletionProposals()
	 */
	@Override
	public ICompletionProposal[] getCompletionProposals() {
		proposals.clear();
		
		IFile editorFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(getLocation()));
		
		StylesheetModel model = XSLCore.getInstance().getStylesheet(editorFile);

		List<Template> templates = model.getTemplates();
		ArrayList<String> modes = new ArrayList();
		
		for (Template template : templates) {
			XSLAttribute attribute = template.getAttribute("mode");
			IDOMNode xmlNode = (IDOMNode)node;

			if (attribute != null && xmlNode.getStartOffset() != template.getOffset()) {
				CustomCompletionProposal proposal = new CustomCompletionProposal(
						attribute.getValue(), getStartOffset() + 1, 0,
						attribute.getValue().length(), XSLPluginImageHelper
								.getInstance().getImage(
										XSLPluginImages.IMG_MODE), attribute
								.getValue(), null, null, 0);
				if (modes.indexOf(attribute.getValue()) == -1) {
					proposals.add(proposal);
					modes.add(attribute.getValue());
				}
			}
		}
		modes.clear();
		return super.getCompletionProposals();
	}
	
	/**
	 * Retrieves the base location for the IDOMDocument for this class. This is
	 * used to populate a new Path class for retrieving an IFile instance.
	 * @return
	 */
	protected String getLocation() {
		IDOMDocument document = (IDOMDocument) node.getOwnerDocument();
		return document.getModel().getBaseLocation();		
	}

}