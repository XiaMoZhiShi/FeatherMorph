package xiamomc.morph.providers;

import org.jetbrains.annotations.Nullable;
import xiamomc.morph.backends.DisguiseWrapper;

/**
 * @param wrapperInstance Wrapper实例，在失败时为空
 * @param success 操作是否成功
 * @param isCopy 此伪装是否克隆自其他实体或其他玩家的伪装
 */
public record DisguiseResult(@Nullable DisguiseWrapper<?> wrapperInstance, boolean success, boolean isCopy, boolean failedCollisionCheck)
{
    public static final DisguiseResult FAIL = new DisguiseResult(null, false, false, false);
    public static final DisguiseResult FAILED_COLLISION = new DisguiseResult(null, false, false, true);

    public static DisguiseResult fail()
    {
        return DisguiseResult.FAIL;
    }

    public static DisguiseResult success(DisguiseWrapper<?> disguise, boolean isCopy)
    {
        return new DisguiseResult(disguise, true, isCopy, false);
    }

    public static DisguiseResult success(DisguiseWrapper<?> disguise)
    {
        return new DisguiseResult(disguise, true, false, false);
    }
}
