package com.massivecraft.factions.cmd;

import net.dmulloy2.swornnations.SwornNations;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;

public class CmdMoneyDeposit extends FCommand
{

	public CmdMoneyDeposit()
	{
		super();
		this.aliases.add("d");
		this.aliases.add("deposit");

		this.requiredArgs.add("amount");
		this.optionalArgs.put("faction", "yours");

		this.permission = Permission.MONEY_DEPOSIT.node;
		this.setHelpShort("deposit money");

		senderMustBePlayer = true;
		senderMustBeMember = false;
		senderMustBeModerator = false;
		senderMustBeAdmin = false;
	}

	@Override
	public void perform()
	{
		double amount = this.argAsDouble(0, 0d);
		EconomyParticipator faction = this.argAsFaction(1, myFaction);
		if (faction == null)
			return;
		boolean success = Econ.transferMoney(fme, fme, faction, amount);

		if (success && Conf.logMoneyTransactions)
			SwornNations.get().log(ChatColor.stripColor(SwornNations.get().txt.parse("%s deposited %s in the faction bank: %s", fme.getName(), Econ.moneyString(amount),
					faction.describeTo(null))));
	}

}
