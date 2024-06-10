package fr.xyness.SCS.Support;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class ClaimVault {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private static Economy econ;
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to setup the economy
	public static boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	// Method to get the player's balance
	public static double getPlayerBalance(String playerName) {
		return econ.getBalance(playerName);
	}
	
	// Method to add money to the player's balance
	public static void addPlayerBalance(String playerName, double money) {
		econ.depositPlayer(playerName, money);
	}
	
	// Method to remove money from the player's balance
	public static void removePlayerBalance(String playerName, double money) {
		econ.withdrawPlayer(playerName, money);
	}
}
