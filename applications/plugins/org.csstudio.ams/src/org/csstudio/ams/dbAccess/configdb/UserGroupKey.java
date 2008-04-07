/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
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
 package org.csstudio.ams.dbAccess.configdb;

import org.csstudio.ams.dbAccess.Key;

public class UserGroupKey extends Key
{
	private static final long serialVersionUID = -6015323159737509596L;
	
	public int userGroupID;
	public String userGroupName;
	public int groupRef;
	
	public UserGroupKey()
	{
		this(-1,"",-1);
	}
	
	public UserGroupKey(int id, String name, int groupRef)
	{
		super(Key.USERGROUP_KEY);
		this.userGroupID = id;
		this.userGroupName = name;
		this.groupRef = groupRef;
	}
	
	
	public String toString()
	{
		return userGroupName == null ? "" : userGroupName;
	}
	
	public int hashCode()
	{
		return (userGroupID + " " + userGroupName).hashCode();
	}
	
	public boolean equals(Object obj)
	{
		if(obj instanceof UserGroupKey)
			return ((UserGroupKey)obj).userGroupID == userGroupID;
		return false;
	}
	
	public int getID()
	{
		return userGroupID;
	}
	
	public int getGroupRef()
	{
		return groupRef;
	}
	
	public void setGroupRef(int groupRef)
	{
		this.groupRef = groupRef;
	}
}
