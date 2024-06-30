package fr.xyness.SCS.Support;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

/**
 * This class handles the integration with the Vault economy system.
 */
public class ClaimVault {
	
	// ***************
	// *  Variables  *
	// ***************
	
	/** The economy instance provided by Vault. */
	private static Economy econ;
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	/**
	 * Sets up the economy by hooking into Vault.
	 *
	 * @return true if the economy was successfully set up, false otherwise.
	 */
	public static boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	/**
	 * Gets the balance of a player.
	 *
	 * @param playerName the name of the player.
	 * @return the balance of the player.
	 */
	public static double getPlayerBalance(String playerName) {
		return econ.getBalance(playerName);
	}
	
	/**
	 * Adds money to a player's balance.
	 *
	 * @param playerName the name of the player.
	 * @param money the amount of money to add.
	 */
	public static void addPlayerBalance(String playerName, double money) {
		econ.depositPlayer(playerName, money);
	}
	
	/**
	 * Removes money from a player's balance.
	 *
	 * @param playerName the name of the player.
	 * @param money the amount of money to remove.
	 */
	public static void removePlayerBalance(String playerName, double money) {
		econ.withdrawPlayer(playerName, money);
	}
}
