package com.upm.miot.asp.smartresidential.element;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SrElement implements Serializable {

	private String type = null;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
