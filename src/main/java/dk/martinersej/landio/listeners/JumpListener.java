package dk.martinersej.landio.listeners;

import dk.martinersej.landio.Land_io;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JumpListener implements Listener {

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new JumpListener(), JavaPlugin.getProvidingPlugin(JumpListener.class));
    }

    @EventHandler
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo().getY() > event.getFrom().getY() && Land_io.getInstance().getGameManager().isPlaying(player)) {
            player.teleport(event.getFrom());
            player.playSound(player.getLocation(), Sound.ENDERMAN_STARE, 1, 1);
        }
    }
}
