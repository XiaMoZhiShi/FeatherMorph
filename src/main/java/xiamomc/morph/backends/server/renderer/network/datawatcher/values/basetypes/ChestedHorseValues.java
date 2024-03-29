package xiamomc.morph.backends.server.renderer.network.datawatcher.values.basetypes;

import xiamomc.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

public class ChestedHorseValues extends AbstractHorseValues
{
    public final SingleValue<Boolean> HAS_CHEST = getSingle("chested_horse_has_chest", false);

    public ChestedHorseValues()
    {
        super();

        registerSingle(HAS_CHEST);
    }
}
