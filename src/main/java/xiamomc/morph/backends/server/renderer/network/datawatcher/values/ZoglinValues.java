package xiamomc.morph.backends.server.renderer.network.datawatcher.values;

public class ZoglinValues extends MonsterValues
{
    public final SingleValue<Boolean> IS_BABY = getSingle(false);

    public ZoglinValues()
    {
        super();

        registerSingle(IS_BABY);
    }
}
