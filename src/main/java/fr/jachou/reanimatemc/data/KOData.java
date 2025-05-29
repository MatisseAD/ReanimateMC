package fr.jachou.reanimatemc.data;


import org.bukkit.entity.ArmorStand;

public class KOData {
    private boolean isKo;
    private int taskId;
    private boolean crawling;
    private int barTaskId;
    private ArmorStand mount;

    public boolean isKo() {
        return isKo;
    }

    public void setBarTaskId(int barTaskId) {
        this.barTaskId = barTaskId;
    }

    public int getBarTaskId() {
        return barTaskId;
    }

    public void setKo(boolean isKo) {
        this.isKo = isKo;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public boolean isCrawling() {
        return crawling;
    }

    public void setCrawling(boolean crawling) {
        this.crawling = crawling;
    }

    public ArmorStand getMount() {
        return mount;
    }

    public void setMount(ArmorStand mount) {
        this.mount = mount;
    }
}


