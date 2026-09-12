package dev.muon.raven_apoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.event.GetItemSocketsEvent;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Map;
import java.util.Set;

public class SocketBonusAffix extends Affix {
    public static final Codec<SocketBonusAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            LootRarity.mapCodec(SocketData.CODEC).fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
        .apply(inst, SocketBonusAffix::new));

    private static final String DESC_KEY = "affix.raven_apoth.socket_bonus.desc";
    private static final String DESC_MULTIPLE_KEY = "affix.raven_apoth.socket_bonus.desc_multiple";

    protected final Map<LootRarity, SocketData> values;
    protected final Set<LootCategory> types;

    public SocketBonusAffix(AffixDefinition definition, Map<LootRarity, SocketData> values, Set<LootCategory> types) {
        super(definition);
        this.values = values;
        this.types = types;
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        SocketData data = this.values.get(inst.getRarity());
        if (data == null) return Component.empty();
        return describe(data.level().getInt(inst.level()));
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        SocketData data = this.values.get(inst.getRarity());
        if (data == null) return Component.empty();

        MutableComponent comp = describe(data.level().getInt(inst.level()));
        int minBonus = data.level().getInt(0);
        int maxBonus = data.level().getInt(1);
        if (minBonus != maxBonus) {
            comp.append(Affix.valueBounds(Component.literal(String.valueOf(minBonus)), Component.literal(String.valueOf(maxBonus))));
        }
        return comp;
    }

    private static MutableComponent describe(int bonus) {
        return Component.translatable(bonus == 1 ? DESC_KEY : DESC_MULTIPLE_KEY, bonus);
    }

    public int getBonusSockets(LootRarity rarity, float level) {
        SocketData data = this.values.get(rarity);
        return data != null ? data.level().getInt(level) : 0;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (cat == null || cat.isNone()) return false;
        return (this.types.isEmpty() || this.types.contains(cat)) && this.values.containsKey(rarity);
    }

    public static void addBonusSockets(GetItemSocketsEvent e) {
        int bonus = AffixHelper.streamAffixes(e.getStack())
            .filter(inst -> inst.getAffix() instanceof SocketBonusAffix)
            .mapToInt(inst -> ((SocketBonusAffix) inst.getAffix()).getBonusSockets(inst.getRarity(), inst.level()))
            .sum();
        if (bonus > 0) {
            e.setSockets(e.getSockets() + bonus);
        }
    }

    public record SocketData(StepFunction level) {
        private static final Codec<SocketData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(StepFunction.CODEC.optionalFieldOf("sockets", StepFunction.constant(1)).forGetter(SocketData::level))
            .apply(inst, SocketData::new));
    }
}
