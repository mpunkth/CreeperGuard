package com.mpunkth.creeperguard;

import com.mpunkth.creeperguard.command.CreeperGuardCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreeperGuard implements ModInitializer {
	public static final String MOD_ID = "creeperguard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> CreeperGuardCommand.register(dispatcher)
		);
		LOGGER.info("CreeperGuard initialisiert – /creeperguard ist verfügbar.");
	}
}
