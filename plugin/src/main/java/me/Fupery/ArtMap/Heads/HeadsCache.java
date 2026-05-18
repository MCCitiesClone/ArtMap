package me.Fupery.ArtMap.Heads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.api.Compatability.IBedrockPlayerSupport;
import me.Fupery.ArtMap.api.Compatability.IHeadsRetriever;
import me.Fupery.ArtMap.api.Compatability.IHeadsRetriever.HeadCacheResponeType;
import me.Fupery.ArtMap.api.Compatability.IHeadsRetriever.HeadCacheType;
import me.Fupery.ArtMap.api.Compatability.IHeadsRetriever.TextureData;
import me.Fupery.ArtMap.api.Exception.HeadFetchException;

/**
 * Heads handler to be used with caching head textures.
 * 
 * @author wispoffates
 */
public class HeadsCache {
	private static final String API_PROFILE_LINK = "https://sessionserver.mojang.com/session/minecraft/profile/";
	private static final String API_GEYSER_SKIN = "https://api.geysermc.org/v2/skin/";
	private static final long CACHE_SAVE_DELAY_TICKS = 100L;

	private static final Map<UUID, TextureData> textureCache = Collections.synchronizedMap(new HashMap<>());
	/** Map to convert names to UUIDs for players that have never logged in to the server. */
	private static final Map<String, UUID> nameToUUID = new HashMap<>();
	private final File cacheFile;
	private final ArtMap plugin;
	private final IBedrockPlayerSupport bedrockSupport;
	private volatile boolean cacheDirty;
	private BukkitTask pendingSaveTask;

	public HeadsCache(ArtMap plugin) {
		this(plugin, plugin.getConfiguration().HEAD_PREFETCH);
	}

	public HeadsCache(ArtMap plugin, boolean prefetch) {
		this.plugin = plugin;
		this.bedrockSupport = plugin.getCompatManager().getBedrockPlayerSupport();
		cacheFile = new File(plugin.getDataFolder(), "heads_cache.json");
		if (cacheFile.exists()) {
			loadCacheFile(cacheFile);
		}
		if (prefetch) {
			plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::initHeadCache,
					plugin.getConfiguration().HEAD_PREFETCH_DELAY);
		}
		textureCache.entrySet().forEach(entry -> nameToUUID.put(entry.getValue().name, entry.getKey()));
	}

	public void updateCache(UUID playerId) {
		updateTexture(playerId);
	}

	/**
	 * Snapshot the artist skin when saving artwork (player should be online).
	 */
	public void cacheArtistSkin(Player player) {
		if (player == null) {
			return;
		}
		UUID playerId = player.getUniqueId();
		if (isHeadCached(playerId)) {
			return;
		}
		try {
			IHeadsRetriever headsRetriever = ArtMap.instance().getCompatManager().getHeadsRetriever();
			Optional<TextureData> textData = headsRetriever.getTextureData(player);
			if (textData.isPresent()) {
				storeTexture(playerId, textData.get(), player.getName());
				return;
			}
		} catch (Exception e) {
			ArtMap.instance().getLogger().log(Level.FINE, "Could not snapshot artist skin from profile", e);
		}
		updateTexture(playerId);
	}

	public void flushCache() {
		if (pendingSaveTask != null) {
			pendingSaveTask.cancel();
			pendingSaveTask = null;
		}
		if (cacheDirty || cacheFile.exists()) {
			saveCacheFile(cacheFile);
			cacheDirty = false;
		}
	}

	private void initHeadCache() {
		int cached = 0;
		int mojang = 0;
		int geyser = 0;
		int server = 0;
		int failed = 0;
		int artistsCount = 0;
		try {
			List<UUID> artists = plugin.getArtDatabase().listArtists(UUID.randomUUID());
			artistsCount = artists.size();
			plugin.getLogger().info(MessageFormat.format(
					"Async load of {0} artists started. {1} retrieved from disk cache.",
					artists.size(), textureCache.size()));
			for (UUID artist : artists) {
				if (isHeadCached(artist)) {
					cached++;
				} else {
					HeadCacheResponeType response = updateTexture(artist);
					switch (response) {
						case MOJANG_API:
							mojang++;
							sleepPrefetchDelay();
							break;
						case GEYSER_API:
							geyser++;
							sleepPrefetchDelay();
							break;
						case NONE:
							failed++;
							if (plugin.getConfiguration().HEAD_FETCH_MOJANG
									|| plugin.getConfiguration().HEAD_FETCH_GEYSER) {
								sleepPrefetchDelay();
							}
							break;
						case SERVER:
							server++;
							break;
						default:
							break;
					}
				}
			}
		} catch (Exception e) {
			plugin.getLogger().log(Level.SEVERE, "Exception during prefetch!", e);
		}
		if ((cached + mojang + geyser) == 0 && artistsCount > 1) {
			plugin.getLogger().warning(
					"Could not preload any player heads! Is the server in offline mode and not behind a Bungeecord?");
		} else {
			plugin.getLogger().info(MessageFormat.format(
					"Loaded {0} from disk cache, {1} from server, {2} from mojang, {3} from Geyser out of {4} artists with {5} failures",
					cached, server, mojang, geyser, artistsCount - 1, failed));
			if (cached + mojang + geyser < artistsCount) {
				plugin.getLogger().info("Remaining artists will be loaded when needed.");
			}
		}
	}

	private void sleepPrefetchDelay() throws InterruptedException {
		Thread.sleep(plugin.getConfiguration().HEAD_PREFETCH_PERIOD);
	}

	private void loadCacheFile(File cacheFile) {
		try (FileReader reader = new FileReader(cacheFile)) {
			Gson gson = ArtMap.instance().getGson(true);
			Type collectionType = new TypeToken<Map<UUID, TextureData>>() {
			}.getType();
			Map<UUID, TextureData> loadedCache = gson.fromJson(reader, collectionType);
			if (loadedCache != null && !loadedCache.isEmpty()) {
				textureCache.putAll(loadedCache);
			} else {
				ArtMap.instance().getLogger().warning("HeadCache load was null? Creating new empty cache.");
			}
		} catch (Exception e) {
			ArtMap.instance().getLogger().log(Level.SEVERE, "Failure parsing head cache! Will start with an empty cache.",
					e);
		}
	}

	private synchronized void saveCacheFile(File cacheFile) {
		try (FileWriter writer = new FileWriter(cacheFile)) {
			Gson gson = ArtMap.instance().getGson(true);
			Type collectionType = new TypeToken<Map<UUID, TextureData>>() {
			}.getType();
			gson.toJson(textureCache, collectionType, writer);
		} catch (IOException e) {
			ArtMap.instance().getLogger().log(Level.SEVERE, "Failure writing head cache!", e);
		}
	}

	private void scheduleDebouncedSave() {
		cacheDirty = true;
		if (pendingSaveTask != null) {
			pendingSaveTask.cancel();
		}
		pendingSaveTask = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
			pendingSaveTask = null;
			if (cacheDirty) {
				saveCacheFile(cacheFile);
				cacheDirty = false;
			}
		}, CACHE_SAVE_DELAY_TICKS);
	}

	private void storeTexture(UUID playerId, TextureData data, String playerName) {
		textureCache.put(playerId, data);
		if (playerName != null) {
			nameToUUID.put(playerName, playerId);
		}
		scheduleDebouncedSave();
	}

	public ItemStack getHead(UUID playerId) throws HeadFetchException {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
		IHeadsRetriever headsRetriever = ArtMap.instance().getCompatManager().getHeadsRetriever();
		if (!isHeadCached(playerId)) {
			updateTexture(playerId);
		}
		TextureData textureData = textureCache.get(playerId);
		Optional<SkullMeta> meta = headsRetriever.getHeadMeta(playerId, textureData);
		if (!meta.isPresent()) {
			SkullMeta headmeta = (SkullMeta) head.getItemMeta();
			OfflinePlayer player = ArtMap.instance().getServer().getOfflinePlayer(playerId);
			if (player.hasPlayedBefore()) {
				headmeta.setOwningPlayer(player);
				headmeta.setDisplayName(player.getName());
				head.setItemMeta(headmeta);
			}
			return head;
		}
		head.setItemMeta(meta.get());
		return head;
	}

	public boolean isHeadCached(UUID playerId) {
		return textureCache.containsKey(playerId);
	}

	public String getPlayerName(UUID playerId) {
		if (textureCache.containsKey(playerId)) {
			return textureCache.get(playerId).name;
		}
		return null;
	}

	public List<String> searchCache(String term) {
		return nameToUUID.keySet().stream().filter(name -> name.contains(term)).collect(Collectors.toList());
	}

	public Optional<UUID> getPlayerUUID(String playername) {
		return Optional.ofNullable(nameToUUID.get(playername));
	}

	protected HeadCacheResponeType updateTexture(UUID playerId) {
		try {
			OfflinePlayer player = ArtMap.instance().getServer().getOfflinePlayer(playerId);
			if (player.hasPlayedBefore()) {
				IHeadsRetriever headsRetriever = ArtMap.instance().getCompatManager().getHeadsRetriever();
				Optional<TextureData> textData = headsRetriever.getTextureData(player);
				if (textData.isPresent()) {
					storeTexture(playerId, textData.get(), player.getName());
					return HeadCacheResponeType.SERVER;
				}
			}

			boolean bedrock = bedrockSupport.isFloodgatePlayer(playerId);
			if (bedrock && plugin.getConfiguration().HEAD_FETCH_GEYSER) {
				Optional<TextureData> geyserData = getGeyserSkin(playerId, player.getName());
				if (geyserData.isPresent()) {
					storeTexture(playerId, geyserData.get(), geyserData.get().name);
					return HeadCacheResponeType.GEYSER_API;
				}
			}

			if (!bedrock && plugin.getConfiguration().HEAD_FETCH_MOJANG) {
				Optional<TextureData> data = getSkinUrl(playerId);
				if (data.isPresent()) {
					storeTexture(playerId, data.get(), data.get().name);
					return HeadCacheResponeType.MOJANG_API;
				}
			}
		} catch (Exception e) {
			ArtMap.instance().getLogger().log(Level.FINE, "Headfetch failure!", e);
		}
		return HeadCacheResponeType.NONE;
	}

	public int getCacheSize() {
		return textureCache.size();
	}

	private Optional<TextureData> getGeyserSkin(UUID playerId, String fallbackName) throws HeadFetchException {
		Optional<String> xuid = bedrockSupport.getXuid(playerId);
		if (!xuid.isPresent()) {
			return Optional.empty();
		}
		String json = getContent(API_GEYSER_SKIN + xuid.get());
		if (json == null || json.isEmpty()) {
			return Optional.empty();
		}
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		if (!root.has("value")) {
			return Optional.empty();
		}
		String value = root.get("value").getAsString();
		String name = bedrockSupport.getUsername(playerId).orElse(fallbackName);
		if (name == null || name.isEmpty()) {
			name = decodeProfileName(value).orElse("Bedrock");
		}
		return Optional.of(new TextureData(name, value, HeadCacheType.PROFILE));
	}

	private static Optional<String> decodeProfileName(String base64Value) {
		try {
			String json = new String(java.util.Base64.getDecoder().decode(base64Value));
			JsonObject profile = JsonParser.parseString(json).getAsJsonObject();
			if (profile.has("profileName")) {
				return Optional.of(profile.get("profileName").getAsString());
			}
		} catch (Exception ignored) {
			// fall through
		}
		return Optional.empty();
	}

	private static String getContent(String link) throws HeadFetchException {
		BufferedReader br = null;
		try {
			URL url = new URI(link).toURL();
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuilder sb = new StringBuilder();
			while ((inputLine = br.readLine()) != null) {
				sb.append(inputLine);
			}
			return sb.toString();
		} catch (MalformedURLException | URISyntaxException e) {
			ArtMap.instance().getLogger().log(Level.SEVERE, "Failure getting head!", e);
			throw new HeadFetchException("Failure getting head!", e);
		} catch (IOException e) {
			ArtMap.instance().getLogger().info(
					"Error retrieving head texture. The head will be fetched on use later.");
			throw new HeadFetchException(
					"Error retrieving head texture. The head will be fetched on use later.", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ignored) {
					// don't care on close
				}
			}
		}
	}

	private static Optional<TextureData> getSkinUrl(UUID uuid) throws HeadFetchException {
		String id = uuid.toString().replace("-", "");
		try {
			String json = getContent(API_PROFILE_LINK + id);
			if (json == null) {
				throw new HeadFetchException("Skin texture could not be loaded! invalid uuid!");
			}
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			String name = o.get("name").getAsString();
			JsonArray jArray = o.get("properties").getAsJsonArray();
			if (jArray.size() > 0) {
				String jsonBase64 = jArray.get(0).getAsJsonObject().get("value").getAsString();
				return Optional.of(new TextureData(name, jsonBase64, HeadCacheType.PROFILE));
			}
			return Optional.empty();
		} catch (Throwable e) {
			throw new HeadFetchException(API_PROFILE_LINK + id
					+ " :: Failure parsing skin texture json. You may ignore this warning.", e);
		}
	}
}
