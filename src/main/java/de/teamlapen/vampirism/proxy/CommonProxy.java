package de.teamlapen.vampirism.proxy;


import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.TaskFinishedPacket;
import de.teamlapen.vampirism.player.skills.SkillTree;
import de.teamlapen.vampirism.player.skills.SkillTreeManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;

/**
 * Abstract proxy base for both client and server.
 * Try to keep this quite empty and move larger code parts into dedicated classes.
 *
 * @author Maxanier
 */
public abstract class CommonProxy implements IProxy {

    @Override
    public SkillTree getSkillTree(boolean client) {
        return SkillTreeManager.getInstance().getSkillTree();
    }

    @Override
    public void onInitStep(Step step, ModLifecycleEvent event) {
    }

    @Override
    public void handleTaskFinishedPacket(TaskFinishedPacket msg, PlayerEntity playerEntity) {
        FactionPlayerHandler.getOpt(playerEntity).ifPresent(factionPlayerHandler -> factionPlayerHandler.getCurrentFactionPlayer().ifPresent(sd -> {
            sd.getTaskManager().addCompletedTask(msg.task);
            sd.getTaskManager().removeRequirements(msg.task);
        }));
    }
}
