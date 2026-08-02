package com.mpunkth.creeperguard.zone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Eine benannte Schutzzone. Grundfläche wird über X/Z definiert, die Höhe (Y)
 * überspannt im MVP fest die gesamte Overworld-Bauhöhe.
 */
public record Zone(
		String name,
		Identifier dimension,
		int minX, int minZ, int maxX, int maxZ,
		int minY, int maxY,
		ExplosionMode mode,
		Set<ProtectionCategory> protection
) {
	/** Set<ProtectionCategory> als Liste (de)serialisieren; robust auch bei leerer Menge. */
	public static final Codec<Set<ProtectionCategory>> PROTECTION_CODEC =
			ProtectionCategory.CODEC.listOf().xmap(Zone::toSet, List::copyOf);

	public static final Codec<Zone> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.STRING.fieldOf("name").forGetter(Zone::name),
			Identifier.CODEC.fieldOf("dimension").forGetter(Zone::dimension),
			Codec.INT.fieldOf("minX").forGetter(Zone::minX),
			Codec.INT.fieldOf("minZ").forGetter(Zone::minZ),
			Codec.INT.fieldOf("maxX").forGetter(Zone::maxX),
			Codec.INT.fieldOf("maxZ").forGetter(Zone::maxZ),
			Codec.INT.fieldOf("minY").forGetter(Zone::minY),
			Codec.INT.fieldOf("maxY").forGetter(Zone::maxY),
			ExplosionMode.CODEC.fieldOf("mode").forGetter(Zone::mode),
			PROTECTION_CODEC.fieldOf("protection").forGetter(Zone::protection)
	).apply(inst, Zone::new));

	private static Set<ProtectionCategory> toSet(List<ProtectionCategory> list) {
		EnumSet<ProtectionCategory> set = EnumSet.noneOf(ProtectionCategory.class);
		set.addAll(list);
		return set;
	}

	/** Positionsbasierte Prüfung (Modell A): liegt dieser Block in der Zone? */
	public boolean contains(BlockPos pos) {
		return pos.getX() >= minX && pos.getX() <= maxX
				&& pos.getZ() >= minZ && pos.getZ() <= maxZ
				&& pos.getY() >= minY && pos.getY() <= maxY;
	}

	public boolean protects(ProtectionCategory category) {
		return protection.contains(category);
	}

	/** Ob diese Zone gegen die gegebene Quelle greift. */
	public boolean appliesTo(boolean creeperSource) {
		return mode == ExplosionMode.ALL || creeperSource;
	}

	public String protectionList() {
		if (protection.isEmpty()) {
			return "(keine)";
		}
		return protection.stream()
				.sorted()
				.map(c -> c.name().toLowerCase(Locale.ROOT))
				.collect(Collectors.joining(", "));
	}
}
