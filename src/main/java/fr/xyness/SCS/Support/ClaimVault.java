package fr.xyness.SCS.Support;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class ClaimVault {

	private static Economy econ;
	
	public static boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	public static double getPlayerBalance(String playerName) {
		return econ.getBalance(playerName);
	}
	
	public static void addPlayerBalance(String playerName, double money) {
		econ.depositPlayer(playerName, money);
	}
	
	public static void removePlayerBalance(String playerName, double money) {
		econ.withdrawPlayer(playerName, money);
	}
}
