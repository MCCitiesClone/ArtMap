package me.Fupery.ArtMap.api.Compatability;

import java.util.Optional;
import java.util.UUID;

/**
 * Optional Floodgate integration for resolving Bedrock player identity.
 */
public interface IBedrockPlayerSupport {

	boolean isLoaded();

	boolean isFloodgatePlayer(UUID uuid);

	/** Bedrock XUID as a decimal string for the Geyser skin API. */
	Optional<String> getXuid(UUID uuid);

	Optional<String> getUsername(UUID uuid);
}
