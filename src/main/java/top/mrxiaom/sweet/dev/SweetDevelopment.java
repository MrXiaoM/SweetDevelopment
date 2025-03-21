package top.mrxiaom.sweet.dev;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;

public class SweetDevelopment extends BukkitPlugin {
    public static SweetDevelopment getInstance() {
        return (SweetDevelopment) BukkitPlugin.getInstance();
    }

    public SweetDevelopment() {
        super(options()
                .adventure(true)
                .bungee(false)
                .adventure(true)
                .database(false)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(false)
                .scanIgnore("top.mrxiaom.sweet.dev.libs")
        );
    }


    @Override
    protected void afterEnable() {
        getLogger().info("SweetDevelopment 加载完毕");
    }
}
