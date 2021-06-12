package space.shugen.CyouServer.util;

import arc.ApplicationListener;
import arc.Events;
import arc.util.Align;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static space.shugen.CyouServer.CyouVars.configFiles;

public class Scoreboard implements ApplicationListener {
    Stack<Long> minuteTick = new Stack<>();
    public List<ScoreboardLine> commonLines = new ArrayList<>();
    public List<ScoreboardLine> commonPlayerLines = new ArrayList<>();
    public List<ScoreboardLine> roundLines = new ArrayList<>();
    public List<ScoreboardLine> roundPlayerLines = new ArrayList<>();

    private String welcomeText = "[magenta]欢迎！[goldenrod]%s[magenta]来到树根老家伙服";
    public String format =
            "[magenta]欢迎！[goldenrod]%s[magenta]来到树根老家伙服\n" +
                    "[magenta]服务器TPS: [goldenrod]%.1f\n" +
                    "[magenta]游戏时间：%.1f";
    long lastUpdateTime = System.currentTimeMillis();
    private Logger logger;


    @Override
    public void init() {
        commonLines.add(new ScoreboardLine() {
            public int order = 1;

            @Override
            public String run(Player player) {
                return "";// TODO
            }
        });
        this.logger = Logger.getLogger("CyouScoreboard");
        var folder = configFiles.parent().child("CyouScoreboard");
        folder.mkdirs();
        Events.on(EventType.WorldLoadEvent.class, (e) -> {
            roundLines = new ArrayList<>();
            roundLines.addAll(commonLines);
            roundPlayerLines = new ArrayList<>();
            roundPlayerLines.addAll(commonPlayerLines);
        });

    }

    @Override
    public void update() {
        if (lastUpdateTime - System.currentTimeMillis() > 0.99) {
            lastUpdateTime = System.currentTimeMillis();
            this.showPlayer();
        }
    }

    public void showPlayer() {
        String commonLine = "";

        commonLine = String.join("\n", roundLines.parallelStream().map((line) -> {
            try {

            } catch (Exception e) {

            }

            return line.run(null);
        }).collect(Collectors.toList()));
        Groups.player.forEach((e) -> {
            if (e.con == null) {
                return;
            }
            String playerLine = "";

            if (e.con.mobile) {
                Call.infoPopup(
                        e.con,
                        playerLine,
                        1f,
                        Align.topLeft,
                        210,
                        0,
                        0,
                        0
                );
            } else {
                Call.infoPopup(
                        e.con,
                        playerLine,
                        1f,
                        Align.topLeft,
                        155,
                        0,
                        0,
                        0
                );
            }
        });
    }

    public interface ScoreboardLine {
        public int order = 0;

        public String run(Player player);
    }

    private class ScoreboardLineInstance {
        public int order = 0;
        public String result;
    }
}
