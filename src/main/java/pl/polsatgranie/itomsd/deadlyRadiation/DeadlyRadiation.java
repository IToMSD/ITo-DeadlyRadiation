package pl.polsatgranie.itomsd.deadlyRadiation;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class DeadlyRadiation extends JavaPlugin implements Listener, TabCompleter {

    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, BossBar> activeBossRadiationBars = new HashMap<>();
    private final Map<UUID, BukkitTask> lugolaTasks = new HashMap<>();

    private String bossbar_text;
    private String bossbar_color;
    private String bossbar_color2;
    private String bossbar_style;

    private String radiation_bossbar_text;
    private String radiation_bossbar_color;
    private String radiation_bossbar_style;

    public static int lugola_time;
    public static String lugola_name;
    public static List<String> lugola_lore = new ArrayList<>();
    public static boolean blindness_effect;
    private boolean craft_lugola;
    private boolean radiationEnabled;
    private boolean deadly_radiation_flag_is_save_zone;
    private double damagePerTick;
    private WorldGuardPlugin worldGuard;

    @Override
    public void onEnable() {
        worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");

        if (worldGuard == null) {
            getLogger().severe("WorldGuard not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Metrics metrics = new Metrics(this, 22934);
        this.getLogger().info("""
                
                ------------------------------------------------------------
                |                                                          |
                |      _  _______        __     __    _____   ____         |
                |     | ||___ ___|      |  \\   /  |  / ____| |  _ \\        |
                |     | |   | |   ___   | |\\\\ //| | | (___   | | \\ \\       |
                |     | |   | |  / _ \\  | | \\_/ | |  \\___ \\  | |  ) )      |
                |     | |   | | | (_) | | |     | |  ____) | | |_/ /       |
                |     |_|   |_|  \\___/  |_|     |_| |_____/  |____/        |
                |                                                          |
                |                                                          |
                ------------------------------------------------------------
                |                 +==================+                     |
                |                 |  DeadlyRadiation |                     |
                |                 |------------------|                     |
                |                 |        1.0       |                     |
                |                 |------------------|                     |
                |                 |  PolsatGraniePL  |                     |
                |                 +==================+                     |
                ------------------------------------------------------------
                """);

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        damagePerTick = config.getDouble("damage_per_tick");
        radiationEnabled = config.getBoolean("deadly_radiation_enabled");
        deadly_radiation_flag_is_save_zone = config.getBoolean("deadly_radiation_flag_is_save_zone");
        craft_lugola = config.getBoolean("craft_lugola");
        lugola_name = ChatColor.translateAlternateColorCodes('&',config.getString("lugola.name"));
        lugola_lore.clear();
        for (String s : config.getStringList("lugola.lore")) {
            lugola_lore.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        blindness_effect = config.getBoolean("blindness_effect");
        lugola_time = config.getInt("lugola_time");

        bossbar_text = ChatColor.translateAlternateColorCodes('&',config.getString("bossbar.lugola.text"));
        bossbar_color = config.getString("bossbar.lugola.color");
        bossbar_color2 = config.getString("bossbar.lugola.color_in_radiation");
        bossbar_style = config.getString("bossbar.lugola.style");

        radiation_bossbar_text = ChatColor.translateAlternateColorCodes('&',config.getString("bossbar.radiation.text"));
        radiation_bossbar_color = config.getString("bossbar.radiation.color");
        radiation_bossbar_style = config.getString("bossbar.radiation.style");

        Bukkit.getPluginManager().registerEvents(this, this);
        DeadlyRadiationFlag.registerFlags();
        getCommand("radiation").setExecutor(this);
        getCommand("radiation").setTabCompleter(this);
        getCommand("radiation").setAliases(Arrays.asList("deadlyradiation", "dr"));

        if (craft_lugola){
            List<?> line1 = config.getList("lugola-recipe.line1");
            List<?> line2 = config.getList("lugola-recipe.line2");
            List<?> line3 = config.getList("lugola-recipe.line3");

            NamespacedKey key = new NamespacedKey(this, "lugola");
            ShapedRecipe recipe = new ShapedRecipe(key, LugolaItem.createLugola());
            recipe.shape("ABC", "DEF", "GHI");
            recipe.setIngredient('A', Material.valueOf((String) line1.get(0)));
            recipe.setIngredient('B', Material.valueOf((String) line1.get(1)));
            recipe.setIngredient('C', Material.valueOf((String) line1.get(2)));
            recipe.setIngredient('D', Material.valueOf((String) line2.get(0)));
            recipe.setIngredient('E', Material.valueOf((String) line2.get(1)));
            recipe.setIngredient('F', Material.valueOf((String) line2.get(2)));
            recipe.setIngredient('G', Material.valueOf((String) line3.get(0)));
            recipe.setIngredient('H', Material.valueOf((String) line3.get(1)));
            recipe.setIngredient('I', Material.valueOf((String) line3.get(2)));
            Bukkit.addRecipe(recipe);
        }

    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.POTION) {
            String name = item.getItemMeta().getDisplayName();
            String name2 = ChatColor.stripColor(lugola_name);
            if (Objects.equals(name, name2)) {
                Player player = event.getPlayer();
                showBossBar(player);
                BukkitTask task = getServer().getScheduler().runTaskTimer(this, () -> {
                    BossBar bossBar = activeBossBars.get(player.getUniqueId());
                    if (bossBar != null) {
                        double progress = bossBar.getProgress() - (1.0 / lugola_time);
                        bossBar.setProgress(Math.max(0, progress));
                    }
                }, 0L, 20L);

                lugolaTasks.put(player.getUniqueId(), task);
                getServer().getScheduler().runTaskLater(this, () -> {
                    removeBossBar(player);
                    activeBossBars.remove(player.getUniqueId());
                }, lugola_time * 20L);
            }
        } else if (item.getType() == Material.MILK_BUCKET) {
            Player player = event.getPlayer();
            if (activeBossBars.containsKey(player.getUniqueId())) {
                removeBossBar(player);
            }
        }
    }

    private void showBossBar(Player player) {
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(bossbar_text, BarColor.valueOf(bossbar_color), BarStyle.valueOf(bossbar_style));
            activeBossBars.put(player.getUniqueId(), bossBar);
        }
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
    }

    private void removeBossBar(Player player) {
        BossBar bossBar = activeBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }

        BukkitTask task = lugolaTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void showRadiationBossBar(Player player) {
        if (!activeBossRadiationBars.containsKey(player.getUniqueId())) {
            BossBar bossBar = Bukkit.createBossBar(radiation_bossbar_text, BarColor.valueOf(radiation_bossbar_color), BarStyle.valueOf(radiation_bossbar_style));
            bossBar.addPlayer(player);
            activeBossRadiationBars.put(player.getUniqueId(), bossBar);
            getServer().getScheduler().runTaskLater(this, ()->{
                bossBar.removePlayer(player);
                activeBossRadiationBars.remove(player.getUniqueId());
            }, 100L);
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Goodbye!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (radiationEnabled) {
            if (Objects.equals(isRegionAffected(player), !deadly_radiation_flag_is_save_zone)) {
                if (player.hasPermission("itomsd.deadlyradiation.bypass")) {
                    return;
                }
                if (!activeBossBars.containsKey(player.getUniqueId())) {
                    player.damage(damagePerTick);
                    if (blindness_effect) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1, false, false));
                    }
                    showRadiationBossBar(player);
                } else {
                    BossBar bossBar = activeBossBars.get(player.getUniqueId());
                    bossBar.setColor(BarColor.valueOf(bossbar_color2));
                }

            } else{
                if (activeBossBars.containsKey(player.getUniqueId())) {
                    BossBar bossBar = activeBossBars.get(player.getUniqueId());
                    bossBar.setColor(BarColor.valueOf(bossbar_color));
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("radiation")) {
            if (!sender.hasPermission("itomsd.deadlyradiation.admin")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("no_permission")));
                return true;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("§a/radiation help §8-> §adisplays a list of commands with a description of their use");
                sender.sendMessage("§a/radiation [on/off] §8-> §aturns on/off radiation");
                sender.sendMessage("§a/radiation givelugola [NICK] [amount] §8-> §agives the player Lugola");
                sender.sendMessage("§a/radiation removeeffect [NICK] §8-> §aremoves the Lugola effect from the player");
                sender.sendMessage("§a/radiation reload §8-> §areloads the plugin configuration");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "on":
                    radiationEnabled = true;
                    sender.sendMessage("§aRadiation ON.");
                    break;
                case "off":
                    radiationEnabled = false;
                    sender.sendMessage("§aRadiation OFF.");
                    break;
                case "givelugola":
                    int amount = 1;
                    Player target = (Player) sender;
                    if (args.length >= 2) {
                        target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage("§cPlayer not found.");
                            return true;
                        }
                    }
                    if (args.length >= 3) {
                        try {
                            amount = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            amount = 1;
                        }
                    }

                    for (int i = 0; i < amount; i++) {
                        target.getInventory().addItem(LugolaItem.createLugola());
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("unknown_command")
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%player%", target.getName())
                    ));
                    break;

                case "removeeffect":
                    if (args.length < 2) {
                        sender.sendMessage("§aUsage: /radiation removeeffect [NICK]");
                        return true;
                    }
                    Player target2 = Bukkit.getPlayer(args[1]);
                    if (target2 == null) {
                        sender.sendMessage("§aPlayer not found.");
                        return true;
                    }
                    if (activeBossBars.containsKey(target2.getUniqueId())) {
                        removeBossBar(target2);
                        sender.sendMessage("§aRemoved Lugola effect from " + target2.getName());
                    } else {
                        sender.sendMessage("§a" + target2.getName() + " does not have the Lugola effect.");
                    }
                    break;
                case "reload":
                    reloadConfig();
                    FileConfiguration config = getConfig();

                    craft_lugola = config.getBoolean("craft_lugola");
                    damagePerTick = config.getDouble("damage_per_tick");
                    radiationEnabled = config.getBoolean("deadly_radiation_enabled");
                    deadly_radiation_flag_is_save_zone = config.getBoolean("deadly_radiation_flag_is_save_zone");
                    lugola_name = ChatColor.translateAlternateColorCodes('&', config.getString("lugola.name"));
                    lugola_lore.clear();
                    for (String s : config.getStringList("lugola.lore")) {
                        lugola_lore.add(ChatColor.translateAlternateColorCodes('&', s));
                    }
                    lugola_time = config.getInt("lugola_time");
                    blindness_effect = config.getBoolean("blindness_effect");

                    bossbar_text = ChatColor.translateAlternateColorCodes('&', config.getString("bossbar.lugola.text"));
                    bossbar_color = config.getString("bossbar.lugola.color");
                    bossbar_color2 = config.getString("bossbar.lugola.color_in_radiation");
                    bossbar_style = config.getString("bossbar.lugola.style");

                    radiation_bossbar_text = ChatColor.translateAlternateColorCodes('&', config.getString("bossbar.radiation.text"));
                    radiation_bossbar_color = config.getString("bossbar.radiation.color");
                    radiation_bossbar_style = config.getString("bossbar.radiation.style");

                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("plugin_reloaded")));
                    break;
                default:
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("unknown_command")));
            }
            return true;
        }
        return false;
    }

    private boolean isRegionAffected(Player player) {
        Location loc = BukkitAdapter.adapt(player.getLocation());
        World world = BukkitAdapter.adapt(player.getWorld());

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(world);

        if (regionManager != null) {
            ApplicableRegionSet regions = regionManager.getApplicableRegions(loc.toVector().toBlockPoint());
            for (ProtectedRegion region : regions) {
                if (region.getFlag(DeadlyRadiationFlag.DEADLY_RADIATION) == StateFlag.State.ALLOW) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("radiation")) {
            if (args.length == 1) {
                return Arrays.asList("help", "on", "off", "givelugola", "removeeffect", "reload");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("removeeffect")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            }
        }
        return null;
    }
}
