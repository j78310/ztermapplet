package org.zhouer.vt;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.zhouer.zterm.Model;
import org.zhouer.zterm.Session;

public class User implements KeyListener, MouseListener, MouseMotionListener {
	private final Config config;
	private boolean isDefaultCursor;
	private final Session parent;
	private int pressX, pressY, dragX, dragY;
	private final VT100 vt;

	public User(final Session p, final VT100 v, final Config c) {
		this.parent = p;
		this.vt = v;
		this.config = c;
		this.isDefaultCursor = true;
	}

	public void keyPressed(final KeyEvent e) {
		
		if (parent.isDisconnected() && e.getKeyCode() == KeyEvent.VK_ENTER) {
			Model.getInstance().reopenSession(parent);
		} else if (parent.isDisconnected()) {
			return;
		}
		
		int len;
		final byte[] buf = new byte[4];

		/*
		 * System.out.println( "key presses: " + e ); System.out.println( "key
		 * modifier: " + e.getModifiers() );
		 */

		// 其他功能鍵
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			if (this.vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
				buf[0] = 0x1b;
				buf[1] = 0x5b;
				buf[2] = 'A';
				len = 3;
			} else {
				buf[0] = 0x1b;
				buf[1] = 0x4f;
				buf[2] = 'A';
				len = 3;
			}
			break;
		case KeyEvent.VK_DOWN:
			if (this.vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
				buf[0] = 0x1b;
				buf[1] = 0x5b;
				buf[2] = 'B';
				len = 3;
			} else {
				buf[0] = 0x1b;
				buf[1] = 0x4f;
				buf[2] = 'B';
				len = 3;
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (this.vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
				buf[0] = 0x1b;
				buf[1] = 0x5b;
				buf[2] = 'C';
				len = 3;
			} else {
				buf[0] = 0x1b;
				buf[1] = 0x4f;
				buf[2] = 'C';
				len = 3;
			}
			break;
		case KeyEvent.VK_LEFT:
			if (this.vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
				buf[0] = 0x1b;
				buf[1] = 0x5b;
				buf[2] = 'D';
				len = 3;
			} else {
				buf[0] = 0x1b;
				buf[1] = 0x4f;
				buf[2] = 'D';
				len = 3;
			}
			break;
		case KeyEvent.VK_INSERT:
			buf[0] = 0x1b;
			buf[1] = 0x5b;
			buf[2] = '2';
			buf[3] = '~';
			len = 4;
			break;
		case KeyEvent.VK_HOME:
			if (this.vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
				buf[0] = 0x1b;
				buf[1] = 0x5b;
				buf[2] = '1';
				buf[3] = '~';
				len = 4;
			} else {
				buf[0] = 0x1b;
				buf[1] = 0x4f;
				buf[2] = 'H';
				len = 3;
			}
			break;
		case KeyEvent.VK_PAGE_UP:
			buf[0] = 0x1b;
			buf[1] = 0x5b;
			buf[2] = '5';
			buf[3] = '~';
			len = 4;
			break;
		case KeyEvent.VK_DELETE:
			buf[0] = 0x1b;
			buf[1] = 0x5b;
			buf[2] = '3';
			buf[3] = '~';
			len = 4;
			break;
		case KeyEvent.VK_END:
			if (this.vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
				buf[0] = 0x1b;
				buf[1] = 0x5b;
				buf[2] = '4';
				buf[3] = '~';
				len = 4;
			} else {
				buf[0] = 0x1b;
				buf[1] = 0x4f;
				buf[2] = 'F';
				len = 3;
			}
			break;
		case KeyEvent.VK_PAGE_DOWN:
			buf[0] = 0x1b;
			buf[1] = 0x5b;
			buf[2] = '6';
			buf[3] = '~';
			len = 4;
			break;
		default:
			len = 0;
		}

		if (len != 0) {
			this.parent.writeBytes(buf, 0, len);
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			// XXX: 在 Mac 上 keyTyped 似乎收不到 esc
			this.parent.writeByte((byte) 0x1b);
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			// XXX: ptt 只吃 0x0d, 只送 0x0a 沒用
			this.parent.writeByte((byte) 0x0d);
			return;
		}

	}

	public void keyReleased(final KeyEvent e) {
	}

	public void keyTyped(final KeyEvent e) {
		// System.out.println( "key typed: " + e );

		// 功能鍵，不理會
		if (e.isAltDown() || e.isMetaDown()) {
			return;
		}

		// delete, enter, esc 會在 keyPressed 被處理
		if ((e.getKeyChar() == KeyEvent.VK_DELETE)
				|| (e.getKeyChar() == KeyEvent.VK_ENTER)
				|| (e.getKeyChar() == KeyEvent.VK_ESCAPE)) {
			return;
		}

		// 一般按鍵，直接送出
		this.parent.writeChar(e.getKeyChar());
	}

	public void mouseClicked(final MouseEvent e) {
		// System.out.println( e );

		do {
			if (e.getButton() == MouseEvent.BUTTON1) {
				// 左鍵
				if (this.vt.coverURL(e.getX(), e.getY())) {
					// click
					// 開啟瀏覽器
					final String url = this.vt.getURL(e.getX(), e.getY());

					if (url.length() != 0) {
						this.parent.openExternalBrowser(url);
					}
					break;
				} else if (e.getClickCount() == 2) {
					// double click
					// 選取連續字元
					this.vt.selectConsequtive(e.getX(), e.getY());
					this.vt.repaint();
					break;
				} else if (e.getClickCount() == 3) {
					// triple click
					// 選取整行
					this.vt.selectEntireLine(e.getX(), e.getY());
					this.vt.repaint();
					break;
				}
			} else if (e.getButton() == MouseEvent.BUTTON2) {
				// 中鍵
				// 貼上
				if (e.isControlDown()) {
					// 按下 ctrl 則彩色貼上
					this.parent.colorPaste();
				} else {
					this.parent.paste();
				}
				break;
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				// 右鍵
				// 跳出 popup menu
				this.parent.showPopup(e.getX(), e.getY());
				break;
			}

			this.vt.requestFocusInWindow();
			this.vt.resetSelected();
			this.vt.repaint();

		} while (false);
	}

	public void mouseDragged(final MouseEvent e) {
		this.dragX = e.getX();
		this.dragY = e.getY();

		this.vt.setSelected(this.pressX, this.pressY, this.dragX, this.dragY);
		this.vt.repaint();
	}

	public void mouseEntered(final MouseEvent e) {
	}

	public void mouseExited(final MouseEvent e) {
	}

	public void mouseMoved(final MouseEvent e) {
		final boolean cover = this.vt.coverURL(e.getX(), e.getY());

		// 只有滑鼠游標需改變時才 setCursor
		if (this.isDefaultCursor && cover) {
			this.vt.setCursor(new Cursor(Cursor.HAND_CURSOR));
			this.isDefaultCursor = false;
		} else if (!this.isDefaultCursor && !cover) {
			this.vt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			this.isDefaultCursor = true;
		}
	}

	public void mousePressed(final MouseEvent e) {
		this.pressX = e.getX();
		this.pressY = e.getY();
	}

	public void mouseReleased(final MouseEvent e) {
		boolean meta, ctrl;

		// 只處理左鍵
		if (e.getButton() != MouseEvent.BUTTON1) {
			return;
		}

		meta = e.isAltDown() || e.isMetaDown();
		ctrl = e.isControlDown();

		// select 時按住 meta 表反向，即：
		// 若有 copy on select 則按住 meta 代表不複製，若沒有 copy on select 則按住 meta 代表要複製。
		if (this.config.getBooleanValue(Config.COPY_ON_SELECT) == meta) {
			return;
		}

		// ctrl 代表複製時包含色彩。
		if (ctrl) {
			this.parent.colorCopy();
		} else {
			this.parent.copy();
		}
	}
}
