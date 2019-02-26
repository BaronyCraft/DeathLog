package de.guntram.bukkit.DeathLog;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin implements Listener {
    
    public static Main instance;
    private File configDir;
    private boolean suppressDeathMessages;
    
    @Override 
    public void onEnable() {
        if (instance==null)
            instance=this;
        this.saveDefaultConfig();
        FileConfiguration config = getConfig();
        suppressDeathMessages=config.getBoolean("suppressDeathMessages");
        configDir = this.getDataFolder();
        configDir.mkdirs();
        if (!(configDir.isDirectory())) {
            this.getLogger().log(Level.WARNING, "Can''t create config directory {0}", configDir.getAbsolutePath());
        }
        API.initialize(this, configDir);
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName=command.getName();
        Location targetLocation;
        Player player;
        if (commandName.equals("deathlog")) {
            if (args.length == 0) {
                if (!(sender.hasPermission("deathlog.self"))) {
                    sender.sendMessage("You can't do that!");
                    return true;
                }
                if (sender instanceof Player) {
                    player=(Player) sender;
                } else {
                    sender.sendMessage("You need to be a player to use this without an argument");
                    return true;
                }
            } else {
                if (!(sender.hasPermission("deathlog.other"))) {
                    sender.sendMessage("You can't do that!");
                    return true;
                }
                player=Bukkit.getPlayer(args[0]);
                if (player==null) {
                    sender.sendMessage("Player "+args[0]+" not found");
                    return true;
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    API.sendDeathLog(sender, player);
                }
            } .runTaskAsynchronously(this);
            return true;
        }
        return false;
    }
    
    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        DeathInfo info=new DeathInfo();
        Player deceased=event.getEntity();
        info.deathTime=System.currentTimeMillis();
        info.player=deceased.getName();
        if (deceased.getKiller() == null) {
            info.killer="";
        } else {
            info.killer=deceased.getKiller().getName();
        }
        info.message=event.getDeathMessage();
        Location loc=deceased.getLocation();
        info.world=loc.getWorld().getName();
        info.x=loc.getBlockX();
        info.y=loc.getBlockY();
        info.z=loc.getBlockZ();
        info.lostExperience=deceased.getTotalExperience();
        info.lostLevel=deceased.getLevel();
        
        if (suppressDeathMessages) {
            event.setDeathMessage("");
        }
        
        new BukkitRunnable() {
            @Override 
            public void run() {
                List<DeathInfo>deaths=API.loadDeathInfo(deceased);
                deaths.add(info);
                API.saveDeathInfo(deceased, deaths);
            }
        }.runTaskAsynchronously(this);
    }
}
