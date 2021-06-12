package space.shugen.KHBot.types.CardComponent;

import com.alibaba.fastjson.JSONArray;

public class Card {
    public String type = "card";
    public String theme = "secondary";
    public String size = "lg";
    public JSONArray modules = new JSONArray();
}
