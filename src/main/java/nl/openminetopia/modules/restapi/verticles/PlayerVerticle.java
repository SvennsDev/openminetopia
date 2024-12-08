package nl.openminetopia.modules.restapi.verticles;

import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.restapi.base.BaseVerticle;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

public class PlayerVerticle extends BaseVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        router.get("/api/player/:player").handler(this::handleGetPlayer);
    }

    @SuppressWarnings("unchecked")
    private void handleGetPlayer(RoutingContext context) {
        String playerName = context.pathParam("player");
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

        JSONObject jsonObject = new JSONObject();

        if (!player.hasPlayedBefore()) {
            jsonObject.put("success", false);
            context.response().end(jsonObject.toJSONString());
            return;
        }

        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                jsonObject.put("success", false);
            }

            if (minetopiaPlayer == null) {
                jsonObject.put("success", false);
            } else {
                jsonObject.put("success", true);
                jsonObject.put("uuid", player.getUniqueId().toString());
                jsonObject.put("level", minetopiaPlayer.getLevel());
                jsonObject.put("fitness", minetopiaPlayer.getFitness().getTotalFitness());
                jsonObject.put("prefix", minetopiaPlayer.getActivePrefix().getPrefix());
                jsonObject.put("playtimeSeconds", minetopiaPlayer.getPlaytime());
            }
            context.response().end(jsonObject.toJSONString());
        }).join();
    }
}