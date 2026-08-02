package com.mpunkth.creeperguard.util;

import com.mpunkth.creeperguard.zone.ProtectionCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Ordnet eine Entity genau einer Schutzkategorie zu.
 * Reihenfolge ist wichtig: Deko-Entities (z. B. ArmorStand) sind zwar LivingEntity,
 * werden aber vor dem Lebewesen-Fallback als ITEMS erkannt.
 */
public final class EntityClassifier {

	private EntityClassifier() {
	}

	/**
	 * @return die Kategorie der Entity, oder {@code null}, wenn sie nie geschützt wird
	 *         (Spieler, Projektile, XP-Kugeln, TNT, fallende Blöcke …).
	 */
	@Nullable
	public static ProtectionCategory categoryOf(Entity entity) {
		// Spieler sind niemals schützbar.
		if (entity instanceof PlayerEntity) {
			return null;
		}
		// Sachwerte / Deko (GlowItemFrame erbt von ItemFrame -> mit abgedeckt).
		if (entity instanceof ItemEntity
				|| entity instanceof ItemFrameEntity
				|| entity instanceof ArmorStandEntity
				|| entity instanceof PaintingEntity) {
			return ProtectionCategory.ITEMS;
		}
		// Fahrzeuge (alle Boote/Flöße und alle Loren).
		if (entity instanceof AbstractBoatEntity || entity instanceof AbstractMinecartEntity) {
			return ProtectionCategory.VEHICLES;
		}
		// NPC-Händler.
		if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
			return ProtectionCategory.VILLAGERS;
		}
		// Feindliche Mobs (inkl. Creeper, Zombie-Villager …).
		if (entity instanceof HostileEntity) {
			return ProtectionCategory.HOSTILES;
		}
		// Übrige Lebewesen: Tiere, Haustiere, Golems, Wasser-/Ambient-Kreaturen.
		if (entity instanceof LivingEntity) {
			return ProtectionCategory.ANIMALS;
		}
		return null;
	}
}
