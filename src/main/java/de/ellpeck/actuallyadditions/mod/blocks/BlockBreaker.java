/*
 * This file ("BlockBreaker.java") is part of the Actually Additions mod for Minecraft.
 * It is created and owned by Ellpeck and distributed
 * under the Actually Additions License to be found at
 * http://ellpeck.de/actaddlicense
 * View the source code at https://github.com/Ellpeck/ActuallyAdditions
 *
 * © 2015-2017 Ellpeck
 */

package de.ellpeck.actuallyadditions.mod.blocks;

import de.ellpeck.actuallyadditions.mod.ActuallyAdditions;
import de.ellpeck.actuallyadditions.mod.blocks.base.BlockContainerBase;
import de.ellpeck.actuallyadditions.mod.inventory.GuiHandler;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBreaker;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPlacer;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBreaker extends BlockContainerBase {

    private final boolean isPlacer;

    public BlockBreaker(boolean isPlacer, String name) {
        super(Material.ROCK, name);
        this.isPlacer = isPlacer;
        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(1.5F);
        this.setResistance(10.0F);
        this.setSoundType(SoundType.STONE);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int par2) {
        return this.isPlacer
            ? new TileEntityPlacer()
            : new TileEntityBreaker();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, BlockState state, PlayerEntity player, Hand hand, EnumFacing par6, float par7, float par8, float par9) {
        if (this.tryToggleRedstone(world, pos, player)) {
            return true;
        }
        if (!world.isRemote) {
            TileEntityBreaker breaker = (TileEntityBreaker) world.getTileEntity(pos);
            if (breaker != null) {
                player.openGui(ActuallyAdditions.INSTANCE, GuiHandler.GuiTypes.BREAKER.ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return true;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, EntityLivingBase player, ItemStack stack) {
        int rotation = EnumFacing.getDirectionFromEntityLiving(pos, player).ordinal();
        world.setBlockState(pos, this.getStateFromMeta(rotation), 2);

        super.onBlockPlacedBy(world, pos, state, player, stack);
    }

    @Override
    public BlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(BlockDirectional.FACING, EnumFacing.byIndex(meta));
    }

    @Override
    public int getMetaFromState(BlockState state) {
        return state.getValue(BlockDirectional.FACING).getIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockDirectional.FACING);
    }

    @Override
    public BlockState withRotation(BlockState state, Rotation rot) {
        return state.withProperty(BlockDirectional.FACING, rot.rotate(state.getValue(BlockDirectional.FACING)));
    }

    @Override
    public BlockState withMirror(BlockState state, Mirror mirror) {
        return this.withRotation(state, mirror.toRotation(state.getValue(BlockDirectional.FACING)));
    }
}
