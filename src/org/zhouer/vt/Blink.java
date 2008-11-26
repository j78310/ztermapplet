/**
 * Blink.java
 */
package org.zhouer.vt;

/**
 * Blink is the model of cursor or text blinking.
 * @author Chin-Chang Yang
 */
public class Blink {

	private boolean visible = false;

	/**
	 * Getter of visible
	 *
	 * @return the visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Setter of visible
	 *
	 * @param visible the visible to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	/**
	 * Toggle visible.
	 */
	public void toggleVisible() {
		visible = !visible;
	}
	
}
