package com.massivecraft.factions;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import net.dmulloy2.swornnations.SwornNations;
import net.dmulloy2.swornnations.exception.EnableException;

import org.bukkit.ChatColor;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.persist.EntityCollection;
import com.massivecraft.factions.util.DiscUtil;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.util.TextUtil;

public class Factions extends EntityCollection<Faction>
{
	public static Factions i = new Factions();
	private final SwornNations p;

	private Factions()
	{
		super(Faction.class, new CopyOnWriteArrayList<Faction>(), new ConcurrentHashMap<String, Faction>(),
				new File(SwornNations.get().getDataFolder(), "factions.json"), SwornNations.get().getGson());
		this.p = SwornNations.get();
	}

	@Override
	public Type getMapType()
	{
		return new TypeToken<Map<String, Faction>>() { }.getType();
	}

	@Override
	public boolean loadFromDisc() throws EnableException
	{
		if (! super.loadFromDisc())
			return false;

		// Make sure the default neutral faction exists
		if (! exists("0"))
		{
			Faction faction = create("0");
			faction.setTag(ChatColor.DARK_GREEN + "Wilderness");
			faction.setDescription("");
		}

		// Make sure the safe zone faction exists
		if (! exists("-1"))
		{
			Faction faction = create("-1");
			faction.setTag("SafeZone");
			faction.setDescription("Free from PVP and monsters");
		}
		else
		{
			// if SafeZone has old pre-1.6.0 name, rename it to remove
			// troublesome " "
			Faction faction = getSafeZone();
			if (faction.getTag().contains(" "))
				faction.setTag("SafeZone");
		}

		// Make sure the war zone faction exists
		if (! exists("-2"))
		{
			Faction faction = create("-2");
			faction.setTag("WarZone");
			faction.setDescription("Not the safest place to be");
		}
		else
		{
			// if WarZone has old pre-1.6.0 name, rename it to remove
			// troublesome " "
			Faction faction = getWarZone();
			if (faction.getTag().contains(" "))
				faction.setTag("WarZone");
		}

		// populate all faction player lists
		for (Faction faction : i.get())
		{
			faction.refreshFPlayers();
		}

		return true;
	}

	// ----------------------------------------------//
	// GET
	// ----------------------------------------------//

	@Override
	public Faction get(String id)
	{
		if (! exists(id))
		{
			if (! DiscUtil.checkDiskSpace())
			{
				// TODO This becomes the bane of server owners' existances
				// Surely there must be a better way to handle this
				p.log(Level.WARNING, "Non existing factionId " + id + " requested! Issuing cleaning!");
				Board.clean();
				FPlayers.i.clean();
			}
		}

		return super.get(id);
	}

	public Faction getNone()
	{
		return get("0");
	}

	public Faction getSafeZone()
	{
		return get("-1");
	}

	public Faction getWarZone()
	{
		return get("-2");
	}

	// ----------------------------------------------//
	// Faction tag
	// ----------------------------------------------//

	public static List<String> validateTag(String str)
	{
		List<String> errors = new ArrayList<String>();

		if (MiscUtil.getComparisonString(str).length() < Conf.factionTagLengthMin)
		{
			errors.add(SwornNations.get().txt.parse("<i>The faction tag can't be shorter than <h>%s<i> chars.", Conf.factionTagLengthMin));
		}

		if (str.length() > Conf.factionTagLengthMax)
		{
			errors.add(SwornNations.get().txt.parse("<i>The faction tag can't be longer than <h>%s<i> chars.", Conf.factionTagLengthMax));
		}

		for (char c : str.toCharArray())
		{
			if (! MiscUtil.substanceChars.contains(String.valueOf(c)))
			{
				errors.add(SwornNations.get().txt.parse("<i>Faction tag must be alphanumeric. \"<h>%s<i>\" is not allowed.", c));
			}
		}

		return errors;
	}

	public Faction getByTag(String str)
	{
		String compStr = MiscUtil.getComparisonString(str);
		for (Faction faction : get())
		{
			if (faction.getComparisonTag().equals(compStr))
				return faction;
		}

		return null;
	}

	public Faction getById(String str)
	{
		for (Faction faction : get())
		{
			if (faction.getId().equalsIgnoreCase(str))
				return faction;
		}

		return null;
	}

	public Faction getBestTagMatch(String searchFor)
	{
		Map<String, Faction> tag2faction = new HashMap<String, Faction>();

		for (Faction faction : get())
		{
			tag2faction.put(ChatColor.stripColor(faction.getTag()), faction);
		}

		String tag = TextUtil.getBestStartWithCI(tag2faction.keySet(), searchFor);
		if (tag == null)
			return null;

		return tag2faction.get(tag);
	}

	public boolean isTagTaken(String str)
	{
		return getByTag(str) != null;
	}
}