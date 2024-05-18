package dk.martinersej.landio;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Getter
public class GPlayer {

    private final UUID uuid;
    @Setter
    private ItemStack block;
    @Setter
    private boolean expanding = false;
    private final Map<Location, ItemStack> blocks = new HashMap<>();
    private final List<Location> platformBlocks = new ArrayList<>();

    public GPlayer(OfflinePlayer player, ItemStack block) {
        this.uuid = player.getUniqueId();
        this.block = block;
    }

    public byte getColor() {
        return block.getData().getData();
    }

    public Player getPlayer() {
        return Land_io.getInstance().getServer().getPlayer(uuid);
    }

    public void addExpandingBlock(Location location, Block block) {
        if (!expanding) {
            return;
        }
        ItemStack itemStack = new ItemStack(block.getType(), 1, block.getData());
        blocks.put(location, itemStack);
    }

    public void killed() {
        blocks.forEach((location, itemStack) -> {
            location.getBlock().setType(itemStack.getType());
            location.getBlock().setData(itemStack.getData().getData());
            location.getBlock().getState().update();
        });
        platformBlocks.forEach(location -> {
            location.getBlock().setType(Material.CLAY);
            location.getBlock().setData((byte) 0);
            location.getBlock().getState().update();
        });
        blocks.clear();
        platformBlocks.clear();
        expanding = false;

        getPlayer().setGameMode(org.bukkit.GameMode.SPECTATOR);
        new BukkitRunnable() {
            @Override
            public void run() {
                Land_io.getInstance().getGameManager().playerJoined(getPlayer());
            }
        }.runTaskLater(Land_io.getInstance(), 20L * 10); // 10 seconds
    }
}
