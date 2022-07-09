package dev.trxsson.simplenpcs.events;

import dev.trxsson.simplenpcs.NPC;
import dev.trxsson.simplenpcs.SimpleNPCs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractAtEntityListener implements Listener {

    private final SimpleNPCs simpleNPCs;

    public PlayerInteractAtEntityListener(SimpleNPCs simpleNPCs) {
        this.simpleNPCs = simpleNPCs;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        System.out.println(PlayerInteractAtEntityEvent.class.getSimpleName());
        if (simpleNPCs.getIdMap().containsKey(event.getRightClicked().getEntityId()) && event.getHand() == EquipmentSlot.HAND) {
            NPC npc = simpleNPCs.getCachedNPCs().get(simpleNPCs.getIdMap().get(event.getRightClicked().getEntityId()));
            npc.getClickAction().accept(ClickType.RIGHT_CLICK, event.getPlayer());
        }
    }

}
