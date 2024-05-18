package dk.martinersej.landio.handlers;

import dk.martinersej.landio.utils.FileUtils;
import dk.martinersej.landio.utils.FlatLandGenerator;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;

public class WorldHandler {

    @Getter
    private World world;
    private static final String worldName = "LandioWorld";
    @Getter
    private final int offsetFromCenter = 50;

    public void setupWorld() {
        deleteGameWorld();

        WorldCreator wc = new WorldCreator(worldName);
        wc.generateStructures(false);
        wc.generator(new FlatLandGenerator());
        wc.type(WorldType.FLAT);
        wc.environment(World.Environment.NORMAL);
        wc.generateStructures(false);
        world = wc.createWorld();
        world.setAutoSave(false);

        world.setSpawnLocation(0, 1, 0);

        world.setGameRuleValue("doMobSpawning", "false"); // No mobs spawning
        world.setGameRuleValue("randomTickSpeed", "0"); // No crops growing
        world.setGameRuleValue("doDaylightCycle", "false"); // No day/night cycle
        world.setGameRuleValue("doWeatherCycle", "false"); // No rain (doesn't work in 1.8.8)
        world.setGameRuleValue("showDeathMessages", "false"); // No death messages
        world.setDifficulty(Difficulty.EASY);
        world.setPVP(false);

        createGamePlatform();
    }

    private void createGamePlatform() {
        for (int x = -offsetFromCenter; x <= offsetFromCenter; x++) {
            for (int z = -offsetFromCenter; z <= offsetFromCenter; z++) {
                if (x == -offsetFromCenter || x == offsetFromCenter || z == -offsetFromCenter || z == offsetFromCenter) {
                    for (int y = 1; y <= 2; y++) {
                        world.getBlockAt(x, y, z).setType(Material.COAL_BLOCK);
                    }
                }
            }
        }
    }

    public void deleteGameWorld() {
        World gameWorld = Bukkit.getWorld(getClass().getSimpleName());

        if (gameWorld != null) {
            World world = Bukkit.getWorlds().get(0);
            for (Player player : gameWorld.getPlayers()) {
                player.teleport(world.getSpawnLocation());
            }
        }

        try {
            Bukkit.unloadWorld(worldName, false);
            this.world = null;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            System.out.println("Failed unloading the world!");
        }

        FileUtils.deleteDir(new File(worldName));
    }
}
