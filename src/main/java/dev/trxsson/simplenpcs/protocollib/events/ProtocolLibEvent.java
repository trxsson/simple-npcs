package dev.trxsson.simplenpcs.protocollib.events;

import com.comphenix.protocol.events.PacketEvent;

public interface ProtocolLibEvent {

    void process(PacketEvent event);

    void unregister();

    void loadAgain();

}
