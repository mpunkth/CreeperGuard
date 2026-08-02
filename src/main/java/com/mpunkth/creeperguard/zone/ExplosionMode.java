package com.mpunkth.creeperguard.zone;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Achse 1 – die Explosionsquelle, gegen die eine Zone schützt.
 */
public enum ExplosionMode {
	/** Nur Creeper-Explosionen (normal und geladen). */
	CREEPER,
	/** Alle Explosionen (Creeper, TNT, Ghast, End-Kristall, Bett/Wither, …). */
	ALL;

	public static final Codec<ExplosionMode> CODEC = Codec.STRING.xmap(
			s -> valueOf(s.toUpperCase(Locale.ROOT)),
			m -> m.name().toLowerCase(Locale.ROOT)
	);

	public String lower() {
		return name().toLowerCase(Locale.ROOT);
	}
}
