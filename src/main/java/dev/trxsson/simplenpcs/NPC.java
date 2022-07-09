package dev.trxsson.simplenpcs;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.trxsson.simplenpcs.attributes.Skin;
import dev.trxsson.simplenpcs.events.ClickType;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

public abstract class NPC {

    protected final SimpleNPCs simpleNPCs;
    protected final Plugin plugin;
    protected final String teamName;
    protected final String identifier;
    protected String name;
    protected Skin skin;
    protected boolean nameInvisible;
    protected Location location;
    protected ServerPlayer serverPlayer;
    protected boolean watchingPlayer;
    protected int watchPlayerTask;
    protected boolean created;
    protected BiConsumer<ClickType, Player> clickAction;

    protected NPC(@NotNull SimpleNPCs simpleNPCs, @NotNull Plugin plugin, @Nullable String name, @NotNull String identifier, boolean nameInvisible, @NotNull Location location) {
        this.teamName = generateTeamName();
        this.simpleNPCs = simpleNPCs;
        this.plugin = plugin;
        this.name = name == null ? teamName : name;
        this.identifier = identifier;
        this.nameInvisible = nameInvisible;
        this.location = location;
        this.skin = new Skin("", "");
        this.clickAction = null;
    }

    public static class Global extends NPC {

        private final Set<Player> viewers = new HashSet<>();
        private boolean autoShow = true;

        protected Global(@NotNull SimpleNPCs simpleNPCs, @NotNull Plugin plugin, @Nullable String name, @NotNull String identifier, boolean nameInvisible, @NotNull Location location) {
            super(simpleNPCs, plugin, name, identifier, nameInvisible, location);
            create();
        }

        @Override
        public void create() {
            if (created) return;
            created = true;
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            ServerLevel world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
            gameProfile.getProperties().put("textures", new Property("textures", skin.getValue(), skin.getSignature()));
            serverPlayer = new ServerPlayer(server, world, gameProfile, null);
            serverPlayer.setYRot(location.getYaw());
            serverPlayer.setXRot(location.getPitch());
            serverPlayer.setPos(location.getX(), location.getY(), location.getZ());
            simpleNPCs.getCachedNPCs().put(identifier, this);
            simpleNPCs.getIdMap().put(serverPlayer.getId(), identifier);
            viewers.forEach(this::show);

            if (watchingPlayer) {
                Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    for (Player viewer : viewers) {
                        Location location1 = serverPlayer.getBukkitEntity().getLocation();
                        location1.setDirection(viewer.getLocation().subtract(location1).toVector());

                        ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;
                        connection.send(new ClientboundRotateHeadPacket(serverPlayer, (byte) (location1.getYaw() * 256F / 360F)));
                        connection.send(new ClientboundMoveEntityPacket.Rot(serverPlayer.getId(),
                                (byte) (location1.getYaw() * 256F / 360F),
                                (byte) (location1.getPitch() * 256F / 360F), false));
                    }
                }, 0, 1);
            }
        }

        @Override
        public void remove() {
            if (!created) return;
            created = false;
            viewers.forEach(this::hide);
            if (watchingPlayer) {
                Bukkit.getScheduler().cancelTask(watchPlayerTask);
            }
            serverPlayer.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            simpleNPCs.getCachedNPCs().remove(identifier);
            simpleNPCs.getIdMap().remove(serverPlayer.getId());
        }

        public void show(@NotNull Player player) {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
            connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, serverPlayer));
            connection.send(new ClientboundAddPlayerPacket(serverPlayer));

            SynchedEntityData entityData = serverPlayer.getEntityData();
            entityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 126);
            connection.send(new ClientboundSetEntityDataPacket(serverPlayer.getId(), entityData, true));

            if (isNameInvisible()) {
                Scoreboard scoreboard = player.getScoreboard();
                Team team = scoreboard.getTeam(teamName) != null ? scoreboard.getTeam(teamName) : scoreboard.registerNewTeam(teamName);
                Objects.requireNonNull(team);
                team.setPrefix("§8[NPC] ");
                team.setColor(ChatColor.DARK_GRAY);
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                team.addEntry(getName());
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, serverPlayer)), 60L);

            viewers.add(player);
        }

        public void hide(@NotNull Player player) {
            viewers.remove(player);
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
            connection.send(new ClientboundRemoveEntitiesPacket(serverPlayer.getId()));
            if (isNameInvisible()) {
                Scoreboard scoreboard = player.getScoreboard();
                Team team = scoreboard.getTeam(teamName);
                Objects.requireNonNull(team);
                team.unregister();
            }
        }

        public Set<Player> getViewers() {
            return viewers;
        }

        public boolean isAutoShow() {
            return autoShow;
        }

        public void setAutoShow(boolean autoShow) {
            this.autoShow = autoShow;
        }
    }

    public static class Personal extends NPC {

        private final Player viewer;

        public Personal(@NotNull SimpleNPCs simpleNPCs, @NotNull Plugin plugin, @Nullable String name, @NotNull String identifier, boolean nameInvisible, @NotNull Location location, @NotNull Player viewer) {
            super(simpleNPCs, plugin, name, identifier, nameInvisible, location);
            this.viewer = viewer;
            create();
        }

        @Override
        protected void create() {
            if (created) return;
            created = true;
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            ServerLevel world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
            serverPlayer = new ServerPlayer(server, world, gameProfile, null);
            serverPlayer.setYRot(location.getYaw());
            serverPlayer.setXRot(location.getPitch());
            serverPlayer.setPos(location.getX(), location.getY(), location.getZ());
            simpleNPCs.getCachedNPCs().put(identifier, this);
            simpleNPCs.getIdMap().put(serverPlayer.getId(), identifier);
            show();
        }

        @Override
        public void remove() {
            if (!created) return;
            created = false;
            simpleNPCs.getIdMap().remove(serverPlayer.getId());
            hide();
            serverPlayer.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            simpleNPCs.getCachedNPCs().remove(identifier);
        }

        public void show() {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;
            connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, serverPlayer));
            connection.send(new ClientboundAddPlayerPacket(serverPlayer));

            SynchedEntityData entityData = serverPlayer.getEntityData();
            entityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 126);
            connection.send(new ClientboundSetEntityDataPacket(serverPlayer.getId(), entityData, true));

            if (isNameInvisible()) {
                Scoreboard scoreboard = viewer.getScoreboard();
                Team team = scoreboard.getTeam(teamName) != null ? scoreboard.getTeam(teamName) : scoreboard.registerNewTeam(teamName);
                Objects.requireNonNull(team);
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                team.setPrefix("§8[NPC] ");
                team.setColor(ChatColor.DARK_GRAY);
                team.addEntry(getName());
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, serverPlayer));
            }, 60L);
        }

        public void hide() {
            ServerGamePacketListenerImpl connection = ((CraftPlayer) viewer).getHandle().connection;
            connection.send(new ClientboundRemoveEntitiesPacket(serverPlayer.getId()));
            if (isNameInvisible()) {
                Scoreboard scoreboard = viewer.getScoreboard();
                Team team = scoreboard.getTeam(teamName);
                Objects.requireNonNull(team);
                team.unregister();
            }
        }

        public Player getViewer() {
            return viewer;
        }
    }

    protected abstract void create();

    public abstract void remove();

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public boolean isNameInvisible() {
        return nameInvisible;
    }

    public void setNameInvisible(boolean nameInvisible) {
        this.nameInvisible = nameInvisible;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
    }

    public void setSkin(@NotNull Skin skin) {
        this.skin = skin;
    }

    public boolean isWatchingPlayer() {
        return watchingPlayer;
    }

    public void setWatchingPlayer(boolean watchingPlayer) {
        this.watchingPlayer = watchingPlayer;
    }

    public void update() {
        remove();
        create();
    }

    private String generateTeamName() {
        return "npc-" + (this instanceof NPC.Global ? "gbl" : "psl") + "_" + (new Random().nextInt(8999) + 1000);
    }

    public BiConsumer<ClickType, Player> getClickAction() {
        return clickAction;
    }

    public void setClickAction(BiConsumer<ClickType, Player> clickAction) {
        this.clickAction = clickAction;
    }

    public void disableClickAction() {
        this.clickAction = null;
    }
}
