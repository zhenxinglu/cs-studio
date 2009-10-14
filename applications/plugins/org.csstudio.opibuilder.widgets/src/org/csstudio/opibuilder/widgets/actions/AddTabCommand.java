package org.csstudio.opibuilder.widgets.actions;

import org.csstudio.opibuilder.widgets.editparts.TabEditPart;
import org.eclipse.gef.commands.Command;

/**The command which add a tab to the tab widget.
 * @author Xihui Chen
 *
 */
public class AddTabCommand extends Command {
	private int tabIndex;
	private TabEditPart tabEditPart;
	
	public AddTabCommand(TabEditPart tabEditPart, boolean before) {
		this.tabEditPart = tabEditPart;
		if(before)
			this.tabIndex = Math.max(0, tabEditPart.getActiveTabIndex()-1);
		else
			this.tabIndex = tabEditPart.getActiveTabIndex()+1;
	}
	
	@Override
	public void execute() {
		tabEditPart.addTab(tabIndex);
	}
	
	@Override
	public void undo() {
		tabEditPart.removeTab(tabIndex);
	}
	
	
	
	
	
}
