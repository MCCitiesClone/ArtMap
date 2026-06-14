package me.Fupery.ArtMap.Compatibility.impl;

import java.util.Optional;
import java.util.UUID;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import me.Fupery.ArtMap.api.Compatability.IBedrockPlayerSupport;

public class FloodgateCompat implements IBedrockPlayerSupport {

	private final FloodgateApi api;

	public FloodgateCompat() {
		this.api = FloodgateApi.getInstance();
	}

	@Override
	public boolean isLoaded() {
		return api != null;
	}

	@Override
	public boolean isFloodgatePlayer(UUID uuid) {
		return api.isFloodgatePlayer(uuid);
	}

	@Override
	public Optional<String> getXuid(UUID uuid) {
		if (!api.isFloodgatePlayer(uuid)) {
			return Optional.empty();
		}
		FloodgatePlayer player = api.getPlayer(uuid);
		if (player != null) {
			return Optional.of(player.getXuid());
		}
		return Optional.of(Long.toUnsignedString(uuid.getLeastSignificantBits()));
	}

	@Override
	public Optional<String> getUsername(UUID uuid) {
		FloodgatePlayer player = api.getPlayer(uuid);
		if (player != null) {
			return Optional.of(player.getCorrectUsername());
		}
		return Optional.empty();
	}
}
