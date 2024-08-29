package dk.martinersej.landio.handlers;

import dk.martinersej.landio.GPlayer;
import dk.martinersej.landio.listeners.JumpListener;
import dk.martinersej.landio.listeners.PlayerConnectionListener;
import dk.martinersej.landio.listeners.WalkOnBlockListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class GameManager implements org.bukkit.event.Listener {

    @Getter
    private final List<GPlayer> players = new ArrayList<>();
    @Getter
    private final WorldHandler worldHandler;
    private final Material[] materials = {
        Material.WOOL,
        Material.STAINED_CLAY
    };
    private final byte[] data = {
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15
    };

    public GameManager() {
        worldHandler = new WorldHandler();
        worldHandler.setupWorld();

        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getProvidingPlugin(getClass()));

        // Register listeners
        PlayerConnectionListener.init();
        JumpListener.init();
        WalkOnBlockListener.init();
    }

    public Block getBlockUnderPlayer(Player player) {
        // use BlockFace.DOWN to get the block under the player
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    }

    public boolean isPlaying(Player player) {
        for (GPlayer gPlayer : players) {
            if (gPlayer.getUuid().equals(player.getUniqueId()) && player.getGameMode() == GameMode.ADVENTURE) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onRain(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    public GPlayer getGPlayer(Player player) {
        for (GPlayer gPlayer : players) {
            if (gPlayer.getUuid().equals(player.getUniqueId())) {
                return gPlayer;
            }
        }
        return null;
    }

    public ItemStack getRandomColorItemStack() {
        Map<Material, Short> materials = new HashMap<>();

        for (GPlayer gPlayer : players) {
            materials.put(gPlayer.getBlock().getType(), gPlayer.getBlock().getDurability());
        }

        for (Material material : this.materials) {
            for (byte data : this.data) {
                if (!materials.containsKey(material) || materials.get(material) != data) {
                    return new ItemStack(material, 1, data);
                }
            }
        }

        return null;
    }

    public void playerJoined(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2f);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setFallDistance(0);
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 128, false, false));

        if (getGPlayer(player) == null) {
            spawnPlayerRandomOnPlatform(new GPlayer(player, getRandomColorItemStack()));
        } else {
            spawnPlayerRandomOnPlatform(getGPlayer(player));
        }
    }

    private void spawnPlayerRandomOnPlatform(GPlayer gplayer) {
        int offsetFromCenter = worldHandler.getOffsetFromCenter() - 3;
        Random random = new Random();
        int x = random.nextInt(offsetFromCenter * 2) - offsetFromCenter;
        int z = random.nextInt(offsetFromCenter * 2) - offsetFromCenter;
        gplayer.getPlayer().teleport(new Location(getWorldHandler().getWorld(), x - 0.5, 1, z - 0.5));

        makePlatform(gplayer);
    }

    private void makePlatform(GPlayer gPlayer) {
        Player player = gPlayer.getPlayer();
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Block block = player.getWorld().getBlockAt(x + i, 0, z + j);
                block.setType(gPlayer.getBlock().getType());
                block.setData(gPlayer.getBlock().getData().getData());
                block.getState().update();

                gPlayer.getPlatformBlocks().add(block.getLocation());
            }
        }
    }

    public void addGPlayer(GPlayer gPlayer) {
        players.add(gPlayer);
    }

    public void removeGPlayer(GPlayer gPlayer) {
        players.remove(gPlayer);
    }

    public void updateToPlatformBlock(GPlayer gPlayer, Block block) {
        block.setType(gPlayer.getBlock().getType());
        block.setData(gPlayer.getColor());
        block.getState().update();
        gPlayer.getPlatformBlocks().add(block.getLocation());
    }
}
