package mekanism.common.item.block;

import java.util.List;
import javax.annotation.Nonnull;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.machine.prefab.BlockBase;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.tier.InductionProviderTier;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemBlockInductionProvider extends ItemBlockTooltip<BlockBase<BlockType>> {

    public ItemBlockInductionProvider(BlockBase<BlockType> block) {
        super(block);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addStats(@Nonnull ItemStack stack, World world, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        InductionProviderTier tier = (InductionProviderTier) Attribute.get(getBlock(), AttributeTier.class).getTier();
        if (tier != null) {
            tooltip.add(MekanismLang.INDUCTION_PORT_OUTPUT_RATE.translateColored(tier.getBaseTier().getColor(), EnumColor.GRAY, EnergyDisplay.of(tier.getOutput())));
        }
    }
}