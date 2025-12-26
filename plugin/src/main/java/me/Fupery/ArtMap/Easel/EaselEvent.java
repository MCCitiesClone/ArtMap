package me.Fupery.ArtMap.Easel;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.api.Config.Lang;
import me.Fupery.ArtMap.IO.MapArt;
import me.Fupery.ArtMap.IO.Database.Map;
import me.Fupery.ArtMap.Recipe.ArtItem;
import me.Fupery.ArtMap.Recipe.ArtMaterial;
import me.Fupery.ArtMap.Utils.ItemUtils;
import me.Fupery.ArtMap.api.Easel.ClickType;
import me.Fupery.ArtMap.api.Exception.ArtMapException;
import net.md_5.bungee.api.ChatColor;

public final class EaselEvent {
	private final Easel easel;
	private final ClickType click;
	private final Player player;

	public EaselEvent(Easel easel, ClickType click, Player player) {
		this.easel = easel;
		this.click = click;
		this.player = player;
	}

	public void callEvent() {
		if (!player.hasPermission("artmap.artist")) {
			Lang.NO_PERM.send(player);
			return;
		}
		if (easel.isBeingUsed()) {
			Lang.ActionBar.ELSE_USING.send(player);
			easel.playEffect(EaselEffect.USE_DENIED);
			return;
		}
		if (ArtMap.instance().getPreviewManager().endPreview(player))
			return;

		switch (click) {
			case LEFT_CLICK:
				Lang.ActionBar.EASEL_HELP.send(player);
				return;
			case RIGHT_CLICK:
				if (easel.getItem().getType() == Material.FILLED_MAP) {
					// If the easel has a canvas, player rides the easel
					try {
						Optional<Integer> mapId = ItemUtils.getMapID(easel.getItem());
						if(mapId.isPresent()) {
							ArtMap.instance().getArtistHandler().addPlayer(player, easel,
								new Map(mapId.get()),
								EaselPart.getYawOffset(easel.getFacing()));
						} else {
							ArtMap.instance().getLogger().warning("Broken map on easel for player " + player.getName());
							Lang.ERROR_ON_EASEL.send(player);
						}
					} catch (Exception e) {
						ArtMap.instance().getLogger().log(Level.SEVERE, "Failure having player mount the Easel!", e);
					}
					return;
				} else if (easel.getItem().getType() != Material.AIR) {
					// remove items that were added while instance is unloaded etc.
					try {
						easel.removeItem();
					} catch (SQLException | ArtMapException e) {
						ArtMap.instance().getLogger().log(Level.SEVERE, "Unexpected item on Easel!", e);
					}
					return;
				}
				ItemStack itemInHand = player.getInventory().getItemInMainHand();
				ArtMaterial material = ArtMaterial.getCraftItemType(itemInHand);

				if (material == ArtMaterial.CANVAS) {
					if (easel.getItem().getType() != Material.AIR) {
						return;
					}
					ItemStack removed = itemInHand.clone();
					removed.setAmount(1);
					java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().removeItem(removed);
					if (!leftover.isEmpty()) {
						return;
					}
					Map map = null;
					try {
						map = ArtMap.instance().getArtDatabase().createMap();
					} catch (NoSuchFieldException | IllegalAccessException e) {
						java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
						if (!addLeftover.isEmpty()) {
							easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
						}
						player.sendMessage(
							ChatColor.RED + " Severe Error.  Pleae contact a server Admin! " + ChatColor.RESET);
						ArtMap.instance().getLogger().log(Level.SEVERE, "Error creating map!", e);
						return;
					}
					if (map == null) {
						java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
						if (!addLeftover.isEmpty()) {
							easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
						}
						player.sendMessage(
								ChatColor.RED + " Severe Error.  Pleae contact a server Admin! " + ChatColor.RESET);
						return;
					}
					try {
						map.update(player);
						mountCanvas(removed, new Canvas(map,player.getName()));
					} catch (Exception e) {
						java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
						if (!addLeftover.isEmpty()) {
							easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
						}
						player.sendMessage("Error placing canvas on the Easel!");
						ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing canvas on easel!",e );
					}
			} else if (ArtItem.isArtwork(itemInHand)) {
				// Edit an artwork on the easel
				ArtMap.instance().getScheduler().ASYNC.run(() -> {
					Optional<MapArt> art;
					int id;
					boolean unsaved;
					try {
						id = ItemUtils.getMapID(itemInHand).orElseThrow(()-> new ArtMapException("Artwork does not have a mapview!"));
						art = ArtMap.instance().getArtDatabase().getArtwork(id);
						unsaved = ArtMap.instance().getArtDatabase().containsUnsavedArtwork(id);
					} catch(Exception e) {
						player.sendMessage("Error placing art on the Easel!");
						ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!",e );
						return;
					}
					final int finalId = id;
					ArtMap.instance().getScheduler().SYNC.run(() -> {
						if (easel.getItem().getType() != Material.AIR) {
							return;
						}
						ItemStack currentItem = player.getInventory().getItemInMainHand();
						if (!ArtItem.isArtwork(currentItem)) {
							return;
						}
						Optional<Integer> currentId;
						try {
							currentId = ItemUtils.getMapID(currentItem);
						} catch (ArtMapException e) {
							return;
						}
						if (!currentId.isPresent() || currentId.get() != finalId) {
							return;
						}
						ItemStack removed = currentItem.clone();
						removed.setAmount(1);
						java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().removeItem(removed);
						if (!leftover.isEmpty()) {
							return;
						}
						if (art.isPresent()) {
							if (!player.getUniqueId().equals(art.get().getArtistPlayer().getUniqueId()) && !player.hasPermission("artmap.admin")) {
								java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
								if (!addLeftover.isEmpty()) {
									easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
								}
								Lang.ActionBar.NO_EDIT_PERM.send(player);
								easel.playEffect(EaselEffect.USE_DENIED);
								return;
							}
							try {
								Canvas canvas = new Canvas.CanvasCopy(art.get().getMap().cloneMap(), art.get());
								mountCanvas(removed, canvas);
							} catch (Exception e) {
								java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
								if (!addLeftover.isEmpty()) {
									easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
								}
								player.sendMessage("Error placing art on the Easel!");
								ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!",e );
							}
						} else if ( unsaved ) {
							try {
								Canvas canvas = new Canvas(id, player.getName());
								mountCanvas(removed, canvas);
							} catch (Exception e) {
								java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
								if (!addLeftover.isEmpty()) {
									easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
								}
								player.sendMessage("Error placing art on the Easel!");
								ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!",e );
							}
						} else {
							java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
							if (!addLeftover.isEmpty()) {
								easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
							}
							Lang.ActionBar.NEED_CANVAS.send(player);
							easel.playEffect(EaselEffect.USE_DENIED);
						}
					});
				});
			} else {
				Lang.ActionBar.NEED_CANVAS.send(player);
				easel.playEffect(EaselEffect.USE_DENIED);
			}
			return;

		case SHIFT_RIGHT_CLICK:
			/*
			if (easel.hasItem()) {
				this.player.sendMessage(Lang.SAVE_ARTWORK.get());
				this.player.sendMessage(Lang.SAVE_ARTWORK_2.get());
				return;
			}*/
			easel.breakEasel();
		}
	}

	private void mountCanvas(ItemStack itemInHand, Canvas canvas) {
		if (!easel.mountCanvas(canvas)) {
			java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(itemInHand);
			if (!addLeftover.isEmpty()) {
				easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
			}
			return;
		}
	}
}
