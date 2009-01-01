package org.zhouer.zterm.view;

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
import org.zhouer.protocol.Telnet;
import org.zhouer.utils.Convertor;
import org.zhouer.utils.TextUtils;
import org.zhouer.vt.Application;
import org.zhouer.vt.VT100;
import org.zhouer.zterm.model.Model;
import org.zhouer.zterm.model.Resource;
import org.zhouer.zterm.model.Site;

public class SessionPane extends JPanel implements Runnable, Application,
		AdjustmentListener, MouseWheelListener {

	private class AntiIdleTask implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			// 如果超過 antiIdelInterval milliseconds 沒有送出東西，
			// lastInputTime 在 writeByte, writeBytes 會被更新。
			final long now = new Date().getTime();
			if (SessionPane.this.antiidle
					&& SessionPane.this.isConnected()
					&& (now - SessionPane.this.lastInputTime > SessionPane.this.antiIdleInterval)) {
				// System.out.println( "Sent antiidle char" );
				// TODO: 設定 antiidle 送出的字元
				if (SessionPane.this.site.getProtocol()
						.equalsIgnoreCase(Protocol.TELNET)) {

					final String buf = TextUtils
							.BSStringToString(SessionPane.this.resource
									.getStringValue(Resource.ANTI_IDLE_STRING));
					final char[] ca = buf.toCharArray();
					SessionPane.this.writeChars(ca, 0, ca.length);

					// 較正規的防閒置方式
					// writeByte( Telnet.IAC );
					// writeByte( Telnet.NOP );
				}
			}
		}
	}

	// 連線狀態常數
	public static final int STATE_ALERT = 4;
	public static final int STATE_CLOSED = 3;
	public static final int STATE_CONNECTED = 2;	
	public static final int STATE_TRYING = 1;
	
	private static final long serialVersionUID = 2180544188833033537L;
	
	// 連線狀態
	private int state;

	// 防閒置用
	private boolean antiidle;
	private final Convertor conv;

	// 這個 session 是否擁有一個 tab, 可能 session 還沒結束，但 tab 已被關閉。
	private boolean hasTab;

	private InputStream is;
	private long lastInputTime, antiIdleInterval;

	private final Model model;

	// 與遠端溝通用的物件
	private Protocol network;
	private OutputStream os;
	private final Resource resource;

	private JScrollBar scrollbar;

	private final Site site;
	// 自動重連用
	private long startTime;
	private Timer ti;

	private final VT100 vt;

	public SessionPane(final Site site, final BufferedImage image) {
		this.site = site;
		this.resource = Resource.getInstance();
		this.conv = Convertor.getInstance();
		this.model = Model.getInstance();

		// 設定擁有一個分頁
		this.hasTab = true;

		this.setBackground(Color.BLACK);

		// VT100
		this.vt = new VT100(this, this.resource, this.conv, image);

		// FIXME: 是否應該在這邊設定？
		this.vt.setEncoding(this.site.getEncoding());
		this.vt.setEmulation(this.site.getEmulation());

		// 設定 layout 並把 vt 及 scrollbar 放進去
		this.setLayout(new BorderLayout());
		this.add(this.vt, BorderLayout.CENTER);
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

	public void bell(final SessionPane s) {
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
			s.setState(SessionPane.STATE_ALERT);
		}
	}

	public void close(final boolean fromRemote) {
		if (this.isClosed()) {
			return;
		}

		// 移除 listener
		this.removeMouseWheelListener(this);

		// 中斷連線
		this.network.disconnect();

		// 停止防閒置用的 timer
		if (this.ti != null) {
			this.ti.stop();
		}

		// 通知 vt 停止運作
		if (this.vt != null) {
			this.vt.close();
		}

		// 將連線狀態改為斷線
		this.setState(SessionPane.STATE_CLOSED);

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
		return this.site.getEmulation();
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

	/**
	 * Getter of state
	 *
	 * @return the state
	 */
	public int getState() {
		return state;
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

	/*
	 * 自己的
	 */

	public int readBytes(final byte[] buf) {
		try {
			return this.is.read(buf);
		} catch (final IOException e) {
			// e.printStackTrace();
			// 可能是正常中斷，也可能是異常中斷，在下層沒有區分
			this.close(true);
			return -1;
		}
	}

	public void remove() {
		// 設定分頁被移除了
		this.hasTab = false;
	}

	public boolean requestFocusInWindow() {
		return this.vt.requestFocusInWindow();
	}

	public void resetSelected() {
		this.vt.resetSelected();
	}

	public void run() {
		// 設定連線狀態為 trying
		this.setState(SessionPane.STATE_TRYING);

		// 新建連線
		if (this.site.getProtocol().equalsIgnoreCase(Protocol.TELNET)) {
			this.network = new Telnet(this.site.getHost(), this.site.getPort());
			this.network.setTerminalType(this.site.getEmulation());
		} else {
			System.err.println("Unknown protocol: " + this.site.getProtocol()); //$NON-NLS-1$
			// 設定連線狀態為 closed
			this.setState(SessionPane.STATE_CLOSED);
			return;
		}

		// 連線失敗
		if (this.network.connect() == false) {
			// 設定連線狀態為 closed
			this.setState(SessionPane.STATE_CLOSED);
			return;
		}

		this.is = this.network.getInputStream();
		this.os = this.network.getOutputStream();

		// TODO: 如果需要 input filter or trigger 可以在這邊套上

		// 設定連線狀態為 connected
		this.setState(SessionPane.STATE_CONNECTED);

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
		site.setEmulation(emu);

		// 通知遠端 terminal type 已改變
		this.network.setTerminalType(emu);

		this.vt.setEmulation(emu);
	}

	public void setEncoding(final String enc) {
		site.setEncoding(enc);
		this.vt.setEncoding(this.site.getEncoding());
		requestScreenData();
	}
	
	public void requestScreenData() {
		final int CTRL_L = 12;
		this.writeChar((char) CTRL_L);
	}

	public void setState(final int s) {
		this.state = s;
		this.model.updateTabState(s, this);
	}

	/* (non-Javadoc)
	 * @see org.zhouer.vt.Application#showMessage(java.lang.String)
	 */
	public void showMessage(final String msg) {
		// 當分頁仍存在時才會顯示訊息
		if (this.hasTab) {
			this.model.showMessage(msg);
		}
	}

	/* (non-Javadoc)
	 * @see org.zhouer.vt.Application#showPopup(int, int)
	 */
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
			this.close(true);
		}
	}

	public void writeBytes(final byte[] buf, final int offset, final int len) {
		this.lastInputTime = new Date().getTime();
		try {
			this.os.write(buf, offset, len);
		} catch (final IOException e) {
			// e.printStackTrace();
			System.out
					.println("Caught IOException in Session::writeBytes(...)"); //$NON-NLS-1$
			this.close(true);
		}
	}

	public void writeChar(final char c) {
		byte[] buf;

		buf = this.conv.charToBytes(c, this.site.getEncoding());

		this.writeBytes(buf, 0, buf.length);
	}

	public void writeChars(final char[] buf, final int offset, final int len) {
		int count = 0;
		// FIXME: magic number
		final byte[] writeBuffer = new byte[len * 4];
		byte[] byteBuffer;

		for (int i = 0; i < len; i++) {
			byteBuffer = this.conv.charToBytes(buf[offset + i], this.site.getEncoding());
			for (int j = 0; j < byteBuffer.length; j++) {
				writeBuffer[count++] = byteBuffer[j];
			}
		}

		this.writeBytes(writeBuffer, 0, count);
	}
}
