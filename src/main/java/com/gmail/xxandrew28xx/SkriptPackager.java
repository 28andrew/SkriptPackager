package com.gmail.xxandrew28xx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.xxandrew28xx.versions.AnyVersionMatcher;
import com.gmail.xxandrew28xx.versions.FixedVersionMatcher;
import com.gmail.xxandrew28xx.versions.StartsWithVersionMatcher;
import com.gmail.xxandrew28xx.versions.VersionMatcher;

public class SkriptPackager extends JavaPlugin implements Listener{
	private static SkriptPackager instance;
	private String version;
	//private VersionMatcherPrioritizer versionMatcherSorter;
	
	
	public static Boolean restart_needed = false;
	public static Boolean skript_loaded = false;
	public static Boolean addons_loaded = true;
	
	FileConfiguration lang;
	
	private HashMap<VersionMatcher, URL> skript_download_urls = new HashMap<VersionMatcher, URL>();
	
	public static Plugin getInstance(){
		return instance;
	}
	@EventHandler
	public void onLogin(PlayerJoinEvent e){
		if (e.getPlayer().isOp() && restart_needed){
			e.getPlayer().sendMessage(getLang("please-restart") + " " + getLang("to-install", instance.getName()));
		}
	}
	public String getLang(String name, String... replace){
		String msg = lang.getString(name);
		if (replace != null){
			for (String s : replace){
				msg = msg.replaceFirst("\\{\\}", s);
			}
		}
		return msg;
	}
	public String getLang(String name){
		return getLang(name, (String[]) null);
	}
	@Override
	public void onEnable(){
		instance = this;
		lang = YamlConfiguration.loadConfiguration(instance.getTextResource("lang.yml"));
		version = removeLastChar(this.getServer().getVersion().split("\\(MC: ")[1]).replace('.', '_');
		//versionMatcherSorter = new VersionMatcherPrioritizer();
		this.getServer().getPluginManager().registerEvents(this, this);
		info(version);
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable(){
			
			@Override
			public void run() {
				FileConfiguration config = YamlConfiguration.loadConfiguration(instance.getTextResource("config.yml"));
				
				if (!pluginLoaded("Skript")){
					if (config.contains("skript-links.any-version")){
						
						String URL = config.getString("skript-links.any-version");
						try {
							skript_download_urls.put(new AnyVersionMatcher(), new URL(URL));
						} catch (MalformedURLException e) {
							info(getLang("invalid-url", "skript-links.any-version"));
							e.printStackTrace();
							return;
						}
						
					}else if (config.contains("skript-links.version-specific")){
						ConfigurationSection versions = config.getConfigurationSection("skript-links").getConfigurationSection("version-specific");
						for (String version : versions.getKeys(false)){
							String URL = versions.getString(version);
							try {
								skript_download_urls.put(versionToMatcher(version), new URL(URL));
							} catch (MalformedURLException e) {
								info(getLang("invalid-url", "skript-links.version-specific." + version + ".download-url:"));
								e.printStackTrace();
								return;
							}
						}
						
					}else{
						info(getLang("missing-dependency-dl", "Skript"));
						return;
					}
					
					ArrayList<Entry<VersionMatcher, URL>> al = new ArrayList<Entry<VersionMatcher, URL>>(skript_download_urls.entrySet());
					/*Collections.sort(al, new Comparator<Entry<VersionMatcher, URL>>(){

						@Override
						public int compare(Entry<VersionMatcher, URL> o1, Entry<VersionMatcher, URL> o2) {
							return versionMatcherSorter.compare(o1.getKey(), o2.getKey());
						}});
					*/
					Iterator<Entry<VersionMatcher, URL>> it = al.iterator();
					Boolean done = false;
					while (it.hasNext() && !done){
						Entry<VersionMatcher, URL> entry = it.next();
						VersionMatcher vm = entry.getKey();
						URL url = entry.getValue();
						if (vm.matches(version)){
							done = true;
							try{
								info(getLang("installing-dependency", "Skript"));
								saveFile(url, new File("plugins/Skript.jar"));
								SkriptPackager.restart_needed = true;
								info(getLang("installed-dependency", "Skript"));
							}catch(Exception e){
								info(getLang("could-not-download-and-save", url.toString(), "plugins/Skript.jar"));
								e.printStackTrace();
							}
						}
					}
					if (!done){
						info(getLang("no-applicable-links", "Skript"));
						return;
					}
				}else{
					skript_loaded = true;
				}
				//Addons (set addons_loaded to false upon fails)
				ConfigurationSection addons = config.getConfigurationSection("addons");
				if (addons != null){
					HashMap<VersionMatcher, URL> addon_urls = new HashMap<VersionMatcher, URL>();
					for (String addon_name : addons.getKeys(false)){
						if (pluginLoaded(addon_name)){
							continue;
						}
						ConfigurationSection addon = addons.getConfigurationSection(addon_name);
						if (addon.contains("any-version")){
							try{
								addon_urls.put(new AnyVersionMatcher(), new URL(addon.getString("any-version")));
							}catch(MalformedURLException e){
								info(getLang("invalid-url", "addons." + addon_name + ".any-version"));
								e.printStackTrace();
								return;
							}
						}else if (addon.contains("version-specific")){
							ConfigurationSection versions = addon.getConfigurationSection("version-specific");
							for (String version : versions.getKeys(false)){
								String URL = versions.getString(version);
								try {
									addon_urls.put(versionToMatcher(version), new URL(URL));
								} catch (MalformedURLException e) {
									info(getLang("invalid-url", "addons." + addon_name + ".version-specific." + version));
									e.printStackTrace();
									return;
								}
							}
						}else{
							info(getLang("missing-dependency-dl", addon_name));
							return;
						}
						ArrayList<Entry<VersionMatcher, URL>> al = new ArrayList<Entry<VersionMatcher, URL>>(addon_urls.entrySet());
						Iterator<Entry<VersionMatcher, URL>> it = al.iterator();
						Boolean done = false;
						while (it.hasNext() && !done){
							Entry<VersionMatcher, URL> entry = it.next();
							VersionMatcher vm = entry.getKey();
							URL url = entry.getValue();
							if (vm.matches(version)){
								done = true;
								try{
									addons_loaded = false;
									SkriptPackager.restart_needed = true;
									
									info(getLang("installing-dependency", addon_name));
									
									//FileUtils.copyURLToFile(url, new File("plugins/" + addon_name + ".jar"));
									saveFile(url, new File("plugins/" + addon_name + ".jar"));
									info(getLang("installed-dependency", addon_name));
								}catch(Exception e){
									info(getLang("could-not-download-and-save", url.toString(), "plugins/" + addon_name + ".jar"));
									e.printStackTrace();
									return;
								}
							}
						}
					}
				}
				if (restart_needed){
					infoNoSpace(ChatColor.GREEN + "--------------------");
					infoNoSpace(ChatColor.GREEN + " ");
					infoNoSpace(ChatColor.GREEN + getLang("please-restart"));
					infoNoSpace(ChatColor.GREEN + getLang("to-install", instance.getName()));
					infoNoSpace(ChatColor.GREEN + " ");
					infoNoSpace(ChatColor.GREEN + "--------------------");
				}
				if (skript_loaded && addons_loaded){
					//Put in Skripts
					ConfigurationSection scripts = config.getConfigurationSection("scripts");
					if (scripts != null){
						for (String script_name : scripts.getKeys(false)){
							ConfigurationSection script = scripts.getConfigurationSection(script_name);
							String file_name = script.getString("file-name");
							if (script.contains("hide") && script.getString("hide").equalsIgnoreCase("true")){
								enableHiddenScript(instance.getResource(file_name), script_name);
							}else{
								File output = new File("plugins/Skript/scripts/" + file_name);
								if (output.exists()){
									continue;
								}
								try {
									FileUtils.copyInputStreamToFile(instance.getResource(file_name), output);
								} catch (IOException e) {
									info(getLang("could-not-copy-script", file_name));
									e.printStackTrace();
								}
								enableScript(file_name);
							}
						}
						info(getLang("ready-to-use"));
					}
				}
			}
			private void enableScript(String script){
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skript enable " + script);
			}
			private void enableHiddenScript(InputStream script, String name){
				try{
					File tempFile = File.createTempFile("skript_" + name, Integer.toString(new Random().nextInt(100)));
					tempFile.deleteOnExit();
					FileOutputStream out = new FileOutputStream(tempFile);
					IOUtils.copy(script, out);
					Skript.loadScript(tempFile);
				}catch(Exception e){
					info(getLang("could-not-load-hidden-script", name));
				}
			}
			private void saveFile(URL download, File output) throws IOException{
				URLConnection conn = download.openConnection();
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
				conn.connect();
				FileUtils.copyInputStreamToFile(conn.getInputStream(), output);
			}
			private Boolean pluginLoaded(String pl){
				Plugin[] plugins = Bukkit.getServer().getPluginManager().getPlugins();
				for (Plugin p : plugins){
					if (p.getName().equalsIgnoreCase(pl)) return true;
				}
				return false;
			}
		}, 10L);
	}
	public class HTTPError extends Exception{

		private static final long serialVersionUID = 1L;
		
		public HTTPError(Integer response){
			super("HTTP Status Code: " + response);
		}
	}
	public static String join(String delimitter, String... elements){ //Java 6 doesn't have it in String
		String output = "";
		Boolean first = true;
		for (String element : elements){
			if (first){
				first = false;
				output = element;
			}else{
				output += delimitter + element;
			}
		}
		return output;
	}
	private static String removeLastChar(String str) {
        return str.substring(0,str.length()-1);
    }
	private static String removeLastXChars(String str, int x){
		return str.substring(0, str.length() - x);
	}
	private void info(String info){
		this.getServer().getLogger().info("");
		this.getServer().getLogger().info("[" + this.getName() + "] " + info);
	}
	private void infoNoSpace(String info){
		this.getServer().getLogger().info("[" + this.getName() + "] " + info);
	}
	private VersionMatcher versionToMatcher(String version){
		if (version.endsWith("_X")){
			return new StartsWithVersionMatcher(removeLastXChars(version, 2));
		}else if (version.equalsIgnoreCase("ANY") || version.equalsIgnoreCase("OTHER")){
			return new AnyVersionMatcher();
		}else{
			return new FixedVersionMatcher(version);
		}
	}
}
