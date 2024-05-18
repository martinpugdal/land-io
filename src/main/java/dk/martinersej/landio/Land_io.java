package dk.martinersej.landio;

import dk.martinersej.landio.handlers.GameManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Land_io extends JavaPlugin implements Listener {

    @Getter
    private static Land_io instance;
    @Getter
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        gameManager = new GameManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack randomBlock = Land_io.getInstance().getGameManager().getRandomColorItemStack();
            if (randomBlock == null) {
                player.sendMessage("§c§oAlle blocks er blevet brugt op, prøv igen senere med §e/join");
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                return;
            }
            Land_io.getInstance().getGameManager().playerJoined(player);
            GPlayer gPlayer = new GPlayer(player, randomBlock);
            Land_io.getInstance().getGameManager().getPlayers().add(gPlayer);
            player.sendMessage("§7Du har fået blokken §f" + randomBlock.getType().name() + "§7 med data §f" + randomBlock.getData().getData());
        }
    }

    @Override
    public void onDisable() {
        stopTasks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(new org.bukkit.Location(Bukkit.getWorld("world"), 0, 100, 0));
        }
    }

    private void stopTasks() {
        Bukkit.getServer().getScheduler().cancelTasks(instance);
    }

}
