package xiamomc.morph.backends.server.renderer.network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.GameType;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.Debug;
import xiamomc.morph.MorphPluginObject;
import xiamomc.morph.backends.server.renderer.network.datawatcher.values.AbstractValues;
import xiamomc.morph.backends.server.renderer.network.datawatcher.values.SingleValue;
import xiamomc.morph.backends.server.renderer.network.datawatcher.watchers.SingleWatcher;
import xiamomc.morph.backends.server.renderer.network.registries.EntryIndex;
import xiamomc.morph.backends.server.renderer.utilties.ProtocolRegistryUtils;
import xiamomc.morph.config.ConfigOption;
import xiamomc.morph.config.MorphConfigManager;
import xiamomc.morph.misc.DisguiseEquipment;
import xiamomc.morph.misc.MorphGameProfile;
import xiamomc.morph.misc.NmsRecord;
import xiamomc.morph.utilities.EntityTypeUtils;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.*;

public class PacketFactory extends MorphPluginObject
{
    public static final String MORPH_PACKET_METAKEY = "fm";

    private final Bindable<String> randomBase = new Bindable<>("Stateof");

    @Initializer
    private void load(MorphConfigManager config)
    {
        config.bind(randomBase, ConfigOption.UUID_RANDOM_BASE);
    }

    public List<PacketContainer> buildSpawnPackets(Player player, DisplayParameters parameters)
    {
        List<PacketContainer> packets = new ObjectArrayList<>();

        //logger.info("Build spawn packets, player is " + player.getName() + " :: parameters are " + parameters);

        var disguiseType = parameters.getEntityType();
        var nmsType = EntityTypeUtils.getNmsType(disguiseType);
        if (nmsType == null)
        {
            logger.error("No NMS Type for Bukkit Type '%s'".formatted(disguiseType));
            logger.error("Not build spawn packets!");

            //addSchedule(() -> registry.unregister(player));
            return packets;
        }

        var nmsPlayer = NmsRecord.ofPlayer(player);
        UUID spawnUUID = player.getUniqueId();

        //如果是玩家
        if (disguiseType == org.bukkit.entity.EntityType.PLAYER)
        {
            //logger.info("Building player info packet!");

            var parametersProfile = parameters.getProfile();
            Objects.requireNonNull(parametersProfile, "Null game profile!");
            var gameProfile = new MorphGameProfile(parametersProfile);

            if (!parameters.dontRandomProfileUUID())
            {
                //todo: Get random UUID from world to prevent duplicate UUID
                //玩家在客户端的UUID会根据其GameProfile中的UUID设定，我们需要避免伪装的UUID和某一玩家自己的UUID冲突
                var str = randomBase.get() + player.getName();
                gameProfile.setUUID(UUID.nameUUIDFromBytes(str.getBytes()));
            }

            var lastUUID = parameters.getWatcher().getOrDefault(EntryIndex.TABLIST_UUID, null);

            if (lastUUID != null)
            {
                gameProfile.setUUID(lastUUID);

                var packetTabRemove = new ClientboundPlayerInfoRemovePacket(List.of(lastUUID));
                packets.add(PacketContainer.fromPacket(packetTabRemove));
            }

            //Minecraft需要在生成玩家实体前先发送PlayerInfoUpdate消息
            var uuid = gameProfile.getId();

            var profileName =  gameProfile.getName();
            if (profileName.length() > 16)
            {
                logger.warn("Profile name '%s' exceeds the maximum length 16!".formatted(profileName));
                var subStr = profileName.substring(0, 15);
                gameProfile.setName(subStr);
            }

            if (gameProfile.getName().isBlank())
                throw new IllegalArgumentException("GameProfile name is empty!");

            var packetPlayerInfo = new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                    new ClientboundPlayerInfoUpdatePacket.Entry(
                            uuid, gameProfile, false, 114514, GameType.DEFAULT_MODE,
                            Component.literal(":>"), null
                    )
            );

            spawnUUID = uuid;
            packets.add(PacketContainer.fromPacket(packetPlayerInfo));

            parameters.getWatcher().write(EntryIndex.TABLIST_UUID, uuid);
        }

        var pitch = player.getPitch();
        var yaw = player.getYaw();

        if (disguiseType == EntityType.PHANTOM)
            pitch = -player.getPitch();

        if (disguiseType == EntityType.ENDER_DRAGON)
            yaw = 180 + yaw;

        //生成实体
        var packetAdd = new ClientboundAddEntityPacket(
                player.getEntityId(), spawnUUID,
                player.getX(), player.getY(), player.getZ(),
                pitch, yaw,
                nmsType, 0,
                nmsPlayer.getDeltaMovement(),
                nmsPlayer.getYHeadRot()
        );

        var spawnPacket = PacketContainer.fromPacket(packetAdd);

        packets.add(spawnPacket);

        var watcher = parameters.getWatcher();

        //生成装备和Meta
        var displayingFake = watcher.getOrDefault(EntryIndex.DISPLAY_FAKE_EQUIPMENT, false);
        var equip = displayingFake
                ? watcher.getOrDefault(EntryIndex.EQUIPMENT, new DisguiseEquipment())
                : player.getEquipment();

        var equipmentPacket = new ClientboundSetEquipmentPacket(player.getEntityId(),
                ProtocolEquipment.toPairs(equip));

        packets.add(PacketContainer.fromPacket(equipmentPacket));

        if (parameters.includeMeta())
            packets.add(buildFullMetaPacket(player, parameters.getWatcher()));

        if (player.getVehicle() != null)
        {
            var nmsEntity = ((CraftEntity)player.getVehicle()).getHandle();
            packets.add(PacketContainer.fromPacket(new ClientboundSetPassengersPacket(nmsEntity)));
        }

        if (!player.getPassengers().isEmpty())
            packets.add(PacketContainer.fromPacket(new ClientboundSetPassengersPacket(nmsPlayer)));

        var attributes = nmsPlayer.getAttributes().getSyncableAttributes();

        var attributePacket = new ClientboundUpdateAttributesPacket(player.getEntityId(), attributes);
        packets.add(PacketContainer.fromPacket(attributePacket));

        for (PacketContainer packet : packets)
            packet.setMeta(MORPH_PACKET_METAKEY, true);

        return packets;
    }

    /**
     * 从给定的meta包中移除不属于给定AbstractValues的数据
     * @return 剔除后的包
     */
    public PacketContainer removeNonLivingValues(AbstractValues av, PacketContainer originalPacket)
    {
        if (originalPacket.getType() != PacketType.Play.Server.ENTITY_METADATA)
            throw new IllegalArgumentException("Original packet is not a valid metadata packet!");

        var values = av.getValues();
        var modifier = originalPacket.getDataValueCollectionModifier();

        //获取原Meta包中的数据
        var wrappedData = modifier.read(0);

        //剔除不属于给定AbstractValues中的数据
        wrappedData.removeIf(w ->
        {
            var rawValue = w.getRawValue();

            var match = values.stream().filter(sv ->
                    w.getIndex() == sv.index() && (rawValue == null || rawValue.getClass() == sv.defaultValue().getClass())
            ).findFirst().orElse(null);

            return match == null;
        });

        modifier.write(0, wrappedData);

        return originalPacket;
    }

    public PacketContainer buildDiffMetaPacket(Player player, SingleWatcher watcher)
    {
        var metaPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metaPacket.getIntegers().write(0, player.getEntityId());

        var modifier = metaPacket.getDataValueCollectionModifier();

        List<WrappedDataValue> wrappedDataValues = new ObjectArrayList<>();
        Map<SingleValue<?>, Object> valuesToSent = watcher.getDirty();

        valuesToSent.forEach((single, v) ->
        {
            WrappedDataWatcher.Serializer serializer;

            try
            {
                serializer = ProtocolRegistryUtils.getSerializer(single);
            }
            catch (Throwable t)
            {
                logger.warn("Error occurred while generating meta packet with id '%s': %s".formatted(single.index(), t.getMessage()));
                return;
            }

            var value = new WrappedDataValue(single.index(), serializer, v);
            wrappedDataValues.add(value);
        });

        modifier.write(0, wrappedDataValues);
        metaPacket.setMeta(MORPH_PACKET_METAKEY, true);

        return metaPacket;
    }

    public PacketContainer buildFullMetaPacket(Player player, SingleWatcher watcher)
    {
        watcher.sync();

        var metaPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        metaPacket.getIntegers().write(0, player.getEntityId());

        var modifier = metaPacket.getDataValueCollectionModifier();

        List<WrappedDataValue> wrappedDataValues = new ObjectArrayList<>();

        Map<SingleValue<?>, Object> valuesToSent = watcher.getRegistry();

        valuesToSent.forEach((single, v) ->
        {
            WrappedDataWatcher.Serializer serializer;

            try
            {
                serializer = ProtocolRegistryUtils.getSerializer(single);
            }
            catch (Throwable t)
            {
                logger.warn("Error occurred while generating meta packet with id '%s': %s".formatted(single.index(), t.getMessage()));
                return;
            }

            var value = new WrappedDataValue(single.index(), serializer, v);
            wrappedDataValues.add(value);
        });

        modifier.write(0, wrappedDataValues);
        metaPacket.setMeta(MORPH_PACKET_METAKEY, true);

        return metaPacket;
    }

    public PacketContainer getEquipmentPacket(Player player, SingleWatcher watcher)
    {
        var shouldDisplayFakeEquip = watcher.getOrDefault(EntryIndex.DISPLAY_FAKE_EQUIPMENT, false);
        EntityEquipment equipment = shouldDisplayFakeEquip
                    ? watcher.getOrDefault(EntryIndex.EQUIPMENT, new DisguiseEquipment())
                    : player.getEquipment();

        var rawPacket = new ClientboundSetEquipmentPacket(player.getEntityId(),
                ProtocolEquipment.toPairs(equipment));

        var container = PacketContainer.fromPacket(rawPacket);
        container.setMeta(MORPH_PACKET_METAKEY, true);

        return container;
    }
}
