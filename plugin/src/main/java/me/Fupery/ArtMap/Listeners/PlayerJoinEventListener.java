package me.Fupery.ArtMap.Listeners;

import me.Fupery.ArtMap.ArtMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

class PlayerJoinEventListener implements RegisteredListener {
    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        // update the playes skin in the cache
        ArtMap.instance().getScheduler().ASYNC.run(() -> 
            ArtMap.instance().getHeadsCache().updateCache(event.getPlayer().getUniqueId())
        );
    }

    @Override
    public void unregister() {
        PlayerJoinEvent.getHandlerList().unregister(this);
    }
}