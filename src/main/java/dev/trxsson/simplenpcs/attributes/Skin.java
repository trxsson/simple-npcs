package dev.trxsson.simplenpcs.attributes;

import com.mojang.authlib.properties.Property;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Skin {

    private final String value;
    private final String signature;

    public Skin(@NotNull String value, @NotNull String signature) {
        this.value = value;
        this.signature = signature;
    }

    public Skin(@NotNull Player player) {
        Property property = (Property) ((CraftPlayer) player).getHandle().getGameProfile().getProperties().get("textures").toArray()[0];
        this.value = property.getValue();
        this.signature = property.getSignature();
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }
}
