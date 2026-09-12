package dev.muon.raven_apoth.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_apoth.RavenApoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.util.DamageSourceExtension;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class TransmutationAffix extends Affix {
    public static final Codec<TransmutationAffix> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Affix.affixDef(),
            ResourceLocation.CODEC.fieldOf("school").forGetter(a -> a.schoolId),
            LootCategory.SET_CODEC.fieldOf("types").forGetter(a -> a.types))
        .apply(inst, TransmutationAffix::new));

    protected final ResourceLocation schoolId;
    protected final Set<LootCategory> types;
    protected final TagKey<DamageType> damageTypeTag;
    private boolean missingSchoolWarned;

    public TransmutationAffix(AffixDefinition definition, ResourceLocation schoolId, Set<LootCategory> types) {
        super(definition);
        this.schoolId = schoolId;
        this.types = types;
        this.damageTypeTag = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(schoolId.getNamespace(), schoolId.getPath() + "_magic"));
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }

    @Override
    public MutableComponent getDescription(AffixInstance inst, AttributeTooltipContext ctx) {
        SchoolType school = this.getSchool();
        Component schoolName = school != null ? school.getDisplayName().copy() : Component.literal(this.schoolId.toString());
        return Component.translatable("affix.raven_apoth:transmutation.desc", schoolName);
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (this.getSchool() == null || cat == null || cat.isNone()) {
            return false;
        }
        return this.types.isEmpty() || this.types.contains(cat);
    }

    @Nullable
    public SchoolType getSchool() {
        SchoolType school = SchoolRegistry.getSchool(this.schoolId);
        if (school == null && !this.missingSchoolWarned) {
            this.missingSchoolWarned = true;
            RavenApoth.LOGGER.warn("Transmutation affix references unknown school {}; the affix is disabled", this.schoolId);
        }
        return school;
    }

    public TagKey<DamageType> getDamageTypeTag() {
        return this.damageTypeTag;
    }

    public static void tagDamageSource(LivingIncomingDamageEvent e) {
        DamageSource source = e.getSource();
        if (!isWeaponDamage(source) || !(source instanceof DamageSourceExtension ext)) return;

        findTransmutation(source).ifPresent(affix -> {
            ext.addTag(affix.getDamageTypeTag());
            RavenApoth.LOGGER.debug("Transmutation: {} -> {} tagged {}", source.getEntity(), e.getEntity(), affix.getDamageTypeTag().location());
        });
    }

    private static boolean isWeaponDamage(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
            || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.ARROW);
    }

    private static Optional<TransmutationAffix> findTransmutation(DamageSource source) {
        Stream<AffixInstance> affixes;
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            affixes = AffixHelper.streamAffixes(arrow);
        }
        else if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack mainHand = attacker.getMainHandItem();
            if (mainHand.isEmpty()) return Optional.empty();
            affixes = AffixHelper.streamAffixes(mainHand);
        }
        else {
            return Optional.empty();
        }
        return affixes.map(AffixInstance::getAffix)
            .filter(TransmutationAffix.class::isInstance)
            .map(TransmutationAffix.class::cast)
            .findFirst();
    }
}
