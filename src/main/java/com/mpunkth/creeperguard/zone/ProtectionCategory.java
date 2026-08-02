package com.mpunkth.creeperguard.zone;

import com.mojang.serialization.Codec;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Achse 2 – die Ziel-Kategorien, die eine Zone vor Explosionen bewahrt.
 * Spieler sind bewusst KEINE Kategorie und daher niemals schützbar.
 */
public enum ProtectionCategory {
	BLOCKS,
	ITEMS,
	VEHICLES,
	ANIMALS,
	VILLAGERS,
	HOSTILES;

	public static final Codec<ProtectionCategory> CODEC = Codec.STRING.xmap(
			s -> valueOf(s.toUpperCase(Locale.ROOT)),
			c -> c.name().toLowerCase(Locale.ROOT)
	);

	public String lower() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** Preset "standard": alles außer feindliche Mobs. */
	public static Set<ProtectionCategory> standard() {
		return EnumSet.of(BLOCKS, ITEMS, VEHICLES, ANIMALS, VILLAGERS);
	}

	/** Preset "blocksonly": nur Blöcke. */
	public static Set<ProtectionCategory> blocksOnly() {
		return EnumSet.of(BLOCKS);
	}

	/** Preset "full": alles außer Spieler. */
	public static Set<ProtectionCategory> full() {
		return EnumSet.allOf(ProtectionCategory.class);
	}
}
