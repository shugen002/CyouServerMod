package space.shugen.CyouServer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

import static space.shugen.CyouServer.CyouVars.configFiles;

public class CyouPlugin {
    public String name = "Unknown";

    public boolean onLoad() {
        return true;
    }

    public boolean onEnable() {
        return false;
    }

    public boolean onDisable() {
        return false;
    }

    public void onUpdate() {}

    public Logger getLogger() {
        return LogManager.getLogger(this.name);
    }

    /***
     * 获取设置
     * @return 当前设置
     */
    public JSONObject getConfig() {
        return new JSONObject();
    }

    /***
     * 获取设置
     * @param config 默认设置
     * @return 当前设置
     */
    public JSONObject getConfig(JSONObject config) {
        var folder = configFiles.parent().child(this.name);
        folder.mkdirs();
        var pluginConfigFile = folder.child("config.json");
        if (!pluginConfigFile.exists()) {
            pluginConfigFile.writeBytes(config.toString(SerializerFeature.PrettyFormat).getBytes(StandardCharsets.UTF_8), false);
            return (JSONObject) config.clone();
        } else {
            try {
                return JSONObject.parseObject(pluginConfigFile.readString());
            } catch (Exception e) {
                this.getLogger().error("Fail to parse exist config, use Default", e);
                return (JSONObject) config.clone();
            }
        }
    }
}
