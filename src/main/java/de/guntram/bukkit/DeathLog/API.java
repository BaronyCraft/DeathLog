/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.DeathLog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author gbl
 */
class API {

    static private File logDirectory;
    static private Plugin plugin;
    static void initialize(Plugin plugin, File dir) {
        API.plugin=plugin;
        API.logDirectory=dir;
    }

    // In order to save a few ms, the plugin runs code that accesses the
    // file in a separate BukkitRunnable; to avoid problems with this,
    // we make loading and saving synchronized. It would be more intelligent
    // to synchronize on the single player file, not on the class, but it
    // isn't expected that these methods get called *that* frequently.

    public static synchronized List<DeathInfo> loadDeathInfo(Player deceased) {
        List<DeathInfo> info;
        File infoFile=new File(logDirectory, deceased.getUniqueId().toString());
        Type mapType = new TypeToken<ArrayList<DeathInfo>>(){}.getType();
        try (JsonReader reader=new JsonReader(new FileReader(infoFile))) {
            Gson gson=new Gson();
            info=gson.fromJson(reader, mapType);
            return info;
        } catch (FileNotFoundException ex) {
            return new ArrayList<>();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Error reading {0} : {1}", new Object[]{infoFile.getAbsolutePath(), ex});
            return new ArrayList<>();
        }
    }
    
    static synchronized void saveDeathInfo(Player deceased, List<DeathInfo> info) {
        File infoFile=new File(logDirectory, deceased.getUniqueId().toString());
        try (FileWriter writer = new FileWriter(infoFile)) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(info, writer);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Trying to save death log {0} : {1}", new Object[]{infoFile.getAbsolutePath(), ex});
        }        
    }
    
    public static void sendDeathLog(CommandSender sender, Player player) {
        List<DeathInfo> deathlog = loadDeathInfo(player);
        // we may be executed async but need to call an api so wrap ...
        new BukkitRunnable() {
            @Override
            public void run() {
                if (deathlog.isEmpty()) {
                    sender.sendMessage("no deaths recorded");
                } else {
                    int first=deathlog.size() > 5 ? deathlog.size()-5 : 0;
                    for (int i=first; i<deathlog.size(); i++) {
                        DeathInfo item=deathlog.get(i);
                        Date date=new Date(item.deathTime);
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm");
                        StringBuilder builder=new StringBuilder(100);
                        builder.append(format.format(date)).append(" at ").append(item.world).
                                append(":").append(item.x).append("/").append(item.y).append("/").append(item.z).append(" ").append(item.message);
                        sender.sendMessage(builder.toString());
                    }
                }
            }
        } .runTaskLater(plugin, 0);
    }
}
