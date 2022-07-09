package dev.trxsson.simplenpcs.events;

import dev.trxsson.simplenpcs.SimpleNPCs;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final SimpleNPCs simpleNPCs;

    public PlayerJoinListener(SimpleNPCs simpleNPCs) {
        this.simpleNPCs = simpleNPCs;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        simpleNPCs.getCachedGlobalNPCs().values().forEach(npc -> {
            if (npc.isAutoShow()) {
                Bukkit.getScheduler().runTaskLater(simpleNPCs.getPlugin(), () -> {
                    npc.show(event.getPlayer());
                }, 5L);
            }
        });
    }
}