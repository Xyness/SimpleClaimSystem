package fr.xyness.SCS.Listeners;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.EventExecutor;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.WorldMode;

/**
 * Protects claimed containers from copper golems (Minecraft 1.21.9+).
 * Uses a Paper event via reflection, so it compiles on older APIs and is a no-op without it.
 */
public class CopperGolemEvents implements Listener {

    /** Fully-qualified name of the Paper event, absent on older servers/Spigot. */
    private static final String EVENT_CLASS = "io.papermc.paper.event.entity.ItemTransportingEntityValidateTargetEvent";

    /** Entity type name of the copper golem. */
    private static final String COPPER_GOLEM = "COPPER_GOLEM";

    /** Instance of SimpleClaimSystem. */
    private final SimpleClaimSystem instance;

    /** PDC key storing the creator's UUID on the golem. */
    private final NamespacedKey creatorKey;

    /** Cache golemUUID -> creatorUUID. */
    private final Map<UUID, UUID> golemCreators = new ConcurrentHashMap<>();

    /** Reflective accessors for the event. */
    private Method getEntityMethod;
    private Method getBlockMethod;
    private Method setAllowedMethod;

    /**
     * Constructor.
     *
     * @param instance the SimpleClaimSystem instance.
     */
    public CopperGolemEvents(SimpleClaimSystem instance) {
        this.instance = instance;
        this.creatorKey = new NamespacedKey(instance, "copper-golem-creator");
    }

    /**
     * Registers the listeners if the server exposes the copper golem event.
     *
     * @return true if registered, false otherwise.
     */
    @SuppressWarnings("unchecked")
    public boolean register() {
        Class<? extends Event> eventClass;
        try {
            eventClass = (Class<? extends Event>) Class.forName(EVENT_CLASS);
            getEntityMethod = eventClass.getMethod("getEntity");
            getBlockMethod = eventClass.getMethod("getBlock");
            setAllowedMethod = eventClass.getMethod("setAllowed", boolean.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
        try {
            EventExecutor executor = (listener, event) -> handle(event);
            instance.getServer().getPluginManager()
                    .registerEvent(eventClass, this, EventPriority.HIGH, executor, instance, false);
            instance.getServer().getPluginManager().registerEvents(this, instance);
            cacheLoadedGolems();
            return true;
        } catch (Throwable t) {
            instance.getLogger().warning("Could not register copper golem protection: " + t.getMessage());
            return false;
        }
    }

    /**
     * Tags a newly created copper golem with the UUID of the nearest player (its creator).
     *
     * @param event the creature spawn event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCopperGolemSpawn(CreatureSpawnEvent event) {
        try {
            LivingEntity golem = event.getEntity();
            if (!golem.getType().name().equals(COPPER_GOLEM)) return;
            Player creator = nearestPlayer(golem);
            if (creator != null) {
                golem.getPersistentDataContainer().set(creatorKey, PersistentDataType.STRING, creator.getUniqueId().toString());
                golemCreators.put(golem.getUniqueId(), creator.getUniqueId());
            }
        } catch (Exception e) {
        }
    }

    /**
     * Restores creator tags into the cache when entities load.
     *
     * @param event the entities load event.
     */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity e : event.getEntities()) {
            if (!e.getType().name().equals(COPPER_GOLEM)) continue;
            UUID creator = readCreatorTag(e);
            if (creator != null) golemCreators.put(e.getUniqueId(), creator);
        }
    }

    /**
     * Removes cached tags when entities unload.
     *
     * @param event the entities unload event.
     */
    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity e : event.getEntities()) {
            if (e.getType().name().equals(COPPER_GOLEM)) golemCreators.remove(e.getUniqueId());
        }
    }

    /**
     * Denies a copper golem access to a protected container in a claim unless its creator is allowed.
     *
     * @param event the copper golem event.
     */
    private void handle(Event event) {
        try {
            Block block = (Block) getBlockMethod.invoke(event);
            if (block == null) return;
            if (!instance.getSettings().isRestrictedContainer(block.getType())) return;
            Chunk chunk = block.getChunk();
            if (instance.getMain().checkIfClaimExists(chunk)) {
                Claim claim = instance.getMain().getClaim(chunk);
                Object entity = getEntityMethod.invoke(event);
                UUID creator = entity instanceof Entity ? golemCreators.get(((Entity) entity).getUniqueId()) : null;
                if (!creatorCanInteract(claim, creator)) {
                    setAllowedMethod.invoke(event, false);
                }
                return;
            }
            WorldMode mode = instance.getSettings().getWorldMode(block.getWorld().getName());
            if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("InteractBlocks")) {
                setAllowedMethod.invoke(event, false);
            }
        } catch (Exception e) {
        }
    }

    /**
     * Whether the golem's creator may interact with containers in the claim.
     *
     * @param claim   the claim the container is in.
     * @param creator the creator's UUID, or null if untagged.
     * @return true if access is allowed.
     */
    private boolean creatorCanInteract(Claim claim, UUID creator) {
        if (creator == null) return false;
        if (creator.equals(claim.getUUID())) return true;
        String role = claim.isMember(creator) ? "members" : "visitors";
        return claim.getPermission("InteractBlocks", role);
    }

    /**
     * Caches creator tags from copper golems already loaded (used on enable, skipped on Folia).
     */
    private void cacheLoadedGolems() {
        if (instance.isFolia()) return;
        for (World w : instance.getServer().getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (!e.getType().name().equals(COPPER_GOLEM)) continue;
                UUID creator = readCreatorTag(e);
                if (creator != null) golemCreators.put(e.getUniqueId(), creator);
            }
        }
    }

    /**
     * Reads the creator UUID stored on an entity.
     *
     * @param e the entity.
     * @return the creator UUID, or null if absent/invalid.
     */
    private UUID readCreatorTag(Entity e) {
        String s = e.getPersistentDataContainer().get(creatorKey, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Finds the nearest player to the golem.
     *
     * @param golem the copper golem.
     * @return the nearest player within 6 blocks, or null.
     */
    private Player nearestPlayer(LivingEntity golem) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity e : golem.getNearbyEntities(6, 6, 6)) {
            if (e instanceof Player) {
                double d = e.getLocation().distanceSquared(golem.getLocation());
                if (d < best) {
                    best = d;
                    nearest = (Player) e;
                }
            }
        }
        return nearest;
    }
}
