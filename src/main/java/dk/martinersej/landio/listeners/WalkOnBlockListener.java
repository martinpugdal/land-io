package dk.martinersej.landio.listeners;

import dk.martinersej.landio.GPlayer;
import dk.martinersej.landio.Land_io;
import dk.martinersej.landio.handlers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class WalkOnBlockListener implements Listener {

    private static final BlockFace[] FILL_FACES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };
    private final Material expandingGlass = Material.STAINED_GLASS;

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new WalkOnBlockListener(), JavaPlugin.getProvidingPlugin(WalkOnBlockListener.class));
    }

    @EventHandler
    public void onPlayerWalk(PlayerMoveEvent event) {
        GameManager gameManager = Land_io.getInstance().getGameManager();
        if (gameManager.isPlaying(event.getPlayer())) {
            GPlayer gPlayer = gameManager.getGPlayer(event.getPlayer());
            Block block = gameManager.getBlockUnderPlayer(event.getPlayer());

            if (block.getType().equals(Material.AIR)) {
                return;
            }
            if (block.getType().equals(expandingGlass) && !walkingOnMyGlassBlock(block, gPlayer)) {
                for (GPlayer player : gameManager.getPlayers()) {
                    if (player.getBlocks().containsKey(block.getLocation())) {
                        player.killed();
                        player.getPlayer().sendMessage("§7Du blev dræbt af en anden spiller");
                        gPlayer.getPlayer().sendMessage("§7Du dræbte §a" + player.getPlayer().getName());
                        break;
                    }
                }
            }

            if (walkingOnGameBlock(block)) {
                block.setType(expandingGlass);
                block.setData(gPlayer.getColor());
                block.getState().update();
                gPlayer.setExpanding(true);
                gPlayer.addExpandingBlock(block.getLocation(), block);
            } else if (walkingOnMyBlock(block, gPlayer) && gPlayer.isExpanding()) {
                gPlayer.setExpanding(false);
                fillBlocksInsideBox(gPlayer);
            } else if (walkingOnOtherBlock(block, gPlayer)) {
                gPlayer.killed();
                gPlayer.getPlayer().sendMessage("§7Du blev dræbt af en anden spiller");
                getOwnerOfThisBlock(block).getPlayer().sendMessage("§7Du dræbte §a" + gPlayer.getPlayer().getName());
            } else if (walkingOnMyGlassBlock(block, gPlayer)) {
                gPlayer.addExpandingBlock(block.getLocation(), block);
            } else if (!walkingOnMyBlock(block, gPlayer) && gPlayer.isExpanding()) {
                gPlayer.setExpanding(false);
                fillBlocksInsideBox(gPlayer);
            }
        }
    }

    private GPlayer getOwnerOfThisBlock(Block block) {
        for (GPlayer player : Land_io.getInstance().getGameManager().getPlayers()) {
            if (player.getBlocks().containsKey(block.getLocation())) {
                return player;
            }
        }
        return null;
    }

    private boolean walkingOnOtherBlock(Block block, GPlayer gPlayer) {
        GPlayer owner = getOwnerOfThisBlock(block);
        return owner != null && !owner.getUuid().equals(gPlayer.getUuid());
    }

    private boolean walkingOnGameBlock(Block block) {
        return block.getType().getId() == 82 && block.getData() == 0;
    }

    private boolean walkingOnMyBlock(Block block, GPlayer gPlayer) {
        return block.getType().equals(gPlayer.getBlock().getType()) && block.getData() == gPlayer.getColor();
    }

    private boolean walkingOnMyGlassBlock(Block block, GPlayer gPlayer) {
        return gPlayer.getBlocks().containsKey(block.getLocation());
    }

    private void fillBlocksInsideBox(GPlayer gPlayer) {
        Location minLoc = gPlayer.getBlocks().keySet().stream().reduce((loc1, loc2) -> new Location(loc1.getWorld(),
            Math.min(loc1.getBlockX(), loc2.getBlockX()),
            Math.min(loc1.getBlockY(), loc2.getBlockY()),
            Math.min(loc1.getBlockZ(), loc2.getBlockZ()))).get();

        Location maxLoc = gPlayer.getBlocks().keySet().stream().reduce((loc1, loc2) -> new Location(loc1.getWorld(),
            Math.max(loc1.getBlockX(), loc2.getBlockX()),
            Math.max(loc1.getBlockY(), loc2.getBlockY()),
            Math.max(loc1.getBlockZ(), loc2.getBlockZ()))).get();

        if (minLoc.equals(maxLoc))  // only one block
            Land_io.getInstance().getGameManager().updateToPlatformBlock(gPlayer, minLoc.getBlock());
        else {
            Location center = getCenterLocation(minLoc, maxLoc);
            if (gPlayer.getBlocks().containsKey(center)) // center is a glass block
                Land_io.getInstance().getGameManager().updateToPlatformBlock(gPlayer, center.getBlock());
            else {
                Set<Location> filled = new HashSet<>(gPlayer.getBlocks().keySet());
                filled.addAll(gPlayer.getPlatformBlocks());
                fill(gPlayer, center, filled, 0);
                gPlayer.getBlocks().forEach((loc, block) ->
                    Land_io.getInstance().getGameManager().updateToPlatformBlock(gPlayer, loc.getBlock()));
                gPlayer.getBlocks().clear();
            }
        }

        //test for minLoc and maxLoc
        minLoc.getBlock().setType(Material.SPONGE);
        maxLoc.getBlock().setType(Material.SPONGE);
        minLoc.getBlock().getState().update();
        maxLoc.getBlock().getState().update();
    }

    private Location getCenterLocation(Location corner1, Location corner2) {
        double x = (corner1.getX() + corner2.getX()) / 2;
        double y = (corner1.getY() + corner2.getY()) / 2;
        double z = (corner1.getZ() + corner2.getZ()) / 2;
        return new Location(corner1.getWorld(), x, y, z);
    }

    private void fill(GPlayer gplayer, Location location, Set<Location> filled, int tries) {
        if (!filled.add(location)) {
            return;
        }
        if (tries > 25) {
            // too many tries, return to avoid infinite loop
            return;
        }
        Block block = location.getBlock();
        Land_io.getInstance().getGameManager().updateToPlatformBlock(gplayer, block);
        for (BlockFace face : FILL_FACES) {
            // recursively fill all blocks around the current block
            fill(gplayer, block.getRelative(face).getLocation(), filled, tries + 1);
        }
    }
}
