package org.zhouer.utils;

public class Pair {

	protected Object first, second;

	public Pair() {
		this.first = this.second = null;
	}

	public Pair(final Object f, final Object s) {
		this.first = f;
		this.second = s;
	}

	public Object getFirst() {
		return this.first;
	}

	public Object getSecond() {
		return this.second;
	}

	public void setFirst(final Object f) {
		this.first = f;
	}

	public void setSecond(final Object s) {
		this.second = s;
	}
}
