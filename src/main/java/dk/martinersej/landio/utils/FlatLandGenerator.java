package dk.martinersej.landio.utils;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class FlatLandGenerator extends ChunkGenerator implements ChunkGenerator.BiomeGrid {

    @Override
    public byte[] generate(World world, Random random, int chunkX, int chunkZ) {
        byte[] result = new byte[16*16*128]; // (chunk size)

        // Fill the bottom with 1 layer of block
        for (int i = 0; i < 16*16*128; i += 128) {
            result[i] = 82; // clay
        }

        return result;
    }

    @Override
    public Biome getBiome(int x, int z) {
        return Biome.PLAINS;
    }

    @Override
    public void setBiome(int x, int z, Biome bio) {
        // do nothing
    }
}
