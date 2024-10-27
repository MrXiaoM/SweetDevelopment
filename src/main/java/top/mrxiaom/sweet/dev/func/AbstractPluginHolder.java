package top.mrxiaom.sweet.dev.func;
        
import top.mrxiaom.sweet.dev.SweetDevelopment;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetDevelopment> {
    public AbstractPluginHolder(SweetDevelopment plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetDevelopment plugin, boolean register) {
        super(plugin, register);
    }
}
