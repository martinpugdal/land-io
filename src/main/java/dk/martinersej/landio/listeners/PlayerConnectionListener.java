package dk.martinersej.landio.listeners;

import dk.martinersej.landio.GPlayer;
import dk.martinersej.landio.Land_io;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerConnectionListener implements Listener {

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(), JavaPlugin.getProvidingPlugin(PlayerConnectionListener.class));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("§8[§a+§8] §7" + event.getPlayer().getName());
        ItemStack randomBlock = Land_io.getInstance().getGameManager().getRandomColorItemStack();
        if (randomBlock == null) {
            event.getPlayer().sendMessage("§c§oAlle blocks er blevet brugt op, prøv igen senere med §e/join");
            event.getPlayer().setGameMode(org.bukkit.GameMode.SPECTATOR);
            return;
        }
        Land_io.getInstance().getGameManager().playerJoined(event.getPlayer());
        GPlayer gPlayer = new GPlayer(event.getPlayer(), randomBlock);
        Land_io.getInstance().getGameManager().addGPlayer(gPlayer);
        event.getPlayer().sendMessage("§7Du har fået blokken §f" + randomBlock.getType().name() + "§7 med data §f" + randomBlock.getData().getData());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage("§8[§c-§8] §7" + event.getPlayer().getName());
        GPlayer gPlayer = Land_io.getInstance().getGameManager().getGPlayer(event.getPlayer());
        Land_io.getInstance().getGameManager().removeGPlayer(gPlayer);
    }
}
