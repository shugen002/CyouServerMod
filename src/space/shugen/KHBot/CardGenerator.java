package space.shugen.KHBot;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.HashMap;

public class CardGenerator {
    private static HashMap<String, String> resourceMap;

    static {
        resourceMap = new HashMap<>();
        resourceMap.put("copper", "3331796095471506/98e7f903f7a93600w00w");
        resourceMap.put("lead", "3331796095471506/8c2c778a26146b00w00w");
        resourceMap.put("graphite", "3331796095471506/63f4470f3fe65600w00w");
        resourceMap.put("metaglass", "3331796095471506/bc5609ffc3382300w00w");
        resourceMap.put("titanium", "3331796095471506/53b25954dd703d00w00w");
        resourceMap.put("thorium", "3331796095471506/c6dd8ffc8ed59c00w00w");
        resourceMap.put("silicon", "3331796095471506/30a86ca778370f00w00w");
        resourceMap.put("plastanium", "3331796095471506/afdf4fd7d1c0d500w00w");
        resourceMap.put("surge-alloy", "3331796095471506/f846a82e2c99ed00w00w");
        resourceMap.put("phase-fabric", "3331796095471506/0509eb885ccebc00w00w");
    }

    private class Text {
        public String content;
        public String type = "plain-text";

        Text(String content) {
            this.content = content;
        }

        Text(String content, String type) {
            this.content = content;
            this.type = type;
        }
    }

    private class Header {
        public String type = "header";
        public Text text;

        Header(String content) {
            this.text = new Text(content);
        }
    }

    private class Image {
        public String type = "image";
        public String src;
        public String size = "lg";
        public Boolean circle = false;

        Image(String src) {
            this.src = src;
        }

        Image(String src, String size) {
            this.src = src;
            this.size = size;
        }
    }

    private class ResourceImage {
        public String type = "image";
        public String src;
        public String content;

        ResourceImage(String itemType) {
            if (resourceMap.containsKey(itemType)) {
                this.type = "image";
                this.src = resourceMap.get(itemType);
            } else {
                this.type = "plain-text";
                this.content = itemType;
            }
        }
    }

    private class Section {
        public String type = "section";
        public Text text;
        public Object accessory = null;
        public String mode = "left";

        Section(String content) {
            this.text = new Text(content);
        }

        Section(String content, String type) {
            this.text = new Text(content, type);
        }
    }

    private class ImageGroup {
        public String type = "image-group";
        public JSONArray elements = new JSONArray();

        ImageGroup() {}

        ImageGroup(String url) {
            this.elements.add(new Image(url));
        }

    }


    public class Context {
        public String type = "context";
        public JSONArray elements = new JSONArray();
    }

    public static class File {
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
    }
}
