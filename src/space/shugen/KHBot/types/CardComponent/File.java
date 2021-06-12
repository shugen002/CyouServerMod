package space.shugen.KHBot.types.CardComponent;

import com.alibaba.fastjson.annotation.JSONField;

public class File {
    @JSONField(name = "type", defaultValue = "file")
    public String type = "file";
    @JSONField(name = "title", defaultValue = "")
    public String title = "";
    @JSONField(name = "src")
    public String src;

    File(String src) {
        this.src = src;
    }

    File(String name, String src) {
        this.src = src;
        this.title = name;
    }

    public File() {
    }
}
