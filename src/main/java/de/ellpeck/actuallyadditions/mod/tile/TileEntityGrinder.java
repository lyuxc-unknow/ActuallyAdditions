/*
 * This file ("TileEntityGrinder.java") is part of the Actually Additions mod for Minecraft.
 * It is created and owned by Ellpeck and distributed
 * under the Actually Additions License to be found at
 * http://ellpeck.de/actaddlicense
 * View the source code at https://github.com/Ellpeck/ActuallyAdditions
 *
 * © 2015-2017 Ellpeck
 */

package de.ellpeck.actuallyadditions.mod.tile;

import de.ellpeck.actuallyadditions.api.recipe.CrusherRecipe;
import de.ellpeck.actuallyadditions.mod.blocks.BlockFurnaceDouble;
import de.ellpeck.actuallyadditions.mod.misc.SoundHandler;
import de.ellpeck.actuallyadditions.mod.network.gui.IButtonReactor;
import de.ellpeck.actuallyadditions.mod.recipe.CrusherRecipeRegistry;
import de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA.IAcceptor;
import de.ellpeck.actuallyadditions.mod.util.ItemStackHandlerAA.IRemover;
import de.ellpeck.actuallyadditions.mod.util.StackUtil;
import de.ellpeck.actuallyadditions.mod.util.Util;
import net.minecraft.block.state.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.energy.IEnergyStorage;

public class TileEntityGrinder extends TileEntityInventoryBase implements IButtonReactor {

    public static final int SLOT_INPUT_1 = 0;
    public static final int SLOT_OUTPUT_1_1 = 1;
    public static final int SLOT_OUTPUT_1_2 = 2;
    public static final int SLOT_INPUT_2 = 3;
    public static final int SLOT_OUTPUT_2_1 = 4;
    public static final int SLOT_OUTPUT_2_2 = 5;
    public static final int ENERGY_USE = 40;
    public final CustomEnergyStorage storage = new CustomEnergyStorage(60000, 100, 0);
    public int firstCrushTime;
    public int secondCrushTime;
    public boolean isDouble;
    public boolean isAutoSplit;
    private int lastEnergy;
    private int lastFirstCrush;
    private int lastSecondCrush;
    private boolean lastAutoSplit;
    private boolean lastCrushed;

    public TileEntityGrinder(int slots, String name) {
        super(slots, name);
    }

    public TileEntityGrinder() {
        super(3, "grinder");
        this.isDouble = false;
    }

    @Override
    public void writeSyncableNBT(CompoundNBT compound, NBTType type) {
        if (type != NBTType.SAVE_BLOCK) {
            compound.setInteger("FirstCrushTime", this.firstCrushTime);
            compound.setInteger("SecondCrushTime", this.secondCrushTime);
            compound.setBoolean("IsAutoSplit", this.isAutoSplit);
        }
        this.storage.writeToNBT(compound);
        super.writeSyncableNBT(compound, type);
    }

    @Override
    public void readSyncableNBT(CompoundNBT compound, NBTType type) {
        if (type != NBTType.SAVE_BLOCK) {
            this.firstCrushTime = compound.getInteger("FirstCrushTime");
            this.secondCrushTime = compound.getInteger("SecondCrushTime");
            this.isAutoSplit = compound.getBoolean("IsAutoSplit");
        }
        this.storage.readFromNBT(compound);
        super.readSyncableNBT(compound, type);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!this.world.isRemote) {
            if (this.isDouble && this.isAutoSplit) {
                TileEntityFurnaceDouble.autoSplit(this.inv, SLOT_INPUT_1, SLOT_INPUT_2);
            }

            boolean crushed = false;

            boolean canCrushOnFirst = this.canCrushOn(SLOT_INPUT_1, SLOT_OUTPUT_1_1, SLOT_OUTPUT_1_2);
            boolean canCrushOnSecond = false;
            if (this.isDouble) {
                canCrushOnSecond = this.canCrushOn(SLOT_INPUT_2, SLOT_OUTPUT_2_1, SLOT_OUTPUT_2_2);
            }

            boolean shouldPlaySound = false;

            if (canCrushOnFirst) {
                if (this.storage.getEnergyStored() >= ENERGY_USE) {
                    if (this.firstCrushTime % 20 == 0) {
                        shouldPlaySound = true;
                    }
                    this.firstCrushTime++;
                    if (this.firstCrushTime >= this.getMaxCrushTime()) {
                        this.finishCrushing(SLOT_INPUT_1, SLOT_OUTPUT_1_1, SLOT_OUTPUT_1_2);
                        this.firstCrushTime = 0;
                    }
                    this.storage.extractEnergyInternal(ENERGY_USE, false);
                }
                crushed = this.storage.getEnergyStored() >= ENERGY_USE;
            } else {
                this.firstCrushTime = 0;
            }

            if (this.isDouble) {
                if (canCrushOnSecond) {
                    if (this.storage.getEnergyStored() >= ENERGY_USE) {
                        if (this.secondCrushTime % 20 == 0) {
                            shouldPlaySound = true;
                        }
                        this.secondCrushTime++;
                        if (this.secondCrushTime >= this.getMaxCrushTime()) {
                            this.finishCrushing(SLOT_INPUT_2, SLOT_OUTPUT_2_1, SLOT_OUTPUT_2_2);
                            this.secondCrushTime = 0;
                        }
                        this.storage.extractEnergyInternal(ENERGY_USE, false);
                    }
                    crushed = this.storage.getEnergyStored() >= ENERGY_USE;
                } else {
                    this.secondCrushTime = 0;
                }
            }

            BlockState currState = this.world.getBlockState(this.pos);
            boolean current = currState.getValue(BlockFurnaceDouble.IS_ON);
            boolean changeTo = current;
            if (this.lastCrushed != crushed) {
                changeTo = crushed;
            }
            if (this.isRedstonePowered) {
                changeTo = true;
            }
            if (!crushed && !this.isRedstonePowered) {
                changeTo = false;
            }

            if (changeTo != current) {
                this.world.setBlockState(this.pos, currState.withProperty(BlockFurnaceDouble.IS_ON, changeTo));
            }

            this.lastCrushed = crushed;

            if ((this.lastEnergy != this.storage.getEnergyStored() || this.lastFirstCrush != this.firstCrushTime || this.lastSecondCrush != this.secondCrushTime || this.isAutoSplit != this.lastAutoSplit) && this.sendUpdateWithInterval()) {
                this.lastEnergy = this.storage.getEnergyStored();
                this.lastFirstCrush = this.firstCrushTime;
                this.lastSecondCrush = this.secondCrushTime;
                this.lastAutoSplit = this.isAutoSplit;
            }

            if (shouldPlaySound) {
                this.world.playSound(null, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), SoundHandler.crusher, SoundCategory.BLOCKS, 0.025F, 1.0F);
            }
        }
    }

    @Override
    public IAcceptor getAcceptor() {
        return (slot, stack, automation) -> !automation || (slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2) && CrusherRecipeRegistry.getRecipeFromInput(stack) != null;
    }

    @Override
    public IRemover getRemover() {
        return (slot, automation) -> !automation || slot == SLOT_OUTPUT_1_1 || slot == SLOT_OUTPUT_1_2 || slot == SLOT_OUTPUT_2_1 || slot == SLOT_OUTPUT_2_2;
    }

    public boolean canCrushOn(int theInput, int theFirstOutput, int theSecondOutput) {
        if (StackUtil.isValid(this.inv.getStackInSlot(theInput))) {
            CrusherRecipe recipe = CrusherRecipeRegistry.getRecipeFromInput(this.inv.getStackInSlot(theInput));
            if (recipe == null) {
                return false;
            }
            ItemStack outputOne = recipe.getOutputOne();
            ItemStack outputTwo = recipe.getOutputTwo();
            if (StackUtil.isValid(outputOne)) {
                if (outputOne.getItemDamage() == Util.WILDCARD) {
                    outputOne.setItemDamage(0);
                }
                if (StackUtil.isValid(outputTwo) && outputTwo.getItemDamage() == Util.WILDCARD) {
                    outputTwo.setItemDamage(0);
                }
                if ((!StackUtil.isValid(this.inv.getStackInSlot(theFirstOutput)) || this.inv.getStackInSlot(theFirstOutput).isItemEqual(outputOne) && this.inv.getStackInSlot(theFirstOutput).getCount() <= this.inv.getStackInSlot(theFirstOutput).getMaxStackSize() - outputOne.getCount()) && (!StackUtil.isValid(outputTwo) || !StackUtil.isValid(this.inv.getStackInSlot(theSecondOutput)) || this.inv.getStackInSlot(theSecondOutput).isItemEqual(outputTwo) && this.inv.getStackInSlot(theSecondOutput).getCount() <= this.inv.getStackInSlot(theSecondOutput).getMaxStackSize() - outputTwo.getCount())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getMaxCrushTime() {
        return this.isDouble
            ? 150
            : 100;
    }

    public void finishCrushing(int theInput, int theFirstOutput, int theSecondOutput) {
        CrusherRecipe recipe = CrusherRecipeRegistry.getRecipeFromInput(this.inv.getStackInSlot(theInput));
        if (recipe == null) {
            return;
        }
        ItemStack outputOne = recipe.getOutputOne();
        if (StackUtil.isValid(outputOne)) {
            if (outputOne.getItemDamage() == Util.WILDCARD) {
                outputOne.setItemDamage(0);
            }
            if (!StackUtil.isValid(this.inv.getStackInSlot(theFirstOutput))) {
                this.inv.setStackInSlot(theFirstOutput, outputOne.copy());
            } else if (this.inv.getStackInSlot(theFirstOutput).getItem() == outputOne.getItem()) {
                this.inv.setStackInSlot(theFirstOutput, StackUtil.grow(this.inv.getStackInSlot(theFirstOutput), outputOne.getCount()));
            }
        }

        ItemStack outputTwo = recipe.getOutputTwo();
        if (StackUtil.isValid(outputTwo)) {
            if (outputTwo.getItemDamage() == Util.WILDCARD) {
                outputTwo.setItemDamage(0);
            }
            int rand = this.world.rand.nextInt(100) + 1;
            if (rand <= recipe.getSecondChance()) {
                if (!StackUtil.isValid(this.inv.getStackInSlot(theSecondOutput))) {
                    this.inv.setStackInSlot(theSecondOutput, outputTwo.copy());
                } else if (this.inv.getStackInSlot(theSecondOutput).getItem() == outputTwo.getItem()) {
                    this.inv.setStackInSlot(theSecondOutput, StackUtil.grow(this.inv.getStackInSlot(theSecondOutput), outputTwo.getCount()));
                }
            }
        }

        this.inv.getStackInSlot(theInput).shrink(1);
    }

    public int getEnergyScaled(int i) {
        return this.storage.getEnergyStored() * i / this.storage.getMaxEnergyStored();
    }

    public int getFirstTimeToScale(int i) {
        return this.firstCrushTime * i / this.getMaxCrushTime();
    }

    public int getSecondTimeToScale(int i) {
        return this.secondCrushTime * i / this.getMaxCrushTime();
    }

    @Override
    public void onButtonPressed(int buttonID, PlayerEntity player) {
        if (buttonID == 0) {
            this.isAutoSplit = !this.isAutoSplit;
            this.markDirty();
        }
    }

    @Override
    public IEnergyStorage getEnergyStorage(EnumFacing facing) {
        return this.storage;
    }
}
