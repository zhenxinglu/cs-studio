package org.csstudio.nams.configurator.beans.filters;

import org.csstudio.nams.common.fachwert.MessageKeyEnum;
import org.csstudio.nams.common.fachwert.Millisekunden;
import org.csstudio.nams.common.material.regelwerk.StringRegelOperator;
import org.csstudio.nams.configurator.beans.AbstractConfigurationBean;
import org.csstudio.nams.service.configurationaccess.localstore.internalDTOs.filterConditionSpecifics.TimeBasedType;

public class TimeBasedFilterConditionBean extends
		AbstractConfigurationBean<TimeBasedFilterConditionBean> implements
		FilterConditionAddOnBean {

	public enum PropertyNames {
		cStartKeyValue, sStartOperator, cStartCompValue, 
		cConfirmKeyValue, sConfirmOperator, cConfirmCompValue, 
		sTimePeriod, sTimeBehavior;
	}

	private String cStartKeyValue;
	private StringRegelOperator sStartOperator;
	private String cStartCompValue;

	private MessageKeyEnum cConfirmKeyValue;
	private StringRegelOperator sConfirmOperator;
	private String cConfirmCompValue;

	private Millisekunden sTimePeriodDomainValue;
	private TimeBasedType sTimeBehavior;

	@Override
	protected void doUpdateState(TimeBasedFilterConditionBean bean) {
		setCStartKeyValue(bean.getCStartKeyValue());
		setSStartOperator(bean.getSStartOperator());
		setCStartCompValue(bean.getCStartCompValue());
		
		setCConfirmKeyValue(bean.getCConfirmKeyValue());
		setSConfirmOperator(bean.getSConfirmOperator());
		setCConfirmCompValue(bean.getCConfirmCompValue());
		
		setSTimePeriod(bean.getSTimePeriod());
		setSTimeBehavior(bean.getSTimeBehavior());
	}

	public String getDisplayName() {
		return cStartCompValue + " " + sStartOperator + " " + cStartCompValue
				+ " " + cConfirmKeyValue + " " + sConfirmOperator + " "
				+ cConfirmCompValue + " " + sTimePeriodDomainValue + " " + sTimeBehavior;
	}

	public int getID() {
		return 0;
	}

	public String getCStartKeyValue() {
		return cStartKeyValue;
	}

	public void setCStartKeyValue(String startKeyValue) {
		String oldValue = cStartKeyValue;
		cStartKeyValue = startKeyValue;
		pcs.firePropertyChange(PropertyNames.cStartKeyValue.name(), oldValue, startKeyValue);
	}

	public StringRegelOperator getSStartOperator() {
		return sStartOperator;
	}

	public void setSStartOperator(StringRegelOperator stringRegelOperator) {
		StringRegelOperator oldValue = stringRegelOperator;
		sStartOperator = stringRegelOperator;
		pcs.firePropertyChange(PropertyNames.sStartOperator.name(), oldValue, stringRegelOperator);
	}

	public String getCStartCompValue() {
		return cStartCompValue;
	}

	public void setCStartCompValue(String startCompValue) {
		String oldValue = cStartCompValue;
		cStartCompValue = startCompValue;
		pcs.firePropertyChange(PropertyNames.cStartCompValue.name(), oldValue, startCompValue);
	}

	public MessageKeyEnum getCConfirmKeyValue() {
		return cConfirmKeyValue;
	}

	public void setCConfirmKeyValue(MessageKeyEnum messageKeyEnum) {
		MessageKeyEnum oldValue = messageKeyEnum;
		cConfirmKeyValue = messageKeyEnum;
		pcs.firePropertyChange(PropertyNames.cConfirmKeyValue.name(), oldValue, messageKeyEnum);
	}

	public StringRegelOperator getSConfirmOperator() {
		return sConfirmOperator;
	}

	public void setSConfirmOperator(StringRegelOperator stringRegelOperator) {
		StringRegelOperator oldValue = sConfirmOperator;
		sConfirmOperator = stringRegelOperator;
		pcs.firePropertyChange(PropertyNames.sConfirmOperator.name(), oldValue, stringRegelOperator);
		}

	public String getCConfirmCompValue() {
		return cConfirmCompValue;
	}

	public void setCConfirmCompValue(String confirmCompValue) {
		String oldValue = cConfirmCompValue;
		cConfirmCompValue = confirmCompValue;
		pcs.firePropertyChange(PropertyNames.cConfirmCompValue.name(), oldValue, confirmCompValue);
	}

	public Millisekunden getSTimePeriod() {
		return sTimePeriodDomainValue;
	}

	public void setSTimePeriod(Millisekunden millisekunden) {
		Millisekunden oldValue = sTimePeriodDomainValue;
		if (oldValue == null) oldValue = Millisekunden.valueOf(0);
		sTimePeriodDomainValue = millisekunden;
		pcs.firePropertyChange(PropertyNames.sTimePeriod.name(), oldValue.alsLongVonMillisekunden(), millisekunden.alsLongVonMillisekunden());
	}
	
	public TimeBasedType getSTimeBehavior() {
		return sTimeBehavior;
	}

	public void setSTimeBehavior(TimeBasedType timeBasedType) {
		TimeBasedType oldValue = sTimeBehavior;
		sTimeBehavior = timeBasedType;
		pcs.firePropertyChange(PropertyNames.sTimeBehavior.name(), oldValue, timeBasedType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((cConfirmCompValue == null) ? 0 : cConfirmCompValue
						.hashCode());
		result = prime
				* result
				+ ((cConfirmKeyValue == null) ? 0 : cConfirmKeyValue.hashCode());
		result = prime * result
				+ ((cStartCompValue == null) ? 0 : cStartCompValue.hashCode());
		result = prime * result
				+ ((cStartKeyValue == null) ? 0 : cStartKeyValue.hashCode());
		result = prime
				* result
				+ ((sConfirmOperator == null) ? 0 : sConfirmOperator.hashCode());
		result = prime * result
				+ ((sStartOperator == null) ? 0 : sStartOperator.hashCode());
		result = prime * result
				+ ((sTimeBehavior == null) ? 0 : sTimeBehavior.hashCode());
		result = prime * result
				+ ((sTimePeriodDomainValue == null) ? 0 : sTimePeriodDomainValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TimeBasedFilterConditionBean other = (TimeBasedFilterConditionBean) obj;
		if (cConfirmCompValue == null) {
			if (other.cConfirmCompValue != null)
				return false;
		} else if (!cConfirmCompValue.equals(other.cConfirmCompValue))
			return false;
		if (cConfirmKeyValue == null) {
			if (other.cConfirmKeyValue != null)
				return false;
		} else if (!cConfirmKeyValue.equals(other.cConfirmKeyValue))
			return false;
		if (cStartCompValue == null) {
			if (other.cStartCompValue != null)
				return false;
		} else if (!cStartCompValue.equals(other.cStartCompValue))
			return false;
		if (cStartKeyValue == null) {
			if (other.cStartKeyValue != null)
				return false;
		} else if (!cStartKeyValue.equals(other.cStartKeyValue))
			return false;
		if (sConfirmOperator == null) {
			if (other.sConfirmOperator != null)
				return false;
		} else if (!sConfirmOperator.equals(other.sConfirmOperator))
			return false;
		if (sStartOperator == null) {
			if (other.sStartOperator != null)
				return false;
		} else if (!sStartOperator.equals(other.sStartOperator))
			return false;
		if (sTimeBehavior == null) {
			if (other.sTimeBehavior != null)
				return false;
		} else if (!sTimeBehavior.equals(other.sTimeBehavior))
			return false;
		if (sTimePeriodDomainValue == null) {
			if (other.sTimePeriodDomainValue != null)
				return false;
		} else if (!sTimePeriodDomainValue.equals(other.sTimePeriodDomainValue))
			return false;
		return true;
	}

	public void setID(int id) {
	}

}
