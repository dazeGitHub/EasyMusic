package com.example.easymusic.util;

import android.media.MediaPlayer;

public class StringUtil {

	// 将音乐时长-毫秒转换为00:00格式
	public static String formatDuration(long dur) {
		long totalSecond = dur / 1000;
		String minute = totalSecond / 60 + "";
		if (minute.length() < 2)
			minute = "0" + minute;
		String second = totalSecond % 60 + "";
		if (second.length() < 2)
			second = "0" + second;
		return minute + ":" + second;
	}
	
	public static String removeReg(String source, String reg) {
		if(source == null){
			return "";
		}
		if (reg != null) {
			return source.replaceAll(reg, "");
		}
		//保留中文和英文字符
		return source.replaceAll("[^a-zA-Z \u4e00-\u9fa5]", "");
	}
	
	public static String getMusicDuration(String url) {
		int duration = 0;
		MediaPlayer mp = null;
		try {
			mp = new MediaPlayer();
			mp.reset();
			mp.setDataSource(url);
			mp.prepare();
			duration = mp.getDuration();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mp.release();
		}
		return formatDuration(duration);
		
	}
	
	public static long getMusicLongDuration(String url) {
		long duration = 0;
		MediaPlayer mp = null;
		try {
			mp = new MediaPlayer();
			mp.reset();
			mp.setDataSource(url);
			mp.prepare();
			duration = mp.getDuration();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mp.release();
		}
		return duration;
		
	}

}
