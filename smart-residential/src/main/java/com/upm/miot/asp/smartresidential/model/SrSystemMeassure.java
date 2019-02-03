package com.upm.miot.asp.smartresidential.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class SrSystemMeassure implements Serializable {

	private Map<String, Object> measures = new HashMap<String, Object>();

	public void addMeassure(final String key, final Object measure) {
		measures.put(key, measure);
	}

	public Map<String, Object> getMeasures() {
		return measures;
	}

	public void setMeasures(Map<String, Object> measures) {
		this.measures = measures;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SrSystemMeassure [measures=").append(measures).append("]");
		return builder.toString();
	}

	
}
