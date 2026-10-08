package com.citytour.game.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Board {
    private static final List<BoardSpace> SPACES = buildSpaces();

    private Board() {
    }

    public static List<BoardSpace> spaces() {
        return SPACES;
    }

    public static BoardSpace space(int index) {
        return SPACES.get(Math.floorMod(index, SPACES.size()));
    }

    public static List<BoardSpace> colorGroup(String group) {
        if (group == null || group.isBlank()) {
            return List.of();
        }
        return SPACES.stream().filter(space -> group.equals(space.colorGroup())).toList();
    }

    private static List<BoardSpace> buildSpaces() {
        List<BoardSpace> spaces = new ArrayList<>();
        spaces.add(space(0, "出发", SpaceType.GO, null, null, 0, 0, List.of(), 0, "经过或抵达可获得 200 旅行币"));
        spaces.add(property(1, "喀什古城", "棕", "#8b5e3c", 60, 50, 2, 10, 30, 90, 160, 250));
        spaces.add(special(2, "命运", SpaceType.FATE, "抽取一张命运卡"));
        spaces.add(property(3, "大理古城", "棕", "#8b5e3c", 60, 50, 4, 20, 60, 180, 320, 450));
        spaces.add(space(4, "城市税", SpaceType.TAX, null, null, 0, 0, List.of(), 200, "缴纳 200 旅行币"));
        spaces.add(railroad(5, "北京站"));
        spaces.add(property(6, "丽江古城", "浅蓝", "#8fd3e8", 100, 50, 6, 30, 90, 270, 400, 550));
        spaces.add(special(7, "机会", SpaceType.CHANCE, "抽取一张机会卡"));
        spaces.add(property(8, "桂林山水", "浅蓝", "#8fd3e8", 100, 50, 6, 30, 90, 270, 400, 550));
        spaces.add(property(9, "张家界", "浅蓝", "#8fd3e8", 120, 50, 8, 40, 100, 300, 450, 600));
        spaces.add(space(10, "城市监狱 / 探访", SpaceType.JAIL, null, null, 0, 0, List.of(), 0, "探访不收取费用"));
        spaces.add(property(11, "厦门鼓浪屿", "粉", "#d85b9a", 140, 100, 10, 50, 150, 450, 625, 750));
        spaces.add(space(12, "国家电网", SpaceType.UTILITY, "设施", "#7d8ca3", 150, 0, List.of(4, 10), 0, "租金为骰子点数 × 4 或 × 10"));
        spaces.add(property(13, "青岛栈桥", "粉", "#d85b9a", 140, 100, 10, 50, 150, 450, 625, 750));
        spaces.add(property(14, "哈尔滨中央大街", "粉", "#d85b9a", 160, 100, 12, 60, 180, 500, 700, 900));
        spaces.add(railroad(15, "上海站"));
        spaces.add(property(16, "西安城墙", "橙", "#df8a38", 180, 100, 14, 70, 200, 550, 750, 950));
        spaces.add(special(17, "命运", SpaceType.FATE, "抽取一张命运卡"));
        spaces.add(property(18, "成都锦里", "橙", "#df8a38", 180, 100, 14, 70, 200, 550, 750, 950));
        spaces.add(property(19, "重庆洪崖洞", "橙", "#df8a38", 200, 100, 16, 80, 220, 600, 800, 1000));
        spaces.add(space(20, "免费停车", SpaceType.FREE_PARKING, null, null, 0, 0, List.of(), 0, "休息一回合"));
        spaces.add(property(21, "杭州西湖", "红", "#d84b4b", 220, 150, 18, 90, 250, 700, 875, 1050));
        spaces.add(special(22, "机会", SpaceType.CHANCE, "抽取一张机会卡"));
        spaces.add(property(23, "苏州园林", "红", "#d84b4b", 220, 150, 18, 90, 250, 700, 875, 1050));
        spaces.add(property(24, "南京夫子庙", "红", "#d84b4b", 240, 150, 20, 100, 300, 750, 925, 1100));
        spaces.add(railroad(25, "广州站"));
        spaces.add(property(26, "长沙橘子洲", "黄", "#e2c84d", 260, 150, 22, 110, 330, 800, 975, 1150));
        spaces.add(property(27, "武汉黄鹤楼", "黄", "#e2c84d", 260, 150, 22, 110, 330, 800, 975, 1150));
        spaces.add(space(28, "城市水务", SpaceType.UTILITY, "设施", "#7d8ca3", 150, 0, List.of(4, 10), 0, "租金为骰子点数 × 4 或 × 10"));
        spaces.add(property(29, "郑州黄河", "黄", "#e2c84d", 280, 150, 24, 120, 360, 850, 1025, 1200));
        spaces.add(space(30, "前往城市监狱", SpaceType.GO_TO_JAIL, null, null, 0, 0, List.of(), 0, "直接进入城市监狱"));
        spaces.add(property(31, "北京胡同", "绿", "#4f9b65", 300, 200, 26, 130, 390, 900, 1100, 1275));
        spaces.add(property(32, "天津五大道", "绿", "#4f9b65", 300, 200, 26, 130, 390, 900, 1100, 1275));
        spaces.add(special(33, "命运", SpaceType.FATE, "抽取一张命运卡"));
        spaces.add(property(34, "大连星海", "绿", "#4f9b65", 320, 200, 28, 150, 450, 1000, 1200, 1400));
        spaces.add(railroad(35, "成都站"));
        spaces.add(special(36, "机会", SpaceType.CHANCE, "抽取一张机会卡"));
        spaces.add(property(37, "上海外滩", "深蓝", "#335c99", 350, 200, 35, 175, 500, 1100, 1300, 1500));
        spaces.add(space(38, "城市维护税", SpaceType.TAX, null, null, 0, 0, List.of(), 100, "缴纳 100 旅行币"));
        spaces.add(property(39, "香港维港", "深蓝", "#335c99", 400, 200, 50, 200, 600, 1400, 1700, 2000));
        return Collections.unmodifiableList(spaces);
    }

    private static BoardSpace property(int index, String name, String group, String color, int price, int houseCost,
                                       int... rents) {
        List<Integer> rentTable = new ArrayList<>();
        for (int rent : rents) {
            rentTable.add(rent);
        }
        return new BoardSpace(index, name, SpaceType.PROPERTY, group, color, price, houseCost,
                List.copyOf(rentTable), 0, "地产");
    }

    private static BoardSpace railroad(int index, String name) {
        return new BoardSpace(index, name, SpaceType.RAILROAD, "交通", "#6b7280", 200, 0,
                List.of(25, 50, 100, 200), 0, "租金根据拥有车站数量计算");
    }

    private static BoardSpace special(int index, String name, SpaceType type, String description) {
        return new BoardSpace(index, name, type, null, null, 0, 0, List.of(), 0, description);
    }

    private static BoardSpace space(int index, String name, SpaceType type, String group, String color,
                                    int price, int houseCost, List<Integer> rents, int tax, String description) {
        return new BoardSpace(index, name, type, group, color, price, houseCost, List.copyOf(rents), tax, description);
    }
}
