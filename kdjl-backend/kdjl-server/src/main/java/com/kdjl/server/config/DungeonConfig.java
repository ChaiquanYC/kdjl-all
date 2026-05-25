package com.kdjl.server.config;

import java.util.*;

/**
 * Dungeon configuration — matches PHP config.fuben.php.
 * Each dungeon: id, name, cooldown(seconds), level requirement, monster IDs.
 */
public class DungeonConfig {

    public record DungeonInfo(
        String key, int id, String name, int cooldown, int level, String desc,
        List<Integer> monsterIds
    ) {}

    public static final List<DungeonInfo> DUNGEONS = List.of(
        new DungeonInfo("ysw", 11, "伊苏王的神墓", 86400, 30,
            "远古伊苏王的陵墓，机关重重，宝藏无数",
            range(1530, 1551)),
        new DungeonInfo("hlw", 12, "火龙王的宫殿", 36000, 50,
            "火龙王的巢穴，烈焰与熔岩的试炼",
            range(158, 187)),
        new DungeonInfo("hhd", 151, "辉煌的大道", 86400, 30,
            "通往辉煌之路，勇士的必经之途",
            range(1417, 1428)),
        new DungeonInfo("sfk", 13, "史芬克斯密穴", 43200, 70,
            "神秘狮身人面兽的隐藏巢穴",
            range(188, 214)),
        new DungeonInfo("llc", 14, "玲珑城", 54000, 85,
            "精巧机关之城，暗藏无数玄机",
            List.of(263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,289,290,291,292)),
        new DungeonInfo("sy", 50, "厄非斯深渊", 54000, 90,
            "无尽的深渊，黑暗中的恐怖生物",
            range(429, 455)),
        new DungeonInfo("ml", 124, "阿尔提密林", 60000, 1,
            "神秘的远古密林，精灵的家园",
            range(505, 513)),
        new DungeonInfo("fl", 127, "菲拉苛地域", 72000, 1,
            "灼热地狱，菲拉苛的领地",
            List.of(774,775,776,777,778,779,780,781,782,783,784,785,786,787,789,790)),
        new DungeonInfo("ry", 143, "熔岩地宫", 72000, 1,
            "地心熔岩宫殿，岩浆与火焰的考验",
            range(1145, 1159)),
        new DungeonInfo("hm", 144, "幻魔之境", 72000, 1,
            "幻影重重的魔境，精神与意志的试炼",
            range(1160, 1184))
    );

    private static List<Integer> range(int from, int to) {
        List<Integer> list = new ArrayList<>();
        for (int i = from; i <= to; i++) list.add(i);
        return list;
    }

    public static Optional<DungeonInfo> getById(int mapId) {
        return DUNGEONS.stream().filter(d -> d.id() == mapId).findFirst();
    }
}
