package org.zhouer.vt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.zhouer.utils.Convertor;
import org.zhouer.utils.TextUtils;
import org.zhouer.utils.UrlRecognizer;
import org.zhouer.zterm.Session;

public class VT100 extends JComponent {
	class CursorRepaintTask implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			cursorBlink.toggleVisible();

			// 游標閃爍
			if (resource.getBooleanValue(Config.CURSOR_BLINK)) {
				setRepaint(crow, ccol);
				VT100.this.repaint();
			}
		}
	}

	class TextRepaintTask implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			int prow;
			boolean isNeedRepaint = false;

			textBlink.toggleVisible();

			// 文字閃爍
			for (int i = 1; i <= maxrow; i++) {
				for (int j = 1; j <= maxcol; j++) {
					prow = physicalRow(i - scrolluprow);
					if ((attributes[prow][j - 1] & VT100.BLINK) != 0) {
						setRepaintPhysical(prow, j - 1);
						isNeedRepaint = true;
					}
				}
			}

			if (isNeedRepaint) {
				VT100.this.repaint();
			}
		}
	}

	public static final int			APPLICATION_KEYPAD	= 2;

	public static final int			NUMERIC_KEYPAD		= 1;

	private static final byte		BACKGROUND			= 1;
	private static final byte		BLINK				= 16;
	// 各種屬性
	private static final byte		BOLD				= 1;
	private static final byte		CURSOR				= 2;
	private static Color			cursorColor			= Color.GREEN;
	private final static boolean	DEBUG				= false;

	private static byte				defAttr				= 0;

	private static byte				defBg				= 0;

	// 預設顏色設定，前景、背景、游標色
	private static byte				defFg				= 7;
	// 取得色彩用的一些狀態
	private static final byte		FOREGROUND			= 0;
	private static Color[]			highlight_colors	= {
			new Color(128, 128, 128),
			new Color(255, 0, 0),
			new Color(0, 255, 0),
			new Color(255, 255, 0),
			new Color(0, 0, 255),
			new Color(255, 0, 255),
			new Color(0, 255, 255),
			new Color(255, 255, 255),
														};

	// 調色盤
	private static Color[]			normal_colors		= {
			new Color(0, 0, 0),
			new Color(128, 0, 0),
			new Color(0, 128, 0),
			new Color(128, 128, 0),
			new Color(0, 0, 128),
			new Color(128, 0, 128),
			new Color(0, 128, 128),
			new Color(192, 192, 192),
														};

	private static final byte		REVERSE				= 64;

	private static final long		serialVersionUID	= -5704767444883397941L;

	private static final byte		UNDERLINE			= 8;
	private static final byte		URL					= 3;
	private static Color			urlColor			= Color.ORANGE;
	private byte[]					attrBuf, fgBuf, bgBuf;
	private byte[][]				attributes;									// 屬性

	private byte[][]				bgcolors;										// 背景色
	// 畫面
	private BufferedImage			bi;
	// 目前的屬性及前景、背景色
	private byte					cattribute;
	// 目前、上次、儲存的游標所在位址
	private int						ccol, crow;
	private byte					cfgcolor, cbgcolor;
	// 轉碼用
	private final Convertor			conv;

	private int						debug_counter		= 0;
	private String					emulation;

	private String					encoding;
	private byte[][]				fgcolors;										// 前景色

	// 各種字型參數
	private Font					font;
	private int						fontsize;
	// 字元的垂直與水平間距
	private int						fontverticalgap, fonthorizontalgap,
			fontdescentadj;
	private int						fontwidth, fontheight, fontdescent;

	// 紀錄是否所有初始化動作皆已完成
	private boolean					init_ready;
	// 判斷畫面上的網址
	private boolean[][]				isurl;

	private int						keypadmode;
	private int						lcol, lrow;
	// 記錄游標是否位於最後一個字上，非最後一個字的下一個
	private boolean					linefull;

	// 模擬螢幕的相關資訊
	private int						maxrow, maxcol;								// terminal 的大小

	private int[][]					mbc;											// multibyte character 的第幾個 byte
	// 把從 nvt 來的資料暫存起來
	private byte[]					nvtBuf;
	private int						nvtBufPos, nvtBufLen;

	private final Session		parent;
	private Object					repaintLock;
	// 記錄螢幕上何處需要 repaint
	private FIFOSet					repaintSet;

	// 各種參數
	private final Config			resource;
	private int						scol, srow;
	private int						scrolllines;									// scroll buffer 的行數
	private int						scrolluprow;									// 往上捲的行數

	// ASCII
	// private static final byte BEL = 7;
	// private static final byte BS = 8;
	// private static final byte TAB = 9;
	// private static final byte LF = 10;
	// private static final byte VT = 11;
	// private static final byte FF = 12;
	// private static final byte CR = 13;
	// private static final byte SO = 14;
	// private static final byte SI = 15;

	// 是否被滑鼠選取
	private boolean[][]				selected;
	// 儲存螢幕上的相關資訊
	private char[][]				text;											// 轉碼後的 char

	// 閃爍用
	private final Blink				textBlink			= new Blink();
	private final Blink				cursorBlink			= new Blink();

	// multibytes char 暫存
	private byte[]					textBuf;
	private int						textBufPos;

	// 重繪用的 Timer
	private Timer					cursorBlinkTimer, textBlinkTimer;
	private int						topmargin, buttommargin, leftmargin,
			rightmargin;

	// 第一 row 所在位置
	private int						toprow;

	// 總 row, col 數，包含 scroll buffer
	private int						totalrow, totalcol;

	// 螢幕 translate 的座標
	private int						transx, transy;

	// 處理來自使用者的事件
	private User					user;

	// 畫面的寬與高
	private int						width, height;

	public VT100(final Session p, final Config c, final Convertor cv,
			final BufferedImage b) {
		super();

		parent = p;
		resource = c;
		conv = cv;
		bi = b;

		// 初始化一些變數、陣列
		initValue();
		initArray();
		initOthers();
	}

	public void close() {
		// 停止重繪用的 timer
		cursorBlinkTimer.stop();
		textBlinkTimer.stop();

		// 停止來自使用者的事件
		removeKeyListener(user);
		removeMouseListener(user);
		removeMouseMotionListener(user);
	}

	/**
	 * 滑鼠游標是否在網址上
	 * 
	 * @param x
	 * @param y
	 * @return true if (x,y) is on a URL; false, otherwise
	 */
	public boolean coverURL(int x, int y) {
		int c, r;
		int prow;

		x -= transx;
		y -= transy;

		c = x / fontwidth + 1;
		r = y / fontheight + 1;

		// 超出螢幕範圍
		if ((r < 1) || (r > maxrow) || (c < 1) || (c > maxcol)) {
			return false;
		}

		prow = physicalRow(r - scrolluprow);

		return isurl[prow][c - 1];
	}

	public String getEmulation() {
		return emulation;
	}

	public String getEncoding() {
		return encoding;
	}

	public int getKeypadMode() {
		return keypadmode;
	}

	@Override
	public Dimension getPreferredSize() {
		// FIXME: magic number
		return new Dimension(800, 600);
	}

	public String getSelectedColorText() {
		// TODO: 這裡寫的不太好，應該再改進
		int i, j, k, last;
		byte[] buf;
		final Vector a = new Vector(); // attributes
		final Vector b = new Vector(); // bytes
		final Vector fg = new Vector(); // foreground color
		final Vector bg = new Vector(); // background color

		for (i = 0; i < totalrow; i++) {

			// 若整行都沒被選取，直接換下一行
			for (j = 0; j < totalcol; j++) {
				if (selected[i][j]) {
					break;
				}
			}
			if (j == totalcol) {
				continue;
			}

			// 找到最後一個有資料的地方
			for (last = totalcol - 1; last >= 0; last--) {
				if (selected[i][last] && (mbc[i][last] != 0)) {
					break;
				}
			}

			for (j = 0; j <= last; j++) {
				if (selected[i][j]) {
					if (mbc[i][j] == 0) {
						// 後面還有資料，沒資料的部份用空白取代
						a.addElement(new Byte((byte) 0));
						b.addElement(new Byte((byte) ' '));
						fg.addElement(new Byte(VT100.defFg));
						bg.addElement(new Byte(VT100.defBg));
					} else if (mbc[i][j] == 1) {
						buf = conv.charToBytes(text[i][j], encoding);
						for (k = 0; k < buf.length; k++) {
							b.addElement(new Byte(buf[k]));
							// XXX: 因為最多使用兩格儲存屬性，若超過 2 bytes, 則以第二 bytes 屬性取代之。
							a.addElement(new Byte(attributes[i][j
									+ Math.min(k, 2)]));
							fg.addElement(new Byte(fgcolors[i][j
									+ Math.min(k, 2)]));
							bg.addElement(new Byte(bgcolors[i][j
									+ Math.min(k, 2)]));
						}
					}
				}
			}
			if (true) {
				a.addElement(new Byte((byte) 0));
				b.addElement(new Byte((byte) 0x0d));
				fg.addElement(new Byte(VT100.defFg));
				bg.addElement(new Byte(VT100.defBg));
			}
		}

		return makePasteText(a, b, fg, bg);
	}

	/**
	 * 複製選取的文字
	 * 
	 * @return the selected text
	 */
	public String getSelectedText() {
		// TODO: 這裡寫的不太好，應該再改進
		int i, j, k;
		boolean firstLine = true;
		final StringBuffer sb = new StringBuffer();

		for (i = 0; i < totalrow; i++) {

			// 若整行都沒被選取，直接換下一行
			for (j = 0; j < totalcol; j++) {
				if (selected[i][j]) {
					break;
				}
			}
			if (j == totalcol) {
				continue;
			}

			// 除了第一個被選取行外，其餘每行開始前先加上換行
			if (firstLine) {
				firstLine = false;
			} else {
				sb.append("\n");
			}

			// 找到最後一個有資料的地方
			for (j = totalcol - 1; j >= 0; j--) {
				if (selected[i][j] && (mbc[i][j] != 0)) {
					break;
				}
			}

			for (k = 0; k <= j; k++) {
				// 只複製選取的部份
				if (selected[i][k]) {
					if (mbc[i][k] == 0) {
						// 後面還有資料，雖然沒資料但先替換成空白
						sb.append(" ");
					} else if (mbc[i][k] == 1) {
						sb.append(text[i][k]);
					}
				}
			}

		}

		return sb.toString();
	}

	/**
	 * 取得滑鼠游標處的網址
	 * 
	 * @param x
	 * @param y
	 * @return the URL on (x,y)
	 */
	public String getURL(int x, int y) {
		final StringBuffer sb = new StringBuffer();
		int i;
		int c, r;
		int prow;

		x -= transx;
		y -= transy;

		c = x / fontwidth + 1;
		r = y / fontheight + 1;

		// 超出螢幕範圍
		if ((r < 1) || (r > maxrow) || (c < 1) || (c > maxcol)) {
			return new String();
		}

		prow = physicalRow(r - scrolluprow);

		// TODO: 可複製跨行的 url
		for (i = c; (i > 0) && isurl[prow][i - 1]; i--) {
			;
		}
		for (i++; (i <= maxcol) && isurl[prow][i - 1]; i++) {
			if (mbc[prow][i - 1] == 1) {
				sb.append(text[prow][i - 1]);
			}
		}

		return sb.toString();
	}

	/**
	 * Paste the messages as color text to the session.
	 * 
	 * @param str
	 *            color text
	 */
	public void pasteColorText(final String str) {
		final byte[] tmp = new byte[str.length()];

		for (int i = 0; i < str.length(); i++) {
			tmp[i] = (byte) str.charAt(i);
		}

		parent.writeBytes(tmp, 0, tmp.length);
	}

	/**
	 * 貼上文字
	 * 
	 * @param str
	 */
	public void pasteText(String str) {
		char[] cArr;

		boolean autobreak;
		int breakcount;

		// 從系統取得的字串可能是 null
		if (str == null) {
			return;
		}

		autobreak = resource.getBooleanValue(Config.AUTO_LINE_BREAK);
		breakcount = resource.getIntValue(Config.AUTO_LINE_BREAK_LENGTH);

		if (autobreak) {
			str = TextUtils.fmt(str, breakcount);
		}

		cArr = str.toCharArray();

		// XXX: ptt 只吃 0x0d 當換行
		// TODO: 需判斷 0x0a 0x0d 的狀況，目前可能會送出兩個 0x0d
		for (int i = 0; i < cArr.length; i++) {
			if (cArr[i] == 0x0a) {
				cArr[i] = 0x0d;
			}
		}

		parent.writeChars(cArr, 0, cArr.length);
	}

	/**
	 * 重設選取區域
	 */
	public void resetSelected() {
		for (int i = 0; i < totalrow; i++) {
			for (int j = 0; j < totalcol; j++) {
				if (selected[i][j]) {
					selected[i][j] = false;
					setRepaintPhysical(i, j);
				}
			}
		}
	}

	public void run() {
		// 連線後自動取得 focus
		this.requestFocusInWindow();

		// 至此應該所有的初始化動作都完成了
		init_ready = true;

		while (!parent.isDisconnected()) {
			parse();

			// buffer 裡的東西都處理完才重繪
			if (isBufferEmpty()) {
				this.repaint();
			}
		}
	}

	/**
	 * 選取連續的文字
	 * 
	 * @param x
	 * @param y
	 */
	public void selectConsequtive(int x, int y) {
		int c, r;
		int i, beginx, endx;
		int prow;

		x -= transx;
		y -= transy;

		c = x / fontwidth + 1;
		r = y / fontheight + 1;

		// 超出螢幕範圍
		if ((c < 1) || (c > maxcol) || (r < 1) || (r > maxrow)) {
			return;
		}

		prow = physicalRow(r - scrolluprow);

		// 往前找到第一個非空白的合法字元
		for (beginx = c; beginx > 0; beginx--) {
			if ((mbc[prow][beginx - 1] == 0)
					|| ((mbc[prow][beginx - 1] == 1) && (text[prow][beginx - 1] == ' '))) {
				break;
			}
		}
		// 向後 ...
		for (endx = c; endx <= maxcol; endx++) {
			if ((mbc[prow][endx - 1] == 0)
					|| ((mbc[prow][endx - 1] == 1) && (text[prow][endx - 1] == ' '))) {
				break;
			}
		}

		resetSelected();
		// FIXME: 這裡還需要一些測試
		for (i = beginx + 1; i < endx; i++) {
			selected[prow][i - 1] = true;
			setRepaintPhysical(prow, i - 1);
		}
	}

	/**
	 * 選取整行
	 * 
	 * @param x
	 * @param y
	 */
	public void selectEntireLine(int x, int y) {
		int c, r;
		int prow;

		x -= transx;
		y -= transy;

		c = x / fontwidth + 1;
		r = y / fontheight + 1;

		// 超出螢幕範圍
		if ((c < 1) || (c > maxcol) || (r < 1) || (r > maxrow)) {
			return;
		}

		resetSelected();
		prow = physicalRow(r - scrolluprow);
		for (int i = 1; i < maxcol; i++) {
			selected[prow][i - 1] = true;
			setRepaintPhysical(prow, i - 1);
		}
	}

	@Override
	public void setBounds(final int x, final int y, final int w, final int h) {
		// layout manager 或其他人可能會透過 setBound 來改變 component 的大小，
		// 此時要一併更新 component
		super.setBounds(x, y, w, h);
		updateSize();
	}

	@Override
	public void setBounds(final Rectangle r) {
		super.setBounds(r);
		updateSize();
	}

	/**
	 * 設定模擬終端機的類型
	 * 
	 * @param emu
	 */
	public void setEmulation(final String emu) {
		// XXX: vt100 對各種 terminal type 的處理都相同，不需通知
		emulation = emu;
	}

	/**
	 * 設定 encoding
	 * 
	 * @param enc
	 */
	public void setEncoding(final String enc) {
		encoding = enc;
	}

	/**
	 * 設定目前往上捲的行數
	 * 
	 * @param scroll
	 *            行數
	 */
	public void setScrollUp(final int scroll) {
		scrolluprow = scroll;
		// System.out.println( "scroll up " + scroll + " lines" );
		// TODO: 應改可以不用每次都重繪整個畫面
		updateScreen();
	}

	/**
	 * 設定選取區域
	 * 
	 * @param x1
	 *            開始的 x 座標
	 * @param y1
	 *            開始的 y 座標
	 * @param x2
	 *            結束的 x 座標
	 * @param y2
	 *            結束的 y 座標
	 */
	public void setSelected(int x1, int y1, int x2, int y2) {
		int i, j;
		int r1, c1, r2, c2, tmp;
		int prow;
		boolean orig;

		x1 -= transx;
		y1 -= transy;
		x2 -= transx;
		y2 -= transy;

		c1 = x1 / fontwidth + 1;
		r1 = y1 / fontheight + 1;

		c2 = x2 / fontwidth + 1;
		r2 = y2 / fontheight + 1;

		if (r1 > r2) {
			tmp = r1;
			r1 = r2;
			r2 = tmp;

			tmp = c1;
			c1 = c2;
			c2 = tmp;
		} else if (r1 == r2) {
			if (c1 > c2) {
				tmp = c1;
				c1 = c2;
				c2 = tmp;
			}
		}

		resetSelected();

		// TODO: 只能選取當前畫面的內容，不會自動捲頁
		for (i = 1; i <= maxrow; i++) {
			for (j = 1; j <= maxcol; j++) {

				prow = physicalRow(i - scrolluprow);

				orig = selected[prow][j - 1];

				if ((i > r1) && (i < r2)) {
					selected[prow][j - 1] = true;
				} else if ((i == r1) && (i == r2)) {
					selected[prow][j - 1] = (j >= c1) && (j <= c2);
				} else if (i == r1) {
					selected[prow][j - 1] = (j >= c1);
				} else if (i == r2) {
					selected[prow][j - 1] = (j <= c2);
				} else {
					selected[prow][j - 1] = false;
				}

				if (selected[prow][j - 1] != orig) {
					setRepaintPhysical(prow, j - 1);
				}
			}
		}

	}

	/**
	 * 設定選取所有可見區域
	 */
	public void setSelectedAll() {
		for (int i = 1; i <= maxrow; i++) {
			for (int j = 1; j <= maxcol; j++) {
				final int prow = physicalRow(i - scrolluprow);
				final boolean originalSelected = selected[prow][j - 1];
				selected[prow][j - 1] = true;

				if (selected[prow][j - 1] != originalSelected) {
					setRepaintPhysical(prow, j - 1);
				}
			}
		}
	}

	/**
	 * updateFont 更新字型相關資訊
	 */
	public void updateFont() {
		FontMetrics fm;
		int fh, fw;

		String family;
		int style;

		// 微調
		fonthorizontalgap = resource.getIntValue(Config.FONT_HORIZONTAL_GAP);
		fontverticalgap = resource.getIntValue(Config.FONT_VERTICLAL_GAP);
		fontdescentadj = resource.getIntValue(Config.FONT_DESCENT_ADJUST);

		// 設定 family
		family = resource.getStringValue(Config.FONT_FAMILY);

		// 設定 size
		fontsize = resource.getIntValue(Config.FONT_SIZE);
		if (fontsize == 0) {
			// 按照螢幕的大小設定
			fw = width / maxcol - fonthorizontalgap;
			fh = height / maxrow - fontverticalgap;

			if (fh > 2 * fw) {
				fh = 2 * fw;
			}
			fontsize = fh;
		}

		// 設定 style（bold, italy, plain）
		style = Font.PLAIN;
		if (resource.getBooleanValue(Config.FONT_BOLD)) {
			style |= Font.BOLD;
		}
		if (resource.getBooleanValue(Config.FONT_ITALY)) {
			style |= Font.ITALIC;
		}

		// 建立 font instance
		font = new Font(family, style, fontsize);

		fm = getFontMetrics(font);

		// XXX: 這裡對 fontheight 與 fontwidth 的假設可能有問題
		fontheight = fontsize;
		fontwidth = fontsize / 2;
		fontdescent = (int) (1.0 * fm.getDescent() / fm.getHeight() * fontsize);

		fontheight += fontverticalgap;
		fontwidth += fonthorizontalgap;
		fontdescent += fontdescentadj;

		// 修改字型會影響 translate 的座標
		transx = (width - fontwidth * maxcol) / 2;
		transy = (height - fontheight * maxrow) / 2;
	}

	public void updateImage(final BufferedImage b) {
		bi = b;
	}

	/**
	 * 重繪目前的畫面
	 */
	public void updateScreen() {
		for (int i = 1; i <= maxrow; i++) {
			for (int j = 1; j <= maxcol; j++) {
				setRepaintPhysical(physicalRow(i - scrolluprow), j - 1);
			}
		}

		this.repaint();
	}

	public void updateSize() {
		width = getWidth();
		height = getHeight();

		updateFont();
		updateScreen();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		// 因為多個分頁共用一張 image, 因此只有在前景的分頁才有繪圖的權利，
		// 不在前景時不重繪，以免干擾畫面。
		// 初始化完成之前不重繪。
		if (!parent.isTabForeground() || !init_ready) {
			return;
		}

		// 檢查在螢幕上面的 URL 連結
		checkURLOnScreen();

		// TODO: 考慮 draw 是否一定要擺在這邊，或是是否只在這裡呼叫？
		// 偶爾呼叫一次而不只是在顯示前才呼叫應該可以增進顯示速度。
		draw();

		g.drawImage(bi, 0, 0, null);
	}

	/**
	 * 發出一個 bell
	 */
	private void bell() {
		parent.bell();
	}

	private void checkURLOnScreen() {
		for (int i = 0; i < maxrow; i++) {
			final int prow = physicalRow(i - scrolluprow + 1);
			final String message = String.valueOf(text[prow]);

			for (int j = 0; j < maxcol; j++) {
				final boolean originalIsURL = isurl[prow][j];

				// 判斷 (prow, j) 座標上的字元是否落於一個 http://xxx.xxx 字串裡面
				if (UrlRecognizer.isPartOfHttp(message, j) || UrlRecognizer.isPartOfHttps(message, j)) {
					isurl[prow][j] = true;
				} else {
					isurl[prow][j] = false;
				}

				final boolean updatedIsURL = isurl[prow][j];
				if (originalIsURL != updatedIsURL) {
					setRepaintPhysical(prow, j);
				}
			}
		}
	}

	private void copy(final int dstrow, final int dstcol, final int srcrow,
			final int srccol) {
		int pdstrow, psrcrow;

		pdstrow = physicalRow(dstrow);
		psrcrow = physicalRow(srcrow);

		text[pdstrow][dstcol - 1] = text[psrcrow][srccol - 1];
		mbc[pdstrow][dstcol - 1] = mbc[psrcrow][srccol - 1];
		fgcolors[pdstrow][dstcol - 1] = fgcolors[psrcrow][srccol - 1];
		bgcolors[pdstrow][dstcol - 1] = bgcolors[psrcrow][srccol - 1];
		attributes[pdstrow][dstcol - 1] = attributes[psrcrow][srccol - 1];
		isurl[pdstrow][dstcol - 1] = isurl[psrcrow][srccol - 1];

		setRepaint(dstrow, dstcol);
	}

	/**
	 * 從目前位置（包含）刪除數個字
	 * 
	 * @param n
	 */
	private void delete_characters(final int n) {
		// System.out.println( "delete " + n + " characters, at (" + crow + ", "
		// + ccol + ")" );

		// 目前位置加 n 到行尾的字元往前移，後面則清除
		for (int i = ccol; i <= maxcol; i++) {
			if (i <= maxcol - n) {
				copy(crow, i, crow, i + n);
			} else {
				reset(crow, i);
			}
		}
	}

	/**
	 * 從目前位置（包含）刪除數行
	 * 
	 * @param n
	 */
	private void delete_lines(final int n) {
		int i, j;

		// System.out.println( "delete " + n + " lines, at (" + crow + ", " +
		// ccol + ")" );

		// 刪除的部份超過 buttommargin, 從目前位置到 buttommargin 都清除
		if (crow + n > buttommargin) {
			for (i = crow; i <= buttommargin; i++) {
				eraseline(i, 2);
			}
			return;
		}

		// 目前行號加 n 到 buttommargin 全部往前移，後面則清除。
		for (i = crow + n; i <= buttommargin; i++) {
			for (j = leftmargin; j <= rightmargin; j++) {
				copy(i - n, j, i, j);
			}
		}
		for (i = buttommargin - n + 1; i <= buttommargin; i++) {
			eraseline(i, 2);
		}
	}

	private void draw() {
		int w, h;
		int v, prow, pcol;
		int row, col;
		Graphics2D g;
		boolean show_cursor, show_text, show_underline;

		g = bi.createGraphics();
		g.setFont(font);

		// 畫面置中
		g.translate(transx, transy);

		// 設定 Anti-alias
		if (resource.getBooleanValue(Config.FONT_ANTIALIAS)) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}

		while (!repaintSet.isEmpty()) {
			// 取得下一個需要重繪的位置
			synchronized (repaintLock) {
				v = repaintSet.remove();
				prow = v >> 8;
				pcol = v & 0xff;
			}

			// 取得待重繪的字在畫面上的位置
			// 加上捲動的判斷
			row = logicalRow(prow);
			col = pcol + 1;

			// 若是需重繪的部份不在顯示範圍內則不理會
			if ((row < 1) || (row > maxrow) || (col < 1) || (col > maxcol)) {
				continue;
			}

			// 本次待繪字元的左上角座標
			h = (row - 1) * fontheight;
			w = (col - 1) * fontwidth;

			// 閃爍控制與色彩屬性
			show_text = ((attributes[prow][pcol] & VT100.BLINK) == 0)
					|| textBlink.isVisible();
			show_cursor = (physicalRow(crow) == prow) && (ccol == col)
					&& cursorBlink.isVisible();
			show_underline = (attributes[prow][pcol] & VT100.UNDERLINE) != 0;

			// 填滿背景色
			g.setColor(getColor(prow, pcol, VT100.BACKGROUND));
			g.fillRect(w, h, fontwidth, fontheight);

			// 如果是游標所在處就畫出游標
			if (show_cursor) {
				// TODO: 多幾種游標形狀
				g.setColor(getColor(prow, pcol, VT100.CURSOR));
				g.fillRect(w, h, fontwidth, fontheight);
			}

			// 空白不重繪前景文字，離開
			if (mbc[prow][pcol] == 0) {
				continue;
			}

			// 設為前景色
			g.setColor(getColor(prow, pcol, VT100.FOREGROUND));

			// 畫出文字
			if (show_text) {
				// 利用 clip 的功能，只畫出部份（半個）中文字。
				// XXX: 每個中文都會畫兩次，又有 clip 的 overhead, 效率應該會受到蠻大的影響！
				final Shape oldclip = g.getClip();
				final char character = text[prow][pcol - mbc[prow][pcol] + 1];
				final int x = w - fontwidth * (mbc[prow][pcol] - 1);
				final int y = h + fontheight - fontdescent;
				g.clipRect(w, h, fontwidth, fontheight);
				g.drawString(Character.toString(character), x, y);
				g.setClip(oldclip);
			}

			// 畫出底線
			if (show_underline || isurl[prow][pcol]) {
				if (isurl[prow][pcol]) {
					g.setColor(getColor(prow, pcol, VT100.URL));
				}
				g.drawLine(w, h + fontheight - 1, w + fontwidth - 1, h
						+ fontheight - 1);
			}
		}

		g.dispose();
	}

	/**
	 * 清除特定行
	 * 
	 * @param row
	 * @param mode
	 */
	private void eraseline(final int row, final int mode) {
		int i, begin, end;

		// System.out.println("erase line: " + row );

		switch (mode) {
		case 0:
			begin = ccol;
			end = rightmargin;
			break;
		case 1:
			begin = leftmargin;
			end = ccol;
			break;
		case 2:
			begin = leftmargin;
			end = rightmargin;
			break;
		default:
			begin = leftmargin;
			end = rightmargin;
			break;
		}

		for (i = begin; i <= end; i++) {
			reset(row, i);
		}
	}

	/**
	 * 清除螢幕
	 * 
	 * @param mode
	 */
	private void erasescreen(final int mode) {
		int i, begin, end;

		// XXX: 這裡該用 maxrow 還是 buttommargin?
		switch (mode) {
		case 0:
			eraseline(crow, mode);
			begin = crow + 1;
			end = maxrow;
			break;
		case 1:
			eraseline(crow, mode);
			begin = 1;
			end = crow - 1;
			break;
		case 2:
			begin = 1;
			end = maxrow;
			break;
		default:
			begin = 1;
			end = maxrow;
			break;
		}

		for (i = begin; i <= end; i++) {
			eraseline(i, 2);
		}
	}

	private Color getColor(final int prow, final int pcol, final byte mode) {
		Color c;
		boolean bold, reverse;

		bold = (attributes[prow][pcol] & VT100.BOLD) != 0;
		reverse = selected[prow][pcol]
				^ ((attributes[prow][pcol] & VT100.REVERSE) != 0);

		if (((mode == VT100.FOREGROUND) && !reverse)
				|| ((mode == VT100.BACKGROUND) && reverse)) {
			// 前景色
			if (bold) {
				c = VT100.highlight_colors[fgcolors[prow][pcol]];
			} else {
				c = VT100.normal_colors[fgcolors[prow][pcol]];
			}
		} else if (((mode == VT100.BACKGROUND) && !reverse)
				|| ((mode == VT100.FOREGROUND) && reverse)) {
			// 背景色
			c = VT100.normal_colors[bgcolors[prow][pcol]];
		} else if (mode == VT100.CURSOR) {
			// 游標色
			c = VT100.cursorColor;
		} else if (mode == VT100.URL) {
			// 網址色
			c = VT100.urlColor;
		} else {
			// 錯誤
			System.err.println("Unknown color mode!");
			c = Color.WHITE;
		}

		return c;
	}

	/**
	 * 取得下一個需要被處理的 byte
	 * 
	 * @return
	 */
	private byte getNextByte() {
		// buffer 用光了，再跟下層拿。
		// 應該用 isBufferEmpty() 判斷的，但為了效率直接判斷。
		if (nvtBufPos == nvtBufLen) {
			nvtBufLen = parent.readBytes(nvtBuf);
			// 連線終止或錯誤，應盡快結束 parse()
			if (nvtBufLen == -1) {
				return 0;
			}
			nvtBufPos = 0;
		}

		return nvtBuf[nvtBufPos++];
	}

	private void initArray() {
		int i, j;

		// 從上層讀取資料用的 buffer
		nvtBuf = new byte[4096];
		nvtBufPos = nvtBufLen = 0;

		text = new char[totalrow][totalcol];
		mbc = new int[totalrow][totalcol];
		fgcolors = new byte[totalrow][totalcol];
		bgcolors = new byte[totalrow][totalcol];
		attributes = new byte[totalrow][totalcol];
		selected = new boolean[totalrow][totalcol];
		isurl = new boolean[totalrow][totalcol];

		textBuf = new byte[4];
		attrBuf = new byte[4];
		fgBuf = new byte[4];
		bgBuf = new byte[4];
		textBufPos = 0;

		// 初始化記載重繪位置用 FIFOSet
		// XXX: 假設 column 數小於 256
		repaintSet = new FIFOSet(totalrow << 8);
		repaintLock = new Object();

		for (i = 0; i < totalrow; i++) {
			for (j = 0; j < totalcol; j++) {
				text[i][j] = ((char) 0);
				mbc[i][j] = 0;

				fgcolors[i][j] = VT100.defFg;
				bgcolors[i][j] = VT100.defBg;
				attributes[i][j] = VT100.defAttr;
				isurl[i][j] = false;
			}
		}

		for (i = 1; i < maxrow; i++) {
			for (j = 1; j < maxcol; j++) {
				setRepaint(i, j);
			}
		}
	}

	private void initOthers() {
		// 進入 run() 以後才確定初始化完成
		init_ready = false;

		// 啟動閃爍控制 thread
		cursorBlinkTimer = new Timer(500, new CursorRepaintTask());
		cursorBlinkTimer.start();

		textBlinkTimer = new Timer(1000, new TextRepaintTask());
		textBlinkTimer.start();

		// 取消 focus traversal key, 這樣才能收到 tab.
		setFocusTraversalKeysEnabled(false);

		// Input Method Framework, set passive-client
		enableInputMethods(true);

		// 設定預設大小
		// FIXME: magic number
		this.setSize(new Dimension(800, 600));

		// User
		user = new User(parent, this, resource);

		addKeyListener(user);
		addMouseListener(user);
		addMouseMotionListener(user);
	}

	private void initValue() {
		// 讀入模擬終端機的大小，一般而言是 80 x 24
		maxcol = resource.getIntValue(Config.TERMINAL_COLUMNS);
		maxrow = resource.getIntValue(Config.TERMINAL_ROWS);

		// 讀入 scroll buffer 行數
		scrolllines = resource.getIntValue(Config.TERMINAL_SCROLLS);

		// 所需要的陣列大小
		totalrow = maxrow + scrolllines;
		totalcol = maxcol;

		// 一開始環狀佇列的起始點在 0
		toprow = 0;

		// 預設 margin 為整個螢幕
		topmargin = 1;
		buttommargin = maxrow;
		leftmargin = 1;
		rightmargin = maxcol;

		// 游標起始位址在螢幕左上角處
		ccol = crow = 1;
		lcol = lrow = 1;
		scol = srow = 1;

		// 設定目前色彩為預設值
		cfgcolor = VT100.defFg;
		cbgcolor = VT100.defBg;
		cattribute = VT100.defAttr;

		linefull = false;

		keypadmode = VT100.NUMERIC_KEYPAD;
	}

	/**
	 * 在目前的位置插入數個空白
	 * 
	 * @param n
	 */
	private void insert_space(final int n) {
		// System.out.println( "insert " + n + " space, at (" + crow + ", " +
		// ccol + ")" );

		for (int i = rightmargin; i >= ccol; i--) {
			if (i >= ccol + n) {
				copy(crow, i, crow, i - n);
			} else {
				reset(crow, i);
			}
		}
	}

	/**
	 * 在特定行之後插入空行
	 * 
	 * @param r
	 * @param n
	 */
	private void insertline(final int r, final int n) {
		// System.out.println( "insert " + n + " line after " + r + " line");
		for (int i = buttommargin; i >= r; i--) {
			for (int j = leftmargin; j <= rightmargin; j++) {
				if (i >= r + n) {
					copy(i, j, i - n, j);
				} else {
					reset(i, j);
				}
			}
		}
	}

	private void insertTextBuf() {
		// XXX: 表格內有些未知字元會填入 '?', 因此可能會有 c < 127 但 textBufPos > 1 的狀況。
		final char c = conv.bytesToChar(textBuf, 0, textBufPos, encoding);
		final boolean isWide = Convertor.isWideChar(c);

		// 一般而言游標都在下一個字將會出現的地方，但若最後一個字在行尾（下一個字應該出現在行首），
		// 游標會在最後一個字上，也就是當最後一個字出現在行尾時並不會影響游標位置，
		// 游標會等到下一個字出現時才移到下一行。
		if (linefull || (isWide && (ccol + 1 > rightmargin))) {
			linefull = false;
			ccol = leftmargin;
			crow++;
			if (crow > buttommargin) {
				scrollpage(1);
				crow--;
			}

			// 游標會跳過行尾，所以需要手動 setRepaint
			setRepaint(crow, leftmargin);
		}

		// 一個 char 可能對應數個 bytes, 但在顯示及儲存時最雙寬字多佔兩格，單寬字最多佔一格，
		// 紀錄 char 後要把對應的屬性及色彩等資料從 buffer 複製過來，並設定重繪。
		final int prow = physicalRow(crow);
		text[prow][ccol - 1] = c;

		// 紀錄暫存的資料，寬字元每個字最多用兩個 bytes，一般字元每字一個 byte
		for (int i = 0; i < (isWide ? Math.min(textBufPos, 2) : 1); i++) {
			fgcolors[prow][ccol + i - 1] = fgBuf[i];
			bgcolors[prow][ccol + i - 1] = bgBuf[i];
			attributes[prow][ccol + i - 1] = attrBuf[i];
			mbc[prow][ccol + i - 1] = i + 1;

			// isurl 不同於 color 與 attribute, isurl 是在 setURL 內設定。
			isurl[prow][ccol + i - 1] = false;

			setRepaint(crow, ccol + i);
		}

		// 重設 textBufPos
		textBufPos = 0;

		// 控制碼不會讓游標跑到 rightmargin 以後的地方，只有一般字元會，所以在這裡判斷 linefull 就可以了。
		ccol++;
		if (isWide) {
			ccol++;
		}
		if (ccol > rightmargin) {
			linefull = true;
			ccol--;
		}
	}

	/**
	 * buffer 是否是空的
	 * 
	 * @return
	 */
	private boolean isBufferEmpty() {
		return (nvtBufPos == nvtBufLen);
	}

	private boolean isControlChar(final char c) {
		// 若讀入的字元小於 32 則視為控制字元。其實應該用列舉的，但這麼寫比較漂亮。
		return (c >= 0) && (c < 32);
	}

	/**
	 * 計算 prow 在目前畫面中的 row NOTE: 這不是 physicalRow 的反函數
	 * 
	 * @param prow
	 * @return
	 */
	private int logicalRow(final int prow) {
		int row, tmptop = toprow - scrolluprow;
		if (tmptop < 0) {
			tmptop += totalrow;
		}
		row = prow - tmptop + 1;
		if (row < 1) {
			row += totalrow;
		}

		return row;
	}

	private String makePasteText(final Vector a, final Vector b,
			final Vector fg, final Vector bg) {
		int i, j, c;

		byte cattr, tmpattr;
		byte cfg, cbg, tmpfg, tmpbg;
		byte mask;

		boolean needReset, needControl, isFirst;
		final StringBuffer sb = new StringBuffer();

		// FIXME: magic number
		final char[] buf = new char[32];

		buf[0] = 21;
		buf[1] = '[';
		buf[2] = 'm';
		sb.append(buf, 0, 3);

		cattr = 0;
		cfg = VT100.defFg;
		cbg = VT100.defBg;

		for (i = 0; i < a.size(); i++) {

			tmpattr = ((Byte) a.elementAt(i)).byteValue();
			tmpfg = ((Byte) fg.elementAt(i)).byteValue();
			tmpbg = ((Byte) bg.elementAt(i)).byteValue();

			for (mask = 1, j = 0, needReset = needControl = false; j < 7; j++) {
				// 如果有屬性消失了，就得 reset
				if (((cattr & (mask << j)) != 0)
						&& ((tmpattr & (mask << j)) == 0)) {
					needReset = true;
				}

				// 如果有屬性改變，則需要加入控制碼
				if ((cattr & (mask << j)) != (tmpattr & (mask << j))) {
					needControl = true;
				}

				// 如果顏色變了，也需要加入控制碼
				if ((cfg != tmpfg) || (cbg != tmpbg)) {
					needControl = true;
				}
			}

			// 顏色、屬性都沒變
			if (!needControl) {
				sb.append((char) ((Byte) b.elementAt(i)).byteValue());
				continue;
			}

			// 如果需要控制碼才往下作

			buf[0] = 21;
			buf[1] = '[';

			if (needReset) {
				buf[2] = '0';

				for (mask = 1, j = 0, c = 3; j < 7; j++) {
					if ((tmpattr & (mask << j)) != 0) {
						buf[c++] = ';';
						buf[c++] = (char) ('1' + j);
					}
				}

				// 若是顏色跟預設的不同則需要設定
				if (tmpfg != VT100.defFg) {
					buf[c++] = ';';
					buf[c++] = '3';
					buf[c++] = (char) (tmpfg + '0');
				}

				if (tmpbg != VT100.defBg) {
					buf[c++] = ';';
					buf[c++] = '4';
					buf[c++] = (char) (tmpbg + '0');
				}

			} else {

				isFirst = true;
				for (mask = 1, j = 0, c = 2; j < 7; j++) {

					// 有新的屬性
					if (((tmpattr & (mask << j)) != 0)
							&& ((cattr & (mask << j)) == 0)) {
						if (isFirst) {
							isFirst = false;
						} else {
							buf[c++] = ';';
						}
						buf[c++] = (char) ('1' + j);
					}
				}

				if (cfg != tmpfg) {
					if (isFirst) {
						isFirst = false;
					} else {
						buf[c++] = ';';
					}
					buf[c++] = '3';
					buf[c++] = (char) (tmpfg + '0');
				}

				if (cbg != tmpbg) {
					if (isFirst) {
						isFirst = false;
					} else {
						buf[c++] = ';';
					}
					buf[c++] = '4';
					buf[c++] = (char) (tmpbg + '0');
				}
			}

			buf[c++] = 'm';
			buf[c++] = (char) ((Byte) b.elementAt(i)).byteValue();
			sb.append(buf, 0, c);

			cattr = tmpattr;
			cfg = tmpfg;
			cbg = tmpbg;
		}

		return sb.toString();
	}

	private void parse() {
		byte b;

		b = getNextByte();

		if (DEBUG) {
			System.out.print((char) b);

			if (debug_counter++ % 100 == 99) {
				System.out.println();
			}
		}

		// 先把原來的游標位置存下來
		lcol = ccol;
		lrow = crow;

		if (isControlChar((char) b)) {
			parse_control(b);
		} else {
			textBuf[textBufPos] = b;
			attrBuf[textBufPos] = cattribute;
			fgBuf[textBufPos] = cfgcolor;
			bgBuf[textBufPos] = cbgcolor;
			textBufPos++;

			// 如果已經可以組成一個合法的字，就將字紀錄下來並移動游標
			if (conv.isValidMultiBytes(textBuf, 0, textBufPos, encoding)) {
				insertTextBuf();
			}
		}

		// 舊的游標位置需要重繪
		setRepaint(lrow, lcol);
		if ((lcol != ccol) || (lrow != crow)) {

			// 移動後游標應該是可見的
			cursorBlink.setVisible(true);

			// 新的游標位置需要重繪
			setRepaint(crow, ccol);

			// XXX: 只要游標有移動過，就清空 textBuf, 以減少收到不完整的字所造成的異狀
			textBufPos = 0;

			// 只要游標有移動過，就一定不是 linefull
			linefull = false;
		}
	}

	private void parse_control(final byte b) {
		switch (b) {
		case 0: // NUL (Null)
			// TODO:
			break;
		// case 1:
		// case 2:
		// case 3:
		// case 4:
		// case 5:
		// case 6:
		case 7: // BEL (Bell)
			bell();
			break;
		case 8: // BS (Backspace)
			if (linefull) {
				linefull = false;
			} else if (ccol > leftmargin) {
				// 收到 Backspace 不需要清除文字，只要往前就好
				ccol--;
			} else if ((ccol == leftmargin) && (crow > topmargin)) {
				ccol = rightmargin;
				crow--;
			}
			break;
		case 9: // HT (Horizontal Tab)
			ccol = ((ccol - 1) / 8 + 1) * 8 + 1;
			if( ccol > rightmargin ) {
				// 如果會跳超過 rightmargin 就只跳到 rightmargin
				ccol = rightmargin;
			}
			break;
		case 10: // LF (Line Feed)
			crow++;
			// 到 buttommargin 就該捲頁
			if (crow > buttommargin) {
				scrollpage(1);
				crow--;
			}
			break;
		// case 11:
		// case 12:
		case 13: // CR (Carriage Return)
			ccol = leftmargin;
			break;
		case 14: // SO (Shift Out)
			// TODO:
			System.out.println("SO (not yet support)");
			break;
		case 15: // SI (Shift In)
			// TODO:
			System.out.println("SI (not yet support)");
			break;
		// case 16:
		// case 17:
		// case 18:
		// case 19:
		// case 20:
		// case 21:
		// case 22:
		// case 23:
		case 24: // CAN (Cancel)
			// TODO:
			System.out.println("CAN (not yet support)");
			break;
		case 25:
		case 26: // SUB (Subsitute)
			// TODO:
			System.out.println("SUB (not yet support)");
			break;
		case 27: // ESC (Escape)
			parse_esc();
			break;
		// case 28:
		// case 29:
		// case 30:
		// case 31:
		default:
			// XXX: 遇到小於 32 的 ASCII, 卻不是控制字元，不知道該怎麼辦。
			break;
		}
	}

	private void parse_csi() {
		int i, argc;
		int arg;
		final int[] argv = new int[256];
		byte b;

		// System.out.print("CSI ");

		// 取得以 ';' 分開的參數
		arg = -1;
		argc = 0;
		while (true) {
			b = getNextByte();

			if (('0' <= b) && (b <= '9')) {
				if (arg == -1) {
					arg = 0;
				} else {
					arg *= 10;
				}
				arg += b - '0';
			} else if (b == ';') {
				argv[argc] = arg;
				argc++;
				arg = -1;
			} else if ((b == '?') || (b == '!') || (b == '"') || (b == '\'')) {
				// FIXME: 這些字元不應該被忽略，目前只是省事的寫法，應該再改寫
			} else {
				argv[argc] = arg;
				argc++;
				break;
			}
		}
		// argc 表參數個數，argv[i] 的內容為參數值，若參數值為 -1 表未設定

		switch (b) {
		case 'd':
			// 設定 row
			// System.out.print("line position absolute (VPA)");
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			crow = argv[0];
			break;
		case 'h':
			// System.out.println( "set mode" );
			for (i = 0; i < argc; i++) {
				set_mode(argv[i]);
			}
			break;
		case 'l':
			// System.out.println( "reset mode" );
			for (i = 0; i < argc; i++) {
				reset_mode(argv[i]);
			}
			break;
		case 'm':
			// Character Attributes (SGR)
			for (i = 0; i < argc; i++) {
				if (argv[i] == -1) {
					argv[i] = 0;
				}
				setColor(argv[i]);
			}
			break;
		case 'r':
			setmargin(argv[0], argv[1]);
			// System.out.println( "Set scroll margin: " + argv[0] + ", " +
			// argv[1] );
			break;
		case 's':
			save_cursor_position();
			break;
		case 'u':
			restore_cursor_position();
			break;
		case 'A':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			crow = Math.max(crow - argv[0], topmargin);
			// System.out.println( argv[0] + " A" );
			break;
		case 'B':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			crow = Math.min(crow + argv[0], buttommargin);
			// System.out.println( argv[0] + " B" );
			break;
		case 'C':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			ccol = Math.min(ccol + argv[0], rightmargin);
			// System.out.println( argv[0] + " C" );
			break;
		case 'D':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			ccol = Math.max(ccol - argv[0], leftmargin);
			// System.out.println( argv[0] + " D" );
			break;
		case 'H':
			// ESC [ Pl ; Pc H
			// 移到 (pl, pc) 去，預設值是 (1, 1)
			// 座標是相對於整個畫面，不是 margin

			if (argv[0] < 1) {
				argv[0] = 1;
			}

			if (argv[1] < 1) {
				argv[1] = 1;
			}

			crow = Math.min(Math.max(argv[0], topmargin), buttommargin);
			ccol = Math.min(Math.max(argv[1], leftmargin), rightmargin);
			// System.out.println( argv[0] + " " + argv[1] + " H" );
			break;
		case 'J':
			if (argv[0] == -1) {
				argv[0] = 0;
			}
			erasescreen(argv[0]);
			// System.out.println( argv[0] + " J" );
			break;
		case 'K':
			if (argv[0] == -1) {
				argv[0] = 0;
			}
			eraseline(crow, argv[0]);
			// System.out.println( argv[0] + " K" );
			break;
		case 'L':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			insertline(crow, argv[0]);
			// System.out.println( argv[0] + " L" );
			break;
		case 'M':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			delete_lines(argv[0]);
			break;
		case 'P':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			delete_characters(argv[0]);
			break;
		case '@':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			insert_space(argv[0]);
			break;
		case '>':
			// TODO
			b = getNextByte();
			if (b == 'c') {
				System.out.println("Send Secondary Device Attributes String");
			} else {
				System.out.println("Unknown control sequence: ESC [ > "
						+ (char) b);
			}
			break;
		default:
			// TODO
			System.out.println("Unknown control sequence: ESC [ " + (char) b);
			break;
		}
	}

	private void parse_esc() {
		byte b;

		b = getNextByte();

		switch (b) {
		case 0x1b:
			// XXX: 有些地方會出現 ESC ESC ...
			parse_esc();
			break;
		case '(': // 0x28
		case ')': // 0x29
			parse_scs(b);
			break;
		case '7': // 0x37
			save_cursor_position();
			break;
		case '8': // 0x38
			restore_cursor_position();
			break;
		case '=': // 0x3d
			keypadmode = VT100.APPLICATION_KEYPAD;
			// System.out.println( "Set application keypad mode" );
			break;
		case '>': // 0x3e
			keypadmode = VT100.NUMERIC_KEYPAD;
			// System.out.println( "Set numeric keypad mode" );
			break;
		case 'M': // 0x4d
			reverseindex();
			// System.out.println( "Reverse index" );
			break;
		case '[': // 0x5b
			parse_csi();
			break;
		default:
			System.out.println("Unknown control sequence: ESC " + (char) b);
			break;
		}
	}

	private void parse_scs(final byte a) {
		byte b;
		// TODO:
		b = getNextByte();
		System.out.println("ESC " + (char) a + " " + (char) b + "(SCS)");

		if (a == '(') {
			// Select G0 Character Set (SCS)
			switch (b) {
			case '0':
				break;
			case '1':
				break;
			case '2':
				break;
			case 'A':
				break;
			case 'B':
				break;
			default:
				break;
			}
		} else if (a == ')') {
			// Select G1 Character Set (SCS)
			switch (b) {
			case '0':
				break;
			case '1':
				break;
			case '2':
				break;
			case 'A':
				break;
			case 'B':
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 計算 row 在陣列中真正的 row NOTE: 這不是 logicalRow 的反函數
	 * 
	 * @param row
	 * @return
	 */
	private int physicalRow(final int row) {
		// row 可能是負值，因此多加上一個 totalrow
		return (toprow + row + totalrow - 1) % totalrow;
	}

	private void reset(final int row, final int col) {
		int prow;

		prow = physicalRow(row);

		text[prow][col - 1] = ((char) 0);
		mbc[prow][col - 1] = 0;

		fgcolors[prow][col - 1] = VT100.defFg;
		bgcolors[prow][col - 1] = VT100.defBg;
		attributes[prow][col - 1] = VT100.defAttr;
		isurl[prow][col - 1] = false;

		setRepaint(row, col);
	}

	private void reset_mode(final int m) {
		// TODO
		switch (m) {
		case 1:
			keypadmode = VT100.NUMERIC_KEYPAD;
			break;
		case 12:
			// TODO: stop blinking cursor, ignore
			break;
		case 25:
			// TODO: hide cursor, ignore
			break;
		default:
			System.out.println("Reset mode " + m + " not support.");
			break;
		}
	}

	private void restore_cursor_position() {
		ccol = scol;
		crow = srow;
		// System.out.println( "Restore cursor position." );
	}

	/**
	 * 在頁首插入一行
	 */
	private void reverseindex() {
		// System.out.println("reverse index at " + crow );
		if (crow == topmargin) {
			insertline(crow, 1);
		} else {
			crow--;
		}
	}

	private void save_cursor_position() {
		scol = ccol;
		srow = crow;
		// System.out.println( "Save cursor position." );
	}

	/**
	 * 捲頁
	 * 
	 * @param line
	 *            行數
	 */
	private void scrollpage(final int line) {
		int i, j;

		// System.out.println("scroll " + line + " lines");

		if ((topmargin == 1) && (buttommargin == maxrow)) {
			toprow += line;
			if (toprow >= totalrow) {
				toprow %= totalrow;
			}
			for (i = 1; i <= maxrow; i++) {
				for (j = leftmargin; j <= rightmargin; j++) {
					if (i <= buttommargin - line) {
						setRepaint(i, j);
					} else {
						reset(i, j);
					}
				}
			}
		} else {
			for (i = topmargin; i <= buttommargin; i++) {
				for (j = leftmargin; j <= rightmargin; j++) {
					if (i <= buttommargin - line) {
						copy(i, j, i + line, j);
					} else {
						reset(i, j);
					}
				}
			}
		}

		// 捲軸不是在最下方時要捲動捲軸，以免影響使用者看緩衝區的內容
		if (scrolluprow != 0) {
			parent.scroll(-line);
		}
	}

	private void set_mode(final int m) {
		// TODO
		switch (m) {
		case 1:
			keypadmode = VT100.APPLICATION_KEYPAD;
			break;
		case 12:
			// TODO: start blinking cursor, ignore
			break;
		case 25:
			// TODO: show cursor, ignore
			break;
		default:
			System.out.println("Set mode " + m + " not support.");
			break;
		}
	}

	/**
	 * 設定目前的 color 與 attribute
	 * 
	 * @param c
	 */
	private void setColor(final int c) {
		if (c == 0) {
			cfgcolor = VT100.defFg;
			cbgcolor = VT100.defBg;
			cattribute = VT100.defAttr;
		} else if (c == 1) {
			cattribute |= VT100.BOLD;
		} else if (c == 4) {
			cattribute |= VT100.UNDERLINE;
		} else if (c == 5) {
			cattribute |= VT100.BLINK;
		} else if (c == 7) {
			cattribute ^= VT100.REVERSE;
		} else if ((30 <= c) && (c <= 37)) {
			cfgcolor = (byte) (c - 30);
		} else if ((40 <= c) && (c <= 47)) {
			cbgcolor = (byte) (c - 40);
		}
	}

	/**
	 * 設定邊界
	 * 
	 * @param top
	 *            上邊界
	 * @param buttom
	 *            下邊界
	 */
	private void setmargin(final int top, final int buttom) {
		topmargin = top;
		buttommargin = buttom;
	}

	/**
	 * 設定最新畫面上的某個位置需要重繪
	 * 
	 * @param row
	 * @param col
	 */
	private void setRepaint(final int row, final int col) {
		if ((row < 1) || (row > maxrow) || (col < 1) || (col > maxcol)) {
			return;
		}

		final int prow = physicalRow(row);
		synchronized (repaintLock) {
			repaintSet.add((prow << 8) | (col - 1));
		}
	}

	/**
	 * 設定某個實際位置需要重繪
	 * 
	 * @param prow
	 * @param pcol
	 */
	private void setRepaintPhysical(final int prow, final int pcol) {
		if ((prow < 0) || (prow >= totalrow) || (pcol < 0)
				|| (pcol >= totalcol)) {
			return;
		}

		synchronized (repaintLock) {
			repaintSet.add((prow << 8) | pcol);
		}
	}
}

class FIFOSet {
	boolean[]	contain;
	int			front, rear;
	int[]		set;

	/**
	 * @param range
	 *            Set 的值域 1...(range - 1)
	 */
	public FIFOSet(final int range) {
		front = rear = 0;

		// 假設最多 256 column
		contain = new boolean[range];
		set = new int[range];

		for (int i = 0; i < contain.length; i++) {
			contain[i] = false;
		}
	}

	public void add(final int v) {
		if (contain[v] == true) {
			return;
		}

		// XXX: 沒有檢查空間是否足夠

		set[rear] = v;
		contain[v] = true;

		if (++rear == set.length) {
			rear = 0;
		}
	}

	public boolean isEmpty() {
		return (front == rear);
	}

	public int remove() {
		int v;

		if (front == rear) {
			throw new NoSuchElementException();
		}

		v = set[front];
		contain[v] = false;

		if (++front == set.length) {
			front = 0;
		}

		return v;
	}
}
