package com.crunch.battleutils;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Core extends JavaPlugin implements Listener, CommandExecutor {

    public static Plugin plugin;
    public static boolean Enabled = false;

    private boolean battleActive = false;
    private int maxDeaths = 0;
    private final HashMap<UUID, Integer> deathCounts = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("battle").setExecutor(this);
        getCommand("battle").setTabCompleter(new BattleTabCompleter());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("battle")) return false;

        if (!sender.hasPermission("battle.toggle")) {
            sender.sendMessage(ChatColor.RED +"You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED +"Usage: /battle on <deathcount> OR /battle off");
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /battle on <deathcount>");
                return true;
            }

            try {
                maxDeaths = Integer.parseInt(args[1]);
                battleActive = true;
                deathCounts.clear();

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist on");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule doMobSpawning false");

                sender.sendMessage(ChatColor.RED + "Battle mode enabled. Max deaths: " + maxDeaths);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid number: " + args[1]);
            }
            return true;

        } else if (args[0].equalsIgnoreCase("off")) {
            battleActive = false;
            deathCounts.clear();

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule doMobSpawning true");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist on");

            sender.sendMessage(ChatColor.RED + "Battle mode disabled.");
            return true;
        }

        sender.sendMessage("Unknown subcommand.");
        return true;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!battleActive) return;

        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        int newDeathCount = deathCounts.getOrDefault(uuid, 0) + 1;
        deathCounts.put(uuid, newDeathCount);

        if (newDeathCount >= maxDeaths) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "whitelist remove " + player.getName());
             Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + "Eliminated from battle.");
            Bukkit.broadcastMessage(player.getName() + ChatColor.RED + " has been eliminated from the battle.");
        } else {
            player.sendMessage("You have died " + newDeathCount + "/" + maxDeaths + " times.");
        }
    }

    public class BattleTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (!command.getName().equalsIgnoreCase("battle")) return Collections.emptyList();

            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                if ("on".startsWith(args[0].toLowerCase())) completions.add("on");
                if ("off".startsWith(args[0].toLowerCase())) completions.add("off");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("on")) {
                for (int i = 1; i <= 5; i++) {
                    completions.add(String.valueOf(i));
                }
            }

            return completions;
        }
    }
}
