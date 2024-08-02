package fr.xyness.SCS;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * This class handles bossbar management.
 */
public class ClaimBossBar {
	
	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** A map to store the BossBars for each player. */
	private final ConcurrentMap<Player, BossBar> bossBars = new ConcurrentHashMap<>();
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    /** Default bossbar color */
    private BarColor defaultColor;
    
    /** Default bossbar style */
    private BarStyle defaultStyle;
    
    /** For sale bossbar color */
    private BarColor saleColor;
    
    /** For sale bossbar style */
    private BarStyle saleStyle;

    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimBossBar.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimBossBar(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
    
    // ********************
    // *  Others Methods  *
    // ********************
    
    
    /**
     * Loads bossbar settings
     */
    public void loadBossbarSettings() {
    	this.defaultColor = BarColor.valueOf(instance.getSettings().getSetting("bossbar-color"));
    	this.defaultStyle = BarStyle.valueOf(instance.getSettings().getSetting("bossbar-style"));
    	this.saleColor = BarColor.valueOf(instance.getSettings().getSetting("announce-sale.bossbar-settings.color"));
    	this.saleStyle = BarStyle.valueOf(instance.getSettings().getSetting("announce-sale.bossbar-settings.style"));
    }
    
    /**
     * Activates the BossBar for the player.
     *
     * @param player the player.
     * @param chunk the chunk the player is in.
     */
    public void activeBossBar(Player player, Chunk chunk) {
        if (player == null) return;
        instance.executeAsync(() -> {
        	BossBar b = checkBossBar(player);

            if (!instance.getSettings().getBooleanSetting("bossbar")) {
                b.setVisible(false);
                return;
            }

            if (!instance.getMain().checkIfClaimExists(chunk)) {
                b.setVisible(false);
                return;
            }

            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            String chunkName = claim.getName();
            String title;

            if(claim.getSale() && instance.getSettings().getBooleanSetting("announce-sale.bossbar")) {
            	b.setColor(saleColor);
            	b.setStyle(saleStyle);
                if (owner.equals("*")) {
                    title = instance.getLanguage().getMessage("bossbar-protected-area-for-sale-message",player)
                    		.replace("%name%", chunkName)
                    		.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
                      		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
                } else if (owner.equals(player.getName())) {
                    title = instance.getLanguage().getMessage("bossbar-owner-for-sale-message",player)
                    		.replace("%owner%", owner)
                    		.replace("%name%", chunkName)
                    		.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
                      		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
                } else if (instance.getMain().checkMembre(claim, player)) {
                    title = instance.getLanguage().getMessage("bossbar-member-for-sale-message",player)
                            .replace("%player%", player.getName())
                            .replace("%owner%", owner)
                            .replace("%name%", chunkName)
                    		.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
                      		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
                } else {
                    title = instance.getLanguage().getMessage("bossbar-visitor-for-sale-message",player)
                            .replace("%player%", player.getName())
                            .replace("%owner%", owner)
                            .replace("%name%", chunkName)
                    		.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
                      		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
                }
            } else {
            	b.setColor(defaultColor);
            	b.setStyle(defaultStyle);
                if (owner.equals("*")) {
                    title = instance.getLanguage().getMessage("bossbar-protected-area-message",player).replace("%name%", chunkName);
                } else if (owner.equals(player.getName())) {
                    title = instance.getLanguage().getMessage("bossbar-owner-message",player).replace("%owner%", owner).replace("%name%", chunkName);
                } else if (instance.getMain().checkMembre(claim, player)) {
                    title = instance.getLanguage().getMessage("bossbar-member-message",player)
                            .replace("%player%", player.getName())
                            .replace("%owner%", owner)
                            .replace("%name%", chunkName);
                } else {
                    title = instance.getLanguage().getMessage("bossbar-visitor-message",player)
                            .replace("%player%", player.getName())
                            .replace("%owner%", owner)
                            .replace("%name%", chunkName);
                }
            }

            b.setTitle(title);
            b.setVisible(true);
        });
    }
    
    /**
     * Updates the color of all BossBars.
     *
     * @param color the new color for the BossBars.
     */
    public void setBossBarColor(BarColor color) {
        bossBars.values().stream().forEach(b -> b.setColor(color));
    }
    
    /**
     * Updates the style of all BossBars.
     *
     * @param style the new style for the BossBars.
     */
    public void setBossBarStyle(BarStyle style) {
        bossBars.values().stream().forEach(b -> b.setStyle(style));
    }
    
    /**
     * Checks if the player has a BossBar and returns it.
     *
     * @param player the player.
     * @return the player's BossBar.
     */
    public BossBar checkBossBar(Player player) {
        return bossBars.computeIfAbsent(player, p -> {
            BossBar b = Bukkit.getServer().createBossBar("", BarColor.valueOf(instance.getSettings().getSetting("bossbar-color")), BarStyle.valueOf(instance.getSettings().getSetting("bossbar-style")));
            b.addPlayer(p);
            return b;
        });
    }
    
    /**
     * Disables the BossBar for the player.
     *
     * @param player the player.
     */
    public void disableBossBar(Player player) {
        if (!instance.getSettings().getBooleanSetting("bossbar")) return;
        if (player == null) return;
        BossBar b = checkBossBar(player);
        b.setVisible(false);
    }
    
    /**
     * Clears all maps and variables.
     */
    public void clearAll() {
        bossBars.values().forEach(b -> b.setVisible(false));
        bossBars.clear();
    }
    
    /**
     * Activates the boss bar for players in the specified chunk.
     *
     * @param chunk the chunk to activate the boss bar in
     */
    public void activateBossBar(Chunk chunk) {
    	if (!instance.getSettings().getBooleanSetting("bossbar")) return;
        Runnable bossBarTask = () -> {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Player) {
                    instance.getBossBars().activeBossBar((Player) entity, chunk);
                }
            }
        };

        if (instance.isFolia()) {
            Bukkit.getRegionScheduler().run(instance.getPlugin(), chunk.getWorld(), chunk.getX(), chunk.getZ(), task -> bossBarTask.run());
        } else {
            bossBarTask.run();
        }
    }
    
    /**
     * Activates the boss bar for players in the specified chunk.
     *
     * @param chunks the chunks to activate the boss bar in
     */
    public void activateBossBar(Set<Chunk> chunks) {
    	if (!instance.getSettings().getBooleanSetting("bossbar")) return;
        Bukkit.getOnlinePlayers().stream().forEach(p -> {
        	Chunk c = p.getLocation().getChunk();
        	if(chunks.contains(c)) {
        		activeBossBar(p,c);
        	}
        });
    }
    
    /**
     * Deactivates the boss bar for players in the specified chunk.
     *
     * @param chunks the chunks to activate the boss bar in
     */
    public void deactivateBossBar(Set<Chunk> chunks) {
    	if (!instance.getSettings().getBooleanSetting("bossbar")) return;
        Bukkit.getOnlinePlayers().stream().forEach(p -> {
        	Chunk c = p.getLocation().getChunk();
        	if(chunks.contains(c)) {
        		disableBossBar(p);
        	}
        });
    }
}
