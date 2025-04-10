package com.github;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class AnSpec extends JavaPlugin {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        // Сохранение стандартного config.yml при первой загрузке плагина
        saveDefaultConfig();
        getLogger().info("AnSpec plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AnSpec plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = getConfig();

        if (command.getName().equalsIgnoreCase("spec")) {
            // Проверка, что отправитель является игроком
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;

            // Проверка прав anspec.moderator - модераторы не могут использовать команду
            if (player.hasPermission("anspec.moderator")) {
                String noModeratorPermissionMessage = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.no_moderator_permission", "&cModerators cannot use this command."));
                player.sendMessage(noModeratorPermissionMessage);
                return true;
            }

            // Проверка прав на использование команды
            if (!player.hasPermission("anspec.use")) {
                String noPermissionMessage = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.no_permission", "&cYou do not have permission to use this command."));
                player.sendMessage(noPermissionMessage);
                return true;
            }

            // Проверка задержки (cooldown)
            int cooldownTimeSeconds = config.getInt("settings.delay", 10); // Задержка в секундах
            if (cooldowns.containsKey(player.getUniqueId())) {
                long lastUsed = cooldowns.get(player.getUniqueId());
                long timeSinceLastUse = (System.currentTimeMillis() - lastUsed) / 1000;

                if (timeSinceLastUse < cooldownTimeSeconds) {
                    int remainingTime = cooldownTimeSeconds - (int) timeSinceLastUse;
                    String cooldownMessage = ChatColor.translateAlternateColorCodes('&',
                                    config.getString("messages.cooldown", "&cYou must wait [time] seconds before using this command again."))
                            .replace("[time]", String.valueOf(remainingTime));
                    player.sendMessage(cooldownMessage);
                    return true;
                }
            }

            // Обновление времени последнего использования команды
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

            // Получение сообщений из config.yml
            String moderatorChatMessage = ChatColor.translateAlternateColorCodes('&',
                            config.getString("messages.moderator_chat", "&eИгрок [player] просит спека"))
                    .replace("[player]", player.getName());
            String title = ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.title", "&cВнимание в чат!"));
            String subtitle = ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.subtitle", "&eИгрок просит спека"));
            String playerMessage = ChatColor.translateAlternateColorCodes('&',
                    config.getString("messages.player_message", "&aВаш запрос на спек отправлен модератору."));

            // Отправка сообщения всем модераторам
            for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
                if (targetPlayer.hasPermission("anspec.moderator")) {
                    targetPlayer.sendMessage(moderatorChatMessage);
                    targetPlayer.sendTitle(title, subtitle, 10, 70, 20);
                }
            }

            // Отправка сообщения игроку
            player.sendMessage(playerMessage);

            return true;
        } else if (command.getName().equalsIgnoreCase("specreload")) {
            if (sender.hasPermission("anspec.reload")) {
                reloadConfig();
                String reloadMessage = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.reload_success", "&aAnSpec configuration reloaded."));
                sender.sendMessage(reloadMessage);
                getLogger().info("Configuration reloaded by " + sender.getName());
            } else {
                String noReloadPermissionMessage = ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.no_reload_permission", "&cYou do not have permission to reload the configuration."));
                sender.sendMessage(noReloadPermissionMessage);
            }
            return true;
        }

        return false;
    }
}