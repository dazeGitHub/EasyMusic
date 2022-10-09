package com.example.easymusic.util;

public class LrcLine {
	private String content = null;
	private String startTime = "00:00.00";
	private int duration;
	private int spanLine = 1;
	
	public LrcLine(String content) {
		this.content = content;
	}
	
	public LrcLine(String content, String startTime) {
		this.content = content;
		this.startTime = startTime;
	}
	
	public LrcLine(String content, String startTime, int duration) {
		this(content, startTime);
		this.duration = duration;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getStartTime() {
		return startTime;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	@Override
	public String toString() {
		return "startTime:" + startTime + " content:" + content;
	}

	public int getSpanLine() {
		return spanLine;
	}

	public void setSpanLine(int spanLine) {
		this.spanLine = spanLine;
	}
	
	

}
