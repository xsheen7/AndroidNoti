package com.word.block.puzzle.free.relax.helper.notify;

public class MsgInfo {
    public int id;//推送消息ID
    public String title;//推送消息标题
    public String content;//推送消息内容
    public boolean isNoon;//是否是中午推送
    public boolean isNight;//是否是晚上推送

    /**
     * 构造函数
     * @param id 推送消息ID
     * @param title 推送消息标题
     * @param content 推送消息内容
     * @param isNoon 是否是中午推送(isNoon和isNight都为false则为早上推送)
     * @param isNight 是否是晚上推送(isNoon和isNight都为false则为早上推送)
     */
    public MsgInfo(int id, String title, String content, boolean isNoon, boolean isNight) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.isNoon = isNoon;
        this.isNight = isNight;
    }

    public MsgInfo(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
