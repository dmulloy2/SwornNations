package com.massivecraft.factions.integration;

import java.util.logging.Level;

import net.dmulloy2.swornnations.SwornNations;
import net.ess3.api.IEssentials;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.massivecraft.factions.Conf;

/**
 * This interface handles Essentials Teleportation
 */
public class EssentialsFeatures
{
	private static IEssentials essentials;

	/**
	 * Integrate main Essentials plugin
	 */
	public static void setup()
	{
		if (essentials == null)
		{
			PluginManager pm = Bukkit.getPluginManager();
			if (pm.isPluginEnabled("Essentials"))
			{
				Plugin essPlugin = pm.getPlugin("Essentials");
				essentials = (IEssentials) essPlugin;

				SwornNations.get().log("Essentials integration successful!");
			}
			else
			{
				SwornNations.get().log("Essentials could not be found. Using backup Teleportation.");
			}
		}
	}

	/**
	 * Handles Essentials Teleportation
	 * 
	 * @param player
	 *            - {@link Player} to teleport
	 * @param loc
	 *            - {@link Location} to teleport to
	 * @return Whether or not the teleportation was successful
	 */
	public static boolean handleTeleport(Player player, Location loc)
	{
		try
		{
			if (!Conf.homesTeleportCommandEssentialsIntegration || essentials == null)
				return false;

			User user = essentials.getUser(player);
			Teleport teleport = user.getTeleport();
			Trade chargeFor = new Trade("fhome", essentials);

			teleport.teleport(loc, chargeFor, TeleportCause.COMMAND);
			return true;
		}
		catch (Exception e)
		{
			player.sendMessage(ChatColor.RED + "Could not teleport using Essentials: " + e.getMessage());
			SwornNations.get().log(Level.WARNING, "Could not teleport player %s using Essentials: %s", player.getName(), e);
			if (Conf.debug)
			{
				e.printStackTrace();
			}
			else
			{
				SwornNations.get().log(Level.WARNING, "To see full stack trace, use \"f config debug true\"");
			}

			return false;
		}
	}
}