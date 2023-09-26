package vorst.treecapitator;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TreeCapitator2 extends JavaPlugin {

        public void onEnable() {
            Bukkit.getPluginManager().registerEvents(new TreeHandler(), this);
        }
    }
