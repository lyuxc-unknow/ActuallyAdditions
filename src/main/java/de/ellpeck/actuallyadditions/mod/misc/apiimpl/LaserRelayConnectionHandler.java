/*
 * This file ("LaserRelayConnectionHandler.java") is part of the Actually Additions mod for Minecraft.
 * It is created and owned by Ellpeck and distributed
 * under the Actually Additions License to be found at
 * http://ellpeck.de/actaddlicense
 * View the source code at https://github.com/Ellpeck/ActuallyAdditions
 *
 * © 2015-2017 Ellpeck
 */

package de.ellpeck.actuallyadditions.mod.misc.apiimpl;

import de.ellpeck.actuallyadditions.api.laser.IConnectionPair;
import de.ellpeck.actuallyadditions.api.laser.ILaserRelayConnectionHandler;
import de.ellpeck.actuallyadditions.api.laser.LaserType;
import de.ellpeck.actuallyadditions.api.laser.Network;
import de.ellpeck.actuallyadditions.mod.data.WorldData;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityLaserRelay;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LaserRelayConnectionHandler implements ILaserRelayConnectionHandler {

    public static CompoundTag writeNetworkToNBT(Network network) {
        ListTag list = new ListTag();
        for (IConnectionPair pair : network.connections) {
            CompoundTag tag = new CompoundTag();
            pair.writeToNBT(tag);
            list.add(tag);
        }
        CompoundTag compound = new CompoundTag();
        compound.put("Network", list);
        return compound;
    }

    public static Network readNetworkFromNBT(CompoundTag tag) {
        ListTag list = tag.getList("Network", 10);
        Network network = new Network();
        for (int i = 0; i < list.size(); i++) {
            ConnectionPair pair = new ConnectionPair();
            pair.readFromNBT(list.getCompound(i));
            network.connections.add(pair);
        }
        return network;
    }

    /**
     * Merges two laserRelayNetworks together
     * (Actually puts everything from the second network into the first one and removes the second one)
     */
    private static void mergeNetworks(Network firstNetwork, Network secondNetwork, Level world) {
        for (IConnectionPair secondPair : secondNetwork.connections) {
            firstNetwork.connections.add(secondPair);
        }

        WorldData data = WorldData.get(world);
        secondNetwork.changeAmount++;
        data.laserRelayNetworks.remove(secondNetwork);
        data.setDirty();
        //System.out.println("Merged Two Networks!");
    }

    /**
     * Gets all Connections for a Relay
     */
    @Override
    public ConcurrentSet<IConnectionPair> getConnectionsFor(BlockPos relay, Level world) {
        ConcurrentSet<IConnectionPair> allPairs = new ConcurrentSet<>();
        if(!world.isClientSide) {
            for (Network aNetwork : WorldData.get(world).laserRelayNetworks) {
                for (IConnectionPair pair : aNetwork.connections) {
                    if (pair.contains(relay)) {
                        allPairs.add(pair);
                    }
                }
            }
        } //TODO ohhh boy
        return allPairs;
    }

    /**
     * Removes a Relay from its Network
     */
    @Override
    public void removeRelayFromNetwork(BlockPos relay, Level world) {
        Network network = this.getNetworkFor(relay, world);
        if (network != null) {
            network.changeAmount++;

            //Setup new network (so that splitting a network will cause it to break into two)
            WorldData data = WorldData.get(world);
            data.laserRelayNetworks.remove(network);
            data.setDirty();
            for (IConnectionPair pair : network.connections) {
                if (!pair.contains(relay)) {
                    this.addConnection(pair.getPositions()[0], pair.getPositions()[1], pair.getType(), world, pair.doesSuppressRender());
                }
            }
            //System.out.println("Removing a Relay from the Network!");
        }
    }

    /**
     * Gets a Network for a Relay
     */
    @Override
    public Network getNetworkFor(BlockPos relay, Level world) {
        if (world != null && !world.isClientSide) {
            for (Network aNetwork : WorldData.get(world).laserRelayNetworks) {
                for (IConnectionPair pair : aNetwork.connections) {
                    if (pair.contains(relay)) {
                        return aNetwork;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean addConnection(BlockPos firstRelay, BlockPos secondRelay, LaserType type, Level world) {
        return this.addConnection(firstRelay, secondRelay, type, world, false);
    }

    /**
     * Adds a new connection between two relays
     * (Puts it into the correct network!)
     */
    @Override
    public boolean addConnection(BlockPos firstRelay, BlockPos secondRelay, LaserType type, Level world, boolean suppressConnectionRender) {
        return this.addConnection(firstRelay, secondRelay, type, world, suppressConnectionRender, false);
    }

    @Override
    public boolean addConnection(BlockPos firstRelay, BlockPos secondRelay, LaserType type, Level world, boolean suppressConnectionRender, boolean removeIfConnected) {
        if (world.isClientSide || firstRelay == null || secondRelay == null || firstRelay == secondRelay || firstRelay.equals(secondRelay)) {
            return false;
        }
        WorldData data = WorldData.get(world);

        Network firstNetwork = this.getNetworkFor(firstRelay, world);
        Network secondNetwork = this.getNetworkFor(secondRelay, world);

        //No Network exists
        if (firstNetwork == null && secondNetwork == null) {
            firstNetwork = new Network();
            data.laserRelayNetworks.add(firstNetwork);
            firstNetwork.connections.add(new ConnectionPair(firstRelay, secondRelay, type, suppressConnectionRender));
            firstNetwork.changeAmount++;
        }
        //The same Network
        else if (firstNetwork == secondNetwork) {
            if (removeIfConnected) {
                this.removeConnection(world, firstRelay, secondRelay);
                return true;
            } else {
                return false;
            }
        }
        //Both relays have laserRelayNetworks
        else if (firstNetwork != null && secondNetwork != null) {
            mergeNetworks(firstNetwork, secondNetwork, world);
            firstNetwork.connections.add(new ConnectionPair(firstRelay, secondRelay, type, suppressConnectionRender));
            firstNetwork.changeAmount++;
        }
        //Only first network exists
        else if (firstNetwork != null) {
            firstNetwork.connections.add(new ConnectionPair(firstRelay, secondRelay, type, suppressConnectionRender));
            firstNetwork.changeAmount++;
        }
        //Only second network exists
        else {
            secondNetwork.connections.add(new ConnectionPair(firstRelay, secondRelay, type, suppressConnectionRender));
            secondNetwork.changeAmount++;
        }
        //System.out.println("Connected "+firstRelay.toString()+" to "+secondRelay.toString());
        //System.out.println(firstNetwork == null ? secondNetwork.toString() : firstNetwork.toString());
        //System.out.println(laserRelayNetworks);
        data.setDirty();
        return true;
    }

    @Override
    public void removeConnection(Level world, BlockPos firstRelay, BlockPos secondRelay) {
        if (world != null && !world.isClientSide && firstRelay != null && secondRelay != null) {
            Network network = this.getNetworkFor(firstRelay, world);

            if (network != null) {
                network.changeAmount++;

                WorldData data = WorldData.get(world);
                data.laserRelayNetworks.remove(network);
                data.setDirty();

                for (IConnectionPair pair : network.connections) {
                    if (!pair.contains(firstRelay) || !pair.contains(secondRelay)) {
                        this.addConnection(pair.getPositions()[0], pair.getPositions()[1], pair.getType(), world, pair.doesSuppressRender());
                    }
                }
            }
        }
    }

    @Override
    public LaserType getTypeFromLaser(BlockEntity tile) {
        if (tile instanceof TileEntityLaserRelay) {
            return ((TileEntityLaserRelay) tile).type;
        } else {
            return null;
        }
    }

    @Override
    public LaserType getTypeFromLaser(BlockPos pos, Level world) {
        return this.getTypeFromLaser(world.getBlockEntity(pos));
    }

}
