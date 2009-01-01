package org.zhouer.zterm.model;

import java.util.Date;
import java.util.Map;
import java.util.Vector;

import org.zhouer.protocol.Protocol;
import org.zhouer.utils.CSV;
import org.zhouer.utils.TextUtils;

/**
 * Site is preference management or said collection of preference set by user.
 * 
 * @author h45
 */
public class Site implements Comparable {

	// 別名
	protected String alias;

	// 自動連線
	protected boolean autoconnect;
	// 連線後自動登入
	protected boolean autologin;

	protected final String defEmulation = "vt100"; //$NON-NLS-1$

	protected final String defEncoding = "Big5"; //$NON-NLS-1$
	protected final String defProtocol = Protocol.TELNET;

	// 終端機模擬
	protected String emulation;
	// 文字編碼
	protected String encoding;

	// host name and port
	protected String host;
	
	// 最近連線時間。
	protected long lastvisit;

	// 識別名稱
	protected String name;

	protected int port;

	// 登入前以及登入後該自動輸入的字串。
	protected String prelogin, postlogin;

	// 通訊協定 (telnet or ssh)
	protected String protocol;

	// 連線總次數
	protected int total;

	// 使用者帳號、密碼及其提示字串。
	protected String usernameprompt, username, userpassprompt, userpass;

	/**
	 * 沒有任何參數的 Site constructor
	 */
	public Site() {
		// A new instance, potential null pointer exception because this
		// constructor doesn't initialize any member field.
	}

	/**
	 * 由 CSV 表示法建構 Site
	 * 
	 * @param h
	 *            CSV
	 */
	public Site(final String h) {
		final Map m = TextUtils.getCsvParameters(h);

		// name, host, port 是必要的，一定會有
		this.name = (String) m.get("name"); //$NON-NLS-1$
		this.host = (String) m.get("host"); //$NON-NLS-1$
		this.port = Integer.parseInt((String) m.get("port")); //$NON-NLS-1$

		if (m.containsKey("protocol")) { //$NON-NLS-1$
			this.protocol = (String) m.get("protocol"); //$NON-NLS-1$
		} else {
			this.protocol = this.defProtocol;
		}

		if (m.containsKey("alias")) { //$NON-NLS-1$
			this.alias = (String) m.get("alias"); //$NON-NLS-1$
		} else {
			this.alias = ""; //$NON-NLS-1$
		}

		if (m.containsKey("encoding")) { //$NON-NLS-1$
			this.encoding = (String) m.get("encoding"); //$NON-NLS-1$
		} else {
			this.encoding = this.defEncoding;
		}

		if (m.containsKey("emulation")) { //$NON-NLS-1$
			this.emulation = (String) m.get("emulation"); //$NON-NLS-1$
		} else {
			this.emulation = this.defEmulation;
		}

		if (m.containsKey("lastvisit")) { //$NON-NLS-1$
			this.lastvisit = Long.parseLong((String) m.get("lastvisit")); //$NON-NLS-1$
		} else {
			this.lastvisit = 0;
		}

		if (m.containsKey("total")) { //$NON-NLS-1$
			this.total = Integer.parseInt((String) m.get("total")); //$NON-NLS-1$
		} else {
			this.total = 0;
		}

		if (m.containsKey("autoconnect")) { //$NON-NLS-1$
			final String message = (String) m.get("autoconnect"); //$NON-NLS-1$
			this.autoconnect = message.equalsIgnoreCase("true"); //$NON-NLS-1$
		} else {
			this.autoconnect = false;
		}

		if (m.containsKey("autologin")) { //$NON-NLS-1$
			final String message = (String) m.get("autologin"); //$NON-NLS-1$
			this.autologin = message.equalsIgnoreCase("true"); //$NON-NLS-1$
		} else {
			this.autologin = false;
		}
	}

	/**
	 * 使用詳細資料建構 Site
	 * 
	 * @param n
	 *            名稱
	 * @param h
	 *            hostname
	 * @param po
	 *            port
	 * @param pr
	 *            protocol
	 */
	public Site(final String n, final String h, final int po, final String pr) {
		this.name = n;
		this.host = h;
		this.port = po;
		this.protocol = pr;

		// 以下使用預設值
		this.alias = ""; //$NON-NLS-1$
		this.encoding = this.defEncoding;
		this.emulation = this.defEmulation;
		this.lastvisit = 0;
		this.total = 0;
		this.autoconnect = false;
		this.autologin = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(final Object o) {
		final Site site = (Site) o;
		if (this.total == site.total) {
			return (int) (site.lastvisit - this.lastvisit);
		}

		return site.total - this.total;
	}

	public boolean equals(final Object o) {
		if (o instanceof Site) {
			final Site site = (Site) o;
			if (this.host.equalsIgnoreCase(site.host)
					&& this.protocol.equalsIgnoreCase(site.protocol)
					&& (this.port == site.port)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Convert and combine protocol and host into a URL.
	 * 
	 * @return URL corresponding to protocol and host recorded in this site.
	 */
	public String getURL() {
		String url = this.protocol + "://" + this.host; //$NON-NLS-1$

		// 當連線 port 不等於該 protocol 預設值時要顯示連線 port
		if (this.protocol.equalsIgnoreCase(Protocol.TELNET) && (this.port != 23)) {
			url = url + ":" + this.port; //$NON-NLS-1$
		}

		return url;
	}

	public String toString() {
		final Vector v = new Vector();

		v.addElement("name=" + this.name); //$NON-NLS-1$
		v.addElement("host=" + this.host); //$NON-NLS-1$
		v.addElement("port=" + this.port); //$NON-NLS-1$
		v.addElement("protocol=" + this.protocol); //$NON-NLS-1$
		v.addElement("alias=" + this.alias); //$NON-NLS-1$
		v.addElement("encoding=" + this.encoding); //$NON-NLS-1$
		v.addElement("emulation=" + this.emulation); //$NON-NLS-1$
		v.addElement("lastvisit=" + this.lastvisit); //$NON-NLS-1$
		v.addElement("total=" + this.total); //$NON-NLS-1$

		if (this.autoconnect) {
			v.addElement("autoconnect=true"); //$NON-NLS-1$
		} else {
			v.addElement("autoconnect=false"); //$NON-NLS-1$
		}

		if (this.autologin) {
			v.addElement("autologin=true"); //$NON-NLS-1$
		} else {
			v.addElement("autologin=false"); //$NON-NLS-1$
		}

		return CSV.generate(v);
	}

	/**
	 * Update information whenever connection is established with this site.
	 */
	public void update() {
		this.total++;
		this.lastvisit = new Date().getTime();
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getEmulation() {
		return emulation;
	}

	public void setEmulation(String emulation) {
		this.emulation = emulation;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public boolean isAutoconnect() {
		return autoconnect;
	}

	public void setAutoconnect(boolean autoconnect) {
		this.autoconnect = autoconnect;
	}
}
