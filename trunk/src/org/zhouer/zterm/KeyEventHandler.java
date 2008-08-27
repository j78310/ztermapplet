package org.zhouer.zterm;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import org.zhouer.vt.VT100;

/**
 * KeyEventHandler is a key event controller for ZTerm applet.
 * 
 * @author h45
 */
public class KeyEventHandler implements KeyEventDispatcher {

	private Model model;

	private ZTerm view;

	public boolean dispatchKeyEvent(final KeyEvent keyEvent) {
		final SessionPane session = model.getCurrentSession();
		final VT100 vt = session.getVt();
		model.requestFocusToCurrentSession();
		
		if (keyEvent.getID() == KeyEvent.KEY_TYPED) {
			// 功能鍵，不理會
			if (keyEvent.isAltDown() || keyEvent.isMetaDown()) {
				return true;
			}

			// delete, enter, esc 會在 keyPressed 被處理
			if ((keyEvent.getKeyChar() == KeyEvent.VK_DELETE)
					|| (keyEvent.getKeyChar() == KeyEvent.VK_ENTER)
					|| (keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE)) {
				return true;
			}

			// 一般按鍵，直接送出
			session.writeChar(keyEvent.getKeyChar());
		}
		
		if (keyEvent.getID() != KeyEvent.KEY_PRESSED) {
			return true;
		}

		if (keyEvent.isAltDown() || keyEvent.isMetaDown()) {

			if (keyEvent.getKeyCode() == KeyEvent.VK_O) {
				this.model.copy();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_U) {
				this.model.colorCopy();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_I) {
				this.model.colorPaste();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_Q) {
				this.model.open();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_R) {
				this.model.reopenSession((SessionPane) this.view.tabbedPane
						.getSelectedComponent());
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_P) {
				this.model.paste();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_W) {
				this.model.closeCurrentTab();
			} else if ((KeyEvent.VK_1 <= keyEvent.getKeyCode())
					&& (keyEvent.getKeyCode() <= KeyEvent.VK_9)) {
				this.model.changeSession(keyEvent.getKeyCode() - KeyEvent.VK_1);
			} else if ((keyEvent.getKeyCode() == KeyEvent.VK_LEFT)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_UP)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_Z)) {
				// meta-left,up,z 切到上一個連線視窗
				// index 是否合法在 changeSession 內會判斷

				this.model.changeSession(this.view.tabbedPane
						.getSelectedIndex() - 1);
			} else if ((keyEvent.getKeyCode() == KeyEvent.VK_RIGHT)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_DOWN)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_X)) {
				// meta-right,up,x 切到下一個連線視窗
				// index 是否合法在 changeSession 內會判斷

				this.model.changeSession(this.view.tabbedPane
						.getSelectedIndex() + 1);
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_HOME) {
				this.model.changeSession(0);
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_END) {
				this.model
						.changeSession(this.view.tabbedPane.getTabCount() - 1);
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_COMMA) {
				// alt-, 開啟偏好設定
				this.model.showPreference();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_PERIOD) {
				// alt-. 開啟站台管理
				this.model.showSiteManager();
			} else {
				// 雖然按了 alt 或 meta, 但不認識，
				// 繼續往下送。
				return false;
			}

			// 功能鍵不再往下送
			return true;
		}
		
		int len;
		final byte[] buf = new byte[4];

		// 其他功能鍵
		switch (keyEvent.getKeyCode()) {
		case KeyEvent.VK_UP:
			if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
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
			if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
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
			if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
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
			if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
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
			if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
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
			if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD) {
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
			session.writeBytes(buf, 0, len);
			return true;
		}

		if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
			session.writeByte((byte) 0x1b);
			return true;
		}

		if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
			session.writeByte((byte) 0x0d);
			return true;
		}
		
		return true;
	}

	/**
	 * Setter of model
	 * 
	 * @param model
	 *            the model to set
	 */
	public void setModel(final Model model) {
		this.model = model;
	}

	/**
	 * Setter of view
	 * 
	 * @param view
	 *            the view to set
	 */
	public void setView(final ZTerm view) {
		this.view = view;
	}

}
