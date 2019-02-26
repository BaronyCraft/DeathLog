package de.guntram.bukkit.DeathLog;

public class DeathInfo implements Comparable<DeathInfo> {
    long deathTime;
    String player;
    String killer;
    String message;
    String world;
    int x, y, z;
    int lostExperience;
    int lostLevel;

    @Override
    public int compareTo(DeathInfo other) {
        long diff=this.deathTime-other.deathTime;
        return (diff > 0 ? 1 : diff<0 ? -1 : 0);
    }
}
