/**
 * UrlChecker.java
 */
package org.zhouer.vt;

/**
 * URL checker is a thread that check the url in VT100.
 * @author Chin-Chang Yang
 */
public class UrlChecker extends Thread {

	private static volatile UrlChecker instance = null;
	
	public static UrlChecker getInstance() {
		if (instance == null) {
			synchronized(UrlChecker.class) {
				if (instance == null) {
					instance = new UrlChecker();
				}
			}
		}
		
		return instance;
	}
	
	private VT100 vt100 = null;
	
	/**
	 * This class applies singleton pattern.
	 */
	private UrlChecker() {}
	
	public void run() {
		while (true) {
			synchronized (UrlChecker.class) {
				try {
					UrlChecker.class.wait();
				} catch (InterruptedException e) {
				}

				if (vt100 != null) {
					vt100.checkURLOnScreen();
				}
			}
		}
	}

	/**
	 * Getter of vt100
	 *
	 * @return the vt100
	 */
	public VT100 getVt100() {
		return vt100;
	}

	/**
	 * Setter of vt100
	 *
	 * @param vt100 the vt100 to set
	 */
	public void setVt100(VT100 vt100) {
		this.vt100 = vt100;
	}
}
