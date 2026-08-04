package com.mpunkth.creeperguard.mixin;

import com.mpunkth.creeperguard.CreeperGuard;
import com.mpunkth.creeperguard.util.EntityClassifier;
import com.mpunkth.creeperguard.zone.ProtectionCategory;
import com.mpunkth.creeperguard.zone.ZoneStore;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.explosion.ExplosionImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

/**
 * Kern von CreeperGuard: zwei rein <b>entfernende</b> Eingriffe in die Vanilla-Explosion.
 * Es wird keine Explosionslogik neu geschrieben – wir streichen nur Einträge aus den
 * beiden Listen, die die Explosion ohnehin abarbeitet.
 */
@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {

	@Shadow @Final private ServerWorld world;

	@Shadow @Final @Nullable private Entity entity;

	/** Einmal-Merker, damit ein dauerhaft inkompatibler Mod-Mix das Log nicht pro Explosion flutet. */
	private static volatile boolean creeperguard$immutableListWarned = false;

	/** Ist diese Explosion von einem Creeper (normal oder geladen) verursacht? */
	private boolean creeperguard$isCreeper() {
		return this.entity instanceof CreeperEntity;
	}

	/**
	 * Block-Schutz an der tatsächlichen Zerstör-Schleife {@code destroyBlocks}.
	 * Wir filtern die Liste, die dort abgearbeitet wird, direkt vor Ort.
	 *
	 * <p>Bewusst NICHT an {@code getBlocksToDestroy}: Explosions-Optimierungen wie
	 * Lithium umgehen diese Methode (sie liefert dann eine leere Liste) und reichen
	 * die betroffenen Blöcke direkt an {@code destroyBlocks} weiter. {@code destroyBlocks}
	 * ist der einzige Punkt, den sowohl Vanilla als auch Lithium garantiert durchlaufen.
	 */
	@Inject(method = "destroyBlocks", at = @At("HEAD"))
	private void creeperguard$filterDestroyedBlocks(List<BlockPos> positions, CallbackInfo ci) {
		ZoneStore store = ZoneStore.get(this.world.getServer());
		if (store.isEmpty()) {
			return;
		}
		boolean creeper = creeperguard$isCreeper();
		try {
			Iterator<BlockPos> it = positions.iterator();
			while (it.hasNext()) {
				if (store.isProtected(it.next(), ProtectionCategory.BLOCKS, creeper)) {
					it.remove();
				}
			}
		} catch (UnsupportedOperationException e) {
			// Sollte die Liste ausnahmsweise unveränderlich sein, können wir nicht eingreifen.
			// Das darf nicht still passieren: Der Blockschutz ist dann wirkungslos.
			if (!creeperguard$immutableListWarned) {
				creeperguard$immutableListWarned = true;
				CreeperGuard.LOGGER.warn(
						"Blockliste der Explosion ist unveränderlich – Blockschutz konnte nicht angewendet werden. "
								+ "Vermutlich liefert eine andere Mod eine unveränderliche Liste an destroyBlocks. "
								+ "(Diese Warnung erscheint nur einmal pro Serverlauf.)", e);
			}
		}
	}

	/**
	 * Entity-Schutz: Die Entity-Abfrage der Explosion abfangen und geschützte Entities
	 * aus dem Ergebnis entfernen. Dadurch entfallen für sie Schaden UND Rückstoß.
	 * Spieler werden nie klassifiziert und bleiben somit voll betroffen.
	 */
	@Redirect(
			method = "damageEntities",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/world/ServerWorld;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"
			)
	)
	private List<Entity> creeperguard$filterEntities(ServerWorld world, @Nullable Entity except, Box box) {
		List<Entity> entities = world.getOtherEntities(except, box);
		ZoneStore store = ZoneStore.get(world.getServer());
		if (store.isEmpty()) {
			return entities;
		}
		boolean creeper = creeperguard$isCreeper();
		entities.removeIf(candidate -> {
			ProtectionCategory category = EntityClassifier.categoryOf(candidate);
			if (category == null) {
				return false;
			}
			return store.isProtected(candidate.getBlockPos(), category, creeper);
		});
		return entities;
	}
}
