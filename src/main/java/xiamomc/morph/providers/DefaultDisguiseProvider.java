package xiamomc.morph.providers;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.ArmorStandWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.abilities.AbilityHandler;
import xiamomc.morph.messages.MorphStrings;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.morph.misc.DisguiseTypes;
import xiamomc.morph.misc.DisguiseUtils;
import xiamomc.morph.skills.MorphSkillHandler;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.ColorUtils;

import java.util.List;

/**
 * 提供一个默认的DisguiseProvider
 * 包括自动设置Bossbar、飞行技能和应用针对LibsDisguises的一些workaround
 */
public abstract class DefaultDisguiseProvider extends DisguiseProvider
{
    @Override
    public boolean unMorph(Player player, DisguiseState state)
    {
        super.unMorph(player, state);

        state.getAbilities().forEach(a -> a.revokeFromPlayer(player, state));
        state.setAbilities(List.of());

        return true;
    }

    @Resolved
    private MorphSkillHandler skillHandler;

    @Resolved
    private AbilityHandler abilityHandler;

    @Resolved
    private Scoreboard scoreboard;

    @Override
    public boolean updateDisguise(Player player, DisguiseState state)
    {
        var disguise = state.getDisguise();
        var watcher = disguise.getWatcher();

        //更新actionbar信息
        var msg = skillHandler.hasSkill(state.getSkillIdentifier())
                ? (state.getSkillCooldown() <= 0
                    ? MorphStrings.disguisingWithSkillAvaliableString()
                    : MorphStrings.disguisingWithSkillPreparingString())
                : MorphStrings.disguisingAsString();

        player.sendActionBar(msg.resolve("what", state.getDisplayName()).toComponent());

        //发光颜色
        var team = scoreboard.getPlayerTeam(player);
        var playerColor = (team == null || !team.hasColor()) ? NamedTextColor.WHITE : team.color();

        if (state.getDisguiseType() != DisguiseTypes.LD)
        {
            //workaround: 复制实体伪装时会一并复制隐身标签
            //            会导致复制出来的伪装永久隐身
            watcher.setInvisible(player.isInvisible());

            //workaround: 伪装不会主动检测玩家有没有发光
            watcher.setGlowing(player.isGlowing());

            //设置发光颜色
            if (!state.haveCustomGlowColor())
                disguise.getWatcher().setGlowColor(ColorUtils.toChatColor(playerColor));

            //设置滑翔状态
            watcher.setFlyingWithElytra(player.isGliding());

            //workaround: 复制出来的伪装会忽略玩家Pose
            if (state.shouldHandlePose())
                watcher.setEntityPose(DisguiseUtils.toEntityPose(player.getPose()));
        }

        return true;
    }

    @Override
    public void postConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
        var watcher = state.getDisguise().getWatcher();
        var disguiseTypeLD = state.getDisguise().getType();

        var disguise = state.getDisguise();

        //workaround: 伪装已死亡的LivingEntity
        if (targetEntity instanceof LivingEntity living && living.getHealth() <= 0)
            ((LivingWatcher) watcher).setHealth(1);

        if (!DisguiseAPI.isDisguised(targetEntity))
        {
            //workaround: LibsDisguises在为实体构建伪装时会把主手的物品复制到副手上，不管目标实体副手拿着什么东西。
            //            复制的伪装暂时不用处理。
            ItemStack offhandItemStack = null;

            if (targetEntity instanceof Player targetPlayer)
                offhandItemStack = targetPlayer.getInventory().getItemInOffHand();

            if (targetEntity instanceof ArmorStand armorStand)
                offhandItemStack = armorStand.getItem(EquipmentSlot.OFF_HAND);

            if (offhandItemStack != null) watcher.setItemInOffHand(offhandItemStack);

            //盔甲架加上手臂
            if (disguise.getType().equals(DisguiseType.ARMOR_STAND))
                ((ArmorStandWatcher) watcher).setShowArms(true);
        }
        else if (state.shouldHandlePose() && targetEntity instanceof Player targetPlayer)
        {
            //如果目标实体是玩家，并且此玩家的伪装类型和我们的一样，那么复制他们的装备
            var theirState = getMorphManager().getDisguiseStateFor(targetPlayer);

            Disguise theirDisguise;

            //优先从DisguiseState判断是否为同类
            if (theirState != null)
            {
                theirDisguise = theirState.getDisguise();

                if (theirState.getDisguiseIdentifier().equals(state.getDisguiseIdentifier()))
                    DisguiseUtils.tryCopyArmorStack(targetPlayer, disguise.getWatcher(), theirDisguise.getWatcher());
            }
            else
            {
                theirDisguise = DisguiseAPI.getDisguise(targetPlayer);

                //不是玩家伪装，判断类型是否一样
                //否则，判断双方伪装的名称是否一致
                if (disguiseTypeLD != DisguiseType.PLAYER)
                {
                    if (disguiseTypeLD.equals(theirDisguise.getType()))
                        DisguiseUtils.tryCopyArmorStack(targetPlayer, disguise.getWatcher(), theirDisguise.getWatcher());
                }
                else if (theirDisguise instanceof PlayerDisguise theirPlayerDisguise
                            && disguise instanceof PlayerDisguise ourPlayerDisguise
                            && theirPlayerDisguise.getName().equals(ourPlayerDisguise.getName()))
                {
                    DisguiseUtils.tryCopyArmorStack(targetPlayer, disguise.getWatcher(), theirDisguise.getWatcher());
                }
            }
        }

        //被动技能
        var abilities = abilityHandler.getAbilitiesFor(state.getSkillIdentifier());
        state.setAbilities(abilities);

        if (abilities != null)
            abilities.forEach(a -> a.applyToPlayer(state.getPlayer(), state));

        //发光颜色
        ChatColor glowColor = null;
        Entity teamTargetEntity = targetEntity;
        var disguiseID = state.getDisguiseIdentifier();
        var morphDisguiseType = DisguiseTypes.fromId(disguiseID);

        switch (morphDisguiseType)
        {
            //LD伪装为玩家时会自动复制他们当前的队伍到名字里
            //为了确保一致，除非targetEntity不是null，不然尝试将teamTargetEntity设置为目标玩家
            case PLAYER ->
            {
                if (teamTargetEntity == null)
                    teamTargetEntity = Bukkit.getPlayer(DisguiseTypes.PLAYER.toStrippedId(disguiseID));
            }

            //LD的伪装直接从伪装flag里获取发光颜色
            //只要和LD直接打交道事情就变得玄学了起来..
            case LD ->
            {
                glowColor = watcher.getGlowColor();
                teamTargetEntity = null;
            }
        }

        //从teamTargetEntity获取发光颜色
        if (teamTargetEntity != null)
        {
            var team = scoreboard.getEntityTeam(teamTargetEntity);

            //如果伪装是复制来的，并且目标实体有伪装，则将颜色设置为他们伪装的发光颜色
            if (state.shouldHandlePose() && DisguiseAPI.isDisguised(teamTargetEntity))
                glowColor = DisguiseAPI.getDisguise(teamTargetEntity).getWatcher().getGlowColor();
            else
                glowColor = team == null ? null : ColorUtils.toChatColor(team.color()); //否则，尝试设置成我们自己的
        }

        //设置发光颜色
        state.setCustomGlowColor(ColorUtils.fromChatColor(glowColor));
        watcher.setGlowColor(glowColor);
    }
}
