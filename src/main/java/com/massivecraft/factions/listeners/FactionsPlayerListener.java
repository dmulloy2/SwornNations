package com.massivecraft.factions.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import me.t7seven7t.factions.util.MyMaterial;
import me.t7seven7t.swornnations.npermissions.NPermission;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;

public class FactionsPlayerListener implements Listener
{
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		// Make sure that all online players do have a fplayer.
		final Player player = event.getPlayer();
		final FPlayer me = FPlayers.i.get(player);

		// Update the lastLoginTime for this fplayer
		me.setLastLoginTime(System.currentTimeMillis());

		handlePlayerMove(player, player.getLocation(), null, false);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		onPlayerDisconnect(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerKick(PlayerKickEvent event)
	{
		if (! event.isCancelled())
		{
			onPlayerDisconnect(event.getPlayer());
		}
	}

	public void onPlayerDisconnect(Player player)
	{
		FPlayer me = FPlayers.i.get(player);

		// Make sure player's power is up to date when they log off.
		me.getPower();

		// Update their last login time to point to when the logged off, for
		// auto-remove routine
		me.setLastLoginTime(System.currentTimeMillis());

		Faction myFaction = me.getFaction();
		if (myFaction != null)
		{
			myFaction.memberLoggedOff();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (! event.isCancelled())
		{
			handlePlayerMove(event.getPlayer(), event.getTo(), event.getFrom(), true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (! event.isCancelled())
		{
			handlePlayerMove(event.getPlayer(), event.getTo(), event.getFrom(), false);
		}
	}

	public void handlePlayerMove(Player player, Location locationTo, Location locationFrom, boolean moveEvent)
	{
		if (moveEvent)
		{
			if (locationFrom.getBlockX() >> 4 == locationTo.getBlockX() >> 4 && locationFrom.getBlockZ() >> 4 == locationTo.getBlockZ() >> 4
					&& locationFrom.getWorld() == locationTo.getWorld())
				return;
		}

		FPlayer me = FPlayers.i.get(player);

		// Did we change coord?
		FLocation from = me.getLastStoodAt();
		FLocation to = new FLocation(locationTo);

		// No, return.
		if (from.yequals(to))
			return;

		// Yes, continue
		me.setLastStoodAt(to);

		// Did we change "host" faction?
		Faction factionFrom = Board.getFactionAt(from);
		Faction factionTo = Board.getFactionAt(to);
		boolean changedFaction = (factionFrom != factionTo);

		// Yes
		if (changedFaction)
		{
			me.sendFactionHereMessage();

			// Map update
			if (me.isMapAutoUpdating())
			{
				me.sendMessage(Board.getMap(me.getFaction(), to, player.getLocation().getYaw()));
			}

			if (! moveEvent)
			{
				if (me.getAutoClaimFor() != null)
				{
					me.setAutoClaimFor(null);
				}
				else if (me.isAutoSafeClaimEnabled())
				{
					me.setIsAutoSafeClaimEnabled(false);
				}
				else if (me.isAutoWarClaimEnabled())
				{
					me.setIsAutoWarClaimEnabled(false);
				}
			}
			else
			{
				if (me.getAutoClaimFor() != null)
				{
					me.attemptClaim(me.getAutoClaimFor(), locationTo, true);
				}
				else if (me.isAutoSafeClaimEnabled())
				{
					if (! Permission.MANAGE_SAFE_ZONE.has(player))
					{
						me.setIsAutoSafeClaimEnabled(false);
					}
					else
					{
						if (! Board.getFactionAt(to).isSafeZone())
						{
							Board.setFactionAt(Factions.i.getSafeZone(), to);
							me.msg("<i>This land is now a safe zone.");
						}
					}
				}
				else if (me.isAutoWarClaimEnabled())
				{
					if (! Permission.MANAGE_WAR_ZONE.has(player))
					{
						me.setIsAutoWarClaimEnabled(false);
					}
					else
					{
						if (! Board.getFactionAt(to).isWarZone())
						{
							Board.setFactionAt(Factions.i.getWarZone(), to);
							me.msg("<i>This land is now a war zone.");
						}
					}
				}
			}
		}

		Faction myFaction = me.getFaction();
		if (myFaction.isNone())
			return;

		if (myFaction == factionTo)
		{
			if (Conf.ownedAreasEnabled)
			{
				if (Conf.ownedMessageOnBorder)
				{
					String ownersTo = myFaction.getOwnerListString(to);
					String ownersFrom = myFaction.getOwnerListString(from);
					if (Conf.ownedMessageByChunk || ! ownersFrom.equals(ownersTo))
					{
						if (! ownersTo.isEmpty())
						{
							me.sendMessage(Conf.ownedLandMessage + ownersTo);
						}
						else
						{
							if (! Conf.publicLandMessage.isEmpty())
							{
								me.sendMessage(Conf.publicLandMessage);
							}
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.isCancelled() || ! event.hasBlock())
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL)
			return;

		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		FPlayer fme = FPlayers.i.get(player);

//		Champster's law, this is a last-ditch effort if needed
//		ItemStack inHand = player.getItemInHand();
//		if (inHand != null && inHand.getType() != Material.AIR)
//		{
//			Faction fac = Board.getAbsoluteFactionAt(new FLocation(block.getLocation()));
//			if (inHand.getType() == Material.WATER_BUCKET || inHand.getType() == Material.LAVA_BUCKET)
//			{
//				if (fac.isSafeZone() && ! Permission.MANAGE_SAFE_ZONE.has(player))
//				{
//					fme.msg("<b>You do not have permission to use buckets in %s<b>.", fac.describeTo(fme));
//					event.setCancelled(true);
//					return;
//				}
//
//				if (fac.isWarZone() && !Permission.MANAGE_WAR_ZONE.has(player))
//				{
//					fme.msg("<b>You do not have permission to use buckets in %s<b>.", fac.describeTo(fme));
//					event.setCancelled(true);
//					return;
//				}
//			}
//			else
//			{
//				if (fac != fme.getFaction() && ! fac.isNone())
//				{
//					fme.msg("<b>You cannot do this with an item in your hand, #ChampstersLaw");
//					event.setCancelled(true);
//					return;
//				}
//			}
//		}

		if (! canPlayerUseBlock(player, block, false))
		{
			event.setCancelled(true);
			if (Conf.handleExploitInteractionSpam)
			{
				String name = player.getName();
				InteractAttemptSpam attempt = interactSpammers.get(name);
				if (attempt == null)
				{
					attempt = new InteractAttemptSpam();
					interactSpammers.put(name, attempt);
				}
				int count = attempt.increment();
				if (count >= 10)
				{
					fme.msg("<b>Ouch, that is starting to hurt. You should give it a rest.");
					player.damage(Math.floor(count / 10.0D));
				}
			}

			return;
		}

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		if (! playerCanUseItemHere(player, block.getLocation(), event.getItem(), false))
		{
			event.setCancelled(true);
			return;
		}

		if (block.getType() == Material.BED_BLOCK)
		{
			if (Board.getFactionAt(new FLocation(block)) != fme.getFaction())
				return;

			if (! Conf.homeBalanceOverride)
			{
				if (fme.getFaction().hasHome())
				{
					FLocation fHome = new FLocation(fme.getFaction().getHome());
					FLocation loc = new FLocation(fme.getPlayer().getLocation());
					if (fHome.getDistanceTo(loc) > 20.0 || Board.getAbsoluteFactionAt(loc) != fme.getFaction())
					{
						return;
					}
				}
				else
				{
					return;
				}
			}

			fme.setHome(fme.getPlayer().getLocation());
			fme.msg("<i>Home set!");
			return;
		}
	}

	// for handling people who repeatedly spam attempts to open a door (or
	// similar) in another faction's territory
	private Map<String, InteractAttemptSpam> interactSpammers = new HashMap<String, InteractAttemptSpam>();

	private static class InteractAttemptSpam
	{
		private int attempts = 0;
		private long lastAttempt = System.currentTimeMillis();

		// returns the current attempt count
		public int increment()
		{
			long Now = System.currentTimeMillis();
			if (Now > lastAttempt + 2000)
				attempts = 1;
			else
				attempts++;
			lastAttempt = Now;
			return attempts;
		}
	}

	public static boolean playerCanUseItemHere(Player player, Location location, ItemStack item, boolean justCheck)
	{
		String name = player.getName();
		if (Conf.playersWhoBypassAllProtection.contains(name))
			return true;

		FPlayer me = FPlayers.i.get(name);
		if (me.isAdminBypassing())
			return true;

		FLocation loc = new FLocation(location);
		Faction otherFaction = Board.getFactionAt(loc);

		if (otherFaction.isNormal())
		{
			if (otherFaction.hasPlayersOnline())
			{
				if (item != null)
				{
					if (! Conf.territoryDenyUseageMaterials.contains(new MyMaterial(item.getType())))
						return true; // Item isn't one we're preventing for online factions.
				}
			}
			else
			{
				if (item != null)
				{
					if (! Conf.territoryDenyUseageMaterialsWhenOffline.contains(new MyMaterial(item.getType())))
						return true; // Item isn't one we're preventing for offline factions.
				}
			}
		}

		if (otherFaction.isNone())
		{
			if (! Conf.wildernessDenyUseage || Conf.worldsNoWildernessProtection.contains(location.getWorld().getName()))
				return true; // This is not faction territory. Use whatever you like here.

			if (! justCheck)
				me.msg("<b>You can't use that in the wilderness.");

			return false;
		}
		else if (otherFaction.isSafeZone())
		{
			if (! Conf.safeZoneDenyUseage || Permission.MANAGE_SAFE_ZONE.has(player))
				return true;

			if (item != null)
			{
				if (! Conf.safeZoneDenyUseageMaterials.contains(new MyMaterial(item.getType())))
					return true; // Item isn't one we're preventing for safezones.
			}

			if (! justCheck)
				me.msg("<b>You can't use that in a safe zone.");

			return false;
		}
		else if (otherFaction.isWarZone())
		{
			if (! Conf.warZoneDenyUseage || Permission.MANAGE_WAR_ZONE.has(player))
				return true;

			if (! justCheck)
				me.msg("<b>You can't use that in a war zone.");

			return false;
		}

		Faction myFaction = me.getFaction();
		Relation rel = myFaction.getRelationTo(otherFaction);

		// Cancel if we are not in our own territory
		if (rel.confDenyUseage())
		{
			if (! justCheck)
				me.msg("<b>You can't use that in the territory of <h>%s<b>.", otherFaction.getTag(myFaction));

			return false;
		}

		// Also cancel if player doesn't have ownership rights for this claim
		if (Conf.ownedAreasEnabled && Conf.ownedAreaDenyUseage && ! otherFaction.playerHasOwnershipRights(me, loc))
		{
			if (! justCheck)
			{
				me.msg("<b>You can't use that in this territory, it is owned by: %s<b>.", otherFaction.getOwnerListString(loc));
				return false;
			}
		}

		return true;
	}

	public static boolean canPlayerUseBlock(Player player, Block block, boolean justCheck)
	{
		String name = player.getName();
		if (Conf.playersWhoBypassAllProtection.contains(name))
			return true;

		FPlayer me = FPlayers.i.get(name);
		if (me.isAdminBypassing())
			return true;

		FLocation loc = new FLocation(block);
		Faction otherFaction = Board.getFactionAt(loc);

		// no door/chest/whatever protection in wilderness, war zones, or safe zones
		if (!otherFaction.isNormal())
			return true;

		if (otherFaction.isSafeZone())
		{
			if (Permission.MANAGE_SAFE_ZONE.has(player))
				return true;

			if (block != null)
			{
				if (! Conf.safeZoneProtectedMaterials.contains(new MyMaterial(block.getType())))
					return true; // Block isn't one we're protecting for safezones.
			}

			return false;
		}

		if (otherFaction.isWarZone())
			return true;

		// We only care about some material types.
		if (otherFaction.hasPlayersOnline())
		{
			if (block != null)
			{
				if (! Conf.territoryProtectedMaterials.contains(new MyMaterial(block.getType())))
					return true;
			}
		}
		else
		{
			if (block != null)
			{
				if (! Conf.territoryProtectedMaterialsWhenOffline.contains(new MyMaterial(block.getType())))
					return true;
			}
		}

		Faction myFaction = me.getFaction();
		Relation rel = myFaction.getRelationTo(otherFaction);

		// You may use any block unless it is another faction's territory...
		if (rel.isNeutral() || (rel.isEnemy() && Conf.territoryEnemyProtectMaterials) || (rel.isAlly() && Conf.territoryAllyProtectMaterials)
				|| (rel.isNation() && Conf.territoryNationProtectMaterials))
		{
			if (block != null)
			{
				if (! justCheck)
					me.msg("<b>You can't %s this in the territory of <h>%s<b>.",
							block.getType() == Material.SOIL ? "trample" : "use", otherFaction.getTag(myFaction));
			}
			return false;
		}

		if (block != null)
		{
			if (block.getType() == Material.CHEST)
			{
				if (! me.getFaction().playerHasPermission(me, NPermission.CHEST))
				{
					me.msg("<b>You can't %s this in the territory of <h>%s<b>.",
							block.getType() == Material.SOIL ? "trample" : "use", otherFaction.getTag(myFaction));
					return false;
				}
			}
			if (block.getType() == Material.LEVER || block.getType() == Material.STONE_BUTTON || block.getType() == Material.STONE_PLATE
					|| block.getType() == Material.WOOD_PLATE)
			{
				if (! me.getFaction().playerHasPermission(me, NPermission.SWITCH))
				{
					me.msg("<b>You can't %s this in the territory of <h>%s<b>.",
							block.getType() == Material.SOIL ? "trample" : "use", otherFaction.getTag(myFaction));
					return false;
				}
			}
		}

		// Also cancel if player doesn't have ownership rights for this claim
		if (Conf.ownedAreasEnabled && Conf.ownedAreaProtectMaterials && ! otherFaction.playerHasOwnershipRights(me, loc))
		{
			if (! justCheck)
				me.msg("<b>You can't use this in this territory, it is owned by: %s<b>.", otherFaction.getOwnerListString(loc));

			return false;
		}

		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event)
	{
		FPlayer me = FPlayers.i.get(event.getPlayer());

		me.getPower(); // update power, so they won't have gained any while dead

		Location home = me.getFaction().getHome();
		if (Conf.homesEnabled && Conf.homesTeleportToOnDeath && home != null
				&& (Conf.homesRespawnFromNoPowerLossWorlds || ! Conf.worldsNoPowerLoss.contains(event.getPlayer().getWorld().getName())))
		{
			event.setRespawnLocation(home);
		}
	}

	// For some reason onPlayerInteract() sometimes misses bucket events
	// depending on distance (something like 2-3 blocks away isn't detected),
	// but these separate bucket events below always fire without fail
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
	{
		if (event.isCancelled())
			return;

		Block block = event.getBlockClicked();
		Player player = event.getPlayer();

		if (! playerCanUseItemHere(player, block.getLocation(), event.getItemStack(), false))
		{
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerBucketFill(PlayerBucketFillEvent event)
	{
		if (event.isCancelled())
			return;

		Block block = event.getBlockClicked();
		Player player = event.getPlayer();

		if (! playerCanUseItemHere(player, block.getLocation(), event.getItemStack(), false))
		{
			event.setCancelled(true);
			return;
		}
	}

	public static boolean preventCommand(String fullCmd, Player player)
	{
		fullCmd = fullCmd.toLowerCase();

		FPlayer me = FPlayers.i.get(player);

		String shortCmd; // command without the slash at the beginning
		if (fullCmd.startsWith("/"))
		{
			shortCmd = fullCmd.substring(1);
		}
		else
		{
			shortCmd = fullCmd;
			fullCmd = "/" + fullCmd;
		}

		if (me.hasFaction() && !me.isAdminBypassing() && ! Conf.permanentFactionMemberDenyCommands.isEmpty() && me.getFaction().isPermanent()
				&& isCommandInList(fullCmd, shortCmd, Conf.permanentFactionMemberDenyCommands))
		{
			me.msg("<b>You can't use the command \"" + fullCmd + "\" because you are in a permanent faction.");
			return true;
		}

		for (String territorycommands : Conf.ownTerritoryOnlyCommands)
		{
			String[] args = fullCmd.split(" ");
			if (args[0].equalsIgnoreCase("/" + territorycommands))
			{
				if (! me.hasFaction())
				{
					me.msg("<b>You need to be in a faction to use this command.");
					return true;
				}

				if (! me.getFaction().hasHome())
				{
					me.msg("<b>Please set a faction home first. "
							+ (me.getRole().value < Role.MODERATOR.value ? "<i>Ask your leader to:" : "<i>You should:"));
					me.sendMessage(P.p.cmdBase.cmdSethome.getUseageTemplate());
					return true;
				}

				if (args.length > 1)
				{
					FLocation fHome = new FLocation(me.getFaction().getHome());
					FPlayer target = FPlayers.i.get(args[1]);
					if (target != null && target.isOnline())
					{
						FLocation loc = new FLocation(target.getPlayer().getLocation());
						if (loc != null)
						{
							if (fHome.getDistanceTo(loc) > 40.0)
							{
								me.msg("<b>You can't use that command for players outside of 40 chunks from your faction home.");
								return true;
							}
						}
					}
					else
					{
						me.msg("<b>Player <b>%s<b> not found", args[1]);
						return true;
					}
				}
			}
		}

		if (! me.isInOthersTerritory())
		{
			return false;
		}

		Relation rel = me.getRelationToLocation();
		if (rel.isAtLeast(Relation.ALLY))
		{
			return false;
		}

		if (rel.isNeutral() && ! Conf.territoryNeutralDenyCommands.isEmpty() && ! me.isAdminBypassing()
				&& isCommandInList(fullCmd, shortCmd, Conf.territoryNeutralDenyCommands))
		{
			me.msg("<b>You can't use the command \"" + fullCmd + "\" in neutral territory.");
			return true;
		}

		if (rel.isEnemy() && !Conf.territoryEnemyDenyCommands.isEmpty() && ! me.isAdminBypassing()
				&& isCommandInList(fullCmd, shortCmd, Conf.territoryEnemyDenyCommands))
		{
			me.msg("<b>You can't use the command \"" + fullCmd + "\" in enemy territory.");
			return true;
		}

		return false;
	}

	private static boolean isCommandInList(String fullCmd, String shortCmd, Set<String> list)
	{
		for (String cmdCheck : list)
		{
			cmdCheck = cmdCheck.toLowerCase();
			if (shortCmd.matches(cmdCheck + ".*") || fullCmd.matches(cmdCheck + ".*"))
				return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInitiateInteract(PlayerInteractEvent event)
	{
		if (event.isCancelled() || ! event.hasBlock())
			return;

		FPlayer fplayer = FPlayers.i.get(event.getPlayer());
		if (! fplayer.hasFaction())
			return;

		Role role = fplayer.getRole();
		if (role != Role.INITIATE)
			return;

		Faction faction = fplayer.getFaction();
		Faction fLoc = Board.getFactionAt(new FLocation(fplayer));
		if (fLoc == null || fLoc != faction)
			return;

		Block block = event.getClickedBlock();
		BlockState state = block.getState();
		if (state instanceof Chest)
		{
			FLocation loc = new FLocation(block);
			if (! faction.doesLocationHaveOwnersSet(loc) || ! faction.playerHasOwnershipRights(fplayer, loc))
			{
				fplayer.msg("<i>You do not have permission to access chests not in your area.");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockedItemInteract(PlayerInteractEvent event)
	{
		if (event.isCancelled() || ! event.hasItem())
			return;

		FPlayer fme = FPlayers.i.get(event.getPlayer());
		FLocation floc = new FLocation(event.getPlayer());
		Faction fac = Board.getFactionAt(floc);

		if (Conf.ownTerritoryOnlyMaterials.contains(new MyMaterial(event.getItem().getType())))
		{
			if (! canUseBlockedItemHere(floc, fac, fme, false))
			{
				fme.msg("<i>You cannot use this item outside your own territory!");
				event.setCancelled(true);
			}
		}

		if (Conf.ownTerritoryAndWildernessMaterials.contains(new MyMaterial(event.getItem().getType())))
		{
			if (! canUseBlockedItemHere(floc, fac, fme, true))
			{
				fme.msg("<i>You cannot use this item outside your own territory or wilderness!");
				event.setCancelled(true);
			}
		}
	}

	public static boolean canUseBlockedItemHere(FLocation floc, Faction fac, FPlayer pl, boolean both)
	{
		if (fac.isSafeZone() || fac.isWarZone())
			return false;

		if (floc.getY() <= 45)
			return true;

		return (both ? (fac == pl.getFaction() || fac.isNone()) : (fac == pl.getFaction()));
	}
}