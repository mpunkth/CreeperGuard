package com.mpunkth.creeperguard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mpunkth.creeperguard.zone.ExplosionMode;
import com.mpunkth.creeperguard.zone.ProtectionCategory;
import com.mpunkth.creeperguard.zone.Zone;
import com.mpunkth.creeperguard.zone.ZoneStore;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registriert und implementiert das /creeperguard-Kommando (Konsole + Op-Level 4).
 */
public final class CreeperGuardCommand {

	private static final int MIN_Y = -64;
	private static final int MAX_Y = 320;
	private static final Identifier OVERWORLD_ID = Identifier.of("minecraft", "overworld");
	private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
	private static final String PREFIX = "[CreeperGuard] ";

	private static final List<String> MODES = List.of("creeper", "all");
	private static final List<String> PRESETS = List.of("standard", "blocksonly", "full");
	private static final List<String> CATEGORIES =
			List.of("blocks", "items", "vehicles", "animals", "villagers", "hostiles");
	private static final List<String> STATES = List.of("on", "off");

	private static final SuggestionProvider<ServerCommandSource> ZONE_NAMES =
			(ctx, builder) -> CommandSource.suggestMatching(
					ZoneStore.get(ctx.getSource().getServer()).zoneNames(), builder);

	private CreeperGuardCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				literal("creeperguard")
						// OWNERS_CHECK entspricht Op-Level 4 (PermissionLevel: ALL=0 … OWNERS=4); Konsole erfüllt es ohnehin.
						.requires(CommandManager.requirePermissionLevel(CommandManager.OWNERS_CHECK))
						// add <name> <x1> <z1> <x2> <z2> [mode] [preset]
						.then(literal("add")
								.then(argument("name", StringArgumentType.word())
										.then(argument("x1", IntegerArgumentType.integer())
												.then(argument("z1", IntegerArgumentType.integer())
														.then(argument("x2", IntegerArgumentType.integer())
																.then(argument("z2", IntegerArgumentType.integer())
																		.executes(CreeperGuardCommand::addDefault)
																		.then(argument("mode", StringArgumentType.word())
																				.suggests((c, b) -> CommandSource.suggestMatching(MODES, b))
																				.executes(CreeperGuardCommand::addWithMode)
																				.then(argument("preset", StringArgumentType.word())
																						.suggests((c, b) -> CommandSource.suggestMatching(PRESETS, b))
																						.executes(CreeperGuardCommand::addWithModeAndPreset)))))))))
						// remove <name>
						.then(literal("remove")
								.then(argument("name", StringArgumentType.word())
										.suggests(ZONE_NAMES)
										.executes(CreeperGuardCommand::remove)))
						// set <name> mode <creeper|all>  /  set <name> <category> <on|off>
						.then(literal("set")
								.then(argument("name", StringArgumentType.word())
										.suggests(ZONE_NAMES)
										.then(literal("mode")
												.then(argument("value", StringArgumentType.word())
														.suggests((c, b) -> CommandSource.suggestMatching(MODES, b))
														.executes(CreeperGuardCommand::setMode)))
										.then(argument("category", StringArgumentType.word())
												.suggests((c, b) -> CommandSource.suggestMatching(CATEGORIES, b))
												.then(argument("state", StringArgumentType.word())
														.suggests((c, b) -> CommandSource.suggestMatching(STATES, b))
														.executes(CreeperGuardCommand::setCategory)))))
						// list
						.then(literal("list").executes(CreeperGuardCommand::list))
		);
	}

	// ---------- add ----------

	private static int addDefault(CommandContext<ServerCommandSource> ctx) {
		return doAdd(ctx, ExplosionMode.CREEPER, ProtectionCategory.standard());
	}

	private static int addWithMode(CommandContext<ServerCommandSource> ctx) {
		ExplosionMode mode = parseMode(ctx.getSource(), StringArgumentType.getString(ctx, "mode"));
		if (mode == null) {
			return 0;
		}
		return doAdd(ctx, mode, ProtectionCategory.standard());
	}

	private static int addWithModeAndPreset(CommandContext<ServerCommandSource> ctx) {
		ExplosionMode mode = parseMode(ctx.getSource(), StringArgumentType.getString(ctx, "mode"));
		if (mode == null) {
			return 0;
		}
		Set<ProtectionCategory> protection = parsePreset(ctx.getSource(), StringArgumentType.getString(ctx, "preset"));
		if (protection == null) {
			return 0;
		}
		return doAdd(ctx, mode, protection);
	}

	private static int doAdd(CommandContext<ServerCommandSource> ctx, ExplosionMode mode, Set<ProtectionCategory> protection) {
		ServerCommandSource src = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "name");
		if (!NAME_PATTERN.matcher(name).matches()) {
			src.sendError(Text.literal(PREFIX + "Ungültiger Name '" + name + "'. Erlaubt: Buchstaben, Ziffern, Unterstrich, keine Leerzeichen."));
			return 0;
		}
		ZoneStore store = ZoneStore.get(src.getServer());
		if (store.exists(name)) {
			src.sendError(Text.literal(PREFIX + "Zone '" + name + "' existiert bereits. Nutze zuerst remove."));
			return 0;
		}
		int x1 = IntegerArgumentType.getInteger(ctx, "x1");
		int z1 = IntegerArgumentType.getInteger(ctx, "z1");
		int x2 = IntegerArgumentType.getInteger(ctx, "x2");
		int z2 = IntegerArgumentType.getInteger(ctx, "z2");
		int minX = Math.min(x1, x2);
		int maxX = Math.max(x1, x2);
		int minZ = Math.min(z1, z2);
		int maxZ = Math.max(z1, z2);

		Zone zone = new Zone(name, OVERWORLD_ID, minX, minZ, maxX, maxZ, MIN_Y, MAX_Y, mode, copyCategories(protection));
		store.add(zone);

		final String feedback = PREFIX + "Zone '" + name + "' angelegt: X " + minX + ".." + maxX
				+ ", Z " + minZ + ".." + maxZ + " | Modus=" + mode.lower() + " | Schutz=" + zone.protectionList();
		src.sendFeedback(() -> Text.literal(feedback), true);
		return 1;
	}

	// ---------- remove ----------

	private static int remove(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "name");
		ZoneStore store = ZoneStore.get(src.getServer());
		if (store.remove(name)) {
			src.sendFeedback(() -> Text.literal(PREFIX + "Zone '" + name + "' entfernt."), true);
			return 1;
		}
		src.sendError(Text.literal(PREFIX + "Zone '" + name + "' existiert nicht."));
		return 0;
	}

	// ---------- set ----------

	private static int setMode(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "name");
		ZoneStore store = ZoneStore.get(src.getServer());
		Zone zone = store.getZone(name);
		if (zone == null) {
			src.sendError(Text.literal(PREFIX + "Zone '" + name + "' existiert nicht."));
			return 0;
		}
		ExplosionMode mode = parseMode(src, StringArgumentType.getString(ctx, "value"));
		if (mode == null) {
			return 0;
		}
		Zone updated = new Zone(zone.name(), zone.dimension(), zone.minX(), zone.minZ(), zone.maxX(), zone.maxZ(),
				zone.minY(), zone.maxY(), mode, copyCategories(zone.protection()));
		store.replace(updated);
		src.sendFeedback(() -> Text.literal(PREFIX + "Zone '" + zone.name() + "': Modus=" + mode.lower()), true);
		return 1;
	}

	private static int setCategory(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		String name = StringArgumentType.getString(ctx, "name");
		ZoneStore store = ZoneStore.get(src.getServer());
		Zone zone = store.getZone(name);
		if (zone == null) {
			src.sendError(Text.literal(PREFIX + "Zone '" + name + "' existiert nicht."));
			return 0;
		}
		ProtectionCategory category = parseCategory(src, StringArgumentType.getString(ctx, "category"));
		if (category == null) {
			return 0;
		}
		Boolean on = parseState(src, StringArgumentType.getString(ctx, "state"));
		if (on == null) {
			return 0;
		}
		EnumSet<ProtectionCategory> protection = copyCategories(zone.protection());
		if (on) {
			protection.add(category);
		} else {
			protection.remove(category);
		}
		Zone updated = new Zone(zone.name(), zone.dimension(), zone.minX(), zone.minZ(), zone.maxX(), zone.maxZ(),
				zone.minY(), zone.maxY(), zone.mode(), protection);
		store.replace(updated);
		src.sendFeedback(() -> Text.literal(PREFIX + "Zone '" + updated.name() + "': " + category.lower()
				+ "=" + (on ? "on" : "off") + " | Schutz=" + updated.protectionList()), true);
		return 1;
	}

	// ---------- list ----------

	private static int list(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource src = ctx.getSource();
		ZoneStore store = ZoneStore.get(src.getServer());
		if (store.isEmpty()) {
			src.sendFeedback(() -> Text.literal(PREFIX + "Keine Zonen definiert."), false);
			return 0;
		}
		src.sendFeedback(() -> Text.literal(PREFIX + store.size() + " Zone(n):"), false);
		for (Zone zone : store.all()) {
			final String line = " - " + zone.name() + ": X " + zone.minX() + ".." + zone.maxX()
					+ ", Z " + zone.minZ() + ".." + zone.maxZ()
					+ " | Modus=" + zone.mode().lower() + " | Schutz=" + zone.protectionList();
			src.sendFeedback(() -> Text.literal(line), false);
		}
		return store.size();
	}

	// ---------- Parser-Helfer ----------

	private static ExplosionMode parseMode(ServerCommandSource src, String value) {
		try {
			return ExplosionMode.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			src.sendError(Text.literal(PREFIX + "Unbekannter Modus '" + value + "'. Erlaubt: creeper, all."));
			return null;
		}
	}

	private static Set<ProtectionCategory> parsePreset(ServerCommandSource src, String value) {
		switch (value.toLowerCase(Locale.ROOT)) {
			case "standard":
				return ProtectionCategory.standard();
			case "blocksonly":
				return ProtectionCategory.blocksOnly();
			case "full":
				return ProtectionCategory.full();
			default:
				src.sendError(Text.literal(PREFIX + "Unbekanntes Preset '" + value + "'. Erlaubt: standard, blocksonly, full."));
				return null;
		}
	}

	private static ProtectionCategory parseCategory(ServerCommandSource src, String value) {
		try {
			return ProtectionCategory.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			src.sendError(Text.literal(PREFIX + "Unbekannte Kategorie '" + value + "'. Erlaubt: "
					+ String.join(", ", CATEGORIES) + "."));
			return null;
		}
	}

	private static Boolean parseState(ServerCommandSource src, String value) {
		String v = value.toLowerCase(Locale.ROOT);
		if (v.equals("on")) {
			return Boolean.TRUE;
		}
		if (v.equals("off")) {
			return Boolean.FALSE;
		}
		src.sendError(Text.literal(PREFIX + "Ungültiger Wert '" + value + "'. Erlaubt: on, off."));
		return null;
	}

	private static EnumSet<ProtectionCategory> copyCategories(Set<ProtectionCategory> source) {
		EnumSet<ProtectionCategory> set = EnumSet.noneOf(ProtectionCategory.class);
		set.addAll(source);
		return set;
	}
}
