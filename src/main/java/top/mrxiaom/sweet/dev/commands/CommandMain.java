package top.mrxiaom.sweet.dev.commands;
        
import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.ColorHelper;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.dev.SweetDevelopment;
import top.mrxiaom.sweet.dev.func.AbstractModule;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static de.tr7zw.changeme.nbtapi.NBT.itemStackToNBT;
import static de.tr7zw.changeme.nbtapi.NBTType.NBTTagCompound;
import static de.tr7zw.changeme.nbtapi.NBTType.NBTTagList;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetDevelopment plugin) {
        super(plugin);
        registerCommand("sweetdevelopment", this);
    }

    @SuppressWarnings({"deprecation"})
    private static ItemStack requireItem(Player player) {
        ItemStack item = player.getItemInHand();
        return item.getType().equals(Material.AIR) ? null : item;
    }

    private static final Map<NBTType, NBTParser> parsersData = new HashMap<NBTType, NBTParser>(){{
        put(NBTType.NBTTagString, NBTParser.of("字符串", "STR", ReadableNBT::getString, (nbt, key) -> nbt.getStringList(key).toListCopy()));
        put(NBTType.NBTTagByte, NBTParser.of("字节", " B ", ReadableNBT::getByte, null));
        put(NBTType.NBTTagShort, NBTParser.of("短整型", " S ", ReadableNBT::getShort, null));
        put(NBTType.NBTTagLong, NBTParser.of("长整型", " L ", ReadableNBT::getLong, (nbt, key) -> nbt.getLongList(key).toListCopy()));
        put(NBTType.NBTTagFloat, NBTParser.of("单精度浮点数", " F ", ReadableNBT::getFloat, (nbt, key) -> nbt.getFloatList(key).toListCopy()));
        put(NBTType.NBTTagDouble, NBTParser.of("双精度浮点数", " D ", ReadableNBT::getDouble, (nbt, key) -> nbt.getDoubleList(key).toListCopy()));
        put(NBTType.NBTTagByteArray, NBTParser.of("字符数组", "[B]", NBTParser::parseByteArray, null, false));
        put(NBTType.NBTTagIntArray, NBTParser.of("整型数组", "[I]", NBTParser::parseIntArray, (nbt, key) -> nbt.getIntArrayList(key).toListCopy(), false));
        put(NBTType.NBTTagLongArray, NBTParser.of("整型数组", "[I]", NBTParser::parseLongArray, null, false));
    }};
    private static class NBTParser {
        final String type;
        final String typeIcon;
        final BiFunction<ReadableNBT, String, Object> getter;
        final BiFunction<ReadableNBT, String, List<?>> listGetter;
        final boolean copyable;

        NBTParser(String type, String typeIcon, BiFunction<ReadableNBT, String, Object> getter, BiFunction<ReadableNBT, String, List<?>> listGetter, boolean copyable) {
            this.type = type;
            this.typeIcon = typeIcon;
            this.getter = getter;
            this.listGetter = listGetter;
            this.copyable = copyable;
        }
        private static String parseByteArray(ReadableNBT nbt, String key) {
            byte[] bytes = nbt.getByteArray(key);
            return "... (" + (bytes == null ? 0 : bytes.length) + ")";
        }
        private static String parseIntArray(ReadableNBT nbt, String key) {
            List<Integer> list = nbt.getIntegerList(key).toListCopy();
            return "(" + list.size() + ") [ " + list.stream().map(String::valueOf).collect(Collectors.joining(", ")) + " ]";
        }
        private static String parseLongArray(ReadableNBT nbt, String key) {
            List<Long> list = nbt.getLongList(key).toListCopy();
            return "(" + list.size() + ") [ " + list.stream().map(String::valueOf).collect(Collectors.joining(", ")) + " ]";
        }
        static NBTParser of(String type, String typeIcon, BiFunction<ReadableNBT, String, Object> getter, BiFunction<ReadableNBT, String, List<?>> listGetter) {
            return of(type, typeIcon, getter, listGetter, false);
        }
        static NBTParser of(String type, String typeIcon, BiFunction<ReadableNBT, String, Object> getter, BiFunction<ReadableNBT, String, List<?>> listGetter, boolean copyable) {
            return new NBTParser(type, typeIcon, getter, listGetter, copyable);
        }
    }

    private static void append(TextComponent.Builder builder, String intent, String type, String typeIcon, String key, @Nullable Object message) {
        append(builder, intent, type, typeIcon, key, message, true);
    }
    private static void append(TextComponent.Builder builder, String intent, String type, String typeIcon, String key, @Nullable Object message, boolean copy) {
        // <类型>键: 值
        builder.append(Component.text(intent));
        builder.append(Component.text(typeIcon).color(NamedTextColor.WHITE)
                .hoverEvent(HoverEvent.showText(Component.text(type).color(NamedTextColor.YELLOW))));
        builder.append(Component.text(" " + key).color(NamedTextColor.YELLOW));
        builder.append(Component.text(": ").color(NamedTextColor.GRAY));
        if (message != null) {
            String s = String.valueOf(message);
            TextComponent component = !copy ? Component.text(s) : (Component.text(s)
                    .hoverEvent(HoverEvent.showText(Component.text("点击复制").color(NamedTextColor.YELLOW)))
                    .clickEvent(ClickEvent.copyToClipboard(s)));
            builder.resetStyle().append(component);
        }
        builder.resetStyle().appendNewline();
    }
    private static void appendLine(TextComponent.Builder builder, String intent, int i, String value) {
        builder.append(Component.text(intent));
        builder.append(Component.text(String.format("%2d. ", i)).color(NamedTextColor.GRAY)).resetStyle();
        builder.append(Component.text(value)).resetStyle().appendNewline();
    }
    private static boolean printTag(Player player, ReadableNBT nbt) {
        TextComponent.Builder builder = Component.text();
        printTagToBuilder(builder, nbt, 2);
        AdventureUtil.adventure().player(player).sendMessage(builder.build());
        return true;
    }
    private static void printTagToBuilder(TextComponent.Builder builder, ReadableNBT nbt, int intentCount) {
        String intent;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intentCount; i++) {
            sb.append(" ");
        }
        intent = sb.toString();
        for (String key : nbt.getKeys()) {
            NBTType type = nbt.getType(key);
            NBTParser data = parsersData.get(type);
            if (data != null) {
                append(builder, intent, data.type, data.typeIcon, key, data.getter.apply(nbt, key));
                continue;
            }
            if (type == NBTTagList) {
                NBTType listType = nbt.getListType(key);
                NBTParser listData = parsersData.get(type);
                if (listData != null && listData.listGetter != null) {
                    List<?> list = listData.listGetter.apply(nbt, key);
                    for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        String value = obj instanceof int[] ? Arrays.toString((int[]) obj) : String.valueOf(obj);
                        appendLine(builder, intent + "  ", i, value);
                    }
                    continue;
                }
                if (listType != null) switch (listType) {
                    case NBTTagList:
                        // TODO: 列表套列表
                        break;
                    case NBTTagCompound:
                        List<ReadWriteNBT> list = nbt.getCompoundList(key).toListCopy();
                        for (int i = 0; i < list.size(); i++) {
                            appendLine(builder, intent + "  ", i, "");
                            printTagToBuilder(builder, list.get(i), intentCount + 4);
                        }
                        break;
                }
                continue;
            }
            if (type == NBTTagCompound) {
                ReadableNBT compound = nbt.getCompound(key);
                if (compound != null) {
                    append(builder, intent, "NBT复合标签", "{ }", key, null);
                    printTagToBuilder(builder, compound, intentCount + 2);
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0]) && sender.isOp()) {
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        if (!(sender instanceof Player)) {
            return t(sender, "只有玩家可以使用该命令");
        }
        Player player = (Player) sender;
        if (args.length == 1 && isCommand("nbt", player, args[0])) {
            ItemStack item = requireItem(player);
            if (item == null) return t(player, "你需要手持一个物品");
            t(player, "&e--------------[&f nbt &e]---------------");
            if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4)) {
                ReadWriteNBT nbt = itemStackToNBT(item);
                printTag(player, nbt);
            } else {
                NBT.get(item, nbt -> {
                    printTag(player, nbt);
                });
            }
            return t(player, "&e------------------------------------");
        }
        if (args.length == 1 && isCommand("entity", player, args[0])) {
            RayTraceResult result = player.rayTraceBlocks(10);
            Entity hitEntity = result == null ? null : result.getHitEntity();
            if (hitEntity == null) {
                return t(player, "你需要将准星指向一个实体");
            }
            t(player, "&e-------------[&f entity &e]--------------");
            EntityType type = hitEntity.getType();
            String additionType = "";
            String translateName = null;
            try {
                additionType = ", " + type.getKey();
                translateName = "<lang:" + type.getTranslationKey() + ">";
            } catch (LinkageError ignored) {
            }
            t(player, "  &f实体: &e" + type + additionType + " &7(" + type.getName() + "/" + type.getTypeId() + ")");
            if (translateName != null) {
                AdventureUtil.sendMessage(player, "  &f原版实体名: &r" + translateName);
            }
            Location loc = hitEntity.getLocation();
            t(player, "  &f位置: &b" + hitEntity.getWorld().getName()
                    + "&f, &e" + String.format("%.1f", loc.getX())
                    + "&f, &e" + String.format("%.1f", loc.getY())
                    + "&f, &e" + String.format("%.1f", loc.getZ()));
            t(player, "  &f视角: &b偏航角, 俯仰角 &d" + String.format("%.2f", loc.getYaw())
                    + "&f, &d" + String.format("%.2f", loc.getPitch()) + "&f )");
            List<String> misc = new ArrayList<>();
            if (hitEntity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) hitEntity;
                t(player, "  &f生命值: &e" + living.getHealth() + "&f/&e" + living.getMaxHealth());
                try {
                    t(player, "  &f氧气值: &e" + living.getRemainingAir() + "&f/&e" + living.getMaximumAir());
                } catch (LinkageError ignored) {}
                try {
                    if (living.isInvisible()) misc.add("隐身的");
                } catch (LinkageError ignored) {}
                try {
                    if (!living.hasAI()) misc.add("无AI");
                } catch (LinkageError ignored) {}
            }
            if (hitEntity.isDead()) misc.add("已死亡");
            if (hitEntity.isOp()) misc.add("管理员");
            if (!hitEntity.isEmpty()) misc.add("有乘客");
            try {
                if (hitEntity.isFrozen()) misc.add("被冰冻");
            } catch (LinkageError ignored) {}
            try {
                if (hitEntity.isGlowing()) misc.add("发光");
            } catch (LinkageError ignored) {}
            if (!hitEntity.isCustomNameVisible()) misc.add("自定义名称不可见");
            if (hitEntity.isInsideVehicle()) misc.add("正在使用载具");
            try {
                if (hitEntity.isInvulnerable()) misc.add("无敌");
            } catch (LinkageError ignored) {}
            try {
                if (hitEntity.isInWater()) misc.add("在水中");
            } catch (LinkageError ignored) {}
            if (!hitEntity.isOnGround()) misc.add("在空中");
            if (!hitEntity.hasGravity()) misc.add("无重力");
            if (!misc.isEmpty()) {
                t(player, "  &f杂项属性: &e" + String.join("&f, &e"));
            }
            return t(player, "&e-------------------------------------");
        }
        if (args.length >= 1 && isCommand("item", player, args[0])) {
            if (args.length == 3 && "load".equalsIgnoreCase(args[1])) {
                File file = new File(plugin.getDataFolder(), "items/" + args[2] + ".yml");
                Util.mkdirs(file.getParentFile());
                if (!file.exists()) {
                    file = new File(plugin.getDataFolder(), "items/" + args[2]);
                    if (!file.exists()) {
                        String name = args[2].endsWith(".yml") ? args[2] : (args[2] + "(.yml)");
                        return t(player, "&e文件 items/" + name + " 不存在");
                    }
                }
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ItemStack item = config.getItemStack("item");
                if (item == null) {
                    return t(player, "&e物品读取失败，详见控制台日志");
                }
                player.setItemInHand(item);
                return t(player, "&a物品读取成功");
            }
            ItemStack item = requireItem(player);
            if (item == null) return t(player, "你需要手持一个物品");
            if (args.length == 3 && "save".equalsIgnoreCase(args[1])) {
                File file = new File(plugin.getDataFolder(), "items/" + args[2] + ".yml");
                Util.mkdirs(file.getParentFile());
                if (file.exists()) {
                    t(player, "&e文件已存在，此操作覆盖了现有文件");
                }
                YamlConfiguration config = new YamlConfiguration();
                config.set("item", item);
                try {
                    config.save(file);
                } catch (IOException e) {
                    warn(e);
                    return t(player, "&e物品保存失败，详见控制台日志");
                }
                return t(player, "&a物品保存成功 &7(items/" + args[2] + ".yml)");
            }
            t(player, "&e--------------[&f item &e]---------------");
            String translatable = "";
            try {
                String key = item.getTranslationKey();
                translatable = "  &f原版名字: &e<lang:" + key + ">";
            } catch (LinkageError ignored) {
            }
            AdventureUtil.sendMessage(player, "  &f物品: &e" + item.getType() + translatable);
            int stackSize = item.getMaxStackSize();
            if (stackSize > 1) {
                t(player, "  &f数量: &e" + item.getAmount() + "/" + stackSize);
            } else {
                t(player, "  &f数量: &e" + item.getAmount());
            }
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Repairable) {
                t(player, "  &f旧版ID: &e" + item.getType().getId() + " &7(data: " + item.getData().getData() + ")");
            } else {
                t(player, "  &f旧版ID: &e" + item.getType().getId() + ":" + item.getDurability() + " &7(data: " + item.getData().getData() + ")");
            }
            if (meta != null) {
                if (meta.hasDisplayName()) {
                    player.sendMessage(ColorHelper.parseColor("  &f物品名: &r") + meta.getDisplayName());
                }
                try {
                    if (meta.hasCustomModelData()) {
                        t(player, "  &fCustomModelData: &e" + meta.getCustomModelData());
                    }
                } catch (LinkageError ignored) {
                }
            }
            return t(player, "&e-------------------------------------");
        }
        if (args.length == 2 && "event".equalsIgnoreCase(args[0])) {
            Class<?> eventType = parseEvent(args[1]);
            HandlerList handlerList = null;
            if (eventType != null) {
                try {
                    Method method = eventType.getDeclaredMethod("getHandlerList");
                    handlerList = (HandlerList) method.invoke(null);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (eventType == null || handlerList == null) {
                return t(player, "你输入的事件不可用，无法获取相关信息");
            }
            RegisteredListener[] listeners = handlerList.getRegisteredListeners();
            t(player, "&e--------------[&f event &e]---------------");
            t(player, "  &f 事件: &e" + eventType.getName());
            t(player, "  &f监听器列表: &7(" + listeners.length + ")");
            for (RegisteredListener registered : listeners) {
                String plugin = registered.getPlugin().getName();
                String listener = registered.getListener().getClass().getName();
                String priority = registered.getPriority().name().toUpperCase();
                String ignoreCancelled = registered.isIgnoringCancelled() ? " &7(ignoreCancelled = true)" : "";
                t(player, "    &8" + priority + " &7[&a" + plugin + "&7] &f" + listener + ignoreCancelled);
            }
            return t(player, "&e--------------------------------------");
        }
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return true;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        Set<String> events = new HashSet<>();
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            for (RegisteredListener listener : handlerList.getRegisteredListeners()) {
                try {
                    if (listener instanceof TimedRegisteredListener) {
                        Class<?> type = ((TimedRegisteredListener) listener).getEventClass();
                        if (type != null) {
                            events.add(type.getName());
                        }
                    } else if (listener.getClass().getName().equals(RegisteredListener.class.getName())) {
                        Field field = RegisteredListener.class.getDeclaredField("executor");
                        field.setAccessible(true);
                        EventExecutor executor = (EventExecutor) field.get(listener);
                        for (Field f : executor.getClass().getFields()) {
                            if (f.getType().getName().equals(Class.class.getName())) {
                                f.setAccessible(true);
                                Class<?> type = (Class<?>) f.get(executor);
                                events.add(type.getName());
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        CommandMain.events.clear();
        for (String event : events) {
            for (String p : ignorablePackages) {
                if (event.startsWith(p)) {
                    String shortType = event.substring(p.length());
                    if (!CommandMain.events.contains(shortType)) {
                        CommandMain.events.add(shortType);
                    }
                    break;
                }
            }
        }
        CommandMain.events.addAll(events);
    }

    private static final List<String> events = new ArrayList<>();
    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listItemArg1 = Lists.newArrayList(
            "load", "save"
    );
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (perm("item", sender)) list.add("item");
            if (perm("entity", sender)) list.add("entity");
            if (perm("nbt", sender)) list.add("nbt");
            if (perm("event", sender)) list.add("event");
            if (sender.isOp()) list.add("reload");
            return startsWithL(list, args[0]);
        }
        if (args.length == 2) {
            if (isCommand("item", sender, args[0])) {
                return startsWith(listItemArg1, args[1]);
            }
            if (isCommand("event", sender, args[0])) {
                return startsWith(events, args[1]);
            }
        }
        return emptyList;
    }

    private boolean isCommand(String s1, CommandSender sender, String arg) {
        return s1.equalsIgnoreCase(arg) && perm(s1, sender);
    }

    private boolean perm(String s, CommandSender sender) {
        return sender.hasPermission("sweet.dev." + s);
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }

    public List<String> startsWithL(List<String> list, String s) {
        return startsWithL(null, list, s);
    }
    public List<String> startsWithL(String[] addition, List<String> list, String s) {
        String s1 = s.toLowerCase();
        if (addition != null) list.addAll(0, Lists.newArrayList(addition));
        list.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return list;
    }

    private static final List<String> ignorablePackages = Lists.newArrayList(
            "org.bukkit.event.player.",
            "org.bukkit.event.server.",
            "org.bukkit.event.block.",
            "org.bukkit.event.entity.",
            "org.bukkit.event.enchantment.",
            "org.bukkit.event.inventory.",
            "org.bukkit.event.world.",
            "org.bukkit.event.hanging.",
            "org.bukkit.event.raid.",
            "org.bukkit.event.vehicle.",
            "org.bukkit.event.weather."
    );
    @Nullable
    public static Class<?> parseEvent(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            for (String p : ignorablePackages) {
                try {
                    return Class.forName(p + type);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return null;
    }
}
