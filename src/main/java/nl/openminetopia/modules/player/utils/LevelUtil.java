package nl.openminetopia.modules.player.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.fitness.Fitness;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.LevelCheckConfiguration;
import nl.openminetopia.modules.banking.BankingModule;
import nl.openminetopia.modules.banking.models.BankAccountModel;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.entity.Player;

@UtilityClass
public class LevelUtil {

    public static int calculateLevel(MinetopiaPlayer minetopiaPlayer) {
        LevelCheckConfiguration configuration = OpenMinetopia.getLevelcheckConfiguration();
        double points = 0;

        // TODO: Add points per vehicle

        // Points per 5k balance
        BankAccountModel accountModel = OpenMinetopia.getModuleManager().getModule(BankingModule.class).getAccountById(minetopiaPlayer.getUuid());

        double balance;
        if (accountModel == null) {
            balance = 0;
        } else {
            balance = accountModel.getBalance();
        }

        if (balance >= 5000) {
            for (double tempBalance = balance; tempBalance >= 5000; tempBalance -= 5000) {
                points += configuration.getPointsPer5KBalance();
            }
        }

        // Points for having a prefix
        if (minetopiaPlayer.getPrefixes() != null && !minetopiaPlayer.getPrefixes().isEmpty()) {
            points += configuration.getPointsForPrefix();
        }

        // Points per 20 fitness
        Fitness fitness = minetopiaPlayer.getFitness();
        if (fitness == null) return 0;
        for (int i = fitness.getTotalFitness(); i >= 20; i -= 20) {
            points += configuration.getPointsPer20Fitness();
        }

        // Points per 1 hour playtime
        for (int playtime = minetopiaPlayer.getPlaytime(); playtime >= 3600; playtime -= 3600) {
            points += configuration.getPointsPerHourPlayed();
        }

        // Points per plot
        Player player = minetopiaPlayer.getBukkit().getPlayer();
        if (player == null) return OpenMinetopia.getDefaultConfiguration().getDefaultLevel();

        for (int plots = WorldGuardUtils.getOwnedRegions(player); plots >= 1; plots--) {
            points += configuration.getPointsPerPlot();
        }
        
        int neededPoints = configuration.getPointsNeededForLevelUp();
        int level = (int) Math.floor(points / neededPoints);

        level = Math.max(OpenMinetopia.getDefaultConfiguration().getDefaultLevel(),
                Math.min(level, OpenMinetopia.getLevelcheckConfiguration().getMaxLevel()));


        return level;
    }
}
