package com.ycao.dualbooks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ycao.dualbooks.adapters.BookmarkAdapter;

public class BookmarkExplorer extends ListActivity {

	private List<Bookmark> item = null;
	private TextView myPath;
	private String title;
	private String path;

	private boolean item_changed = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getIntent().getIntExtra("theme", BookSelectorActivity.THEME_LIGHT) == BookSelectorActivity.THEME_DARK) {
			setTheme(android.R.style.Theme_Holo);
		}
		setContentView(R.layout.fileexplorer);
		myPath = (TextView)findViewById(R.id.path);
		myPath.setText("Bookmarks: ");
		path = getIntent().getStringExtra("bookPath");
		item = initializeBookmarks(path);

		BookmarkAdapter bookmarkList =
			new BookmarkAdapter(this, item);

		setListAdapter(bookmarkList);
		registerForContextMenu(getListView());
	}

	private ArrayList<Bookmark> initializeBookmarks(String bookPath) {
		ArrayList<Bookmark> list = new ArrayList<Bookmark>();
		File bookmark = DualBooksUtils.getBookmarkFile(bookPath);
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(bookmark));
		} catch (FileNotFoundException e) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to load bookmark file, no bookmarks");
			return list;
		}
		String line;
		try {
			while((line=br.readLine())!=null) {
				Bookmark mark = DualBooksUtils.getBookmark(line);
				if(mark != null) {
					list.add(mark);
				}
			}
		} catch (IOException e) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to load bookmark file, no bookmarks");
			return list;
		}
		return list;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Bookmark mark = item.get(position);
		int spineIdx = mark.spineIdx;
		int offSet = mark.textOffset;
		Intent in = new Intent();
		in.putExtra("spineIdx", spineIdx);
		in.putExtra("offSet", offSet);
		setResult(BookSelectorActivity.SUCCESS, in);
		if(item_changed) {
			try {
				DualBooksUtils.saveChangedBookMarks(path, item);
			} catch (IOException e) {
				Log.e(BookSelectorActivity.APP_NAME, "Failed to save bookmark file: "+e);
				Toast.makeText(this, "Failed to save bookmark changes", Toast.LENGTH_LONG).show();
			}
		}
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		TextView tv;
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in Action Bar clicked; go home
			Intent intent = new Intent(BookmarkExplorer.this, BookSelectorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return true;
		}
	}

	public static class Bookmark{
		Date date;
		String content;
		int spineIdx;
		int textOffset;

		public Bookmark(Date date, String content, int spineIdx, int textOffset) {
			this.date = date;
			this.content = content;
			this.spineIdx = spineIdx;
			this.textOffset = textOffset;
		}

		public Date getDate() {
			return date;
		}

		public String getContent() {
			return content;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		//super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bookmark_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		switch (item.getItemId()) {
		case R.id.remove_bookmark:
			info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			int pos = info.position;
			this.item.remove(pos);
			((BookmarkAdapter)getListAdapter()).notifyDataSetChanged();
			item_changed=true;
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(item_changed) {
				try {
					DualBooksUtils.saveChangedBookMarks(path, item);
				} catch (IOException e) {
					Log.e(BookSelectorActivity.APP_NAME, "Failed to save bookmark file: "+e);
					Toast.makeText(this, "Failed to save bookmark changes", Toast.LENGTH_LONG).show();
				}

			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
}