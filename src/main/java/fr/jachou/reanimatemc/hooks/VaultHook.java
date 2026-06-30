package fr.jachou.reanimatemc.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Thin wrapper around Vault's Economy API.
 *
 * <p>All callers must check {@link #isEnabled()} before calling any economy
 * method. If Vault or an economy provider is absent, methods are no-ops.
 */
public final class VaultHook {

    private Economy economy = null;
    private boolean enabled = false;

    public VaultHook(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("[ReanimateMC] Vault economy hooked: " + economy.getName());
    }

    public boolean isEnabled() { return enabled; }

    /** Returns the player's current balance, or 0 if unavailable. */
    public double getBalance(Player player) {
        if (!enabled) return 0;
        return economy.getBalance(player);
    }

    /**
     * Withdraws {@code amount} from the player.
     * @return true if the transaction succeeded.
     */
    public boolean withdraw(Player player, double amount) {
        if (!enabled) return false;
        if (!economy.has(player, amount)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Deposits {@code amount} to the player.
     * @return true if the transaction succeeded.
     */
    public boolean deposit(Player player, double amount) {
        if (!enabled) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /** Returns the economy's currency format for the given amount. */
    public String format(double amount) {
        if (!enabled) return String.valueOf(amount);
        return economy.format(amount);
    }
}
