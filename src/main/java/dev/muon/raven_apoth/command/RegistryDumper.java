package dev.muon.raven_apoth.command;

import com.google.common.collect.Multimap;
import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.mixin.compat.irons_spellbooks.SchoolTypeAccessor;
import dev.muon.raven_apoth.mixin.compat.irons_spellbooks.SpellConfigManagerAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.loot.AffixLootRegistry;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.RarityOverrideRegistry;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.AugmentRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.EliteRegistry;
import dev.shadowsoffire.apotheosis.mobs.registries.InvaderRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.ExtraGemBonusRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.socket.gem.PurityWeightsRegistry;
import dev.shadowsoffire.apotheosis.tiers.augments.TierAugmentRegistry;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import dev.shadowsoffire.placebo.systems.wanderer.WandererTradesRegistry;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.common.loot.LootModifierManager;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.theillusivec4.champions.api.champion.ChampionTier;
import top.theillusivec4.champions.common.api.ChampionsRegistries;
import top.theillusivec4.champions.common.archetype.ChampionArchetype;
import top.theillusivec4.champions.common.data.ModifierSetting;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class RegistryDumper {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<String, Supplier<DynamicRegistry<?>>> PLACEBO_REGISTRIES = new LinkedHashMap<>();
    private static final String LOOT_CATEGORIES = "loot_categories";
    private static final String RECIPES = "recipes";
    private static final String ITEMS = "items";
    private static final String SCHOOLS = "schools";
    private static final String SPELLS = "spells";
    private static final String GLOBAL_LOOT_MODIFIERS = "global_loot_modifiers";
    private static final String CHAMPIONS = "champions";
    private static final List<String> TARGETS = new ArrayList<>();

    static {
        PLACEBO_REGISTRIES.put("affixes", () -> AffixRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("rarities", () -> RarityRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("rarity_overrides", () -> RarityOverrideRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("gems", () -> GemRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("extra_gem_bonuses", () -> ExtraGemBonusRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("purity_weights", () -> PurityWeightsRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("affix_loot_entries", () -> AffixLootRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("invaders", () -> InvaderRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("elites", () -> EliteRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("augmentations", () -> AugmentRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("tier_augments", () -> TierAugmentRegistry.INSTANCE);
        PLACEBO_REGISTRIES.put("wanderer_trades", () -> WandererTradesRegistry.INSTANCE);
        TARGETS.addAll(PLACEBO_REGISTRIES.keySet());
        TARGETS.add(LOOT_CATEGORIES);
        TARGETS.add(RECIPES);
        TARGETS.add(ITEMS);
        TARGETS.add(SCHOOLS);
        TARGETS.add(SPELLS);
        TARGETS.add(GLOBAL_LOOT_MODIFIERS);
        TARGETS.add(CHAMPIONS);
    }

    public static List<String> targets() {
        return TARGETS;
    }

    private final MinecraftServer server;
    private final RegistryOps<JsonElement> ops;
    private final Path dir;
    private final JsonObject counts = new JsonObject();
    private int errors;

    public RegistryDumper(MinecraftServer server, Path dir) {
        this.server = server;
        this.ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        this.dir = dir;
    }

    public int errors() {
        return this.errors;
    }

    public int dumpAll() throws IOException {
        int total = 0;
        for (String target : TARGETS) {
            total += this.dump(target);
        }
        return total;
    }

    public int dump(String target) throws IOException {
        Files.createDirectories(this.dir);
        JsonArray entries = this.collect(target);
        this.write(target + ".json", entries);
        this.counts.addProperty(target, entries.size());
        this.writeSummary();
        return entries.size();
    }

    private JsonArray collect(String target) {
        Supplier<DynamicRegistry<?>> placebo = PLACEBO_REGISTRIES.get(target);
        if (placebo != null) return this.dumpDynamicRegistry(placebo.get());
        return switch (target) {
            case LOOT_CATEGORIES -> this.dumpLootCategories();
            case RECIPES -> this.dumpRecipes();
            case ITEMS -> this.dumpItems();
            case SCHOOLS -> this.dumpSchools();
            case SPELLS -> this.dumpSpells();
            case GLOBAL_LOOT_MODIFIERS -> this.dumpGlobalLootModifiers();
            case CHAMPIONS -> this.dumpChampions();
            default -> throw new IllegalArgumentException("Unknown dump target " + target);
        };
    }

    private void writeSummary() throws IOException {
        JsonObject summary = new JsonObject();
        summary.add("counts", this.counts);
        summary.addProperty("errors", this.errors);
        this.write("summary.json", summary);
    }

    private void write(String fileName, JsonElement json) throws IOException {
        try (Writer writer = Files.newBufferedWriter(this.dir.resolve(fileName), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    @SuppressWarnings("unchecked")
    private JsonArray dumpDynamicRegistry(DynamicRegistry<?> registry) {
        JsonArray out = new JsonArray();
        Codec<Object> codec = (Codec<Object>) registry.elementCodec();
        for (ResourceLocation id : registry.getKeys()) {
            Object value = registry.getValue(id);
            JsonObject entry = this.entry(id, dataFile(id, registry.getPath()));
            this.encode(entry, codec, value);
            out.add(entry);
        }
        return out;
    }

    private JsonArray dumpLootCategories() {
        JsonArray out = new JsonArray();
        for (ResourceLocation id : Apoth.BuiltInRegs.LOOT_CATEGORY.keySet()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            out.add(entry);
        }
        return out;
    }

    private JsonArray dumpItems() {
        JsonArray out = new JsonArray();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.get(id);
            ItemStack stack = new ItemStack(item);
            JsonObject entry = new JsonObject();
            entry.addProperty("id", id.toString());
            entry.addProperty("mod", id.getNamespace());
            entry.addProperty("loot_category", LootCategory.forItem(stack).getKey().toString());
            entry.add("attributes", defaultAttributes(stack));
            entry.add("curio_attributes", curioAttributes(stack));
            entry.add("focus_schools", focusSchools(stack));
            out.add(entry);
        }
        return out;
    }

    private static JsonArray defaultAttributes(ItemStack stack) {
        JsonArray out = new JsonArray();
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, stack.getItem().getDefaultAttributeModifiers(stack));
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            out.add(attributeEntry(entry.attribute(), entry.modifier(), entry.slot().getSerializedName()));
        }
        return out;
    }

    private static JsonArray curioAttributes(ItemStack stack) {
        JsonArray out = new JsonArray();
        if (!(stack.getItem() instanceof ICurioItem curio)) return out;
        for (String slotId : CuriosApi.getItemStackSlots(stack, false).keySet()) {
            SlotContext context = new SlotContext(slotId, null, -1, false, true);
            Multimap<Holder<Attribute>, AttributeModifier> modifiers = curio.getAttributeModifiers(context, RavenApoth.loc("dump"), stack);
            for (Map.Entry<Holder<Attribute>, AttributeModifier> entry : modifiers.entries()) {
                out.add(attributeEntry(entry.getKey(), entry.getValue(), "curio:" + slotId));
            }
        }
        return out;
    }

    private static JsonObject attributeEntry(Holder<Attribute> attribute, AttributeModifier modifier, String slot) {
        JsonObject out = new JsonObject();
        out.addProperty("attribute", attributeId(attribute));
        out.addProperty("operation", modifier.operation().getSerializedName());
        out.addProperty("amount", modifier.amount());
        out.addProperty("slot", slot);
        return out;
    }

    private static JsonArray focusSchools(ItemStack stack) {
        JsonArray out = new JsonArray();
        for (SchoolType school : SchoolRegistry.REGISTRY) {
            if (school.isFocus(stack)) out.add(school.getId().toString());
        }
        return out;
    }

    private static String attributeId(Holder<Attribute> holder) {
        return holder.unwrapKey().map(k -> k.location().toString()).orElse("unbound");
    }

    private JsonArray dumpSchools() {
        JsonArray out = new JsonArray();
        for (SchoolType school : SchoolRegistry.REGISTRY) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", school.getId().toString());
            entry.addProperty("display_name", school.getDisplayName().getString());
            SchoolTypeAccessor accessor = (SchoolTypeAccessor) school;
            entry.addProperty("power_attribute", attributeId(accessor.raven_apoth$getPowerAttribute()));
            entry.addProperty("resistance_attribute", attributeId(accessor.raven_apoth$getResistanceAttribute()));
            entry.addProperty("damage_type", school.getDamageType().location().toString());
            out.add(entry);
        }
        return out;
    }

    private JsonArray dumpSpells() {
        // Iron's only builds the per-spell config on the first datapack sync, so on a dedicated server before any
        // player joins every spell still reports the parameter defaults (school evocation). Only force that first
        // build: once a client sync has run, a forced sync rebuilds from the config folder and drops every datapack override
        SpellConfigManagerAccessor manager = (SpellConfigManagerAccessor) SpellConfigManager.INSTANCE;
        boolean unbuilt = manager.raven_apoth$getConfig() == null || manager.raven_apoth$getConfig().isEmpty();
        if (unbuilt || manager.raven_apoth$getDatapackOverride() != null) {
            SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(this.server.getPlayerList(), null));
        }
        JsonArray out = new JsonArray();
        for (AbstractSpell spell : SpellRegistry.REGISTRY) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", spell.getSpellResource().toString());
            entry.addProperty("school", spell.getSchoolType().getId().toString());
            entry.addProperty("enabled", spell.isEnabled());
            entry.addProperty("max_level", spell.getMaxLevel());
            out.add(entry);
        }
        return out;
    }

    private JsonArray dumpRecipes() {
        JsonArray out = new JsonArray();
        for (RecipeHolder<?> holder : this.server.getRecipeManager().getRecipes()) {
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (typeId == null || !isDumpedRecipeType(typeId)) continue;
            JsonObject entry = this.entry(holder.id(), dataFile(holder.id(), "recipe"));
            entry.addProperty("type", typeId.toString());
            this.encode(entry, Recipe.CODEC, holder.value());
            out.add(entry);
        }
        return out;
    }

    private static boolean isDumpedRecipeType(ResourceLocation typeId) {
        if (typeId.getNamespace().equals("ancientreforging")) return true;
        return typeId.getNamespace().equals("apotheosis")
            && (typeId.getPath().equals("salvaging") || typeId.getPath().equals("reforging"));
    }

    private JsonArray dumpGlobalLootModifiers() {
        JsonArray out = new JsonArray();
        Map<ResourceLocation, IGlobalLootModifier> modifiers;
        try {
            modifiers = loadedLootModifiers();
        } catch (ReflectiveOperationException | RuntimeException e) {
            this.errors++;
            JsonObject entry = new JsonObject();
            entry.addProperty("error", "Could not access LootModifierManager: " + e);
            out.add(entry);
            return out;
        }
        for (Map.Entry<ResourceLocation, IGlobalLootModifier> loaded : modifiers.entrySet()) {
            IGlobalLootModifier modifier = loaded.getValue();
            JsonObject entry = this.entry(loaded.getKey(), dataFile(loaded.getKey(), "loot_modifiers"));
            ResourceLocation type = NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.getKey(modifier.codec());
            entry.addProperty("type", type == null ? modifier.getClass().getName() : type.toString());
            this.encode(entry, IGlobalLootModifier.DIRECT_CODEC, modifier);
            out.add(entry);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, IGlobalLootModifier> loadedLootModifiers() throws ReflectiveOperationException {
        Method getter = Class.forName("net.neoforged.neoforge.common.NeoForgeEventHandler").getDeclaredMethod("getLootModifierManager");
        getter.setAccessible(true);
        LootModifierManager manager = (LootModifierManager) getter.invoke(null);
        Field registered = LootModifierManager.class.getDeclaredField("registeredLootModifiers");
        registered.setAccessible(true);
        Map<ResourceLocation, IGlobalLootModifier> byId = (Map<ResourceLocation, IGlobalLootModifier>) registered.get(manager);
        if (byId != null) return byId;
        Map<ResourceLocation, IGlobalLootModifier> indexed = new LinkedHashMap<>();
        int index = 0;
        for (IGlobalLootModifier modifier : manager.getAllLootMods()) {
            indexed.put(ResourceLocation.fromNamespaceAndPath("unknown", "modifier_" + index++), modifier);
        }
        return indexed;
    }

    private JsonArray dumpChampions() {
        JsonArray out = new JsonArray();
        Set<ResourceLocation> builtinTiers = ChampionsRegistries.tiers().getBuiltinIds();
        for (ChampionTier tier : ChampionsRegistries.tiers().getAll()) {
            JsonObject entry = this.entry(tier.id(), dataFile(tier.id(), "champions/tier"));
            entry.addProperty("kind", "tier");
            entry.addProperty("builtin", builtinTiers.contains(tier.id()));
            JsonObject value = new JsonObject();
            value.addProperty("level", tier.level());
            JsonObject display = new JsonObject();
            display.addProperty("color", tier.display().color());
            display.addProperty("icon", tier.display().icon().toString());
            value.add("display", display);
            entry.add("value", value);
            out.add(entry);
        }
        Set<ResourceLocation> builtinArchetypes = ChampionsRegistries.archetypes().getBuiltinIds();
        for (ChampionArchetype archetype : ChampionsRegistries.archetypes().getAll()) {
            JsonObject entry = this.entry(archetype.id(), dataFile(archetype.id(), "champions/archetype"));
            entry.addProperty("kind", "archetype");
            entry.addProperty("builtin", builtinArchetypes.contains(archetype.id()));
            this.encode(entry, ChampionArchetype.CODEC, archetype);
            out.add(entry);
        }
        Set<ResourceLocation> builtinModifiers = ChampionsRegistries.modifiers().getBuiltinIds();
        for (Map.Entry<ResourceLocation, ModifierSetting> loaded : ChampionsRegistries.modifiers().getLoadedData().entrySet()) {
            JsonObject entry = this.entry(loaded.getKey(), loaded.getKey());
            entry.addProperty("kind", "modifier");
            entry.addProperty("builtin", builtinModifiers.contains(loaded.getKey()));
            this.encode(entry, ModifierSetting.MAP_CODEC.codec(), loaded.getValue());
            out.add(entry);
        }
        return out;
    }

    private JsonObject entry(ResourceLocation id, ResourceLocation dataFile) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id.toString());
        entry.add("sources", this.sources(dataFile));
        return entry;
    }

    private static ResourceLocation dataFile(ResourceLocation id, String folder) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), folder + "/" + id.getPath() + ".json");
    }

    private JsonArray sources(ResourceLocation dataFile) {
        JsonArray out = new JsonArray();
        for (Resource resource : this.server.getResourceManager().getResourceStack(dataFile)) {
            out.add(resource.sourcePackId());
        }
        return out;
    }

    private <T> void encode(JsonObject entry, Codec<T> codec, T value) {
        try {
            DataResult<JsonElement> result = codec.encodeStart(this.ops, value);
            if (result.result().isPresent()) {
                entry.add("value", result.result().get());
            } else {
                this.errors++;
                entry.addProperty("error", result.error().map(DataResult.Error::message).orElse("unknown encode error"));
            }
        } catch (RuntimeException e) {
            this.errors++;
            entry.addProperty("error", e.toString());
        }
    }
}
