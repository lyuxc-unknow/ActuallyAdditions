/*
 * This file ("InitEntities.java") is part of the Actually Additions mod for Minecraft.
 * It is created and owned by Ellpeck and distributed
 * under the Actually Additions License to be found at
 * http://ellpeck.de/actaddlicense
 * View the source code at https://github.com/Ellpeck/ActuallyAdditions
 *
 * © 2015-2017 Ellpeck
 */

package de.ellpeck.actuallyadditions.mod.entity;

import de.ellpeck.actuallyadditions.mod.ActuallyAdditions;


public final class InitEntities {

    public static void init() {
        ActuallyAdditions.LOGGER.info("Initializing Entities...");

        //EntityRegistry.registerModEntity(ActuallyAdditions.modLoc("worm"), EntityWorm.class, ActuallyAdditions.MODID + ".worm", 0, ActuallyAdditions.INSTANCE, 64, 1, false);
    }

    
    public static void initClient() {
        //RenderingRegistry.registerEntityRenderingHandler(EntityWorm.class, RenderWorm::new);
    }

}
