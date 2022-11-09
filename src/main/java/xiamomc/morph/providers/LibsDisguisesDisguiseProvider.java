package xiamomc.morph.providers;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.MorphManager;
import xiamomc.morph.config.ConfigOption;
import xiamomc.morph.config.MorphConfigManager;
import xiamomc.morph.messages.MessageUtils;
import xiamomc.morph.messages.MorphStrings;
import xiamomc.morph.misc.DisguiseInfo;
import xiamomc.morph.misc.DisguiseState;
import xiamomc.morph.misc.DisguiseTypes;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

public class LibsDisguisesDisguiseProvider extends VanillaDisguiseProvider
{
    private final Bindable<Boolean> allowLD = new Bindable<>(false);

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(allowLD, ConfigOption.ALLOW_LD_DISGUISES);
    }

    @Override
    public @NotNull String getIdentifier()
    {
        return DisguiseTypes.LD.getNameSpace();
    }

    @Override
    public @NotNull DisguiseResult morph(Player player, DisguiseInfo disguiseInfo, @Nullable Entity targetEntity)
    {
        if (!allowLD.get())
        {
            return DisguiseResult.fail();
        }

        var id = disguiseInfo.getIdentifier();

        if (DisguiseTypes.fromId(id) != DisguiseTypes.LD)
            return DisguiseResult.fail();

        Disguise disguise = DisguiseAPI.getCustomDisguise(DisguiseTypes.LD.toStrippedId(id));

        if (disguise == null)
        {
            logger.error("未能找到叫" + id + "的伪装");
            player.sendMessage(MessageUtils.prefixes(player, MorphStrings.parseErrorString().resolve("id", id)));
            return DisguiseResult.fail();
        }

        DisguiseAPI.disguiseEntity(player, disguise);

        return DisguiseResult.success(disguise);
    }

    @Resolved
    private MorphManager morphs;

    @Override
    protected boolean canConstruct(DisguiseInfo info, Entity targetEntity, @Nullable DisguiseState theirState)
    {
        return theirState != null && theirState.getDisguiseIdentifier().equals(info.getIdentifier());
    }

    @Override
    protected boolean canCopyDisguise(DisguiseInfo info, Entity targetEntity,
                                      @Nullable DisguiseState theirState, @NotNull Disguise theirDisguise)
    {
        if (theirState != null)
            return theirState.getDisguiseIdentifier().equals(info.getIdentifier());

        return false;
    }

    @Override
    public boolean unMorph(Player player, DisguiseState state)
    {
        super.unMorph(player, state);

        return false;
    }

    @Override
    public Component getDisplayName(String disguiseIdentifier)
    {
        return Component.text(DisguiseTypes.LD.toStrippedId(disguiseIdentifier));
    }
}
