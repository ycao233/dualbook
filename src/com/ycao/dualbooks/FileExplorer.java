package com.ycao.dualbooks;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * http://android-er.blogspot.com/2010/01/implement-simple-file-explorer-in.html
 * 
 * This class is a modification of the tutorial posted by android-er
 * 
 * The accompanied layout xml file is also from android-er with minor modifications
 */
public class FileExplorer extends ListActivity {

	private List<String> item = null;
	private List<String> path = null;
	private String root="/";
	private TextView myPath;
	private String[] filters=new String[0];
	private String currDir;
	private FileFilter filter;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getIntent().getIntExtra("theme", BookSelectorActivity.THEME_LIGHT) == BookSelectorActivity.THEME_DARK) {
			setTheme(android.R.style.Theme_Holo);
		}
		setContentView(R.layout.fileexplorer);
		myPath = (TextView)findViewById(R.id.path);
		String launcher = getIntent().getStringExtra("launcher");
		setupFilter(launcher);
		
		if(new File("/sdcard").exists()) {
			getDir("/sdcard");
		} else {
			getDir(root);
		}
	}

	private void setupFilter(String launcher) {
		
		if((""+R.id.select_book_folder).equals(launcher)) {
			filter = new FileFilter() {
				public boolean accept(File file) {
					if(file.isDirectory()) {
						return true;
					}
					String name = file.getName().toLowerCase();
					return name.endsWith(".epub") || name.endsWith(".pdf");
				}
			};
		} else if((""+R.id.select_cover_image).equals(launcher)) {
			filter = new FileFilter() {
				public boolean accept(File file) {
					if(file.isDirectory()) {
						return true;
					}
					String name = file.getName().toLowerCase();
					return name.endsWith(".jpg") 
						|| name.endsWith(".jpeg")
						|| name.endsWith(".png");
				}
			};
		} else if((""+R.id.optional_load_txt).equals(launcher)) {
			filter = new FileFilter() {
				public boolean accept(File file) {
					if(file.isDirectory()) {
						return true;
					}
					String name = file.getName().toLowerCase();
					return name.endsWith(".txt") 
						|| name.endsWith(".log")
						|| name.endsWith(".xhtml")
						|| name.endsWith(".html")
						|| name.endsWith(".htm");
				}
			};
		} else {
			filter = new FileFilter() {
				public boolean accept(File file) {
						return true;
				}
			};
		}
	}
	
	private void getDir(String dirPath) {
		myPath.setText("Current Directory: " + dirPath);
		currDir = dirPath;

		item = new ArrayList<String>();
		path = new ArrayList<String>();

		File f = new File(dirPath);
		File[] files = f.listFiles(filter);
		Arrays.sort(files);

		if(!dirPath.equals(root)) {
			item.add(root);
			path.add(root);

			item.add("../");
			path.add(f.getParent());
		}

		//first add the directories
		for(File file : files) {
			if(file.isDirectory()) {
				path.add(file.getPath());
				item.add(file.getName() + "/");
			}
		}

		for(File file : files) {
			//if(file.isFile() && shouldShow(file.getName())) {
			if(file.isFile()) {
				path.add(file.getPath());
				item.add(file.getName());
			}
		}
		ArrayAdapter<String> fileList =
			new ArrayAdapter<String>(this, R.layout.row, item);
		
		setListAdapter(fileList);
	}

	private boolean shouldShow(String filename) {
		for(String f : filters) {
			if(filename.toUpperCase().endsWith(f.toUpperCase())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));
		if (file.isDirectory()) {
			if(file.canRead()) {
				getDir(path.get(position));
			} else {
				Toast.makeText(this, "Don't have permission to open "+file.getAbsolutePath()+"!", Toast.LENGTH_SHORT).show();
			}
		} else {
			Intent in = new Intent();
			in.putExtra("libraryPath", file.getParent());
			in.putExtra("imagePath", file.getAbsolutePath());
			String itemPosition = getIntent().getStringExtra("itemPosition");
			if(itemPosition != null) {
				in.putExtra("itemPosition", itemPosition);
				Log.d(BookSelectorActivity.APP_NAME, "item position is: "+itemPosition);
			}
			setResult(BookSelectorActivity.SUCCESS, in);
			finish();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if(currDir.equals(root)) {
	    		return super.onKeyDown(keyCode, event);
	    	} else { //act like move up one directory
	    		String dir=(currDir.lastIndexOf("/")==0)?root:currDir.substring(0, currDir.lastIndexOf("/"));
	    		Log.d(BookSelectorActivity.APP_NAME, "new dir: "+dir);
	    		getDir(dir);
	    		return true;
	    	}
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		TextView tv;
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in Action Bar clicked; go home
			Intent intent = new Intent(FileExplorer.this, BookSelectorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return true;
		}
	}
	
}