package dk.martinersej.landio.listeners;

import dk.martinersej.landio.GPlayer;
import dk.martinersej.landio.Land_io;
import dk.martinersej.landio.handlers.GameManager;
import org.bukkit.*;
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
                        player.getPlayer().sendMessage(ChatColor.GRAY + "Du blev dræbt af en anden spiller");
                        gPlayer.getPlayer().sendMessage(ChatColor.GRAY + "Du dræbte " + ChatColor.GREEN + player.getPlayer().getName());
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
                gPlayer.getPlayer().sendMessage(ChatColor.GRAY + "Du blev dræbt af en anden spiller");
                getOwnerOfThisBlock(block).getPlayer().sendMessage(ChatColor.GRAY + "Du dræbte " + ChatColor.GREEN + gPlayer.getPlayer().getName());
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
        return block.getType() == Material.CLAY && block.getData() == 0;
    }

    private boolean walkingOnMyBlock(Block block, GPlayer gPlayer) {
        return block.getType().equals(gPlayer.getBlock().getType()) && block.getData() == gPlayer.getColor();
    }

    private boolean walkingOnMyGlassBlock(Block block, GPlayer gPlayer) {
        return gPlayer.getBlocks().containsKey(block.getLocation());
    }

    private void fillBlocksInsideBox(GPlayer gPlayer) {
        Set<Location> glassBlocks = gPlayer.getBlocks().keySet();
        Set<Location> toFill = new HashSet<>();
        Set<Location> visited = new HashSet<>();

        // Find the bounding box of the glass blocks
        Location minLoc = glassBlocks.stream().reduce((loc1, loc2) -> new Location(loc1.getWorld(),
            Math.min(loc1.getBlockX(), loc2.getBlockX()),
            Math.min(loc1.getBlockY(), loc2.getBlockY()),
            Math.min(loc1.getBlockZ(), loc2.getBlockZ()))).get();

        Location maxLoc = glassBlocks.stream().reduce((loc1, loc2) -> new Location(loc1.getWorld(),
            Math.max(loc1.getBlockX(), loc2.getBlockX()),
            Math.max(loc1.getBlockY(), loc2.getBlockY()),
            Math.max(loc1.getBlockZ(), loc2.getBlockZ()))).get();

        Bukkit.getLogger().info("Filling blocks inside box: minLoc = " + minLoc + ", maxLoc = " + maxLoc);

        Location startLoc = getInteriorStartLocation(glassBlocks, minLoc, maxLoc, gPlayer);

        if (startLoc != null) {
            floodFill(startLoc, glassBlocks, toFill, visited, minLoc, maxLoc, gPlayer);

            toFill.addAll(glassBlocks);
            glassBlocks.clear();
            for (Location loc : toFill) {
                Land_io.getInstance().getGameManager().updateToPlatformBlock(gPlayer, loc.getBlock());
            }

            Bukkit.getLogger().info("Finished filling blocks. Total blocks filled: " + toFill.size());
        } else {
            Bukkit.getLogger().info("No valid interior start location found.");
        }
    }

    //TOOD: recode, so its not only checking for wool
    private Location getInteriorStartLocation(Set<Location> glassBlocks, Location minLoc, Location maxLoc, GPlayer gPlayer) {
        World world = minLoc.getWorld();
        for (int x = minLoc.getBlockX() + 1; x < maxLoc.getBlockX(); x++) {
            for (int z = minLoc.getBlockZ() + 1; z < maxLoc.getBlockZ(); z++) {
                Location loc = new Location(world, x, minLoc.getBlockY(), z);
                Block block = loc.getBlock();
                if (!glassBlocks.contains(loc) && block.getType() != Material.WOOL) {
                    return loc;
                }
            }
        }
        return null;
    }

    private void floodFill(Location loc, Set<Location> glassBlocks, Set<Location> toFill, Set<Location> visited, Location minLoc, Location maxLoc, GPlayer gPlayer) {
        if (visited.contains(loc) || glassBlocks.contains(loc) || toFill.contains(loc)) {
            return;
        }

        visited.add(loc);

        // bounds
        if (loc.getBlockX() < minLoc.getBlockX() || loc.getBlockX() > maxLoc.getBlockX()
            || loc.getBlockY() < minLoc.getBlockY() || loc.getBlockY() > maxLoc.getBlockY()
            || loc.getBlockZ() < minLoc.getBlockZ() || loc.getBlockZ() > maxLoc.getBlockZ()) {
            return;
        }

        Block block = loc.getBlock();
        // Stop if we encounter the player's wool block
        if (block.getType() == gPlayer.getBlock().getType() && block.getData() == gPlayer.getColor()) {
            return;
        }

        //TODO: check for block stuff here
        toFill.add(loc);

        for (BlockFace face : FILL_FACES) {
            floodFill(loc.clone().add(face.getModX(), face.getModY(), face.getModZ()), glassBlocks, toFill, visited, minLoc, maxLoc, gPlayer);
        }
    }
}