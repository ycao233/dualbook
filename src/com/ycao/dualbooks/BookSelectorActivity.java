package com.ycao.dualbooks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.vudroid.pdfdroid.codec.PDFLibraryLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.ycao.dualbooks.adapters.BookIconAdapter;

public class BookSelectorActivity extends Activity {

	public final static int SUCCESS=1;
	public final static String APP_NAME="DualBooks";
	public final static String DEFAULT_LIBRARY_PATH="/sdcard/Books";

	public final static int THEME_LIGHT=0;
	public final static int THEME_DARK=1;

	private GridView bookGrid;
	private ListView folders;
	private int theme = THEME_LIGHT;

	private BookIconAdapter bookLibraryThumbnail;
	private ProgressDialog pd;

	private List<String> bookFolders;
	private List<String> bookFolderNames;

	private Map<String, BookIconAdapter> libViewCache = new ConcurrentHashMap<String, BookIconAdapter>();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		PDFLibraryLoader.load();
		initDualBooks();

		folders = (ListView) findViewById(R.id.folder_list);

		pd = ProgressDialog.show(BookSelectorActivity.this, "Loading", "Please wait while the library is been\npopulated from: "+bookFolders.get(0), true);

		ArrayAdapter<String> list =
			new ArrayAdapter<String>(this, R.layout.book_folders, bookFolderNames);
		folders.setAdapter(list);

		bookGrid = (GridView) findViewById(R.id.BookGrid);

		initializeLibraryView(bookFolders.get(0));
		setupClickListeners();
	}

	private void setupClickListeners() {

		folders.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				initializeLibraryView(bookFolders.get(position));
			}
		});

		folders.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> p, View v, final int position, long id) {
				AlertDialog.Builder alert = new AlertDialog.Builder(BookSelectorActivity.this);

				alert.setTitle("Modify Folders");
				alert.setMessage("Rename Book Folder: ");

				// Set an EditText view to get user input 
				final EditText input = new EditText(BookSelectorActivity.this);
				alert.setView(input);

				alert.setPositiveButton("Rename Folder", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						bookFolderNames.set(position, value);
						((ArrayAdapter)(folders.getAdapter())).notifyDataSetChanged();
						preserveSettings();
					}
				});

				alert.setNegativeButton("Delete Folder", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						bookFolders.remove(position);
						bookFolderNames.remove(position);
						((ArrayAdapter)(folders.getAdapter())).notifyDataSetChanged();
						preserveSettings();
					}
				});

				alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				alert.show();
				return true;
			}

		});

	}

	private void initializeLibraryView(final String path) {
		new Thread(new Runnable() {
			public void run() {
				bookLibraryThumbnail = getLibraryViewByPath(path);
				handler.sendEmptyMessage(0);
			}
		}).start();

	}

	private BookIconAdapter getLibraryViewByPath(final String path) {
		if(libViewCache.containsKey(path)) {
			return libViewCache.get(path);
		}

		synchronized(this) {
			if(pd == null) {
				bookGrid.post(new Runnable(){
					public void run() {
						pd = ProgressDialog.show(BookSelectorActivity.this, "Loading", 
								"Please wait while the library is been\npopulated from: "+path, true);
					}
				});
			}
		}

		BookIconAdapter adapter = new BookIconAdapter(BookSelectorActivity.this, path);
		libViewCache.put(path, adapter);

		return adapter;
	}

	private void initDualBooks() {
		File setting = new File(DEFAULT_LIBRARY_PATH+"/.DualBookSettings");
		if(!setting.exists()) {
			setting.mkdirs();
		}
		File folderList = new File(setting.getAbsolutePath()+"/.bookFolderList");
		if(folderList.exists()) {
			try {
				initBookFolders(folderList);
			} catch (IOException e) {
				Log.e(APP_NAME, "Failed to initialize book folders: "+e);
				getDefaultBooks();
			}
		} else {
			PrintWriter pw;
			try {
				pw = new PrintWriter(new BufferedWriter(new FileWriter(folderList)));
				pw.println("Default"+DualBooksUtils.SEP+DEFAULT_LIBRARY_PATH);
				pw.close();
			} catch (IOException e) {
				Log.e(APP_NAME, "Failed to generate initial book folders: "+e);
			}
			getDefaultBooks();
		}

		//init theme
		File themeFile = new File(setting.getAbsolutePath()+"/.theme");
		try{
			if(folderList.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(themeFile));
				theme = Integer.parseInt(br.readLine());
			}
		} catch (IOException ioe) {
			theme = THEME_LIGHT;
		}
	}

	private void preserveSettings() {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(DEFAULT_LIBRARY_PATH
					+"/.DualBookSettings/.bookFolderList"));
			for(int i=0; i<bookFolders.size(); ++i) {
				pw.println(bookFolderNames.get(i)+DualBooksUtils.SEP+bookFolders.get(i));
			}
			pw.close();
			pw = new PrintWriter(new FileWriter(DEFAULT_LIBRARY_PATH +"/.DualBookSettings/.theme"));
			pw.println(theme);
			pw.close();
		} catch (IOException e) {
			Log.e(APP_NAME, "Failed to preserve settings: "+e);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		preserveSettings();
	}

	private void getDefaultBooks() {
		bookFolders = new ArrayList<String>();
		bookFolders.add(DEFAULT_LIBRARY_PATH);
		bookFolderNames = new ArrayList<String>();
		bookFolderNames.add("Books");
	}

	private void initBookFolders(File list) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(list));
		String line = null;
		ArrayList<String> path = new ArrayList<String>();
		ArrayList<String> names = new ArrayList<String>();
		while((line = br.readLine()) != null) {
			String[] tokens = line.split(DualBooksUtils.SEP);
			names.add(tokens[0]);
			path.add(tokens[1]);
		}
		bookFolders = path;
		bookFolderNames = names;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.isCheckable()) {
			item.setChecked(true);
		} 
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.select_book_folder:
			Intent populateLibrary = new Intent(BookSelectorActivity.this, FileExplorer.class);
			populateLibrary.putExtra("launcher", ""+R.id.select_book_folder);
			populateLibrary.putExtra("theme", theme);
			startActivityForResult(populateLibrary, R.id.select_book_folder);
			return true;
		case R.id.light_theme:
			theme = THEME_LIGHT;
			Toast.makeText(this, "Dark font over light background activated.", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.dark_theme:
			theme = THEME_DARK;
			Toast.makeText(this, "Light font over dark background activated.", Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == R.id.select_book_folder && resultCode == BookSelectorActivity.SUCCESS){

			String libraryPath = data.getStringExtra("libraryPath");
			bookFolders.add(libraryPath);
			if(libraryPath.equals("/")) {
				bookFolderNames.add("Root Folder");
			} else {
				bookFolderNames.add(libraryPath.substring(libraryPath.lastIndexOf("/")+1));
			}

			((ArrayAdapter)folders.getAdapter()).notifyDataSetChanged();
			initializeLibraryView(libraryPath);

		} else if(requestCode == R.id.select_cover_image && resultCode == BookSelectorActivity.SUCCESS) {

			String imageFile = data.getStringExtra("imagePath");
			int itemPosition = Integer.parseInt(data.getStringExtra("itemPosition"));
			String bookPath = (String) bookGrid.getAdapter().getItem(itemPosition);
			Log.d(APP_NAME, "selected item: "+bookPath);
			DualBooksUtils.copyFile(imageFile, DualBooksUtils.generateCoverPageFileName(bookPath), this);
			((BookIconAdapter)bookGrid.getAdapter()).setChanged(itemPosition);
			((BookIconAdapter)bookGrid.getAdapter()).notifyDataSetChanged();

		}
	}


	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(bookGrid == null) {
				bookGrid = (GridView) findViewById(R.id.BookGrid);
			}
			bookGrid.setAdapter(bookLibraryThumbnail);
			bookGrid.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					String launcher = getIntent().getStringExtra("launcher");
					String path = (String)bookGrid.getItemAtPosition(position);
					if(launcher != null && "ReaderPanel".equals(launcher)) {
						//launched as select a new book from context menu
						Log.d("DualBooks", "selected new book: "+path);
						Intent in = new Intent();
						in.putExtra("bookPath", path);
						setResult(SUCCESS,in);
						finish();	
					} else {
						//launched as home activity
						Intent readThisBook = new Intent(BookSelectorActivity.this, ReaderPanelActivity.class);
						readThisBook.putExtra("theme", theme);
						Log.d("DualBooks", "started a new book: "+path);
						readThisBook.putExtra("bookPath", path);
						startActivity(readThisBook);
					}
				}
			});
			registerForContextMenu(bookGrid);
			synchronized(BookSelectorActivity.this) {
				if(pd != null) {
					pd.dismiss();
					pd = null;
				}
			}
		}
	}; // handler

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		//super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.book_context_menu, menu);
		menu.setHeaderTitle("Book Covers");
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent i;
		AdapterView.AdapterContextMenuInfo info;
		switch (item.getItemId()) {
		case R.id.select_cover_image:
			i = new Intent(BookSelectorActivity.this, FileExplorer.class);
			i.putExtra("launcher", ""+R.id.select_cover_image);
			info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			i.putExtra("itemPosition", ""+info.position);
			i.putExtra("theme", theme);
			startActivityForResult(i, R.id.select_cover_image);
			return true;
		case R.id.remove_cover_image:
			info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			String bookPath = (String) bookGrid.getAdapter().getItem(info.position);
			File coverImage = new File(DualBooksUtils.generateCoverPageFileName(bookPath));
			Log.d(APP_NAME, "removing item: "+coverImage.getAbsolutePath());
			coverImage.delete();
			((BookIconAdapter)bookGrid.getAdapter()).setChanged(info.position);
			((BookIconAdapter)bookGrid.getAdapter()).notifyDataSetChanged();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

}