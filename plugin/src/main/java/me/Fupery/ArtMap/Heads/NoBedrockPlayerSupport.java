package me.Fupery.ArtMap.Heads;

import java.util.Optional;
import java.util.UUID;

import me.Fupery.ArtMap.api.Compatability.IBedrockPlayerSupport;

/**
 * Fallback when Floodgate is not installed. Uses Floodgate UUID encoding only.
 */
public class NoBedrockPlayerSupport implements IBedrockPlayerSupport {

	@Override
	public boolean isLoaded() {
		return false;
	}

	@Override
	public boolean isFloodgatePlayer(UUID uuid) {
		return isFloodgateUuid(uuid);
	}

	@Override
	public Optional<String> getXuid(UUID uuid) {
		if (!isFloodgateUuid(uuid)) {
			return Optional.empty();
		}
		return Optional.of(Long.toUnsignedString(uuid.getLeastSignificantBits()));
	}

	@Override
	public Optional<String> getUsername(UUID uuid) {
		return Optional.empty();
	}

	static boolean isFloodgateUuid(UUID uuid) {
		return uuid.getMostSignificantBits() == 0L && (uuid.getLeastSignificantBits() >>> 52) == 9L;
	}
}
