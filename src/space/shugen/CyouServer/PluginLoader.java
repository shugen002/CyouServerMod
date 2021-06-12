package space.shugen.CyouServer;

import arc.ApplicationListener;
import arc.Core;
import mindustry.mod.Plugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import space.shugen.CyouServer.ServerBot.ServerBot;
import space.shugen.CyouServer.util.Logs;
import space.shugen.CyouServer.util.Scoreboard;

import java.nio.charset.StandardCharsets;

import static space.shugen.CyouServer.CyouVars.*;

@SuppressWarnings("unused")
public class PluginLoader extends Plugin {

    //called when game initializes
    @Override
    public void init() {
        Logger logger = LogManager.getLogger("CyouPM");
        configFiles = getConfig();
        if (!configFiles.exists()) {
            configFiles.writeBytes("{}".getBytes(StandardCharsets.UTF_8), false);
        }
        scoreboard = new Scoreboard();
        scoreboard.init();
        logs = new Logs();
        logs.init();
        serverBot = new ServerBot();
        serverBot.name = "ServerBot";
        serverBot.onLoad();
        serverBot.onEnable();
        Core.app.addListener(scoreboard);
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                logger.info("dispose");
                serverBot.onDisable();
            }

            @Override
            public void exit() {
                logger.info("exit");
            }

            @Override
            public void pause() {
                logger.info("pause");
            }

            @Override
            public void update() {
                try {
                    serverBot.onUpdate();
                } catch (Exception e) {
                    logger.error("Fail Tick with plugin", e);
                }
            }
        });
    }
}