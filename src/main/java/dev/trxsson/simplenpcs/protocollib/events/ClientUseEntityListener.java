package dev.trxsson.simplenpcs.protocollib.events;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.trxsson.simplenpcs.NPC;
import dev.trxsson.simplenpcs.SimpleNPCs;
import dev.trxsson.simplenpcs.events.ClickType;

public class ClientUseEntityListener implements ProtocolLibEvent {

    private final SimpleNPCs simpleNPCs;
    private final ProtocolManager manager;
    private final PacketListener listener;
    private boolean registered = false;

    public ClientUseEntityListener(SimpleNPCs simpleNPCs, ProtocolManager manager) {
        this.simpleNPCs = simpleNPCs;
        this.manager = manager;
        this.listener = new PacketAdapter(simpleNPCs.getPlugin(), PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                process(event);
            }
        };
        manager.addPacketListener(listener);
        registered = true;
    }

    @Override
    public void process(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        var entityID = packet.getIntegers().read(0);
        var action = packet.getEnumEntityUseActions().read(0).getAction();
        ClickType clickType;
        if (action == EnumWrappers.EntityUseAction.ATTACK) {
            clickType = ClickType.LEFT_CLICK;
        } else if (action == EnumWrappers.EntityUseAction.INTERACT && packet.getEnumEntityUseActions().read(0).getHand() == EnumWrappers.Hand.MAIN_HAND) {
            clickType = ClickType.RIGHT_CLICK;
        } else {
            return;
        }
        if (simpleNPCs.getIdMap().containsKey(entityID)) {
            NPC npc = simpleNPCs.getCachedNPCs().get(simpleNPCs.getIdMap().get(entityID));
            if (npc.getClickAction() != null) {
                npc.getClickAction().accept(clickType, event.getPlayer());
            }
        }
    }

    @Override
    public void unregister() {
        if (registered) {
            manager.removePacketListener(listener);
            registered = false;
        }
    }

    @Override
    public void loadAgain() {
        if (!registered) {
            manager.addPacketListener(listener);
            registered = true;
        }
    }
}
