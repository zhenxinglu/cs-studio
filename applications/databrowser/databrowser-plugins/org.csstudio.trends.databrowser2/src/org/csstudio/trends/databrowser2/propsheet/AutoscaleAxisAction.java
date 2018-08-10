package org.csstudio.trends.databrowser2.propsheet;

import org.csstudio.trends.databrowser2.Messages;
import org.csstudio.trends.databrowser2.model.AxisConfig;
import org.csstudio.trends.databrowser2.model.Model;
import org.eclipse.jface.action.Action;

public class AutoscaleAxisAction extends Action
{
    private AxisConfig axis_config;

    public AutoscaleAxisAction(final Model model,
            final Integer axis_index)
    {
        super(Messages.AutoScale, Action.AS_CHECK_BOX);
        this.axis_config = model.getAxis(axis_index);
        this.setChecked(axis_config.isAutoScale());
    }

    @Override
    public void run()
    {
        axis_config.setAutoScale(!axis_config.isAutoScale());
        this.setChecked(axis_config.isAutoScale());
    }
}
