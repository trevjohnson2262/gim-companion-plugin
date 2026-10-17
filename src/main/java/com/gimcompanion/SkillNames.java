package com.gimcompanion;

import java.util.Locale;
import net.runelite.api.Skill;

/** The site names skills the way Wise Old Man does ("runecrafting", not "Runecraft"). */
public final class SkillNames
{
	private SkillNames()
	{
	}

	public static String of(Skill skill)
	{
		String name = skill.getName().toLowerCase(Locale.ROOT);
		return name.equals("runecraft") ? "runecrafting" : name;
	}
}
