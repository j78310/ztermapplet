package org.zhouer.zterm;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

/**
 * KeyEventHandler is a key event controller for ZTerm applet.
 * 
 * @author h45
 */
public class KeyEventHandler implements KeyEventDispatcher {

	private Model model;

	private ZTerm view;

	public boolean dispatchKeyEvent(final KeyEvent keyEvent) {
		// 只處理按下的狀況
		if (keyEvent.getID() != KeyEvent.KEY_PRESSED) {
			return false;
		}

		if (keyEvent.isAltDown() || keyEvent.isMetaDown()) {

			if (keyEvent.getKeyCode() == KeyEvent.VK_O) {
				this.model.copy();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_U) {
				this.model.colorCopy();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_I) {
				this.model.colorPaste();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_D) {
				this.view.siteText.selectAll();
				this.view.siteField.requestFocusInWindow();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_Q) {
				this.model.open();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_R) {
				this.model.reopenSession((Session) this.view.tabbedPane
						.getSelectedComponent());
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_S) {
				this.view.siteText.setText("ssh://"); //$NON-NLS-1$
				this.view.siteField.requestFocusInWindow();
			} else if (keyEvent.getKeyCode() == KeyEvent.VK_L) {
				this.view.siteText.setText("telnet://"); //$NON-NLS-1$
				this.view.siteField.requestFocusInWindow();
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
