package org.zhouer.zterm.view;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

import org.zhouer.zterm.model.Model;

/**
 * KeyEventHandler is a key event controller for ZTerm applet.
 * 
 * @author Chin-Chang Yang
 */
public class KeyEventHandler implements KeyEventDispatcher {

	private Model model;

	public boolean dispatchKeyEvent(final KeyEvent keyEvent) {
		
		// 非按下的狀況，繼續往下送。
		if (keyEvent.getID() != KeyEvent.KEY_PRESSED) {
			return false;
		}

		// 按下的狀況，若是功能鍵，則判斷要做些什麼事情。
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
				this.model.reopenCurrentSession();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_P) {
				this.model.paste();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_W) {
				this.model.closeCurrentTab();
			} else if ((KeyEvent.VK_1 <= keyEvent.getKeyCode())
					&& (keyEvent.getKeyCode() <= KeyEvent.VK_9)) {
				this.model.switchSessionTo(keyEvent.getKeyCode() - KeyEvent.VK_1);
			} else if ((keyEvent.getKeyCode() == KeyEvent.VK_LEFT)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_UP)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_Z)) {
				// meta-left,up,z 切到上一個連線視窗
				// index 是否合法在 changeSession 內會判斷
				model.switchToPreviousSession();
			} else if ((keyEvent.getKeyCode() == KeyEvent.VK_RIGHT)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_DOWN)
					|| (keyEvent.getKeyCode() == KeyEvent.VK_X)) {
				// meta-right,up,x 切到下一個連線視窗
				// index 是否合法在 changeSession 內會判斷
				model.switchToNextSession();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_HOME) {
				this.model.switchSessionTo(0);
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_END) {
				model.switchToLastSession();
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

		// 一般鍵，繼續往下送。
		return false;
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
}
