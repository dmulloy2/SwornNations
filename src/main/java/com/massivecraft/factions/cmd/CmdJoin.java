package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;

public class CmdJoin extends FCommand
{
	public CmdJoin()
	{
		super();
		this.aliases.add("join");
		
		this.requiredArgs.add("faction name");
		this.optionalArgs.put("player", "you");
		
		this.permission = Permission.JOIN.node;
		this.disableOnLock = true;
		
		senderMustBePlayer = true;
		senderMustBeMember = false;
		senderMustBeModerator = false;
		senderMustBeAdmin = false;
	}
	
	@Override
	public void perform()
	{
		Faction faction = this.argAsFaction(0);
		if (faction == null) return;

		FPlayer fplayer = this.argAsBestFPlayerMatch(1, fme, false);
		boolean samePlayer = fplayer == fme;

		if (!samePlayer  && ! Permission.JOIN_OTHERS.has(sender, false))
		{
			msg("<b>You do not have permission to move other players into a faction.");
			return;
		}

		if ( ! faction.isNormal())
		{
			msg("<b>Players may only join normal factions. This is a system faction.");
			return;
		}

		if (faction == fplayer.getFaction())
		{
			msg("<b>%s %s already a member of %s", fplayer.describeTo(fme, true), (samePlayer ? "are" : "is"), faction.getTag(fme));
			return;
		}

		if (Conf.factionMemberLimit > 0 && faction.getFPlayers().size() >= Conf.factionMemberLimit)
		{
			msg("<b>The faction %s <b>is at the limit of %d <b>members, so %s <b>annot currently join.", faction.getTag(fme), Conf.factionMemberLimit, fplayer.describeTo(fme, false));
			return;
		}

		if (fplayer.hasFaction())
		{
			msg("<b>%s must leave %s <b>current faction first.", fplayer.describeTo(fme, true), (samePlayer ? "your" : "their"));
			return;
		}

		if (!Conf.canLeaveWithNegativePower && fplayer.getPower() < 0)
		{
			msg("%s <b>cannot join a faction with a negative power level.", fplayer.describeTo(fme, true));
			return;
		}

		if( ! (faction.getOpen() || faction.isInvited(fplayer) || fme.isAdminBypassing() || Permission.JOIN_ANY.has(sender, false)))
		{
			msg("<i>This faction requires invitation.");
			if (samePlayer)
				faction.msg("%s<i> tried to join your faction.", fplayer.describeTo(faction, true));
			return;
		}
		
		if (!faction.isConfirmed(fme) && !fme.isAdminBypassing())
		{
			msg("<i>You must be confirmed to join this faction!");
			msg("<i>Your leader must: " + p.cmdBase.cmdConfirm.getUseageTemplate(false));
			return;
		}

		// if economy is enabled, they're not on the bypass list, and this command has a cost set, make sure they can pay
		if (samePlayer && ! canAffordCommand(Conf.econCostJoin, "to join a faction")) return;

		// trigger the join event (cancellable)
		FPlayerJoinEvent joinEvent = new FPlayerJoinEvent(fplayer, faction, FPlayerJoinEvent.PlayerJoinReason.COMMAND);
		Bukkit.getServer().getPluginManager().callEvent(joinEvent);
		if (joinEvent.isCancelled()) return;

		// then make 'em pay (if applicable)
		if (samePlayer && ! payForCommand(Conf.econCostJoin, "to join a faction", "for joining a faction")) return;
		

		fme.msg("%s <i>successfully joined %s<i>.", fplayer.describeTo(fme, true), faction.getTag(fme));

		if (!samePlayer)
		{
			fplayer.msg("%s <i>moved you into the faction %s<i>.", fme.describeTo(fplayer, true), faction.getTag(fplayer));
		}
		faction.msg("%s <i>joined your faction.", fplayer.describeTo(faction, true));
		
		faction.join(fplayer);
		
		if (samePlayer && ! fme.isAdminBypassing())
		{
			fplayer.setRole(Role.INITIATE);
			faction.msg("%s<i> has been set to \"Initiate\". This rank does not allow the placing of TNT or access to public chests.", fplayer.getName());
			faction.msg("<i>This status can be removed using: " + P.p.cmdBase.cmdInitiate.getUseageTemplate(false) + "<i>.");
				
			fplayer.setTitle("~Initiate~");
		}
		
		if (Conf.logFactionJoin)
		{
			if (samePlayer)
				P.p.log("%s joined the faction %s.", fplayer.getName(), faction.getTag());
			else
				P.p.log("%s moved the player %s into the faction %s.", fme.getName(), fplayer.getName(), faction.getTag());
		}
	}
}