package dev.muon.raven_apoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.modifiers.EntitySlotGroup;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;

import java.util.Map;
import java.util.Set;

public class AptitudeBonusAffix extends Affix {
    public static final Codec<AptitudeBonusAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            Codec.STRING.fieldOf("aptitude").forGetter(a -> a.aptitude),
            LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
        .apply(inst, AptitudeBonusAffix::new));

    protected final String aptitude;
    protected final Map<LootRarity, StepFunction> values;
    protected final Set<LootCategory> types;

    public AptitudeBonusAffix(AffixDefinition definition, String aptitude, Map<LootRarity, StepFunction> values, Set<LootCategory> types) {
        super(definition);
        this.aptitude = aptitude;
        this.values = values;
        this.types = types;
    }

    public String aptitude() {
        return this.aptitude;
    }

    public int bonus(AffixInstance inst) {
        StepFunction value = this.values.get(inst.getRarity());
        return value == null ? 0 : value.getInt(inst.level());
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        String key = isHeld(inst.category()) ? "affix.raven_apoth.aptitude_bonus.desc.held" : "affix.raven_apoth.aptitude_bonus.desc.worn";
        return Component.translatable(key, this.bonus(inst), this.aptitudeName());
    }

    @Override
    public Component getAugmentingText(AffixInstance inst, AttributeTooltipContext ctx) {
        MutableComponent comp = this.getDescription(inst, ctx);
        StepFunction value = this.values.get(inst.getRarity());
        if (value != null && value.getInt(0) != value.getInt(1)) {
            comp.append(Affix.valueBounds(Component.literal(String.valueOf(value.getInt(0))), Component.literal(String.valueOf(value.getInt(1)))));
        }
        return comp;
    }

    private Component aptitudeName() {
        Aptitude aptitude = RegistryAptitudes.getAptitude(this.aptitude);
        return aptitude == null ? Component.literal(this.aptitude) : Component.translatable(aptitude.getKey());
    }

    private static boolean isHeld(LootCategory category) {
        EntitySlotGroup slots = category.getSlots();
        return slots.test(ALObjects.EquipmentSlots.MAINHAND) || slots.test(ALObjects.EquipmentSlots.OFFHAND);
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        return !cat.isNone() && this.values.containsKey(rarity) && this.types.contains(cat);
    }
}
