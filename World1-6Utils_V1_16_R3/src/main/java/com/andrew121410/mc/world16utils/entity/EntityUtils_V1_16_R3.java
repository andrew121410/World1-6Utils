package com.andrew121410.mc.world16utils.entity;

import com.andrew121410.mc.world16utils.utils.SimpleBoundingBox;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntityUtils_V1_16_R3 implements EntityUtils {

    @Override
    public Collection<Entity> getNearbyEntities(World world, SimpleBoundingBox simpleBoundingBox) {
        WorldServer worldServer = ((CraftWorld) world).getHandle();
        AxisAlignedBB bb = new AxisAlignedBB(simpleBoundingBox.getMinX(), simpleBoundingBox.getMinY(), simpleBoundingBox.getMinZ(), simpleBoundingBox.getMaxX(), simpleBoundingBox.getMaxY(), simpleBoundingBox.getMaxZ());
        List<net.minecraft.server.v1_16_R3.Entity> entityList = worldServer.getEntities(null, bb, null);
        List<Entity> bukkitEntityList = new ArrayList<>(entityList.size());
        for (net.minecraft.server.v1_16_R3.Entity entity : entityList) {
            Entity bukkitEntity = entity.getBukkitEntity();
            bukkitEntityList.add(bukkitEntity);
        }
        return bukkitEntityList;
    }
}
