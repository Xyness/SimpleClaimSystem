package fr.xyness.SCS.Support;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * This class handles the integration with PlaceholderAPI for providing claim-related placeholders.
 */
public class ClaimPlaceholdersExpansion extends PlaceholderExpansion {

    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;

    /**
     * Main constructor for ClaimPlaceholdersExpansion.
     *
     * @param instance The instance of SimpleClaimSystem.
     */
    public ClaimPlaceholdersExpansion(SimpleClaimSystem instance) {
        this.instance = instance;
    }

    @Override
    public String getIdentifier() {
        return "scs";
    }

    @Override
    public String getAuthor() {
        return "Xyness";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Gets the claim at the player's current chunk by coordinates, without loading the chunk
     * (safe from any thread, including off the region thread on Folia).
     *
     * @param player the player.
     * @return the claim at the player's location, or null.
     */
    private Claim claimAtPlayer(Player player) {
        Location loc = player.getLocation();
        return instance.getMain().getClaim(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    /**
     * Resolves the player's relation message (owner / member / visitor / no-claim) for a claim.
     *
     * @param claim  the claim, or null.
     * @param player the player.
     * @return the matching language message.
     */
    private String playerRelation(Claim claim, Player player) {
        if (claim == null) return instance.getLanguage().getMessage("claim_player-if-no-claim");
        if (claim.getOwner().equals(player.getName())) return instance.getLanguage().getMessage("claim_player-if-owner");
        if (claim.isMember(player.getUniqueId())) return instance.getLanguage().getMessage("claim_player-if-member");
        return instance.getLanguage().getMessage("claim_player-if-visitor");
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());

        try {
            switch (identifier) {
                case "player_claims_count":
                    return String.valueOf(cPlayer.getClaimsCount());

                case "claim_name": {
                    Claim claim = claimAtPlayer(player);
                    return claim != null ? claim.getName() : instance.getLanguage().getMessage("claim_name-if-no-claim");
                }

                case "player_max_claims":
                    int maxClaims = cPlayer.getMaxClaims();
                    return maxClaims > 0 ? String.valueOf(maxClaims) : "∞";

                case "player_max_chunks_per_claim":
                    int maxChunks = cPlayer.getMaxChunksPerClaim();
                    return maxChunks > 0 ? String.valueOf(maxChunks) : "∞";

                case "player_max_chunks_total":
                    int maxChunksTotal = cPlayer.getMaxChunksTotal();
                    return maxChunksTotal > 0 ? String.valueOf(maxChunksTotal) : "∞";

                case "player_claim_distance":
                    int distance = cPlayer.getClaimDistance();
                    return distance > 0 ? String.valueOf(distance) : instance.getLanguage().getMessage("claim_distance-if-zero");

                case "player_remain_claims":
                    int max = cPlayer.getMaxClaims();
                    if (max == 0) return "∞";
                    int remainingClaims = max - cPlayer.getClaimsCount();
                    return remainingClaims >= 0 ? String.valueOf(remainingClaims) : "0";

                case "player_remain_chunks":
                    int max_chunks = cPlayer.getMaxChunksTotal();
                    if (max_chunks == 0) return "∞";
                    int remainingChunks = max_chunks - instance.getMain().getAllChunksFromAllClaims(player.getName()).size();
                    return remainingChunks >= 0 ? String.valueOf(remainingChunks) : "0";

                case "player_chunks_count":
                    return String.valueOf(instance.getMain().getAllChunksFromAllClaims(player.getName()).size());

                case "player_max_radius_claims":
                    int maxRadiusClaims = cPlayer.getMaxRadiusClaims();
                    return maxRadiusClaims > 0 ? String.valueOf(maxRadiusClaims) : "∞";

                case "player_teleportation_delay":
                    return String.valueOf(cPlayer.getDelay());

                case "player_max_members":
                    int maxMembers = cPlayer.getMaxMembers();
                    return maxMembers > 0 ? String.valueOf(maxMembers) : "∞";

                case "player_claim_cost":
                    return String.valueOf(cPlayer.getCost());

                case "player_claim_cost_multiplier":
                    return String.valueOf(cPlayer.getMultiplier());

                case "claim_owner": {
                    Claim claim = claimAtPlayer(player);
                    return claim != null ? claim.getOwner() : instance.getLanguage().getMessage("claim_owner-if-no-claim");
                }

                case "claim_description": {
                    Claim claim = claimAtPlayer(player);
                    return claim != null ? claim.getDescription() : instance.getLanguage().getMessage("claim_description-if-no-claim");
                }

                case "claim_is_in_sale": {
                    Claim claim = claimAtPlayer(player);
                    return claim != null ? String.valueOf(claim.getSale()) : instance.getLanguage().getMessage("claim_is_in_sale-if-no-claim");
                }

                case "claim_sale_price": {
                    Claim claim = claimAtPlayer(player);
                    if (claim != null) {
                        return claim.getSale() ? String.valueOf(claim.getPrice())
                                : instance.getLanguage().getMessage("claim_sale_price-if-not-in-sale");
                    }
                    return instance.getLanguage().getMessage("claim_sale_price-if-no-claim");
                }

                case "claim_members_count": {
                    Claim claim = claimAtPlayer(player);
                    return claim != null ? String.valueOf(claim.getMembers().size())
                            : instance.getLanguage().getMessage("claim_members_count-if-no-claim");
                }

                case "claim_members_online": {
                    Claim claim = claimAtPlayer(player);
                    if (claim != null) {
                        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getMembers());
                        long onlineMembers = members.stream()
                                .filter(member -> Bukkit.getPlayer(member) != null)
                                .count();
                        return String.valueOf(onlineMembers);
                    }
                    return instance.getLanguage().getMessage("claim_members_online-if-no-claim");
                }

                case "claim_spawn": {
                    Claim claim = claimAtPlayer(player);
                    return claim != null ? String.valueOf(instance.getMain().getClaimCoords(claim))
                            : instance.getLanguage().getMessage("claim_spawn-if-no-claim");
                }

                default:
                    if (identifier.startsWith("claim_chunk_relative_")) {
                        String syntax = identifier.replaceFirst("claim_chunk_relative_", "");
                        Location loc = player.getLocation();
                        int baseX = loc.getBlockX() >> 4;
                        int baseZ = loc.getBlockZ() >> 4;
                        if (syntax.endsWith("_name")) {
                            String[] parts = syntax.replace("_name", "").split("_");
                            Claim claim = instance.getMain().getClaim(parts[0], Integer.parseInt(parts[1]) + baseX, Integer.parseInt(parts[2]) + baseZ);
                            return claim != null ? claim.getName() : instance.getLanguage().getMessage("claim_name-if-no-claim");
                        }
                        if (syntax.endsWith("_owner")) {
                            String[] parts = syntax.replace("_owner", "").split("_");
                            Claim claim = instance.getMain().getClaim(parts[0], Integer.parseInt(parts[1]) + baseX, Integer.parseInt(parts[2]) + baseZ);
                            return claim != null ? claim.getOwner() : instance.getLanguage().getMessage("claim_owner-if-no-claim");
                        }
                        if (syntax.endsWith("_player")) {
                            String[] parts = syntax.replace("_player", "").split("_");
                            Claim claim = instance.getMain().getClaim(parts[0], Integer.parseInt(parts[1]) + baseX, Integer.parseInt(parts[2]) + baseZ);
                            return playerRelation(claim, player);
                        }
                    }
                    if (identifier.startsWith("claim_chunk_")) {
                        String syntax = identifier.replaceFirst("claim_chunk_", "");
                        if (syntax.endsWith("_name")) {
                            String[] parts = syntax.replace("_name", "").split("_");
                            Claim claim = instance.getMain().getClaim(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                            return claim != null ? claim.getName() : instance.getLanguage().getMessage("claim_name-if-no-claim");
                        }
                        if (syntax.endsWith("_owner")) {
                            String[] parts = syntax.replace("_owner", "").split("_");
                            Claim claim = instance.getMain().getClaim(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                            return claim != null ? claim.getOwner() : instance.getLanguage().getMessage("claim_owner-if-no-claim");
                        }
                        if (syntax.endsWith("_player")) {
                            String[] parts = syntax.replace("_player", "").split("_");
                            Claim claim = instance.getMain().getClaim(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                            return playerRelation(claim, player);
                        }
                    }
                    if (identifier.startsWith("claim_setting_")) {
                        Claim claim = claimAtPlayer(player);
                        if (claim != null) {
                            String[] parts = identifier.replaceFirst("claim_setting_", "").split("_");
                            if (parts.length != 2) return instance.getLanguage().getMessage("status-disabled");
                            return claim.getPermission(parts[0], parts[1])
                                    ? instance.getLanguage().getMessage("status-enabled")
                                    : instance.getLanguage().getMessage("status-disabled");
                        }
                        return instance.getLanguage().getMessage("claim_setting-if-no-claim");
                    }
                    return null;
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
