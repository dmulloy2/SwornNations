package com.massivecraft.factions.cmd;

import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;

public class CmdBypass extends FCommand
{
	public CmdBypass()
	{
		super();
		this.aliases.add("bypass");

		this.optionalArgs.put("on/off", "flip");

		this.permission = Permission.BYPASS.node;
		this.disableOnLock = false;

		senderMustBePlayer = true;
		senderMustBeMember = false;
		senderMustBeModerator = false;
		senderMustBeAdmin = false;
	}

	@Override
	public void perform()
	{
		fme.setIsAdminBypassing(argAsBool(0, ! fme.isAdminBypassing()));

		if (fme.isAdminBypassing())
		{
			fme.msg("<i>You have enabled admin bypass mode. You will be able to build or destroy anywhere.");
			P.p.log(fme.getName() + " has ENABLED admin bypass mode.");
		}
		else
		{
			fme.msg("<i>You have disabled admin bypass mode.");
			P.p.log(fme.getName() + " DISABLED admin bypass mode.");
		}
	}
}