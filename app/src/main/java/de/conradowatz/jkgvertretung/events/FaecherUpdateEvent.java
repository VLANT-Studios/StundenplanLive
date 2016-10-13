package de.conradowatz.jkgvertretung.events;

public class FaecherUpdateEvent {

    public static final int TYPE_REMOVED = 1;
    public static final int TYPE_CHANGED = 0;

    private int recyclerIndex = -1;
    private int type = TYPE_CHANGED;
    private boolean removeAbove = false;

    public FaecherUpdateEvent(int type, int recyclerIndex) {
        this.recyclerIndex = recyclerIndex;
        this.type = type;
    }

    public FaecherUpdateEvent() {
        this.type = TYPE_CHANGED;

    }

    public boolean isRemoveAbove() {
        return removeAbove;
    }

    public void setRemoveAbove(boolean removeAbove) {
        this.removeAbove = removeAbove;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRecyclerIndex() {
        return recyclerIndex;
    }

    public void setRecyclerIndex(int recyclerIndex) {
        this.recyclerIndex = recyclerIndex;
    }

}
