package com.openlinkhub.digitalhuman.runtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "digital-human.intent")
public class IntentRoutingProperties {

    private List<CommandRule> commands = defaultCommands();
    private List<String> dateTodayAliases = Arrays.asList("今天日期", "今天几号", "今天星期几", "现在几点", "当前时间");
    private List<String> weatherTodayAliases = Arrays.asList("今天天气", "天气如何", "今天的天气", "今日天气");
    private List<String> weatherWeeklyAliases = Arrays.asList("未来一周天气", "未来七天天气", "未来7天天气", "一周天气");

    public static IntentRoutingProperties defaults() {
        return new IntentRoutingProperties();
    }

    public List<CommandRule> commands() {
        return commands;
    }

    public void setCommands(List<CommandRule> commands) {
        if (commands != null && !commands.isEmpty()) {
            this.commands = commands;
        }
    }

    public List<String> dateTodayAliases() {
        return dateTodayAliases;
    }

    public void setDateTodayAliases(List<String> dateTodayAliases) {
        if (dateTodayAliases != null && !dateTodayAliases.isEmpty()) {
            this.dateTodayAliases = dateTodayAliases;
        }
    }

    public List<String> weatherTodayAliases() {
        return weatherTodayAliases;
    }

    public void setWeatherTodayAliases(List<String> weatherTodayAliases) {
        if (weatherTodayAliases != null && !weatherTodayAliases.isEmpty()) {
            this.weatherTodayAliases = weatherTodayAliases;
        }
    }

    public List<String> weatherWeeklyAliases() {
        return weatherWeeklyAliases;
    }

    public void setWeatherWeeklyAliases(List<String> weatherWeeklyAliases) {
        if (weatherWeeklyAliases != null && !weatherWeeklyAliases.isEmpty()) {
            this.weatherWeeklyAliases = weatherWeeklyAliases;
        }
    }

    private static List<CommandRule> defaultCommands() {
        return Arrays.asList(
                new CommandRule("move.forward", "向前", Arrays.asList("向前", "请向前", "向前吧", "往前走", "前进", "往前", "前走")),
                new CommandRule("turn.left", "向左", Arrays.asList("向左", "向左转", "左转", "往左", "往左转")),
                new CommandRule("turn.right", "向右", Arrays.asList("向右", "向右转", "右转", "往右", "往右转")),
                new CommandRule("video.open", "打开视频", Arrays.asList("打开视频", "开启视频", "显示视频")),
                new CommandRule("tts.stop", "停止播报", Arrays.asList("停", "停止", "别说了", "不用说了", "不用读了", "不要说了", "停止播报", "停止朗读", "stop", "Stop", "STOP")),
                new CommandRule("music.stop", "停止播放", Arrays.asList("停止播放", "暂停播放", "关闭音乐", "停止音乐"))
        );
    }

    public static class CommandRule {
        private String name;
        private String label;
        private List<String> aliases;

        public CommandRule() {
        }

        public CommandRule(String name, String label, List<String> aliases) {
            this.name = name;
            this.label = label;
            this.aliases = aliases;
        }

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String label() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<String> aliases() {
            return aliases;
        }

        public void setAliases(List<String> aliases) {
            this.aliases = aliases;
        }
    }
}
