package dev.trxsson.simplenpcs;

import com.comphenix.protocol.ProtocolLibrary;
import dev.trxsson.simplenpcs.events.EntityDamageByEntityListener;
import dev.trxsson.simplenpcs.events.PlayerInteractAtEntityListener;
import dev.trxsson.simplenpcs.events.PlayerJoinListener;
import dev.trxsson.simplenpcs.protocollib.events.ClientUseEntityListener;
import dev.trxsson.simplenpcs.protocollib.events.ProtocolLibEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleNPCs {

    private static SimpleNPCs instance;
    private final HashMap<Integer, String> idMap = new HashMap<>();
    private final HashMap<String, NPC> cachedNPCs = new HashMap<>();
    private final List<ProtocolLibEvent> protocolLibEvents = new ArrayList<>();
    private final Plugin plugin;

    /**
     * The constructor of NPCLibrary
     * @param plugin the Bukkit plugin the api is used for
     */
    private SimpleNPCs(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerInteractAtEntityListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EntityDamageByEntityListener(this), plugin);
        protocolLibEvents.add(new ClientUseEntityListener(this, ProtocolLibrary.getProtocolManager()));
    }


    /**
     * Gets the instance of NPCLibrary for a specific plugin
     * @param plugin the Bukkit plugin the api is used for
     * @return the NPCLibrary instance for the provided plugin which then can be interacted with
     */
    public static SimpleNPCs get(Plugin plugin) {
        if (instance == null) {
            instance = new SimpleNPCs(plugin);
        }
        return instance;
    }

    /**
     * Generates and returns a new global NPC which by default is automatically visible for all players on the server. It can be hidden and again shown though.
     * @param name the display name for the NPC; null is replaced by the random generated team name
     * @param identifier a unique identifier by which the NPC will be stored and can be received
     * @param nameInvisible if the name of the NPC should be invisible
     * @param location the location where the NPC should be spawned
     * @return the NPC.Global object which then can be interacted with
     */
    public NPC.Global generateGlobalNPC(@Nullable String name, @NotNull String identifier, boolean nameInvisible, @NotNull Location location) {
        NPC.Global npc = new NPC.Global(this, plugin, name, identifier, nameInvisible, location);
        cachedNPCs.put(identifier, npc);
        return npc;
    }

    /**
     * Generates and returns a new personal NPC which by default is automatically visible to the provided Player object.
     * @param name the display name for the NPC; null is replaced by the random generated team name
     * @param identifier a unique identifier by which the NPC will be stored and can be received
     * @param nameInvisible if the name of the NPC should be invisible
     * @param location the location where the NPC should be spawned
     * @param viewer the Player object which the NPC should be visible for
     * @return the NPC.Personal object which then can be interacted with
     */
    public NPC.Personal generatePersonalNPC(@Nullable String name, @NotNull String identifier, boolean nameInvisible, @NotNull Location location, @NotNull Player viewer) {
        NPC.Personal npc = new NPC.Personal(this, plugin, name, identifier, nameInvisible, location, viewer);
        cachedNPCs.put(identifier, npc);
        return npc;
    }

    /**
     * Returns a HashMap of all NPCs mapped by their final identifiers which have to be provided when creating new NPCs
     * @return a list of all registered NPCs mapped by their identifiers
     */
    public HashMap<String, NPC> getCachedNPCs() {
        return cachedNPCs;
    }

    /**
     * Returns a HashMap of all global NPCs mapped by their final identifiers which have to be provided when creating new NPCs
     * @return a list of all registered global NPCs mapped by their identifiers
     */
    public HashMap<String, NPC.Global> getCachedGlobalNPCs() {
        return (HashMap<String, NPC.Global>) cachedNPCs.values()
                .stream()
                .filter(npc -> npc instanceof NPC.Global)
                .collect(Collectors.toMap(NPC::getIdentifier, npc -> (NPC.Global) npc));
    }

    /**
     * Returns a HashMap of all personal NPCs mapped by their final identifiers which have to be provided when creating new NPCs
     * @return a list of all registered personal NPCs mapped by their identifiers
     */
    public HashMap<String, NPC.Personal> getCachedPersonalNPCs() {
        return (HashMap<String, NPC.Personal>) cachedNPCs.values()
                .stream()
                .filter(npc -> npc instanceof NPC.Personal)
                .collect(Collectors.toMap(NPC::getIdentifier, npc -> (NPC.Personal) npc));
    }

    /**
     * Returns a HashMap of the identifiers of all registered NPCs mapped by their entity ids which are provided by the server
     * @return a list of the identifiers of all registered NPCs mapped by their entity ids
     */
    public HashMap<Integer, String> getIdMap() {
        return idMap;
    }

    /**
     * Returns the Bukkit plugin associated with this instance of NPCLibrary
     * @return the plugin associated with this NPCLibrary instance
     */
    public Plugin getPlugin() {
        return plugin;
    }
}
