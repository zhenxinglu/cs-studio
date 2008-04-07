/* 
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.ams.connector.voicemail.internal.pages;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.csstudio.ams.connector.voicemail.Messages;
import org.csstudio.ams.connector.voicemail.VoicemailConnectorPlugin;
import org.csstudio.ams.connector.voicemail.internal.SampleService;

/**
 * A preference page that contains the following types of preferences:<br>
 * </p>
 * <ul>
 * </ul>
 * 
 * @author
 * 
 */
public class Page1 extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	/**
	 * Standard constructor.
	 */
	public Page1() {
		/*
		 * The usage of the style FieldEditorPreferencePage.GRID makes this
		 * preference page have a grid layout.
		 */
		super(FieldEditorPreferencePage.GRID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void createFieldEditors() {

		addField(new StringFieldEditor(SampleService.P_VM_SERVICE,
				Messages.P_VM_SERVICE, getFieldEditorParent()));
		addField(new StringFieldEditor(SampleService.P_VM_PORT,
				Messages.P_VM_PORT, getFieldEditorParent()));

		adjustGridLayout();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void init(final IWorkbench workbench) {
		/*
		 * This methdos does not necessarily need to do anything. But it must be
		 * implemented anyway.
		 */
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final IPreferenceStore doGetPreferenceStore() {
		return VoicemailConnectorPlugin.getDefault().getPreferenceStore();
	}

}
