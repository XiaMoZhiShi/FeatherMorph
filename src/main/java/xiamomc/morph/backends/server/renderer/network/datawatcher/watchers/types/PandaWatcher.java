package xiamomc.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Player;
import xiamomc.morph.backends.server.renderer.network.registries.ValueIndex;

import java.util.Arrays;
import java.util.Random;

public class PandaWatcher extends LivingEntityWatcher
{
    public PandaWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.PANDA);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.PANDA);
    }

    public Panda.Gene getMainGene()
    {
        return Arrays.stream(Panda.Gene.values()).toList().get(get(ValueIndex.PANDA.MAIN_GENE));
    }

    public Panda.Gene getHiddenGene()
    {
        return Arrays.stream(Panda.Gene.values()).toList().get(get(ValueIndex.PANDA.HIDDEN_GENE));
    }

    @Override
    protected void initValues()
    {
        super.initValues();

        var availableValues = Arrays.stream(Panda.Gene.values()).toList();
        var random = new Random();
        var mainGene = availableValues.get(random.nextInt(availableValues.size()));
        var hiddenGene = availableValues.get(random.nextInt(availableValues.size()));

        write(ValueIndex.PANDA.MAIN_GENE, (byte)mainGene.ordinal());
        write(ValueIndex.PANDA.HIDDEN_GENE, (byte)hiddenGene.ordinal());
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("MainGene"))
            write(ValueIndex.PANDA.MAIN_GENE, (byte)getGeneFromName(nbt.getString("MainGene")).ordinal());

        if (nbt.contains("HiddenGene"))
            write(ValueIndex.PANDA.HIDDEN_GENE, (byte)getGeneFromName(nbt.getString("HiddenGene")).ordinal());
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        var mainGene = this.getMainGene();
        var hiddenGene = this.getHiddenGene();

        nbt.putString("MainGene", mainGene.toString().toLowerCase());
        nbt.putString("HiddenGene", hiddenGene.toString().toLowerCase());
    }

    private Panda.Gene getGeneFromName(String name)
    {
        var gene = Panda.Gene.values();
        var match = Arrays.stream(gene).filter(g -> g.name().equalsIgnoreCase(name))
                .findFirst().orElse(null);

        if (match == null)
        {
            logger.warn("Null Gene for name " + name + "?!");
            match = Panda.Gene.NORMAL;
        }

        return match;
    }
}
