package mekanism.generators.common.block.reactor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.block.BlockTileDrops;
import mekanism.common.block.interfaces.IHasGui;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.inventory.container.ContainerProvider;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.util.MekanismUtils;
import mekanism.generators.common.MekanismGenerators;
import mekanism.generators.common.inventory.container.reactor.ReactorLogicAdapterContainer;
import mekanism.generators.common.tile.reactor.TileEntityReactorLogicAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BlockReactorLogicAdapter extends BlockTileDrops implements IHasGui<TileEntityReactorLogicAdapter>, IHasTileEntity<TileEntityReactorLogicAdapter> {

    public BlockReactorLogicAdapter() {
        super(Block.Properties.create(Material.IRON).hardnessAndResistance(3.5F, 8F));
        setRegistryName(new ResourceLocation(MekanismGenerators.MODID, "reactor_logic_adapter"));
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TileEntityMekanism) {
                ((TileEntityMekanism) tileEntity).onNeighborChange(neighborBlock);
            }
        }
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) {
            return true;
        }
        TileEntityMekanism tileEntity = (TileEntityMekanism) world.getTileEntity(pos);
        if (tileEntity.tryWrench(state, player, hand, hit) != WrenchResult.PASS) {
            return true;
        }
        if (tileEntity.openGui(player)) {
            return true;
        }
        return false;
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return new TileEntityReactorLogicAdapter();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getWeakPower(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        TileEntity tile = MekanismUtils.getTileEntitySafe(world, pos);
        if (tile instanceof TileEntityReactorLogicAdapter) {
            return ((TileEntityReactorLogicAdapter) tile).checkMode() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, Direction side) {
        return true;
    }

    @Nullable
    @Override
    public Class<? extends TileEntityReactorLogicAdapter> getTileClass() {
        return TileEntityReactorLogicAdapter.class;
    }

    @Override
    public INamedContainerProvider getProvider(TileEntityReactorLogicAdapter tile) {
        return new ContainerProvider("mekanismgenerators.container.reactor_logic_adapter", (i, inv, player) -> new ReactorLogicAdapterContainer(i, inv, tile));
    }
}