package com.example.easymusic.mysql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {
	
	private final String CREATE_TABLE_LOCALMUSIC = "create table stored_music(" +
			"_id integer primary key autoincrement,title,artist,duration,url)";
	private final String CREATE_TABLE_DOWNLOADING_MUSIC = "create table downloading_music(" +
			"_id integer primary key autoincrement, title,artist,url,file,fileSize,threadNum,currentSize,downloadedSize)";

	public MyDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_LOCALMUSIC);
		db.execSQL(CREATE_TABLE_DOWNLOADING_MUSIC);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
