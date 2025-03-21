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
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.ColorHelper;
import top.mrxiaom.sweet.dev.SweetDevelopment;
import top.mrxiaom.sweet.dev.func.AbstractModule;

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
        if (args.length == 1 && "nbt".equalsIgnoreCase(args[0]) && player.hasPermission("sweet.dev.nbt")) {
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
        if (args.length >= 1 && "item".equalsIgnoreCase(args[0]) && player.hasPermission("sweet.dev.item")) {

            ItemStack item = requireItem(player);
            if (item == null) return t(player, "你需要手持一个物品");
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
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList(
            "nbt");
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "nbt", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        return emptyList;
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
}
