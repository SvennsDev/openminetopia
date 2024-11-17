package nl.openminetopia.modules.staff.mod.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.api.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLevelChangeEvent;

@CommandAlias("mod")
public class ModSetLevelCommand extends BaseCommand {

    @Subcommand("setlevel")
    @Syntax("<player> <level>")
    @CommandPermission("openminetopia.mod.setlevel")
    @CommandCompletion("@players @range:1-100")
    @Description("Set the level of a player.")
    public void level(Player player, OfflinePlayer offlinePlayer, int newLevel) {
        if (offlinePlayer.getPlayer() == null) {
            player.sendMessage("This player does not exist.");
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayerAsync(offlinePlayer.getPlayer(), minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;
            int oldLevel = minetopiaPlayer.getLevel();
            minetopiaPlayer.setLevel(newLevel);

            player.sendMessage("Set the level of the player to " + newLevel + ".");

            Bukkit.getServer().getPluginManager().callEvent(new PlayerLevelChangeEvent(offlinePlayer.getPlayer(), oldLevel, newLevel));
        }, Throwable::printStackTrace);
    }
}
