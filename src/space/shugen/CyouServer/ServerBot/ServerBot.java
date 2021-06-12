package space.shugen.CyouServer.ServerBot;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.Logger;
import space.shugen.CyouServer.CyouPlugin;
import space.shugen.KHBot.KHBot;

import java.util.concurrent.LinkedBlockingDeque;

public class ServerBot extends CyouPlugin {
    protected KHBot bot;
    protected Logger logger;
    protected BotConfig config;
    private ServerUpdate serverupdate;
    volatile LinkedBlockingDeque<Job> jobs = new LinkedBlockingDeque();

    @Override
    public boolean onLoad() {
        logger = getLogger();
        logger.info("ServerBot loaded.");
        this.config = this.getConfig((JSONObject) JSONObject.toJSON(new BotConfig())).toJavaObject(BotConfig.class);
        return true;
    }

    @Override
    public boolean onEnable() {
        if (this.config.enable && this.config.token != null && !this.config.token.isEmpty()) {
            this.bot = new KHBot(this.config.token);
            this.serverupdate = new ServerUpdate(this);
        }

        logger.info("ServerBot Enable.");
        return true;
    }

    @Override
    public boolean onDisable() {
        if(this.serverupdate!=null){
            this.serverupdate.dispose();
        }
        if (this.bot != null) {
            this.bot.dispose();
        }
        logger.info("ServerBot Disabled.");

        return true;
    }

    @Override
    public void onUpdate() {
        while (!jobs.isEmpty()) {
            var job = jobs.poll();
            try {
                job.run();
            } catch (Exception e) {
                logger.error("Fail ", e);
            }
        }
    }

    interface Job {
        void run();
    }
}
