package me.Fupery.ArtMap.Easel;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import javax.validation.constraints.NotNull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.ArtMap.IO.MapArt;
import me.Fupery.ArtMap.IO.Database.Map;
import me.Fupery.ArtMap.Recipe.ArtItem;
import me.Fupery.ArtMap.Recipe.ArtItem.InProgressArtworkItem;
import me.Fupery.ArtMap.Utils.ItemUtils;
import me.Fupery.ArtMap.api.Config.Lang;
import me.Fupery.ArtMap.api.Exception.ArtMapException;

/**
 * Represents a painting canvas..
 *
 */
public class Canvas {

	protected int mapId;
	protected String artist;
	protected final CanvasSize size;

	public Canvas(Map map, String artist) {
		this(map, artist, CanvasSize.defaultSize());
	}

	public Canvas(Map map, String artist, CanvasSize size) {
		this(map.getMapId(), artist, size);
	}

	protected Canvas(int mapId, String artist) {
		this(mapId, artist, CanvasSize.defaultSize());
	}

	protected Canvas(int mapId, String artist, CanvasSize size) {
		this.mapId = mapId;
		this.artist = artist;
		this.size = size == null ? CanvasSize.defaultSize() : size;
	}

	public CanvasSize getSize() {
		return size;
	}

	@NotNull
	/**
	 * Retrieve the canvas of an Artwork.
	 * 
	 * @param item The Artwork to get the canvas of.
	 * @return The canvas if it can be determined or empty if it fails usually mapview being absent.
	 * @throws SQLException Failure getting artwork from the database.
	 * @throws ArtMapException A generic failure parsing the artmap/
	 */
	public static Optional<Canvas> getCanvas(ItemStack item) throws SQLException, ArtMapException {
		if (item == null || item.getType() != Material.FILLED_MAP) {
			throw new ArtMapException("Artmap tried to getCanvas() on something that is not a filled map? :: " + (item==null ? "NULL item" : item.getType()+""));
		}

		OptionalInt optMapId = ItemUtils.getMapID(item);
		if (optMapId.isEmpty()) {
			return Optional.empty();
		}
		int mapId = optMapId.getAsInt();
		MapMeta meta = (MapMeta) item.getItemMeta();
		CanvasSize itemSize = parseCanvasSize(meta);

		if(ArtItem.isUnfinishedArtwork(item)) {
			return Optional.of(new Canvas(mapId, parseArtist(meta.getLore()).orElse("unknown"), itemSize));
		}
		
		if(ArtItem.isCopyArtwork(item)) {
			Optional<MapArt> original = ArtMap.instance().getArtDatabase().getArtwork(meta.getDisplayName());
			if(original.isPresent()) {
				CanvasSize size = resolveStoredSize(mapId, itemSize);
				return Optional.of(new CanvasCopy(new Map(mapId), original.get(), size));
			} else {
				return Optional.of(new Canvas(new Map(mapId), parseArtist(meta.getLore()).orElse("unknown"), itemSize));
			}
		}

		if(ArtMap.instance().getArtDatabase().containsUnsavedArtwork(mapId)){
			CanvasSize size = resolveStoredSize(mapId, itemSize);
			return Optional.of(new Canvas(mapId, "unknown", size));
		} 
		Optional<MapArt> art = ArtMap.instance().getArtDatabase().getArtwork(mapId);
		if(art.isPresent()) {
			CanvasSize size = resolveStoredSize(mapId, itemSize);
			return Optional.of(new CanvasCopy(art.get().getMap(), art.get(), size));
		}
		return Optional.empty();
	}

	private static CanvasSize resolveStoredSize(int mapId, CanvasSize itemSize) throws SQLException {
		CanvasSize stored = ArtMap.instance().getArtDatabase().getMapCanvasSize(mapId);
		if (itemSize != CanvasSize.defaultSize() && stored != itemSize) {
			return itemSize;
		}
		return stored;
	}

	public static CanvasSize parseCanvasSize(ItemMeta meta) {
		if (meta != null && meta.hasLore()) {
			return ArtItem.parseCanvasSize(meta.getLore());
		}
		return CanvasSize.defaultSize();
	}

	public static Optional<String> parseArtist(List<String> meta) {
		String key = Lang.RECIPE_ARTWORK_ARTIST.get().replace("%s", "").trim();
		Optional<String> artistName = meta.stream().filter(s -> s.contains(key)).findFirst();
		if(artistName.isPresent()) {
			return Optional.of(artistName.get().replace(key, "").trim());
		}
		return Optional.empty();
	}

	public ItemStack getEaselItem() {
		return new InProgressArtworkItem(this.mapId, artist, size).toItemStack();
	}

	public int getMapId() {
		return this.mapId;
	}

	public static class CanvasCopy extends Canvas {

		private MapArt original;

		public CanvasCopy(Map map, MapArt original) {
			this(map, original, CanvasSize.defaultSize());
		}

		public CanvasCopy(Map map, MapArt original, CanvasSize size) {
			super(map, original.getArtistName(), size);
			this.original = original;
		}

		@Override
		public ItemStack getEaselItem() {
			return new ArtItem.CopyArtworkItem(this.mapId, original.getTitle(), original.getArtistName(), original.getDate(), size).toItemStack();
		}

		public int getOriginalId() {
			return this.original.getMapId();
		}
    }
}
