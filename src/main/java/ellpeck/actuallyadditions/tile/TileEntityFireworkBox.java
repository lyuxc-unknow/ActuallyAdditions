/*
 * This file ("TileEntityFireworkBox.java") is part of the Actually Additions Mod for Minecraft.
 * It is created and owned by Ellpeck and distributed
 * under the Actually Additions License to be found at
 * http://github.com/Ellpeck/ActuallyAdditions/blob/master/README.md
 * View the source code at https://github.com/Ellpeck/ActuallyAdditions
 *
 * © 2015 Ellpeck
 */

package ellpeck.actuallyadditions.tile;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyReceiver;
import ellpeck.actuallyadditions.util.Util;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityFireworkBox extends TileEntityBase implements IEnergyReceiver{

    public EnergyStorage storage = new EnergyStorage(20000);

    private int timeUntilNextFirework;

    public static final int USE_PER_SHOT = 300;

    @Override
    public void updateEntity(){
        if(!this.worldObj.isRemote && !this.isRedstonePowered){
            if(this.timeUntilNextFirework > 0){
                this.timeUntilNextFirework--;
                if(this.timeUntilNextFirework <= 0 && this.storage.getEnergyStored() >= USE_PER_SHOT){
                    int range = 4;
                    int amount = Util.RANDOM.nextInt(5)+1;
                    for(int i = 0; i < amount; i++){
                        ItemStack firework = this.makeFirework();

                        double x = this.xCoord+MathHelper.getRandomDoubleInRange(Util.RANDOM, 0, range*2)-range;
                        double z = this.zCoord+MathHelper.getRandomDoubleInRange(Util.RANDOM, 0, range*2)-range;
                        EntityFireworkRocket rocket = new EntityFireworkRocket(this.worldObj, x, this.yCoord+0.5, z, firework);
                        this.worldObj.spawnEntityInWorld(rocket);
                    }

                    this.storage.extractEnergy(USE_PER_SHOT, false);
                }
            }
            else{
                this.timeUntilNextFirework = 100;
            }
        }
    }

    private ItemStack makeFirework(){
        NBTTagList list = new NBTTagList();
        int chargesAmount = Util.RANDOM.nextInt(2)+1;
        for(int i = 0; i < chargesAmount; i++){
            list.appendTag(this.makeFireworkCharge());
        }

        NBTTagCompound compound1 = new NBTTagCompound();
        compound1.setTag("Explosions", list);
        compound1.setByte("Flight", (byte)(Util.RANDOM.nextInt(3)+1));

        NBTTagCompound compound = new NBTTagCompound();
        compound.setTag("Fireworks", compound1);

        ItemStack firework = new ItemStack(Items.fireworks);
        firework.setTagCompound(compound);

        return firework;
    }

    private NBTTagCompound makeFireworkCharge(){
        NBTTagCompound compound = new NBTTagCompound();

        if(Util.RANDOM.nextFloat() >= 0.65F){
            if(Util.RANDOM.nextFloat() >= 0.5F){
                compound.setBoolean("Flicker", true);
            }
            else{
                compound.setBoolean("Trail", true);
            }
        }

        int[] colors = new int[MathHelper.getRandomIntegerInRange(Util.RANDOM, 1, 6)];
        for(int i = 0; i < colors.length; i++){
            colors[i] = ItemDye.field_150922_c[Util.RANDOM.nextInt(ItemDye.field_150922_c.length)];
        }
        compound.setIntArray("Colors", colors);

        compound.setByte("Type", (byte)Util.RANDOM.nextInt(5));

        return compound;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate){
        return this.storage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int getEnergyStored(ForgeDirection from){
        return this.storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from){
        return this.storage.getMaxEnergyStored();
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from){
        return true;
    }
}
