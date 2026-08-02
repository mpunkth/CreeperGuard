package com.mpunkth.creeperguard.zone;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Zentraler, persistenter Speicher aller Schutzzonen. Hängt am Overworld-Level
 * und wird als NBT im Welt-Ordner (data/creeperguard.dat) gespeichert.
 */
public class ZoneStore extends PersistentState {
	public static final String ID = "creeperguard";

	// Schlüssel = kleingeschriebener Name (case-insensitive eindeutig), Reihenfolge stabil.
	private final Map<String, Zone> zones = new LinkedHashMap<>();

	public static final Codec<ZoneStore> CODEC = Zone.CODEC.listOf().xmap(
			ZoneStore::fromList,
			store -> new ArrayList<>(store.zones.values())
	);

	public static final PersistentStateType<ZoneStore> TYPE =
			new PersistentStateType<>(ID, ZoneStore::new, CODEC, null);

	public ZoneStore() {
	}

	private static ZoneStore fromList(List<Zone> list) {
		ZoneStore store = new ZoneStore();
		for (Zone zone : list) {
			store.zones.put(key(zone.name()), zone);
		}
		return store;
	}

	/** Holt (oder erzeugt) den Zonenspeicher aus dem Overworld-Level. */
	public static ZoneStore get(MinecraftServer server) {
		ServerWorld overworld = server.getOverworld();
		return overworld.getPersistentStateManager().getOrCreate(TYPE);
	}

	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	public boolean isEmpty() {
		return zones.isEmpty();
	}

	public int size() {
		return zones.size();
	}

	public boolean exists(String name) {
		return zones.containsKey(key(name));
	}

	public Zone getZone(String name) {
		return zones.get(key(name));
	}

	public Collection<Zone> all() {
		return zones.values();
	}

	public List<String> zoneNames() {
		List<String> names = new ArrayList<>(zones.size());
		for (Zone zone : zones.values()) {
			names.add(zone.name());
		}
		return names;
	}

	/** Legt eine neue Zone an. Gibt false zurück, wenn der Name bereits existiert. */
	public boolean add(Zone zone) {
		if (exists(zone.name())) {
			return false;
		}
		zones.put(key(zone.name()), zone);
		markDirty();
		return true;
	}

	/** Ersetzt eine bestehende Zone (für set-Befehle). */
	public void replace(Zone zone) {
		zones.put(key(zone.name()), zone);
		markDirty();
	}

	/** Entfernt eine Zone. Gibt false zurück, wenn sie nicht existierte. */
	public boolean remove(String name) {
		if (zones.remove(key(name)) != null) {
			markDirty();
			return true;
		}
		return false;
	}

	/**
	 * Positionsbasierte Schutzprüfung: Ist {@code pos} in irgendeiner Zone geschützt,
	 * deren Kategorie aktiv ist und deren Modus zur Quelle passt?
	 */
	public boolean isProtected(BlockPos pos, ProtectionCategory category, boolean creeperSource) {
		for (Zone zone : zones.values()) {
			if (!zone.protects(category)) {
				continue;
			}
			if (!zone.appliesTo(creeperSource)) {
				continue;
			}
			if (zone.contains(pos)) {
				return true;
			}
		}
		return false;
	}
}
