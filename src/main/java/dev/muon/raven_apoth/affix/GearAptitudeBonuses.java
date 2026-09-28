package dev.muon.raven_apoth.affix;

import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.network.packet.client.SyncAptitudeCapabilityCP;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_apoth.RavenApoth;
import dev.muon.raven_apoth.RavenApothAttachments;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.compat.CurioEquipmentSlot;
import dev.shadowsoffire.apothic_attributes.modifiers.EquipmentSlotCompat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Applies equipped {@link AptitudeBonusAffix} bonuses as the same baseline shift Raven DnD Origins' innate aptitude
 * bonus power performs, and reports them through that power's lookups so they share its limit-breaking.
 */
public class GearAptitudeBonuses {

    private static volatile Map<String, Integer> localPlayerBonuses = Map.of();

    public static void register(IEventBus modBus) {
        modBus.addListener(GearAptitudeBonuses::registerPayload);
        NeoForge.EVENT_BUS.register(GearAptitudeBonuses.class);
    }

    public static int applied(Entity entity, String aptitudeName) {
        return appliedOn(entity).getOrDefault(aptitudeName, 0);
    }

    public static int appliedSum(Entity entity, Set<String> aptitudeNames) {
        int sum = 0;
        for (Map.Entry<String, Integer> entry : appliedOn(entity).entrySet()) {
            if (aptitudeNames.contains(entry.getKey())) sum += entry.getValue();
        }
        return sum;
    }

    // Clients only ever learn their own bonuses.
    private static Map<String, Integer> appliedOn(Entity entity) {
        if (!(entity instanceof Player player)) return Map.of();
        if (player.level().isClientSide()) return player.isLocalPlayer() ? localPlayerBonuses : Map.of();
        Map<String, Integer> applied = player.getExistingDataOrNull(RavenApothAttachments.GEAR_APTITUDE_BONUSES);
        return applied == null ? Map.of() : applied;
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && updateApplied(player)) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && updateApplied(player)) {
            sync(player);
        }
    }

    // With nothing equipped after respawn, no equipment change fires to remove the bonuses copied over from death.
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && updateApplied(player)) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updateApplied(player);
            sync(player);
        }
    }

    private static boolean updateApplied(ServerPlayer player) {
        Map<String, Integer> applied = appliedOn(player);
        Map<String, Integer> equipped = equippedBonuses(player);
        if (equipped.equals(applied)) return false;
        AptitudeCapability capability = AptitudeCapability.get(player);
        if (capability == null) return false;

        Set<String> aptitudeNames = new HashSet<>(applied.keySet());
        aptitudeNames.addAll(equipped.keySet());
        for (String aptitudeName : aptitudeNames) {
            int delta = equipped.getOrDefault(aptitudeName, 0) - applied.getOrDefault(aptitudeName, 0);
            Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
            if (delta != 0 && aptitude != null) {
                // Baseline shift, not a level-up: writes the map directly so no AptitudeChangedEvent fires.
                capability.aptitudeLevel.put(aptitude.getName(), Math.max(capability.getAptitudeLevel(aptitude) + delta, 1));
            }
        }
        player.setData(RavenApothAttachments.GEAR_APTITUDE_BONUSES, equipped);
        return true;
    }

    private static Map<String, Integer> equippedBonuses(Player player) {
        Map<String, Integer> bonuses = new HashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && LootCategory.forItem(stack).getSlots().test(EquipmentSlotCompat.fromVanilla(slot))) {
                addBonuses(stack, bonuses);
            }
        }
        // Read directly: Apothic's CurioEquipmentSlot#getStacks iterates one index past the end, which throws.
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> curios.getCurios().forEach((curioType, handler) -> {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty() && isCurioSlotOf(LootCategory.forItem(stack), curioType)) {
                    addBonuses(stack, bonuses);
                }
            }
        }));
        return bonuses;
    }

    private static boolean isCurioSlotOf(LootCategory category, String curioType) {
        return category.getSlots().slots().stream()
                .anyMatch(slot -> slot.value() instanceof CurioEquipmentSlot curio && curio.curioType().equals(curioType));
    }

    private static void addBonuses(ItemStack stack, Map<String, Integer> bonuses) {
        AffixHelper.streamAffixes(stack).forEach(inst -> {
            if (inst.getAffix() instanceof AptitudeBonusAffix affix) {
                int bonus = affix.bonus(inst);
                if (bonus != 0) bonuses.merge(affix.aptitude(), bonus, Integer::sum);
            }
        });
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPayload(appliedOn(player)));
        // Sent second: the client clamps each synced aptitude to the level cap plus the bonus it already knows.
        SyncAptitudeCapabilityCP.send(player);
    }

    private static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC,
                (payload, ctx) -> localPlayerBonuses = payload.bonuses());
    }

    public record SyncPayload(Map<String, Integer> bonuses) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(RavenApoth.loc("gear_aptitude_bonuses"));
        public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = ByteBufCodecs
                .<ByteBuf, String, Integer, Map<String, Integer>>map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
                .map(SyncPayload::new, SyncPayload::bonuses);

        @Override
        public Type<SyncPayload> type() {
            return TYPE;
        }
    }
}
