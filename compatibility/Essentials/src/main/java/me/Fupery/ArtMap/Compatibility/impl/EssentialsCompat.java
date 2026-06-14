package me.Fupery.ArtMap.Compatibility.impl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import me.Fupery.ArtMap.api.IArtMap;
import me.Fupery.ArtMap.api.Compatability.EventListener;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.StateChangeEvent;

public class EssentialsCompat implements EventListener {
    private static final long ACTIVITY_PULSE_COOLDOWN_MS = 3000L;
    private final IArtMap artmap;
    private final Essentials essentials;
    private final ConcurrentHashMap<UUID, Long> lastActivityPulse = new ConcurrentHashMap<>();

    public EssentialsCompat(IArtMap artmap) {
        this.artmap = artmap;
        this.essentials = Essentials.getPlugin(Essentials.class);
        artmap.getServer().getPluginManager().registerEvents(this, artmap);
    }

    @EventHandler
    public void onAFKEvent(AfkStatusChangeEvent event) {
        try {
            Player player = event.getAffected().getBase();
            if(artmap.getArtistHandler().containsPlayer(player) && player.hasPermission("artmap.ignore.afk")) {
                    event.setCancelled(true);
            }
        } catch (Exception e) {
            artmap.getLogger().log(Level.SEVERE, "Error interteracting with MarriageMaster!", e);
        }
    }

    public void notifyPaintingActivity(Player player) {
        if (player == null || !artmap.getArtistHandler().containsPlayer(player)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lastPulse = lastActivityPulse.get(player.getUniqueId());
        if (lastPulse != null && (now - lastPulse) < ACTIVITY_PULSE_COOLDOWN_MS) {
            return;
        }
        lastActivityPulse.put(player.getUniqueId(), now);

        try {
            User user = essentials.getUser(player);
            if (user == null) {
                return;
            }
            user.updateActivityOnInteract(false);
            user.setAfk(false, AfkStatusChangeEvent.Cause.INTERACT);
        } catch (Exception e) {
            artmap.getLogger().log(Level.FINE, "Error updating Essentials activity for painter.", e);
        }
    }

    @Override
    public void unregister() {
        StateChangeEvent.getHandlerList().unregister(this);
    }

    @Override
    public boolean isLoaded() {
        return true;
    }
}
