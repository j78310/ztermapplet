package org.zhouer.zterm;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.Timer;

import org.zhouer.protocol.Protocol;
import org.zhouer.protocol.SSH2;
import org.zhouer.protocol.Telnet;
import org.zhouer.utils.Convertor;
import org.zhouer.utils.TextUtils;
import org.zhouer.vt.Application;
import org.zhouer.vt.Config;
import org.zhouer.vt.VT100;

public class Session extends JPanel implements Runnable, Application,
		AdjustmentListener, MouseWheelListener {

	private class AntiIdleTask implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			// 如果超過 antiIdelInterval milliseconds 沒有送出東西，
			// lastInputTime 在 writeByte, writeBytes 會被更新。
			final long now = new Date().getTime();
			if (Session.this.antiidle
					&& Session.this.isConnected()
					&& (now - Session.this.lastInputTime > Session.this.antiIdleInterval)) {
				// System.out.println( "Sent antiidle char" );
				// TODO: 設定 antiidle 送出的字元
				if (Session.this.site.protocol
						.equalsIgnoreCase(Protocol.TELNET)) {

					final String buf = TextUtils
							.BSStringToString(Session.this.resource
									.getStringValue(Resource.ANTI_IDLE_STRING));
					final char[] ca = buf.toCharArray();
					Session.this.writeChars(ca, 0, ca.length);

					// 較正規的防閒置方式
					// writeByte( Telnet.IAC );
					// writeByte( Telnet.NOP );
				}
			}
		}
	}

	public static final int STATE_ALERT = 4;
	public static final int STATE_CLOSED = 3;
	public static final int STATE_CONNECTED = 2;
	// 連線狀態常數
	public static final int STATE_TRYING = 1;

	private static final long serialVersionUID = 2180544188833033537L;
	// 連線狀態
	public int state;

	// 防閒置用
	private boolean antiidle;
	private final Convertor conv;

	// 這個 session 是否擁有一個 tab, 可能 session 還沒結束，但 tab 已被關閉。
	private boolean hasTab;

	private String iconname;
	private InputStream is;
	private long lastInputTime, antiIdleInterval;

	private final Model model;

	// 與遠端溝通用的物件
	private Protocol network;
	private OutputStream os;
	private final Resource resource;

	private JScrollBar scrollbar;

	// 捲頁緩衝區的行數
	private int scrolllines;
	private final Site site;
	// 自動重連用
	private long startTime;
	private Timer ti;

	private final VT100 vt;

	private String windowtitle;
	private boolean disconnected;

	public Session(final Site s, final Resource r, final Convertor c,
			final BufferedImage bi, final Model model) {
		super();

		this.site = s;
		this.resource = r;
		this.conv = c;
		this.model = model;

		// 設定擁有一個分頁
		this.hasTab = true;

		// FIXME: 預設成 host
		this.windowtitle = this.site.host;
		this.iconname = this.site.host;

		// FIXME: magic number
		this.setBackground(Color.BLACK);

		// VT100
		this.vt = new VT100(this, this.resource, this.conv, bi);

		// FIXME: 是否應該在這邊設定？
		this.vt.setEncoding(this.site.encoding);
		this.vt.setEmulation(this.site.emulation);

		// 設定 layout 並把 vt 及 scrollbar 放進去，
		this.setLayout(new BorderLayout());
		this.add(this.vt, BorderLayout.CENTER);

		// chitsaou.070726: 顯示捲軸
		if (this.resource.getBooleanValue(Resource.SHOW_SCROLL_BAR)) {
			this.scrolllines = this.resource
					.getIntValue(Config.TERMINAL_SCROLLS);
			// FIXME: magic number
			this.scrollbar = new JScrollBar(Adjustable.VERTICAL,
					this.scrolllines - 1, 24, 0, this.scrolllines + 23);
			this.scrollbar.addAdjustmentListener(this);

			this.add(this.scrollbar, BorderLayout.EAST);

			// 處理滑鼠滾輪事件
			this.addMouseWheelListener(this);
		}

	}

	public void adjustmentValueChanged(final AdjustmentEvent ae) {
		this.vt
				.setScrollUp(this.scrollbar.getMaximum()
						- this.scrollbar.getValue()
						- this.scrollbar.getVisibleAmount());
	}

	public void bell() {
		this.model.bell(this);
	}

	public void bell(final Session s) {
		if (this.resource.getBooleanValue(Resource.USE_CUSTOM_BELL)) {
			try {
				java.applet.Applet.newAudioClip(
						new File(this.resource
								.getStringValue(Resource.CUSTOM_BELL_PATH))
								.toURI().toURL()).play();
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			java.awt.Toolkit.getDefaultToolkit().beep();
		}

		if (!this.model.isTabForeground(s)) {
			s.setState(Session.STATE_ALERT);
		}
	}

	public void close() {
		if (this.isClosed()) {
			return;
		}

		this.disconnect(false);
		
		// 移除 listener
		this.removeMouseWheelListener(this);
	}

	public void colorCopy() {
		this.model.colorCopy();
	}

	public void colorPaste() {
		this.model.colorPaste();
	}

	public void copy() {
		this.model.copy();
	}

	public String getEmulation() {
		return this.site.emulation;
	}

	public String getIconName() {
		return this.iconname;
	}

	public String getSelectedColorText() {
		return this.vt.getSelectedColorText();
	}

	public String getSelectedText() {
		return this.vt.getSelectedText();
	}

	public Site getSite() {
		return this.site;
	}

	public String getURL() {
		return this.site.getURL();
	}

	public String getWindowTitle() {
		return this.windowtitle;
	}

	public boolean isClosed() {
		// 如果 network 尚未建立則也當成 closed.
		if (this.network == null) {
			return true;
		}
		return this.network.isClosed();
	}

	/*
	 * 送到 network 的
	 */

	public boolean isConnected() {
		// 如果 network 尚未建立則也當成尚未 connect.
		if (this.network == null) {
			return false;
		}
		return this.network.isConnected();
	}

	public boolean isTabForeground() {
		return this.model.isTabForeground(this);
	}

	public void mouseWheelMoved(final MouseWheelEvent arg0) {
		this.scroll(arg0.getWheelRotation());
	}

	public void openExternalBrowser(final String url) {
		this.model.openExternalBrowser(url);
	}

	public void paste() {
		this.model.paste();
	}

	public void pasteColorText(final String str) {
		this.vt.pasteColorText(str);
	}

	public void pasteText(final String str) {
		this.vt.pasteText(str);
	}

	public int readBytes(final byte[] buf) {
		try {
			return this.is.read(buf);
		} catch (final IOException e) {
			// e.printStackTrace();
			// 可能是正常中斷，也可能是異常中斷，在下層沒有區分
			if (!isDisconnected()) {
				this.disconnect(true);
			}
			
			return -1;
		}
	}

	public void remove() {
		// 設定分頁被移除了
		this.hasTab = false;
	}

	@Override
	public boolean requestFocusInWindow() {
		return this.vt.requestFocusInWindow();
	}

	public void resetSelected() {
		this.vt.resetSelected();
	}

	public void run() {
		// 設定連線狀態為 trying
		this.setState(Session.STATE_TRYING);

		// 新建連線
		if (this.site.protocol.equalsIgnoreCase(Protocol.TELNET)) {
			this.network = new Telnet(this.site.host, this.site.port);
			this.network.setTerminalType(this.site.emulation);
		} else if (this.site.protocol.equalsIgnoreCase(Protocol.SSH)) {
			this.network = new SSH2(this.site.host, this.site.port);
			this.network.setTerminalType(this.site.emulation);
		} else {
			System.err.println("Unknown protocol: " + this.site.protocol); //$NON-NLS-1$
		}

		// 連線失敗
		if (this.network.connect() == false) {
			// 設定連線狀態為 closed
			this.setState(Session.STATE_CLOSED);
			this.showMessage(Messages.getString("Session.ConnectionFailed")); //$NON-NLS-1$
			return;
		}

		this.is = this.network.getInputStream();
		this.os = this.network.getOutputStream();

		// TODO: 如果需要 input filter or trigger 可以在這邊套上

		// 設定連線狀態為 connected
		this.setState(Session.STATE_CONNECTED);

		// 連線成功，更新或新增連線紀錄
		this.resource.addFavorite(this.site);
		this.model.updateFavoriteMenu();

		// 防閒置用的 Timer
		this.updateAntiIdleTime();
		this.lastInputTime = new Date().getTime();
		this.ti = new Timer(1000, new AntiIdleTask());
		this.ti.start();

		// 記錄連線開始的時間
		this.startTime = new Date().getTime();

		this.vt.run();
	}

	public void scroll(final int amount) {
		this.scrollbar.setValue(this.scrollbar.getValue() + amount);
	}

	public void setEmulation(final String emu) {
		this.site.emulation = emu;

		// 通知遠端 terminal type 已改變
		this.network.setTerminalType(emu);

		this.vt.setEmulation(emu);
	}

	public void setEncoding(final String enc) {
		this.site.encoding = enc;
		this.vt.setEncoding(this.site.encoding);
		requestScreenData();
	}
	
	public void requestScreenData() {
		final int CTRL_L = 12;
		this.writeChar((char) CTRL_L);
	}

	public void setIconName(final String in) {
		// TODO: 未完成
		this.iconname = in;
		this.model.updateTab();
	}

	public void setState(final int s) {
		this.state = s;
		this.model.updateTabState(s, this);
	}

	public void setWindowTitle(final String wt) {
		// TODO: 未完成
		this.windowtitle = wt;
		this.model.updateTab();
	}

	public void showMessage(final String msg) {
		// 當分頁仍存在時才會顯示訊息
		if (this.hasTab) {
			this.model.showMessage(msg);
		}
	}

	public void showPopup(final int x, final int y) {
		final Point p = this.vt.getLocationOnScreen();
		String link;

		if (this.vt.coverURL(x, y)) {
			link = this.vt.getURL(x, y);
		} else {
			link = null;
		}

		this.model.showPopup(p.x + x, p.y + y, link);
	}

	public void updateAntiIdleTime() {
		// 更新是否需要啟動防閒置
		this.antiidle = this.resource.getBooleanValue(Resource.ANTI_IDLE);

		// 防閒置的作法是定時檢查距上次輸入是否超過 interval,
		// 所以這裡只要設定 antiIdleTime 就自動套用新的值了。
		this.antiIdleInterval = this.resource
				.getIntValue(Resource.ANTI_IDLE_INTERVAL) * 1000;
	}

	public void updateImage(final BufferedImage bi) {
		this.vt.updateImage(bi);
	}

	public void updateScreen() {
		this.vt.updateScreen();
	}

	public void updateSize() {
		this.vt.updateSize();
	}

	public void writeByte(final byte b) {
		this.lastInputTime = new Date().getTime();
		try {
			this.os.write(b);
		} catch (final IOException e) {
			// e.printStackTrace();
			System.out.println("Caught IOException in Session::writeByte(...)"); //$NON-NLS-1$
			
			if (!isDisconnected()) {
				this.disconnect(true);
			}
		}
	}

	/**
	 * Disconnect this session
	 */
	public void disconnect(final boolean fromRemote) {
		setDisconnected();
		
		// 中斷連線
		this.network.disconnect();

		// 停止防閒置用的 timer
		if (this.ti != null) {
			this.ti.stop();
		}

		// 若遠端 server 主動斷線則判斷是否需要重連
		final boolean autoreconnect = this.resource
				.getBooleanValue(Resource.AUTO_RECONNECT);
		
		if (autoreconnect && fromRemote) {
			final long reopenTime = this.resource
					.getIntValue(Resource.AUTO_RECONNECT_TIME);
			final long reopenInterval = this.resource
					.getIntValue(Resource.AUTO_RECONNECT_INTERVAL);
			final long now = new Date().getTime();

			// 判斷連線時間距現在時間是否超過自動重連時間
			// 若設定自動重連時間為 0 則總是自動重連
			if ((now - this.startTime <= reopenTime * 1000)
					|| (reopenTime == 0)) {
				try {
					Thread.sleep(reopenInterval);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}

				this.model.reopenSession(this);
			}
		}

		// 將連線狀態改為斷線
		this.setState(Session.STATE_CLOSED);
	}

	public void writeBytes(final byte[] buf, final int offset, final int len) {
		this.lastInputTime = new Date().getTime();
		try {
			this.os.write(buf, offset, len);
		} catch (final IOException e) {
			if (!isDisconnected()) {
				e.printStackTrace();
				this.disconnect(true);
			}
		}
	}

	public void writeChar(final char c) {
		byte[] buf;

		buf = this.conv.charToBytes(c, this.site.encoding);

		this.writeBytes(buf, 0, buf.length);
	}

	public void writeChars(final char[] buf, final int offset, final int len) {
		int count = 0;
		// FIXME: magic number
		final byte[] tmp = new byte[len * 4];
		byte[] tmp2;

		for (int i = 0; i < len; i++) {
			tmp2 = this.conv.charToBytes(buf[offset + i], this.site.encoding);
			for (int j = 0; j < tmp2.length; j++) {
				tmp[count++] = tmp2[j];
			}
		}

		this.writeBytes(tmp, 0, count);
	}

	/**
	 * @return true, if disconnected; false, o.w.
	 */
	public boolean isDisconnected() {
		return disconnected;
	}
	
	/**
	 * Label this session is disconnected.
	 */
	private void setDisconnected() {
		disconnected = true;
	}
}
