package dev.trxsson.simplenpcs.events;

import dev.trxsson.simplenpcs.NPC;
import dev.trxsson.simplenpcs.SimpleNPCs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageByEntityListener implements Listener {

    private final SimpleNPCs simpleNPCs;

    public EntityDamageByEntityListener(SimpleNPCs simpleNPCs) {
        this.simpleNPCs = simpleNPCs;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        System.out.println(EntityDamageByEntityEvent.class.getSimpleName());
        if (simpleNPCs.getIdMap().containsKey(event.getEntity().getEntityId())) {
            NPC npc = simpleNPCs.getCachedNPCs().get(simpleNPCs.getIdMap().get(event.getEntity().getEntityId()));
            npc.getClickAction().accept(ClickType.RIGHT_CLICK, (Player) event.getDamager());
        }
    }

}
