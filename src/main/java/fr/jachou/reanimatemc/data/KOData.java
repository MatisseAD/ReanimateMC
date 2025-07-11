package fr.jachou.reanimatemc.data;


import org.bukkit.entity.ArmorStand;

public class KOData {
    private boolean isKo;
    private int taskId;
    private boolean crawling;
    private int barTaskId;
    private int suicideTaskId = -1;
    private ArmorStand mount;
    private ArmorStand label;
    private ArmorStand helpMarker;
    private long endTimestamp;
    private double originalJumpStrength = 0.0;
    private String originalListName;

    public boolean isKo() {
        return isKo;
    }

    public void setBarTaskId(int barTaskId) {
        this.barTaskId = barTaskId;
    }

    public int getBarTaskId() {
        return barTaskId;
    }

    public ArmorStand getLabel() {
        return label;
    }

    public void setLabel(ArmorStand label) {
        this.label = label;
    }

    public ArmorStand getHelpMarker() {
        return helpMarker;
    }

    public void setHelpMarker(ArmorStand helpMarker) {
        this.helpMarker = helpMarker;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public double getOriginalJumpStrength() {
        return originalJumpStrength;
    }

    public void setOriginalJumpStrength(double originalJumpStrength) {
        this.originalJumpStrength = originalJumpStrength;
    }

    public int getSuicideTaskId() {
        return suicideTaskId;
    }

    public void setSuicideTaskId(int suicideTaskId) {
        this.suicideTaskId = suicideTaskId;
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

    public String getOriginalListName() {
        return originalListName;
    }

    public void setOriginalListName(String originalListName) {
        this.originalListName = originalListName;
    }
}


