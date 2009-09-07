package org.zhouer.zterm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import org.zhouer.vt.Config;

public class Resource implements Config {
	public static final String RESOURCE_LOCATION = "resource.location";
	public static final String LOCALE_COUNTRY = "locale.country";
	public static final String LOCALE_LANGUAGE = "locale.language";
	public static final String LOCALE_VARIANT = "locale.variant";
	public static final String ANTI_IDLE = "connect.anti-idle";
	public static final String ANTI_IDLE_INTERVAL = "connect.anti-idle-interval";
	public static final String ANTI_IDLE_STRING = "connect.anti-idle-string";
	public static final String AUTO_RECONNECT = "connect.auto-reconnect";

	public static final String AUTO_RECONNECT_INTERVAL = "connect.auto-reconnect-interval";
	public static final String AUTO_RECONNECT_TIME = "connect.autoreconnect-time";
	public static final String CUSTOM_BELL_PATH = "custom-bell-path";

	public static final String EXTERNAL_BROWSER = "external-browser-command";
	public static final String GEOMETRY_HEIGHT = "geometry.height";
	public static final String GEOMETRY_WIDTH = "geometry.width";

	public static final String GEOMETRY_X = "geometry.x";
	public static final String GEOMETRY_Y = "geometry.y";
	public static final String REMOVE_MANUAL_DISCONNECT = "remove-manual-disconnect";
	// chitsaou.070726: 顯示捲軸
	public static final String SHOW_SCROLL_BAR = "show-scroll-bar";
	public static final String SHOW_TOOLBAR = "show-toolbar";
	public static final String SYSTEM_LOOK_FEEL = "use-system-look-and-feel";

	// chitsaou.070726: 分頁編號
	public static final String TAB_NUMBER = "tab-number";
	public static final String USE_CUSTOM_BELL = "use-custom-bell";

	private volatile static Resource resource = null;

	public static Resource getInstance() {
		if (Resource.resource == null) {
			synchronized (Resource.class) {
				if (Resource.resource == null) {
					Resource.resource = new Resource();
				}
			}
		}

		return Resource.resource;
	}

	private final HashMap defmap, map;
	private final String defaultResourceLocation;

	public Locale getLocale() {
		final String country = this.getValue(Resource.LOCALE_COUNTRY);
		final String language = this.getValue(Resource.LOCALE_LANGUAGE);
		final String variant = this.getValue(Resource.LOCALE_VARIANT);
		final Locale locale =  new Locale(language, country, variant);
		
		return locale;
	}
	
	private Resource() {
		
		// Default location: $HOME/.ztermrc
		defaultResourceLocation = getDefaultResourceLocation();
		this.map = new HashMap();
		this.defmap = new HashMap();
		
		final File rc = this.getDefaultRcFile();

		// 載入預設值
		this.loadDefault();

		// 從設定檔讀取設定，若不存在則新建設定檔
		if (rc.exists()) {
			this.readDefaultRcFile();
		} else {
			try {
				rc.createNewFile();
			} catch (final IOException e) {
				System.err.println("catch IOException when create new rcfile.");
			}
		}

		// Exception, it must disable system look and feel to ensure the program
		// works correct initially.
		this.setValue(Resource.SYSTEM_LOOK_FEEL, false);
	}
	
	private String getDefaultResourceLocation() {
		return System.getProperty("user.home") + File.separator + ".ztermrc";
	}

	public void addFavorite(final Site site) {
		int index;
		final Vector favorites = this.getFavorites();

		index = favorites.indexOf(site);

		if (index == -1) {
			favorites.addElement(site);
		} else {
			((Site) favorites.elementAt(index)).update();
		}

		this.setFavorites(favorites);
	}

	public synchronized Vector<Site> getArray(final String name) {
		final Vector<Site> v = new Vector<Site>();
		String s;

		// 應該是連號的，找不到就結束
		for (int count = 0;; count++) {
			s = this.getStringValue(name + "." + count);
			if (s != null) {
				v.addElement(new Site(s));
			} else {
				break;
			}
		}

		return v;
	}

	public boolean getBooleanValue(final String key) {
		return this.getValue(key).equalsIgnoreCase("true");
	}

	public Site getFavorite(final String id) {
		Site fa;
		final Vector f = this.getFavorites();
		final Iterator iter = f.iterator();

		while (iter.hasNext()) {
			fa = (Site) iter.next();
			// 尋找時可用 name 或是 alias
			if (id.equalsIgnoreCase(fa.name) || id.equalsIgnoreCase(fa.alias)) {
				return fa;
			}
		}

		return null;
	}

	public Vector<Site> getFavorites() {
		final Vector<Site> favorites = this.getArray("favorite");

		for (int i = 0; i < favorites.size(); i++) {
			favorites.setElementAt(new Site(favorites.elementAt(i).toString()),
					i);
		}

		return favorites;
	}

	public int getIntValue(final String key) {
		return Integer.parseInt(this.getValue(key));
	}

	public String getStringValue(final String key) {
		return this.getValue(key);
	}

	/**
	 * Read default resource file
	 */
	public void readDefaultRcFile() {
		final File defaultRcFile = this.getDefaultRcFile();
		readRcFile(defaultRcFile);
	}
	
	public File getRcFile() {
		final String resourceLocation = getResourceLocation();
		return new File(resourceLocation);
	}
	
	public void readRcFile() {
		final File rcFile = getRcFile();
		readRcFile(rcFile);
	}
	
	public void writeRcFile() {
		final File rcFile = getRcFile();
		writeRcFile(rcFile);
	}
	
	/**
	 * Read the specified resource file
	 * @param file the specified resource file
	 */
	private void readRcFile(File file) {
		BufferedReader br;
		String buf;

		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file), "UTF8"));

			while ((buf = br.readLine()) != null) {
				this.parseLine(buf);
			}

			br.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void setArray(final String name, final Vector strs) {
		String tmp;
		final Iterator mapiter = this.map.keySet().iterator();
		final Iterator iter = strs.iterator();

		// remove original data
		while (mapiter.hasNext()) {
			tmp = mapiter.next().toString();
			if (tmp.startsWith(name + ".")) {
				mapiter.remove();
			}
		}

		// add new data
		for (int count = 0; iter.hasNext(); count++) {
			this.setValue(name + "." + count, iter.next().toString());
		}
	}

	public void setFavorites(final Vector favorites) {
		this.setArray("favorite", favorites);
	}

	public void setValue(final String key, final boolean value) {
		if (value) {
			this.setValue(key, "true");
		} else {
			this.setValue(key, "false");
		}
	}

	public void setValue(final String key, final int value) {
		this.setValue(key, Integer.toString(value));
	}

	public synchronized void setValue(final String key, final String value) {
		this.map.put(key, value);
	}

	public void writeDefaultRcFile() {
		final File defaultRcFile = this.getDefaultRcFile();
		writeRcFile(defaultRcFile);
	}
	
	private void writeRcFile(File file) {
		TreeSet ts;
		Iterator iter;
		String str;
		PrintWriter pw;

		// TreeSet 才會排序
		ts = new TreeSet(this.map.keySet());
		iter = ts.iterator();

		try {
			pw = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF8"));

			while (iter.hasNext()) {
				str = iter.next().toString();
				pw.println(str + "::" + this.map.get(str));
				// System.out.println( "Setting: " + str + " -> " +
				// settings.get( str ) );
			}

			pw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private File getDefaultRcFile() {
		return new File(defaultResourceLocation);
	}

	private synchronized String getValue(final String key) {
		if (this.map.get(key) != null) {
			return (String) this.map.get(key);
		}

		// 若沒有設定，則使用預設值
		return (String) this.defmap.get(key);
	}

	private void loadDefault() {
		final String os = System.getProperty("os.name");

		// 設定地區
		this.defmap.put(Resource.LOCALE_COUNTRY, Locale.getDefault().getCountry());
		this.defmap.put(Resource.LOCALE_LANGUAGE, Locale.getDefault().getLanguage());
		this.defmap.put(Resource.LOCALE_VARIANT, Locale.getDefault().getVariant());
		
		// 設定視窗相關資訊
		this.defmap.put(Resource.GEOMETRY_X, "0");
		this.defmap.put(Resource.GEOMETRY_Y, "0");
		this.defmap.put(Resource.GEOMETRY_WIDTH, "980");
		this.defmap.put(Resource.GEOMETRY_HEIGHT, "720");
		this.defmap.put(Resource.SHOW_TOOLBAR, "true");

		// chitsaou.070726: 分頁編號
		this.defmap.put(Resource.TAB_NUMBER, "true");
		// chitsaou.070726: 顯示捲軸
		this.defmap.put(Resource.SHOW_SCROLL_BAR, "true");

		// 設定模擬終端機大小
		this.defmap.put(Config.TERMINAL_COLUMNS, "80");
		this.defmap.put(Config.TERMINAL_ROWS, "24");
		this.defmap.put(Config.TERMINAL_SCROLLS, "200");

		// 防閒置設定
		this.defmap.put(Resource.ANTI_IDLE, "true");
		this.defmap.put(Resource.ANTI_IDLE_INTERVAL, "120");
		this.defmap.put(Resource.ANTI_IDLE_STRING, "\\x1bOA\\x1bOB");

		// 自動重連設定
		this.defmap.put(Resource.AUTO_RECONNECT, "true");
		this.defmap.put(Resource.AUTO_RECONNECT_TIME, "10");
		this.defmap.put(Resource.AUTO_RECONNECT_INTERVAL, "500");

		// 字型設定
		this.defmap.put(Config.FONT_FAMILY, "Monospaced");
		this.defmap.put(Config.FONT_SIZE, "0");
		this.defmap.put(Config.FONT_BOLD, "false");
		this.defmap.put(Config.FONT_ITALY, "false");

		if (Pattern.matches(".*Windows.*", os)) {
			this.defmap.put(Config.FONT_ANTIALIAS, "false");
		} else {
			this.defmap.put(Config.FONT_ANTIALIAS, "true");
		}

		this.defmap.put(Config.FONT_VERTICLAL_GAP, "0");
		this.defmap.put(Config.FONT_HORIZONTAL_GAP, "0");
		this.defmap.put(Config.FONT_DESCENT_ADJUST, "0");

		// 游標設定
		this.defmap.put(Config.CURSOR_BLINK, "true");
		this.defmap.put(Config.CURSOR_SHAPE, "block");

		// 一般設定
		if (Pattern.matches(".*Windows.*", os)) {
			this.defmap.put(Resource.EXTERNAL_BROWSER, "explorer \"%u\"");
		} else {
			this.defmap.put(Resource.EXTERNAL_BROWSER, "mozilla %u");
		}
		this.defmap.put(Resource.SYSTEM_LOOK_FEEL, "false");
		this.defmap.put(Config.COPY_ON_SELECT, "false");
		this.defmap.put(Config.CLEAR_AFTER_COPY, "true");
		this.defmap.put(Resource.REMOVE_MANUAL_DISCONNECT, "true");
		this.defmap.put(Config.AUTO_LINE_BREAK, "false");
		this.defmap.put(Config.AUTO_LINE_BREAK_LENGTH, "72");
		this.defmap.put(Resource.USE_CUSTOM_BELL, "false");
		this.defmap.put(Resource.CUSTOM_BELL_PATH, "");
		this.defmap.put(Resource.RESOURCE_LOCATION, defaultResourceLocation);
	}

	private void parseLine(final String line) {
		String[] argv;

		// 用 "::" 隔開參數名與值
		argv = line.split("::");

		if (argv.length != 2) {
			return;
		}

		if (argv[0].length() > 0) {
			this.map.put(argv[0], argv[1]);
		}
	}

	/**
	 * Getter of resourceLocation
	 * @return the resourceLocation
	 */
	public String getResourceLocation() {
		return getStringValue(Resource.RESOURCE_LOCATION);
	}

	/**
	 * Setter of resourceLocation
	 * @param resourceLocation the resourceLocation to set
	 */
	public void setResourceLocation(String resourceLocation) {
		setValue(Resource.RESOURCE_LOCATION, resourceLocation);
	}
	
	public void setLocale(final Locale locale) {
		resource.setValue(Resource.LOCALE_COUNTRY, locale.getCountry());
		resource.setValue(Resource.LOCALE_LANGUAGE, locale.getLanguage());
		resource.setValue(Resource.LOCALE_VARIANT, locale.getVariant());
	}
}
