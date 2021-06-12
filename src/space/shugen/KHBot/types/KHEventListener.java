package space.shugen.KHBot.types;

import com.alibaba.fastjson.JSONObject;

public interface KHEventListener {
    void run(JSONObject event);
}
