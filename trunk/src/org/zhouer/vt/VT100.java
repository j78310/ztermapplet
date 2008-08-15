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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.zhouer.utils.Convertor;
import org.zhouer.utils.TextUtils;

public class VT100 extends JComponent {
	class RepaintTask implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			int prow;
			boolean r = false;

			VT100.this.text_blink_count++;
			VT100.this.cursor_blink_count++;

			// FIXME: magic number
			// 游標閃爍
			if (VT100.this.resource.getBooleanValue(Config.CURSOR_BLINK)) {
				if (VT100.this.cursor_blink_count % 2 == 0) {
					VT100.this.cursor_blink = !VT100.this.cursor_blink;
					VT100.this.setRepaint(VT100.this.crow, VT100.this.ccol);
					r = true;
				}
			}

			// FIXME: magic number
			// 文字閃爍
			// 只需要檢查畫面上有沒有閃爍字，不需要全部都檢查
			if (VT100.this.text_blink_count % 3 == 0) {
				VT100.this.text_blink = !VT100.this.text_blink;
				for (int i = 1; i <= VT100.this.maxrow; i++) {
					for (int j = 1; j <= VT100.this.maxcol; j++) {
						prow = VT100.this.physicalRow(i
								- VT100.this.scrolluprow);
						if ((VT100.this.attributes[prow][j - 1] & VT100.BLINK) != 0) {
							VT100.this.setRepaintPhysical(prow, j - 1);
							r = true;
						}
					}
				}
			}

			if (r) {
				VT100.this.repaint();
			}
		}
	}

	public static final int APPLICATION_KEYPAD = 2;

	public static final int NUMERIC_KEYPAD = 1;

	private static final byte BACKGROUND = 1;
	private static final byte BLINK = 16;
	// 各種屬性
	private static final byte BOLD = 1;
	private static final byte CURSOR = 2;
	private static Color cursorColor = Color.GREEN;
	private static byte defAttr = 0;

	private static byte defBg = 0;

	// 預設顏色設定，前景、背景、游標色
	private static byte defFg = 7;

	// 取得色彩用的一些狀態
	private static final byte FOREGROUND = 0;
	private static Color[] highlight_colors = { new Color(128, 128, 128),
			new Color(255, 0, 0), new Color(0, 255, 0), new Color(255, 255, 0),
			new Color(0, 0, 255), new Color(255, 0, 255),
			new Color(0, 255, 255), new Color(255, 255, 255), };
	// 調色盤
	private static Color[] normal_colors = { new Color(0, 0, 0),
			new Color(128, 0, 0), new Color(0, 128, 0), new Color(128, 128, 0),
			new Color(0, 0, 128), new Color(128, 0, 128),
			new Color(0, 128, 128), new Color(192, 192, 192), };

	private static final byte REVERSE = 64;

	private static final long serialVersionUID = -5704767444883397941L;

	private static final byte UNDERLINE = 8;

	private static final byte URL = 3;
	private static Color urlColor = Color.ORANGE;
	private boolean addurl;

	private byte[] attrBuf, fgBuf, bgBuf;
	private byte[][] attributes; // 屬性
	private byte[][] bgcolors; // 背景色

	// 畫面
	private BufferedImage bi;
	// 目前的屬性及前景、背景色
	private byte cattribute;
	// 目前、上次、儲存的游標所在位址
	private int ccol, crow;
	private byte cfgcolor, cbgcolor;
	// 轉碼用
	private final Convertor conv;
	private String emulation;

	private String encoding;
	private byte[][] fgcolors; // 前景色

	// 各種字型參數
	private Font font;
	private int fontsize;

	// 字元的垂直與水平間距
	private int fontverticalgap, fonthorizontalgap, fontdescentadj;
	private int fontwidth, fontheight, fontdescent;
	// 紀錄是否所有初始化動作皆已完成
	private boolean init_ready;
	// 判斷畫面上的網址
	private boolean[][] isurl;

	private int keypadmode;
	private int lcol, lrow;

	// 記錄游標是否位於最後一個字上，非最後一個字的下一個
	private boolean linefull;
	// 模擬螢幕的相關資訊
	private int maxrow, maxcol; // terminal 的大小
	private int[][] mbc; // multibyte character 的第幾個 byte

	// 把從 nvt 來的資料暫存起來
	private byte[] nvtBuf;

	private int nvtBufPos, nvtBufLen;
	private final Application parent;
	private Vector probablyurl;

	private Object repaintLock;
	// 記錄螢幕上何處需要 repaint
	private FIFOSet repaintSet;
	// 各種參數
	private final Config resource;
	private int scol, srow;

	private int scrolllines; // scroll buffer 的行數
	private int scrolluprow; // 往上捲的行數
	private boolean[][] selected; // 是否被滑鼠選取
	// 儲存螢幕上的相關資訊
	private char[][] text; // 轉碼後的 char

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

	private boolean text_blink, cursor_blink;
	// 閃爍用
	private int text_blink_count, cursor_blink_count;

	// multibytes char 暫存
	private byte[] textBuf;

	private int textBufPos;

	// 重繪用的 Timer
	private Timer ti;
	private int topmargin, buttommargin, leftmargin, rightmargin;

	private int toprow; // 第一 row 所在位置
	private int totalrow, totalcol; // 總 row, col 數，包含 scroll buffer
	// 螢幕 translate 的座標
	private int transx, transy;

	private int urlstate;

	// 處理來自使用者的事件
	private User user;

	// 畫面的寬與高
	private int width, height;

	public VT100(final Application p, final Config c, final Convertor cv,
			final BufferedImage b) {
		super();

		this.parent = p;
		this.resource = c;
		this.conv = cv;
		this.bi = b;

		// 初始化一些變數、陣列
		this.initValue();
		this.initArray();
		this.initOthers();
	}

	public void close() {
		// TODO: 應該還有其他東西應該收尾

		// 停止重繪用的 timer
		this.ti.stop();

		// 停止反應來自使用者的事件
		this.removeKeyListener(this.user);
		this.removeMouseListener(this.user);
		this.removeMouseMotionListener(this.user);
	}

	/**
	 * 滑鼠游標是否在網址上
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean coverURL(int x, int y) {
		int c, r;
		int prow;

		x -= this.transx;
		y -= this.transy;

		c = x / this.fontwidth + 1;
		r = y / this.fontheight + 1;

		// 超出螢幕範圍
		if ((r < 1) || (r > this.maxrow) || (c < 1) || (c > this.maxcol)) {
			return false;
		}

		prow = this.physicalRow(r - this.scrolluprow);

		return this.isurl[prow][c - 1];
	}

	public String getEmulation() {
		return this.emulation;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public int getKeypadMode() {
		return this.keypadmode;
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
		boolean needNewLine;

		for (i = 0; i < this.totalrow; i++) {
			needNewLine = false;

			// 找到最後一個有資料的地方
			for (last = this.totalcol - 1; last >= 0; last--) {
				if (this.selected[i][last] && (this.mbc[i][last] != 0)) {
					break;
				}
			}

			for (j = 0; j <= last; j++) {
				if (this.selected[i][j]) {
					if (this.mbc[i][j] == 0) {
						// 後面還有資料，沒資料的部份用空白取代
						a.addElement(new Byte((byte) 0));
						b.addElement(new Byte((byte) ' '));
						fg.addElement(new Byte(VT100.defFg));
						bg.addElement(new Byte(VT100.defBg));
					} else if (this.mbc[i][j] == 1) {
						buf = this.conv.charToBytes(this.text[i][j],
								this.encoding);
						for (k = 0; k < buf.length; k++) {
							b.addElement(new Byte(buf[k]));
							// XXX: 因為最多使用兩格儲存屬性，若超過 2 bytes, 則以第二 bytes 屬性取代之。
							a.addElement(new Byte(this.attributes[i][j
									+ Math.min(k, 2)]));
							fg.addElement(new Byte(this.fgcolors[i][j
									+ Math.min(k, 2)]));
							bg.addElement(new Byte(this.bgcolors[i][j
									+ Math.min(k, 2)]));
						}
					}
					needNewLine = true;
				}
			}
			if (needNewLine) {
				a.addElement(new Byte((byte) 0));
				b.addElement(new Byte((byte) 0x0d));
				fg.addElement(new Byte(VT100.defFg));
				bg.addElement(new Byte(VT100.defBg));
			}
		}

		return this.makePasteText(a, b, fg, bg);
	}

	/**
	 * 複製選取的文字
	 * 
	 * @return
	 */
	public String getSelectedText() {
		// TODO: 這裡寫的不太好，應該再改進
		int i, j, k;
		boolean firstLine = true;
		final StringBuffer sb = new StringBuffer();

		for (i = 0; i < this.totalrow; i++) {

			// 若整行都沒被選取，直接換下一行
			for (j = 0; j < this.totalcol; j++) {
				if (this.selected[i][j]) {
					break;
				}
			}
			if (j == this.totalcol) {
				continue;
			}

			// 除了第一個被選取行外，其餘每行開始前先加上換行
			if (firstLine) {
				firstLine = false;
			} else {
				sb.append("\n");
			}

			// 找到最後一個有資料的地方
			for (j = this.totalcol - 1; j >= 0; j--) {
				if (this.selected[i][j] && (this.mbc[i][j] != 0)) {
					break;
				}
			}

			for (k = 0; k <= j; k++) {
				// 只複製選取的部份
				if (this.selected[i][k]) {
					if (this.mbc[i][k] == 0) {
						// 後面還有資料，雖然沒資料但先替換成空白
						sb.append(" ");
					} else if (this.mbc[i][k] == 1) {
						sb.append(this.text[i][k]);
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
	 * @returnp
	 */
	public String getURL(int x, int y) {
		final StringBuffer sb = new StringBuffer();
		int i;
		int c, r;
		int prow;

		x -= this.transx;
		y -= this.transy;

		c = x / this.fontwidth + 1;
		r = y / this.fontheight + 1;

		// 超出螢幕範圍
		if ((r < 1) || (r > this.maxrow) || (c < 1) || (c > this.maxcol)) {
			return new String();
		}

		prow = this.physicalRow(r - this.scrolluprow);

		// TODO: 可複製跨行的 url
		for (i = c; (i > 0) && this.isurl[prow][i - 1]; i--) {
			;
		}
		for (i++; (i <= this.maxcol) && this.isurl[prow][i - 1]; i++) {
			if (this.mbc[prow][i - 1] == 1) {
				sb.append(this.text[prow][i - 1]);
			}
		}

		return sb.toString();
	}

	public void pasteColorText(final String str) {
		final byte[] tmp = new byte[str.length()];

		for (int i = 0; i < str.length(); i++) {
			tmp[i] = (byte) str.charAt(i);
		}

		this.parent.writeBytes(tmp, 0, tmp.length);
	}

	/**
	 * 貼上文字
	 * 
	 * @param str
	 */
	public void pasteText(String str) {
		char[] ca;

		boolean autobreak;
		int breakcount;

		// 從系統取得的字串可能是 null
		if (str == null) {
			return;
		}

		autobreak = this.resource.getBooleanValue(Config.AUTO_LINE_BREAK);
		breakcount = this.resource.getIntValue(Config.AUTO_LINE_BREAK_LENGTH);

		if (autobreak) {
			str = TextUtils.fmt(str, breakcount);
		}

		ca = str.toCharArray();

		// XXX: ptt 只吃 0x0d 當換行
		// TODO: 需判斷 0x0a 0x0d 的狀況，目前可能會送出兩個 0x0d
		for (int i = 0; i < ca.length; i++) {
			if (ca[i] == 0x0a) {
				ca[i] = 0x0d;
			}
		}

		this.parent.writeChars(ca, 0, ca.length);
	}

	/**
	 * 重設選取區域
	 */
	public void resetSelected() {
		for (int i = 0; i < this.totalrow; i++) {
			for (int j = 0; j < this.totalcol; j++) {
				if (this.selected[i][j]) {
					this.selected[i][j] = false;
					this.setRepaintPhysical(i, j);
				}
			}
		}
	}

	public void run() {
		// 連線後自動取得 focus
		this.requestFocusInWindow();

		// 至此應該所有的初始化動作都完成了
		this.init_ready = true;

		while (!this.parent.isClosed()) {
			this.parse();

			// buffer 裡的東西都處理完才重繪
			if (this.isBufferEmpty()) {
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

		x -= this.transx;
		y -= this.transy;

		c = x / this.fontwidth + 1;
		r = y / this.fontheight + 1;

		// 超出螢幕範圍
		if ((c < 1) || (c > this.maxcol) || (r < 1) || (r > this.maxrow)) {
			return;
		}

		prow = this.physicalRow(r - this.scrolluprow);

		// 往前找到第一個非空白的合法字元
		for (beginx = c; beginx > 0; beginx--) {
			if ((this.mbc[prow][beginx - 1] == 0)
					|| ((this.mbc[prow][beginx - 1] == 1) && (this.text[prow][beginx - 1] == ' '))) {
				break;
			}
		}
		// 向後 ...
		for (endx = c; endx <= this.maxcol; endx++) {
			if ((this.mbc[prow][endx - 1] == 0)
					|| ((this.mbc[prow][endx - 1] == 1) && (this.text[prow][endx - 1] == ' '))) {
				break;
			}
		}

		this.resetSelected();
		// FIXME: 這裡還需要一些測試
		for (i = beginx + 1; i < endx; i++) {
			this.selected[prow][i - 1] = true;
			this.setRepaintPhysical(prow, i - 1);
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

		x -= this.transx;
		y -= this.transy;

		c = x / this.fontwidth + 1;
		r = y / this.fontheight + 1;

		// 超出螢幕範圍
		if ((c < 1) || (c > this.maxcol) || (r < 1) || (r > this.maxrow)) {
			return;
		}

		this.resetSelected();
		prow = this.physicalRow(r - this.scrolluprow);
		for (int i = 1; i < this.maxcol; i++) {
			this.selected[prow][i - 1] = true;
			this.setRepaintPhysical(prow, i - 1);
		}
	}

	@Override
	public void setBounds(final int x, final int y, final int w, final int h) {
		// layout manager 或其他人可能會透過 setBound 來改變 component 的大小，
		// 此時要一併更新 component
		super.setBounds(x, y, w, h);
		this.updateSize();
	}

	@Override
	public void setBounds(final Rectangle r) {
		super.setBounds(r);
		this.updateSize();
	}

	/**
	 * 設定模擬終端機的類型
	 * 
	 * @param emu
	 */
	public void setEmulation(final String emu) {
		// XXX: vt100 對各種 terminal type 的處理都相同，不需通知
		this.emulation = emu;
	}

	/**
	 * 設定 encoding
	 * 
	 * @param enc
	 */
	public void setEncoding(final String enc) {
		this.encoding = enc;
	}

	/**
	 * 設定目前往上捲的行數
	 * 
	 * @param scroll
	 *            行數
	 */
	public void setScrollUp(final int scroll) {
		this.scrolluprow = scroll;
		// System.out.println( "scroll up " + scroll + " lines" );
		// TODO: 應改可以不用每次都重繪整個畫面
		for (int i = 1; i <= this.maxrow; i++) {
			for (int j = 1; j <= this.maxcol; j++) {
				this.setRepaintPhysical(this.physicalRow(i - this.scrolluprow),
						j - 1);
			}
		}
		this.repaint();
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

		x1 -= this.transx;
		y1 -= this.transy;
		x2 -= this.transx;
		y2 -= this.transy;

		c1 = x1 / this.fontwidth + 1;
		r1 = y1 / this.fontheight + 1;

		c2 = x2 / this.fontwidth + 1;
		r2 = y2 / this.fontheight + 1;

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

		this.resetSelected();
		// TODO: 只能選取當前畫面的內容，不會自動捲頁
		for (i = 1; i <= this.maxrow; i++) {
			for (j = 1; j <= this.maxcol; j++) {

				prow = this.physicalRow(i - this.scrolluprow);

				orig = this.selected[prow][j - 1];

				if ((i > r1) && (i < r2)) {
					this.selected[prow][j - 1] = true;
				} else if ((i == r1) && (i == r2)) {
					this.selected[prow][j - 1] = (j >= c1) && (j <= c2);
				} else if (i == r1) {
					this.selected[prow][j - 1] = (j >= c1);
				} else if (i == r2) {
					this.selected[prow][j - 1] = (j <= c2);
				} else {
					this.selected[prow][j - 1] = false;
				}

				if (this.selected[prow][j - 1] != orig) {
					this.setRepaintPhysical(prow, j - 1);
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
		this.fonthorizontalgap = this.resource
				.getIntValue(Config.FONT_HORIZONTAL_GAP);
		this.fontverticalgap = this.resource
				.getIntValue(Config.FONT_VERTICLAL_GAP);
		this.fontdescentadj = this.resource
				.getIntValue(Config.FONT_DESCENT_ADJUST);

		// 設定 family
		family = this.resource.getStringValue(Config.FONT_FAMILY);

		// 設定 size
		this.fontsize = this.resource.getIntValue(Config.FONT_SIZE);
		if (this.fontsize == 0) {
			// 按照螢幕的大小設定
			fw = this.width / this.maxcol - this.fonthorizontalgap;
			fh = this.height / this.maxrow - this.fontverticalgap;

			if (fh > 2 * fw) {
				fh = 2 * fw;
			}
			this.fontsize = fh;
		}

		// 設定 style（bold, italy, plain）
		style = Font.PLAIN;
		if (this.resource.getBooleanValue(Config.FONT_BOLD)) {
			style |= Font.BOLD;
		}
		if (this.resource.getBooleanValue(Config.FONT_ITALY)) {
			style |= Font.ITALIC;
		}

		// 建立 font instance
		this.font = new Font(family, style, this.fontsize);

		fm = this.getFontMetrics(this.font);

		// XXX: 這裡對 fontheight 與 fontwidth 的假設可能有問題
		this.fontheight = this.fontsize;
		this.fontwidth = this.fontsize / 2;
		this.fontdescent = (int) (1.0 * fm.getDescent() / fm.getHeight() * this.fontsize);

		this.fontheight += this.fontverticalgap;
		this.fontwidth += this.fonthorizontalgap;
		this.fontdescent += this.fontdescentadj;

		// 修改字型會影響 translate 的座標
		this.transx = (this.width - this.fontwidth * this.maxcol) / 2;
		this.transy = (this.height - this.fontheight * this.maxrow) / 2;
	}

	public void updateImage(final BufferedImage b) {
		this.bi = b;
	}

	/**
	 * 重繪目前的畫面
	 */
	public void updateScreen() {
		for (int i = 1; i <= this.maxrow; i++) {
			for (int j = 1; j <= this.maxcol; j++) {
				this.setRepaintPhysical(this.physicalRow(i - this.scrolluprow),
						j - 1);
			}
		}

		this.repaint();
	}

	public void updateSize() {
		this.width = this.getWidth();
		this.height = this.getHeight();

		this.updateFont();
		this.updateScreen();
	}

	@Override
	protected void paintComponent(final Graphics g) {
		// 因為多個分頁共用一張 image, 因此只有在前景的分頁才有繪圖的權利，
		// 不在前景時不重繪，以免干擾畫面。
		// 初始化完成之前不重繪。
		if (!this.parent.isTabForeground() || !this.init_ready) {
			return;
		}

		// TODO: 考慮 draw 是否一定要擺在這邊，或是是否只在這裡呼叫？
		// 偶爾呼叫一次而不只是在顯示前才呼叫應該可以增進顯示速度。
		this.draw();

		g.drawImage(this.bi, 0, 0, null);
	}

	/**
	 * 發出一個 bell
	 */
	private void bell() {
		this.parent.bell();
	}

	private void checkURL(final char c) {
		final String W = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ;/?:@=&{}|^~[]`%#$-_.+!*'(),";

		this.addurl = false;
		switch (this.urlstate) {
		case 0:
			this.probablyurl.removeAllElements();
			if (c == 'h') {
				this.urlstate = 1;
				this.addurl = true;
			}
			break;
		case 1:
			if (c == 't') {
				this.urlstate = 2;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 2:
			if (c == 't') {
				this.urlstate = 3;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 3:
			if (c == 'p') {
				this.urlstate = 4;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 4:
			if (c == ':') {
				this.urlstate = 6;
				this.addurl = true;
			} else if (c == 's') {
				this.urlstate = 5;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 5:
			if (c == ':') {
				this.urlstate = 6;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 6:
			if (c == '/') {
				this.urlstate = 7;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 7:
			if (c == '/') {
				this.urlstate = 8;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 8:
			if (W.indexOf(c) != -1) {
				this.urlstate = 9;
				this.addurl = true;
			} else {
				this.urlstate = 0;
			}
			break;
		case 9:
			if (W.indexOf(c) == -1) {
				this.setURL();
				this.urlstate = 0;
			} else {
				this.addurl = true;
			}
			break;
		default:
			this.urlstate = 0;
		}
	}

	private void copy(final int dstrow, final int dstcol, final int srcrow,
			final int srccol) {
		int pdstrow, psrcrow;

		pdstrow = this.physicalRow(dstrow);
		psrcrow = this.physicalRow(srcrow);

		this.text[pdstrow][dstcol - 1] = this.text[psrcrow][srccol - 1];
		this.mbc[pdstrow][dstcol - 1] = this.mbc[psrcrow][srccol - 1];
		this.fgcolors[pdstrow][dstcol - 1] = this.fgcolors[psrcrow][srccol - 1];
		this.bgcolors[pdstrow][dstcol - 1] = this.bgcolors[psrcrow][srccol - 1];
		this.attributes[pdstrow][dstcol - 1] = this.attributes[psrcrow][srccol - 1];
		this.isurl[pdstrow][dstcol - 1] = this.isurl[psrcrow][srccol - 1];

		this.setRepaint(dstrow, dstcol);
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
		for (int i = this.ccol; i <= this.maxcol; i++) {
			if (i <= this.maxcol - n) {
				this.copy(this.crow, i, this.crow, i + n);
			} else {
				this.reset(this.crow, i);
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
		if (this.crow + n > this.buttommargin) {
			for (i = this.crow; i <= this.buttommargin; i++) {
				this.eraseline(i, 2);
			}
			return;
		}

		// 目前行號加 n 到 buttommargin 全部往前移，後面則清除。
		for (i = this.crow + n; i <= this.buttommargin; i++) {
			for (j = this.leftmargin; j <= this.rightmargin; j++) {
				this.copy(i - n, j, i, j);
			}
		}
		for (i = this.buttommargin - n + 1; i <= this.buttommargin; i++) {
			this.eraseline(i, 2);
		}
	}

	private void draw() {
		int w, h;
		int v, prow, pcol;
		int row, col;
		Graphics2D g;
		boolean show_cursor, show_text, show_underline;

		g = this.bi.createGraphics();
		g.setFont(this.font);

		// 畫面置中
		g.translate(this.transx, this.transy);

		// 設定 Anti-alias
		if (this.resource.getBooleanValue(Config.FONT_ANTIALIAS)) {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}

		while (!this.repaintSet.isEmpty()) {
			// 取得下一個需要重繪的位置
			synchronized (this.repaintLock) {
				v = this.repaintSet.remove();
				prow = v >> 8;
				pcol = v & 0xff;
			}

			// 取得待重繪的字在畫面上的位置
			// 加上捲動的判斷
			row = this.logicalRow(prow);
			col = pcol + 1;

			// 若是需重繪的部份不在顯示範圍內則不理會
			if ((row < 1) || (row > this.maxrow) || (col < 1)
					|| (col > this.maxcol)) {
				continue;
			}

			// 本次待繪字元的左上角座標
			h = (row - 1) * this.fontheight;
			w = (col - 1) * this.fontwidth;

			// 閃爍控制與色彩屬性
			show_text = ((this.attributes[prow][pcol] & VT100.BLINK) == 0)
					|| this.text_blink;
			show_cursor = (this.physicalRow(this.crow) == prow)
					&& (this.ccol == col) && this.cursor_blink;
			show_underline = (this.attributes[prow][pcol] & VT100.UNDERLINE) != 0;

			// 填滿背景色
			g.setColor(this.getColor(prow, pcol, VT100.BACKGROUND));
			g.fillRect(w, h, this.fontwidth, this.fontheight);

			// 如果是游標所在處就畫出游標
			if (show_cursor) {
				// TODO: 多幾種游標形狀
				g.setColor(this.getColor(prow, pcol, VT100.CURSOR));
				g.fillRect(w, h, this.fontwidth, this.fontheight);
			}

			// 空白不重繪前景文字，離開
			if (this.mbc[prow][pcol] == 0) {
				continue;
			}

			// 設為前景色
			g.setColor(this.getColor(prow, pcol, VT100.FOREGROUND));

			// 畫出文字
			if (show_text) {
				// 利用 clip 的功能，只畫出部份（半個）中文字。
				// XXX: 每個中文都會畫兩次，又有 clip 的 overhead, 效率應該會受到蠻大的影響！
				final Shape oldclip = g.getClip();
				g.clipRect(w, h, this.fontwidth, this.fontheight);
				g.drawString(Character.toString(this.text[prow][pcol
						- this.mbc[prow][pcol] + 1]), w - this.fontwidth
						* (this.mbc[prow][pcol] - 1), h + this.fontheight
						- this.fontdescent);
				g.setClip(oldclip);
			}

			// 畫出底線
			if (show_underline || this.isurl[prow][pcol]) {
				if (this.isurl[prow][pcol]) {
					g.setColor(this.getColor(prow, pcol, VT100.URL));
				}
				g.drawLine(w, h + this.fontheight - 1, w + this.fontwidth - 1,
						h + this.fontheight - 1);
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
			begin = this.ccol;
			end = this.rightmargin;
			break;
		case 1:
			begin = this.leftmargin;
			end = this.ccol;
			break;
		case 2:
			begin = this.leftmargin;
			end = this.rightmargin;
			break;
		default:
			begin = this.leftmargin;
			end = this.rightmargin;
			break;
		}

		for (i = begin; i <= end; i++) {
			this.reset(row, i);
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
			this.eraseline(this.crow, mode);
			begin = this.crow + 1;
			end = this.maxrow;
			break;
		case 1:
			this.eraseline(this.crow, mode);
			begin = 1;
			end = this.crow - 1;
			break;
		case 2:
			begin = 1;
			end = this.maxrow;
			break;
		default:
			begin = 1;
			end = this.maxrow;
			break;
		}

		for (i = begin; i <= end; i++) {
			this.eraseline(i, 2);
		}
	}

	private Color getColor(final int prow, final int pcol, final byte mode) {
		Color c;
		boolean bold, reverse;

		bold = (this.attributes[prow][pcol] & VT100.BOLD) != 0;
		reverse = this.selected[prow][pcol]
				^ ((this.attributes[prow][pcol] & VT100.REVERSE) != 0);

		if (((mode == VT100.FOREGROUND) && !reverse)
				|| ((mode == VT100.BACKGROUND) && reverse)) {
			// 前景色
			if (bold) {
				c = VT100.highlight_colors[this.fgcolors[prow][pcol]];
			} else {
				c = VT100.normal_colors[this.fgcolors[prow][pcol]];
			}
		} else if (((mode == VT100.BACKGROUND) && !reverse)
				|| ((mode == VT100.FOREGROUND) && reverse)) {
			// 背景色
			c = VT100.normal_colors[this.bgcolors[prow][pcol]];
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
		if (this.nvtBufPos == this.nvtBufLen) {
			this.nvtBufLen = this.parent.readBytes(this.nvtBuf);
			// 連線終止或錯誤，應盡快結束 parse()
			if (this.nvtBufLen == -1) {
				return 0;
			}
			this.nvtBufPos = 0;
		}

		return this.nvtBuf[this.nvtBufPos++];
	}

	private void initArray() {
		int i, j;

		// 從上層讀取資料用的 buffer
		this.nvtBuf = new byte[4096];
		this.nvtBufPos = this.nvtBufLen = 0;

		this.text = new char[this.totalrow][this.totalcol];
		this.mbc = new int[this.totalrow][this.totalcol];
		this.fgcolors = new byte[this.totalrow][this.totalcol];
		this.bgcolors = new byte[this.totalrow][this.totalcol];
		this.attributes = new byte[this.totalrow][this.totalcol];
		this.selected = new boolean[this.totalrow][this.totalcol];
		this.isurl = new boolean[this.totalrow][this.totalcol];

		this.textBuf = new byte[4];
		this.attrBuf = new byte[4];
		this.fgBuf = new byte[4];
		this.bgBuf = new byte[4];
		this.textBufPos = 0;

		// 初始化記載重繪位置用 FIFOSet
		// XXX: 假設 column 數小於 256
		this.repaintSet = new FIFOSet(this.totalrow << 8);
		this.repaintLock = new Object();

		for (i = 0; i < this.totalrow; i++) {
			for (j = 0; j < this.totalcol; j++) {
				this.text[i][j] = ((char) 0);
				this.mbc[i][j] = 0;

				this.fgcolors[i][j] = VT100.defFg;
				this.bgcolors[i][j] = VT100.defBg;
				this.attributes[i][j] = VT100.defAttr;
				this.isurl[i][j] = false;
			}
		}

		for (i = 1; i < this.maxrow; i++) {
			for (j = 1; j < this.maxcol; j++) {
				this.setRepaint(i, j);
			}
		}
	}

	private void initOthers() {
		// 進入 run() 以後才確定初始化完成
		this.init_ready = false;

		// 啟動閃爍控制 thread
		this.ti = new Timer(250, new RepaintTask());
		this.ti.start();

		// 取消 focus traversal key, 這樣才能收到 tab.
		this.setFocusTraversalKeysEnabled(false);

		// Input Method Framework, set passive-client
		this.enableInputMethods(true);

		// 設定預設大小
		// FIXME: magic number
		this.setSize(new Dimension(800, 600));

		// User
		this.user = new User(this.parent, this, this.resource);

		this.addKeyListener(this.user);
		this.addMouseListener(this.user);
		this.addMouseMotionListener(this.user);
	}

	private void initValue() {
		// 讀入模擬終端機的大小，一般而言是 80 x 24
		this.maxcol = this.resource.getIntValue(Config.TERMINAL_COLUMNS);
		this.maxrow = this.resource.getIntValue(Config.TERMINAL_ROWS);

		// 讀入 scroll buffer 行數
		this.scrolllines = this.resource.getIntValue(Config.TERMINAL_SCROLLS);

		// 所需要的陣列大小
		this.totalrow = this.maxrow + this.scrolllines;
		this.totalcol = this.maxcol;

		// 一開始環狀佇列的起始點在 0
		this.toprow = 0;

		// 預設 margin 為整個螢幕
		this.topmargin = 1;
		this.buttommargin = this.maxrow;
		this.leftmargin = 1;
		this.rightmargin = this.maxcol;

		// 游標起始位址在螢幕左上角處
		this.ccol = this.crow = 1;
		this.lcol = this.lrow = 1;
		this.scol = this.srow = 1;

		// 設定目前色彩為預設值
		this.cfgcolor = VT100.defFg;
		this.cbgcolor = VT100.defBg;
		this.cattribute = VT100.defAttr;

		this.urlstate = 0;
		this.addurl = false;
		this.probablyurl = new Vector();

		this.text_blink_count = 0;
		this.cursor_blink_count = 0;
		this.text_blink = true;
		this.cursor_blink = true;

		this.linefull = false;

		this.keypadmode = VT100.NUMERIC_KEYPAD;
	}

	/**
	 * 在目前的位置插入數個空白
	 * 
	 * @param n
	 */
	private void insert_space(final int n) {
		// System.out.println( "insert " + n + " space, at (" + crow + ", " +
		// ccol + ")" );

		for (int i = this.rightmargin; i >= this.ccol; i--) {
			if (i >= this.ccol + n) {
				this.copy(this.crow, i, this.crow, i - n);
			} else {
				this.reset(this.crow, i);
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
		for (int i = this.buttommargin; i >= r; i--) {
			for (int j = this.leftmargin; j <= this.rightmargin; j++) {
				if (i >= r + n) {
					this.copy(i, j, i - n, j);
				} else {
					this.reset(i, j);
				}
			}
		}
	}

	private void insertTextBuf() {
		char c;
		boolean isWide;
		int prow;

		// XXX: 表格內有些未知字元會填入 '?', 因此可能會有 c < 127 但 textBufPos > 1 的狀況。
		c = this.conv.bytesToChar(this.textBuf, 0, this.textBufPos,
				this.encoding);
		isWide = Convertor.isWideChar(c);

		// 一般而言游標都在下一個字將會出現的地方，但若最後一個字在行尾（下一個字應該出現在行首），
		// 游標會在最後一個字上，也就是當最後一個字出現在行尾時並不會影響游標位置，
		// 游標會等到下一個字出現時才移到下一行。
		if (this.linefull || (isWide && (this.ccol + 1 > this.rightmargin))) {
			this.linefull = false;
			this.ccol = this.leftmargin;
			this.crow++;
			if (this.crow > this.buttommargin) {
				this.scrollpage(1);
				this.crow--;
			}

			// 游標會跳過行尾，所以需要手動 setRepaint
			this.setRepaint(this.crow, this.leftmargin);
		}

		// 一個 char 可能對應數個 bytes, 但在顯示及儲存時最雙寬字多佔兩格，單寬字最多佔一格，
		// 紀錄 char 後要把對應的屬性及色彩等資料從 buffer 複製過來，並設定重繪。
		prow = this.physicalRow(this.crow);
		this.text[prow][this.ccol - 1] = c;

		// 到這裡我們才知道字元真正被放到陣列中的位置，所以現在才紀錄 url 的位置
		if (this.addurl) {
			// XXX: 假設 column 數小於 256
			this.probablyurl.addElement(new Integer((prow << 8)
					| (this.ccol - 1)));
		}

		// 紀錄暫存的資料，寬字元每個字最多用兩個 bytes，一般字元每字一個 byte
		for (int i = 0; i < (isWide ? Math.min(this.textBufPos, 2) : 1); i++) {
			this.fgcolors[prow][this.ccol + i - 1] = this.fgBuf[i];
			this.bgcolors[prow][this.ccol + i - 1] = this.bgBuf[i];
			this.attributes[prow][this.ccol + i - 1] = this.attrBuf[i];
			this.mbc[prow][this.ccol + i - 1] = i + 1;

			// isurl 不同於 color 與 attribute, isurl 是在 setURL 內設定。
			this.isurl[prow][this.ccol + i - 1] = false;

			this.setRepaint(this.crow, this.ccol + i);
		}

		// 重設 textBufPos
		this.textBufPos = 0;

		// 控制碼不會讓游標跑到 rightmargin 以後的地方，只有一般字元會，所以在這裡判斷 linefull 就可以了。
		this.ccol++;
		if (isWide) {
			this.ccol++;
		}
		if (this.ccol > this.rightmargin) {
			this.linefull = true;
			this.ccol--;
		}
	}

	/**
	 * buffer 是否是空的
	 * 
	 * @return
	 */
	private boolean isBufferEmpty() {
		return (this.nvtBufPos == this.nvtBufLen);
	}

	/**
	 * 計算 prow 在目前畫面中的 row NOTE: 這不是 physicalRow 的反函數
	 * 
	 * @param prow
	 * @return
	 */
	private int logicalRow(final int prow) {
		int row, tmptop = this.toprow - this.scrolluprow;
		if (tmptop < 0) {
			tmptop += this.totalrow;
		}
		row = prow - tmptop + 1;
		if (row < 1) {
			row += this.totalrow;
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

		b = this.getNextByte();

		// 先把原來的游標位置存下來
		this.lcol = this.ccol;
		this.lrow = this.crow;

		// 檢查讀入的字元是否為 url
		this.checkURL((char) b);

		// XXX: 若讀入的字元小於 32 則視為控制字元。其實應該用列舉的，但這麼寫比較漂亮。
		if ((b >= 0) && (b < 32)) {
			this.parse_control(b);
		} else {
			this.textBuf[this.textBufPos] = b;
			this.attrBuf[this.textBufPos] = this.cattribute;
			this.fgBuf[this.textBufPos] = this.cfgcolor;
			this.bgBuf[this.textBufPos] = this.cbgcolor;
			this.textBufPos++;

			// 如果已經可以組成一個合法的字，就將字紀錄下來並移動游標
			if (this.conv.isValidMultiBytes(this.textBuf, 0, this.textBufPos,
					this.encoding)) {
				this.insertTextBuf();
			}
		}

		// 舊的游標位置需要重繪
		this.setRepaint(this.lrow, this.lcol);
		if ((this.lcol != this.ccol) || (this.lrow != this.crow)) {

			// 移動後游標應該是可見的
			this.cursor_blink_count = 0;
			this.cursor_blink = true;

			// 新的游標位置需要重繪
			this.setRepaint(this.crow, this.ccol);

			// XXX: 只要游標有移動過，就清空 textBuf, 以減少收到不完整的字所造成的異狀
			this.textBufPos = 0;

			// 只要游標有移動過，就一定不是 linefull
			this.linefull = false;
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
			this.bell();
			break;
		case 8: // BS (Backspace)
			if (this.linefull) {
				this.linefull = false;
			} else if (this.ccol > this.leftmargin) {
				// 收到 Backspace 不需要清除文字，只要往前就好
				this.ccol--;
			} else if ((this.ccol == this.leftmargin)
					&& (this.crow > this.topmargin)) {
				this.ccol = this.rightmargin;
				this.crow--;
			}
			break;
		case 9: // HT (Horizontal Tab)
			this.ccol = ((this.ccol - 1) / 8 + 1) * 8 + 1;
			break;
		case 10: // LF (Line Feed)
			this.crow++;
			// 到 buttommargin 就該捲頁
			if (this.crow > this.buttommargin) {
				this.scrollpage(1);
				this.crow--;
			}
			break;
		// case 11:
		// case 12:
		case 13: // CR (Carriage Return)
			this.ccol = this.leftmargin;
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
			this.parse_esc();
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
			b = this.getNextByte();

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
			this.crow = argv[0];
			break;
		case 'h':
			// System.out.println( "set mode" );
			for (i = 0; i < argc; i++) {
				this.set_mode(argv[i]);
			}
			break;
		case 'l':
			// System.out.println( "reset mode" );
			for (i = 0; i < argc; i++) {
				this.reset_mode(argv[i]);
			}
			break;
		case 'm':
			// Character Attributes (SGR)
			for (i = 0; i < argc; i++) {
				if (argv[i] == -1) {
					argv[i] = 0;
				}
				this.setColor(argv[i]);
			}
			break;
		case 'r':
			this.setmargin(argv[0], argv[1]);
			// System.out.println( "Set scroll margin: " + argv[0] + ", " +
			// argv[1] );
			break;
		case 's':
			this.save_cursor_position();
			break;
		case 'u':
			this.restore_cursor_position();
			break;
		case 'A':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.crow = Math.max(this.crow - argv[0], this.topmargin);
			// System.out.println( argv[0] + " A" );
			break;
		case 'B':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.crow = Math.min(this.crow + argv[0], this.buttommargin);
			// System.out.println( argv[0] + " B" );
			break;
		case 'C':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.ccol = Math.min(this.ccol + argv[0], this.rightmargin);
			// System.out.println( argv[0] + " C" );
			break;
		case 'D':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.ccol = Math.max(this.ccol - argv[0], this.leftmargin);
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

			this.crow = Math.min(Math.max(argv[0], this.topmargin),
					this.buttommargin);
			this.ccol = Math.min(Math.max(argv[1], this.leftmargin),
					this.rightmargin);
			// System.out.println( argv[0] + " " + argv[1] + " H" );
			break;
		case 'J':
			if (argv[0] == -1) {
				argv[0] = 0;
			}
			this.erasescreen(argv[0]);
			// System.out.println( argv[0] + " J" );
			break;
		case 'K':
			if (argv[0] == -1) {
				argv[0] = 0;
			}
			this.eraseline(this.crow, argv[0]);
			// System.out.println( argv[0] + " K" );
			break;
		case 'L':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.insertline(this.crow, argv[0]);
			// System.out.println( argv[0] + " L" );
			break;
		case 'M':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.delete_lines(argv[0]);
			break;
		case 'P':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.delete_characters(argv[0]);
			break;
		case '@':
			if (argv[0] == -1) {
				argv[0] = 1;
			}
			this.insert_space(argv[0]);
			break;
		case '>':
			// TODO
			b = this.getNextByte();
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

		b = this.getNextByte();

		switch (b) {
		case 0x1b:
			// XXX: 有些地方會出現 ESC ESC ...
			this.parse_esc();
			break;
		case '(': // 0x28
		case ')': // 0x29
			this.parse_scs(b);
			break;
		case '7': // 0x37
			this.save_cursor_position();
			break;
		case '8': // 0x38
			this.restore_cursor_position();
			break;
		case '=': // 0x3d
			this.keypadmode = VT100.APPLICATION_KEYPAD;
			// System.out.println( "Set application keypad mode" );
			break;
		case '>': // 0x3e
			this.keypadmode = VT100.NUMERIC_KEYPAD;
			// System.out.println( "Set numeric keypad mode" );
			break;
		case 'M': // 0x4d
			this.reverseindex();
			// System.out.println( "Reverse index" );
			break;
		case '[': // 0x5b
			this.parse_csi();
			break;
		case ']':
			this.parse_text_parameter();
			break;
		default:
			System.out.println("Unknown control sequence: ESC " + (char) b);
			break;
		}
	}

	private void parse_scs(final byte a) {
		byte b;
		// TODO:
		b = this.getNextByte();
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

	private void parse_text_parameter() {
		byte b;
		// FIXME: magic number
		final byte[] text = new byte[80];
		int f, count;

		f = 0;
		b = this.getNextByte();
		while (b != ';') {
			f *= 10;
			f += b - '0';
			b = this.getNextByte();
		}

		count = 0;
		b = this.getNextByte();
		while (b != 0x07) {
			text[count++] = b;
			b = this.getNextByte();
		}

		switch (f) {
		case 0:
			this.parent.setIconName(new String(text, 0, count));
			this.parent.setWindowTitle(new String(text, 0, count));
			break;
		case 1:
			this.parent.setIconName(new String(text, 0, count));
			break;
		case 2:
			this.parent.setWindowTitle(new String(text, 0, count));
			break;
		default:
			System.out.println("Set text parameters(not fully support)");
			break;
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
		return (this.toprow + row + this.totalrow - 1) % this.totalrow;
	}

	private void reset(final int row, final int col) {
		int prow;

		prow = this.physicalRow(row);

		this.text[prow][col - 1] = ((char) 0);
		this.mbc[prow][col - 1] = 0;

		this.fgcolors[prow][col - 1] = VT100.defFg;
		this.bgcolors[prow][col - 1] = VT100.defBg;
		this.attributes[prow][col - 1] = VT100.defAttr;
		this.isurl[prow][col - 1] = false;

		this.setRepaint(row, col);
	}

	private void reset_mode(final int m) {
		// TODO
		switch (m) {
		case 1:
			this.keypadmode = VT100.NUMERIC_KEYPAD;
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
		this.ccol = this.scol;
		this.crow = this.srow;
		// System.out.println( "Restore cursor position." );
	}

	/**
	 * 在頁首插入一行
	 */
	private void reverseindex() {
		// System.out.println("reverse index at " + crow );
		if (this.crow == this.topmargin) {
			this.insertline(this.crow, 1);
		} else {
			this.crow--;
		}
	}

	private void save_cursor_position() {
		this.scol = this.ccol;
		this.srow = this.crow;
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

		if ((this.topmargin == 1) && (this.buttommargin == this.maxrow)) {
			this.toprow += line;
			if (this.toprow >= this.totalrow) {
				this.toprow %= this.totalrow;
			}
			for (i = 1; i <= this.maxrow; i++) {
				for (j = this.leftmargin; j <= this.rightmargin; j++) {
					if (i <= this.buttommargin - line) {
						this.setRepaint(i, j);
					} else {
						this.reset(i, j);
					}
				}
			}
		} else {
			for (i = this.topmargin; i <= this.buttommargin; i++) {
				for (j = this.leftmargin; j <= this.rightmargin; j++) {
					if (i <= this.buttommargin - line) {
						this.copy(i, j, i + line, j);
					} else {
						this.reset(i, j);
					}
				}
			}
		}

		// 捲軸不是在最下方時要捲動捲軸，以免影響使用者看緩衝區的內容
		if (this.scrolluprow != 0) {
			this.parent.scroll(-line);
		}
	}

	private void set_mode(final int m) {
		// TODO
		switch (m) {
		case 1:
			this.keypadmode = VT100.APPLICATION_KEYPAD;
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
			this.cfgcolor = VT100.defFg;
			this.cbgcolor = VT100.defBg;
			this.cattribute = VT100.defAttr;
		} else if (c == 1) {
			this.cattribute |= VT100.BOLD;
		} else if (c == 4) {
			this.cattribute |= VT100.UNDERLINE;
		} else if (c == 5) {
			this.cattribute |= VT100.BLINK;
		} else if (c == 7) {
			this.cattribute ^= VT100.REVERSE;
		} else if ((30 <= c) && (c <= 37)) {
			this.cfgcolor = (byte) (c - 30);
		} else if ((40 <= c) && (c <= 47)) {
			this.cbgcolor = (byte) (c - 40);
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
		this.topmargin = top;
		this.buttommargin = buttom;
	}

	/**
	 * 設定最新畫面上的某個位置需要重繪
	 * 
	 * @param row
	 * @param col
	 */
	private void setRepaint(final int row, final int col) {
		if ((row < 1) || (row > this.maxrow) || (col < 1)
				|| (col > this.maxcol)) {
			return;
		}

		final int prow = this.physicalRow(row);
		synchronized (this.repaintLock) {
			this.repaintSet.add((prow << 8) | (col - 1));
		}
	}

	/**
	 * 設定某個實際位置需要重繪
	 * 
	 * @param prow
	 * @param pcol
	 */
	private void setRepaintPhysical(final int prow, final int pcol) {
		if ((prow < 0) || (prow >= this.totalrow) || (pcol < 0)
				|| (pcol >= this.totalcol)) {
			return;
		}

		synchronized (this.repaintLock) {
			this.repaintSet.add((prow << 8) | pcol);
		}
	}

	private void setURL() {
		int v, prow, pcol;
		Iterator iter;

		iter = this.probablyurl.iterator();
		while (iter.hasNext()) {
			v = ((Integer) iter.next()).intValue();
			prow = v >> 8;
			pcol = v & 0xff;

			this.isurl[prow][pcol] = true;
			this.setRepaintPhysical(prow, pcol);
		}
	}
}

class FIFOSet {
	boolean[] contain;
	int front, rear;
	int[] set;

	/**
	 * @param range
	 *            Set 的值域 1...(range - 1)
	 */
	public FIFOSet(final int range) {
		this.front = this.rear = 0;

		// 假設最多 256 column
		this.contain = new boolean[range];
		this.set = new int[range];

		for (int i = 0; i < this.contain.length; i++) {
			this.contain[i] = false;
		}
	}

	public void add(final int v) {
		if (this.contain[v] == true) {
			return;
		}

		// XXX: 沒有檢查空間是否足夠

		this.set[this.rear] = v;
		this.contain[v] = true;

		if (++this.rear == this.set.length) {
			this.rear = 0;
		}
	}

	public boolean isEmpty() {
		return (this.front == this.rear);
	}

	public int remove() {
		int v;

		if (this.front == this.rear) {
			throw new NoSuchElementException();
		}

		v = this.set[this.front];
		this.contain[v] = false;

		if (++this.front == this.set.length) {
			this.front = 0;
		}

		return v;
	}
}
