package com.example.easymusic.myview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easymusic.PlayMusicActivity;
import com.example.easymusic.util.LrcLine;

public class LrcView extends TextView {
	private static final String TAG = "LrcView";
	private int x, y;
	private int width, height;
	private List<LrcLine> lrcList;
	private int detY = 20;
	private int midY = 0;
	private int lrcHeight;
	private int currentLrcIndex;
	private int clickY;
	private int scrollY;
	private Context context;
	private boolean isScrolling;
	private int preIndex;
	
	
	public LrcView(Context context, List<LrcLine> lrcList) {
		super(context);
		this.context = context;
		this.lrcList = lrcList;
	}

	public LrcView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (lrcList == null || lrcList.size() == 0) return;
		String centerContent = lrcList.get(currentLrcIndex).getContent();
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTextSize(40);
		paint.setColor(Color.MAGENTA);
		Rect rect = new Rect();
		//画当前歌唱的文本内容
		ComputeXY(centerContent, rect, paint);
		int p = 0;//额外占p行
		if (rect.width() > width) {
			p = rect.width() / width;
			drawSpanText(centerContent, 0, midY, canvas, true);
		} else {
			canvas.drawText(centerContent, x, midY, paint);
		}
		if (isScrolling) canvas.drawLine(0, midY, width, midY, paint);//手势滑动歌词时画红线标识当前滑到的歌词
		Log.d(TAG, "p = " + p);
		//画已经播放或者跳过的文本内容
		paint.setColor(Color.GREEN);
		int n = 0;//额外占n行
		int k = 0;
		for (int i=currentLrcIndex-1; i>=0; i--) {
			String content = lrcList.get(i).getContent();
			ComputeXY(content, rect, paint);
			n = currentLrcIndex-i;
			if (rect.width() > width) {
				k += rect.width() / width;
				drawSpanText(content, 0, midY - (n+k) * lrcHeight, canvas, false);
				Log.d(TAG, "rect.width() > width : k = " + k);
			} else {
				canvas.drawText(content, x, midY - (n+k) * lrcHeight, paint);
				Log.d(TAG, "k = " + k);
			}
		}
		//画即将播放的文本内容
		int m = 0;//额外占m行
		for (int j=currentLrcIndex+1; j<lrcList.size(); j++) {
			String content = lrcList.get(j).getContent();
			ComputeXY(content, rect, paint);
			n = j - currentLrcIndex;
			if (rect.width() > width) {
				drawSpanText(content, 0, midY + (m+n+p) * lrcHeight, canvas, false);
				Log.d(TAG, "rect.width() > width : m = " + m);
				m += rect.width() / width;
			} else {
				canvas.drawText(content, x, midY + (m+n+p) * lrcHeight, paint);
				Log.d(TAG, "m = " + m);
			}
		}
	}
	
	//计算每行歌词的位置信息
	private void ComputeXY(String content, Rect rect, Paint paint) {
		paint.getTextBounds(content, 0, content.length(), rect);
		x = width / 2  - rect.centerX();
		y = height / 2 - rect.centerY();
		midY = y;
		lrcHeight = rect.height() + detY;
	}
	
	//绘制一条跨多行的歌词
	private void drawSpanText(String content, int x, int y, Canvas canvas, boolean currentText) {
		TextPaint textPaint = new TextPaint();  
		textPaint.setARGB(0xFF, 0, 255, 0);
		if (currentText) textPaint.setColor(Color.MAGENTA);
		textPaint.setTextSize(40);  
		textPaint.setAntiAlias(true);
		//当前绘制长度大于width后换行继续绘制
		StaticLayout layout = new StaticLayout(content, textPaint, width,  
		        Alignment.ALIGN_CENTER, 1.0F, 0.0F, true);  
		canvas.save();  
		canvas.translate(0, y);//从x,y开始画  
		layout.draw(canvas);  
		canvas.restore();//
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
		midY = height / 2;
	}

	public List<LrcLine> getLrcList() {
		return lrcList;
	}

	//设置歌词序列，LrcLine包装了歌词内容和开始时间
	public void setLrcList(List<LrcLine> lrcList2) {
		this.lrcList = lrcList2;
	}

	//查询当前播放到哪一条歌词
	public int getCurrentLrcIndex() {
		return currentLrcIndex;
	}

	public void setCurrentLrcIndex(int currentLrcIndex) {
		this.currentLrcIndex = currentLrcIndex;
	}

	//该接口实现功能：传递进来一个歌词文件路径即可实现歌词的解析和展示
	public void loadLrc(String localUrl) {
		Log.d(TAG, "loadLrc");
		//解析歌词的异步任务
		new ParseLrcTask(this).execute(localUrl);
	}

	//根据进度条播放的进度实时更新歌词
	public synchronized void checkLrcTime(String playedTime, int add) {
		if (lrcList == null) return;
		Log.d(TAG, "playedTime =" + playedTime + " add =" + add);
		if (add > 0) {
			for (int i=currentLrcIndex; i<lrcList.size(); i++) {
				if (lrcList.get(i).getStartTime().compareTo(playedTime) > 0) {
					currentLrcIndex = i > 0 ? i-1 : 0;
					preIndex = currentLrcIndex;
					invalidate();
					break;
				}
			}
		} else {
			for (int i=currentLrcIndex; i>=0; i--) {
				if (lrcList.get(i).getStartTime().compareTo(playedTime) > 0) {
					currentLrcIndex = i > 0 ? i-1 : 0;
					preIndex = currentLrcIndex;
					invalidate();
					break;
				}
			}
		}
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {//滚动歌词的监听
		Log.d(TAG, "onTouchEvent action = " + event.getAction());
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			clickY = (int)event.getY();
			break;
		case MotionEvent.ACTION_MOVE://滑动过程只是改变歌词显示但是不改变播放进度
			isScrolling = true;
			scrollY = (int)event.getY() - clickY;
			computeDetIndex();
			break;
		case MotionEvent.ACTION_UP://滑动结束时才更新进度
			isScrolling = false;
			checkNewProgress();
			break;
			
		}
		return true;
	}

	//计算新的歌词进度
	private void checkNewProgress() {
		if (Math.abs(currentLrcIndex - preIndex) < 1) return;
		String time = lrcList.get(currentLrcIndex).getStartTime();
		((PlayMusicActivity)context).changeProgressFromUser(transferTime(time));
		invalidate();
	}
	//计算滚动的歌词行数
	private void computeDetIndex() {
		int detIndex = -scrollY / lrcHeight;
		Log.d(TAG, "detIndex = " + detIndex);
		currentLrcIndex = detIndex + preIndex;
		if (currentLrcIndex < 0) currentLrcIndex = 0;
		if (currentLrcIndex > lrcList.size() - 1) currentLrcIndex = lrcList.size() - 1;
		
	}

	//将00：00格式的时间转换为毫秒并更新播放进度条
	private int transferTime(String time) {
		int result = 0;
		String[] str = time.split(":");
		Log.d(TAG, "str.length = " + str.length);
		result = (int)(Integer.parseInt(str[0]) * 60 + Float.parseFloat(str[1])) * 1000;
		Log.d(TAG, "result = " + result);
		return result;
	}
	
	
	//解析歌词文件的异步任务，只需传进去一个文件路径
	public class ParseLrcTask extends AsyncTask<String, Void, Void> {
		private static final String TAG  = "ParseLrcTask";
		private File file = null;
		private BufferedReader reader;
		private StringBuilder sb = new StringBuilder();
		private LrcView lrcView;
		private List<LrcLine> lrcList = new ArrayList<LrcLine>();
		
		public ParseLrcTask(LrcView lrcView) {
			this.lrcView = lrcView;
		}
		

		@Override
		protected Void doInBackground(String... params) {
			file = new File(params[0]);
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
				reader.close();
				extraLrcLine();
			} catch (Exception e) {
				e.printStackTrace();
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			return null;
		}

		//歌词解析方法
		private void extraLrcLine() {
			String[] allContents = sb.toString().split("\n");
			for (String str : allContents) {
				int lrcLineCount = str.length() - str.replaceAll("\\[", "").length();
				if (!str.replaceAll("\\d\\d:\\d\\d", "").equals(str)){
					//正则表达式匹配00：00时间格式，包含该格式说明不是标题，按   【00：00】【00：00】content 格式解析
					//注：上面两个中括号只是一个例子，实际可能3个或者更多，因为有的内容会被重复歌唱，lrcLineCount表示重复次数
					Log.d(TAG, "lrcLineCount = " + lrcLineCount);
					for (int i=0; i<lrcLineCount; i++) {
						String startTime = str.substring(str.indexOf("[")+1, str.indexOf("]"));
						String content = str.substring(str.lastIndexOf("]") + 1);
						lrcList.add(new LrcLine(content, startTime));
						Log.d(TAG, "LrcLine = " + new LrcLine(content, startTime));
						str = str.substring(str.indexOf("]") + 1);
					}
				} else {//标题解析，按【content】格式解析
					Log.d(TAG, "lrcLineCount = " + lrcLineCount);
					for (int i=0; i<lrcLineCount; i++) {
						String content = str.substring(str.indexOf("[")+1, str.indexOf("]"));
						lrcList.add(new LrcLine(content));
						Log.d(TAG, "LrcLine = " + new LrcLine(content));
						str = str.substring(str.indexOf("]") + 1);
					}
					
				}
			}
			//对歌词按时间进行排序
			Collections.sort(lrcList, new Comparator<LrcLine>() {
				public int compare(LrcLine ll1, LrcLine ll2) {
					return ll1.getStartTime().compareTo(ll2.getStartTime());
				}
				
			});
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		//解析完成后显示歌词
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			lrcView.setLrcList(lrcList);
			if (lrcList.size() == 0) {
				Toast.makeText(context, "歌词格式不对", 300).show();
			}
			lrcView.setVisibility(View.VISIBLE);
			lrcView.invalidate();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

	}

}
