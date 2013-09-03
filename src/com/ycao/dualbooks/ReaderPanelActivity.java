package com.ycao.dualbooks;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.ycao.dualbooks.panel.EpubReader;
import com.ycao.dualbooks.panel.ImageNotesPane;
import com.ycao.dualbooks.panel.NotesPane;
import com.ycao.dualbooks.panel.PDFReader;
import com.ycao.dualbooks.panel.SynchronizableScrollView;
import com.ycao.dualbooks.panel.WebPane;

public class ReaderPanelActivity extends Activity implements EpubReaderCallback, ScrollEventListener {

	public static enum Mode {
		NONE,
		READING,
		IMAGE_NOTE,
		NOTES,
		WEB
	};

	enum Type {
		PDF,
		EPUB,
	};

	private ReaderFragment readerPane;
	private OptionalFragment secondPane;

	private int theme=BookSelectorActivity.THEME_LIGHT;

	private Mode mode = Mode.NONE;

	private boolean synchronizedScrolling = true;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getIntent().getIntExtra("theme", BookSelectorActivity.THEME_LIGHT) == BookSelectorActivity.THEME_DARK) {
			setTheme(android.R.style.Theme_Holo);
			theme=BookSelectorActivity.THEME_DARK;
		}
		setContentView(R.layout.reading_panel);

		/* main panel setup */
		readerPane = (ReaderFragment) getFragmentManager().findFragmentById(R.id.readingPane);
		readerPane.activity = this;
		readerPane.theme = this.theme;

		/* secondary panel setup */
		secondPane = (OptionalFragment) getFragmentManager().findFragmentById(R.id.secondPane);
		secondPane.activity = this;
		secondPane.theme = this.theme;
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.hide(secondPane);
		ft.commit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.panel_option_menu, menu);
		return true;
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() != R.id.sync_scroll && item.isCheckable()) {
			item.setChecked(true);
		} 
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.sync_scroll:
			if(item.isChecked()) {
				item.setChecked(false);
				item.setIcon(R.drawable.icon_empty_checkbox);
				synchronizedScrolling = false;
			} else {
				item.setChecked(true);
				item.setIcon(R.drawable.icon_checked_checkbox);
				synchronizedScrolling = true;
			}
			return true;
		case android.R.id.home:
			// app icon in Action Bar clicked; go home
			Intent intent = new Intent(ReaderPanelActivity.this, BookSelectorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("theme", theme);
			startActivity(intent);
			return true;
		case R.id.select_none:
			mode = Mode.NONE;
			hideSecondPanel();
			return true;
		case R.id.select_my_notes:
			mode = Mode.NOTES;
			showSecondPanel();
			secondPane.showNotesPanel();
			secondPane.initNotes();
			return true;
		case R.id.select_second_read_pane:
			mode = Mode.READING;
			showSecondPanel();
			secondPane.showReadingPanel();
			return true;
		case R.id.select_google:
			mode = Mode.WEB;
			showSecondPanel();
			secondPane.showWebPanel();
			return true;
		case R.id.select_images:
			mode = Mode.IMAGE_NOTE;
			showSecondPanel();
			secondPane.showImagePanel();
			return true;
		case R.id.font_decrease:
			readerPane.epubReader.fontChange(-6);
			secondPane.epubReader.fontChange(-6);
			return true;
		case R.id.font_increase:
			readerPane.epubReader.fontChange(6);
			secondPane.epubReader.fontChange(6);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	private void hideSecondPanel() {
		readerPane.epubReader.markCurrentTextOffset();
		readerPane.epubReader.explicitScroll = true;
		View bar = findViewById(R.id.vertical_bar);
		bar.setVisibility(View.INVISIBLE);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.hide(secondPane);
		ft.commit();
	}

	private void showSecondPanel() {
		readerPane.epubReader.markCurrentTextOffset();
		readerPane.epubReader.explicitScroll = true;

		View bar = findViewById(R.id.vertical_bar);
		bar.setVisibility(View.VISIBLE);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.show(secondPane);
		ft.commit();
		secondPane.epubReader.setTextSize(readerPane.epubReader.getTextSize());
	}

	@Override
	public String getCurrentPrimaryBookPath() {
		if(readerPane.bookType == Type.EPUB) {
			return readerPane.epubReader.bookPath;
		} else if(readerPane.bookType  == Type.PDF){
			return readerPane.pdfReader.pdfPath;
		}
		return "";
	}

	@Override
	public String getCurrentPrimaryBookHref() {
		if(readerPane.bookType == Type.EPUB) {
			return readerPane.epubReader.resourceHref;
		} else if(readerPane.bookType  == Type.PDF){
			return "pdf";
		}
		return "";
	}

	@Override
	public void chapterChangedCallback(boolean primary) {
		if(primary) {
			if( mode==Mode.IMAGE_NOTE ) {
				secondPane.imagePane.initImagePane();
			} else if(mode == Mode.NOTES) {
				secondPane.initNotes();
			}    
		}
	}

	@Override
	public void onScrollChanged(SynchronizableScrollView scrollView, int x,
			int y, int oldx, int oldy) {
		if(synchronizedScrolling == true) {
			if(scrollView == readerPane.epubReader.scroller 
					&& secondPane.epubReader.scroller != null) 
			{
				secondPane.epubReader.scroller.scrollTo(x, y);
			}
		}
	}

	public void gotoPdfPage(final PDFReader reader) {
		AlertDialog.Builder alert = new AlertDialog.Builder(ReaderPanelActivity.this);

		alert.setTitle("PDF Navigation");
		alert.setMessage("Go to Page Number (1 - "+reader.getDocPageCount()+") : ");

		// Set an EditText view to get user input 
		final EditText input = new EditText(ReaderPanelActivity.this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);

		alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				try {
					int page = (Integer.parseInt(input.getText().toString().trim())-1);
					if(page < 0) { 
						page = 0; 
					} else if(page >= reader.getDocPageCount()) {
						page = reader.getDocPageCount()-1;
					}
					reader.gotoPage(page);
					Toast.makeText(ReaderPanelActivity.this, "Load page "+(page+1), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(ReaderPanelActivity.this, "Failed to load page", Toast.LENGTH_SHORT).show();
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//do nothing
			}
		});
		alert.show();
	}
	/**
	 * First Reading panel
	 * @author ycao
	 */
	public static class ReaderFragment extends Fragment {

		int theme=BookSelectorActivity.THEME_LIGHT;
		ReaderPanelActivity activity;

		EpubReader epubReader;
		PDFReader pdfReader;

		Type bookType;

		@Override
		public void onActivityCreated(Bundle state) {
			super.onActivityCreated(state);
			//initialize the book reader
			String path = getActivity().getIntent().getStringExtra("bookPath");
			if(DualBooksUtils.isPdf(path)) {
				initPdf(path);
			} else {
				initBook(path);
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.panel_fragment, container, false);
			epubReader = (EpubReader) v.findViewById(R.id.epub_panel_reader);
			epubReader.setupCallbackContoller((ReaderPanelActivity)getActivity());
			epubReader.primary = true;

			pdfReader= (PDFReader) v.findViewById(R.id.pdf_panel_reader);

			registerForContextMenu(epubReader.readPane);
			registerForContextMenu(pdfReader.pdfPane);

			return v;
		}

		public void initBook(String path) {
			bookType = Type.EPUB;
			epubReader.initailizeBook(path);
			epubReader.setVisibility(View.VISIBLE);
			pdfReader.setVisibility(View.INVISIBLE);
		}

		public void initPdf(String path) {
			bookType = Type.PDF;
			pdfReader.initializePdf(path);
			epubReader.setVisibility(View.INVISIBLE);
			pdfReader.setVisibility(View.VISIBLE);
		}

		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			MenuInflater inflater = getActivity().getMenuInflater();
			switch(v.getId()) {
			case R.id.epub_textPane: 
				inflater.inflate(R.menu.reader_pane_context_menu, menu);
				//TODO delete
				//				MenuItem sync_scroll = menu.findItem(R.id.sync_scroll);
				//				sync_scroll.setChecked(activity.synchronizedScrolling);
				break;
			case R.id.pdfPane:
				inflater.inflate(R.menu.pdf_pane_context_menu, menu);
				break;
			}
			menu.setHeaderTitle("Book Navigation");
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
			Intent i;
			String path;
			switch (item.getItemId()) {
			case R.id.select_new_book:
				i = new Intent(getActivity(), BookSelectorActivity.class);
				i.putExtra("launcher", "ReaderPanel");
				startActivityForResult(i, item.getItemId());
				return true;
			case R.id.toc:
				i = new Intent(getActivity(), TOCExplorer.class);
				i.putExtra("launcher", "ReaderPanel");
				path = epubReader.bookPath;
				if(path == null) {
					Log.d(BookSelectorActivity.APP_NAME, "book is null????");
					return true;
				}
				i.putExtra("bookPath", path);
				i.putExtra("theme", theme);
				Log.d(BookSelectorActivity.APP_NAME, "start activity TOCExplorer");
				startActivityForResult(i, item.getItemId());
				return true;
			case R.id.new_bookmark:
				epubReader.createBookmark();
				return true;
			case R.id.view_bookmarks:
				i = new Intent(getActivity(), BookmarkExplorer.class);
				i.putExtra("launcher", "ReaderPanel");
				path = epubReader.bookPath;
				if(path == null) {
					Log.d(BookSelectorActivity.APP_NAME, "view bookmark, book is null????");
					return true;
				}
				i.putExtra("bookPath", path);
				i.putExtra("theme", theme);
				Log.d(BookSelectorActivity.APP_NAME, "start activity BookmarkExplorer");
				startActivityForResult(i, item.getItemId());
				return true;
				//TODO delete
				/*
			case R.id.sync_scroll:
				if(item.isChecked()) {
					activity.synchronizedScrolling = false;
				} else {
					activity.synchronizedScrolling = true;
				}
				return true;
				 */
			case R.id.goto_page:
				activity.gotoPdfPage(pdfReader);
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if(resultCode==BookSelectorActivity.SUCCESS){
				if(requestCode == R.id.select_new_book) {
					String path = data.getStringExtra("bookPath");
					if(DualBooksUtils.isPdf(path)) {
						initPdf(path);
					} else {
						initBook(path);
					}
					activity.secondPane.imagePaneInitialized = false;
					if(activity.mode == Mode.IMAGE_NOTE) {
						activity.secondPane.imagePane.initImagePane();
					} else if(activity.mode == Mode.NOTES) {
						activity.secondPane.initNotes();
					}
				} else if(requestCode == R.id.toc) {
					String href = data.getStringExtra("href");
					epubReader.readHref(href);
				} else if(requestCode == R.id.view_bookmarks) {
					epubReader.gotoBookmark(data);
				}
			}
		}

	} //ReaderFragment

	/**
	 * Second optional panel
	 * @author ycao
	 */
	public static class OptionalFragment extends Fragment {

		int theme=BookSelectorActivity.THEME_LIGHT;

		Type bookType;

		//Epub reader
		EpubReader epubReader;

		//PDF reader
		PDFReader pdfReader;

		//notes panel 
		NotesPane notesPane;

		//web panel
		WebPane webPane;

		//image notes pane
		ImageNotesPane imagePane;
		boolean imagePaneInitialized = false;

		ReaderPanelActivity activity;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.optional_panel_fragment, container, false);

			//epub reader
			epubReader = (EpubReader) v.findViewById(R.id.opt_epub_reader);
			epubReader.setupCallbackContoller((ReaderPanelActivity) getActivity());
			Log.d(BookSelectorActivity.APP_NAME, "initialized reading panel");


			//notes panel -- started as hidden
			notesPane = (NotesPane) v.findViewById(R.id.opt_notes_pane);
			Log.d(BookSelectorActivity.APP_NAME, "initialized notes text panel");

			//image notes
			imagePane = (ImageNotesPane) v.findViewById(R.id.opt_image_notes_pane);
			imagePane.setupCallbackContoller((ReaderPanelActivity) getActivity());
			Log.d(BookSelectorActivity.APP_NAME, "initialized notes image panel");

			//pdf reader
			pdfReader = (PDFReader) v.findViewById(R.id.opt_pdf_reader);
			Log.d(BookSelectorActivity.APP_NAME, "initialized pdf reader");

			//web view
			webPane = (WebPane) v.findViewById(R.id.opt_web_pane);
			Log.d(BookSelectorActivity.APP_NAME, "initialized web panel");

			registerForContextMenu(epubReader.readPane);
			registerForContextMenu(pdfReader.pdfPane);
			registerForContextMenu(notesPane.notes);
			return v;
		}

		public void initBook(String path) {
			bookType = Type.EPUB;
			epubReader.initailizeBook(path);
			epubReader.setVisibility(View.VISIBLE);
			pdfReader.setVisibility(View.INVISIBLE);
		}

		public void initPdf(String path) {
			bookType = Type.PDF;
			pdfReader.initializePdf(path);
			epubReader.setVisibility(View.INVISIBLE);
			pdfReader.setVisibility(View.VISIBLE);
		}


		public void initNotes() {
			//internal notes 
			notesPane.initNotes(DualBooksUtils.getNotePath(activity.getCurrentPrimaryBookPath(), 
					activity.getCurrentPrimaryBookHref()));
		}

		public void initNotes(String path) {
			notesPane.initNotes(path);
		}

		public void showReadingPanel() {
			notesPane.setVisibility(View.INVISIBLE);
			imagePane.setVisibility(View.INVISIBLE);
			webPane.setVisibility(View.INVISIBLE);
			pdfReader.setVisibility(View.INVISIBLE);

			epubReader.setVisibility(View.VISIBLE);
		}

		public void showNotesPanel() {
			imagePane.setVisibility(View.INVISIBLE);
			epubReader.setVisibility(View.INVISIBLE);
			pdfReader.setVisibility(View.INVISIBLE);
			webPane.setVisibility(View.INVISIBLE);

			notesPane.setVisibility(View.VISIBLE);
		}

		public void showImagePanel() {
			notesPane.setVisibility(View.INVISIBLE);
			epubReader.setVisibility(View.INVISIBLE);
			pdfReader.setVisibility(View.INVISIBLE);
			webPane.setVisibility(View.INVISIBLE);

			if(!imagePaneInitialized) {
				imagePane.initImagePane();
				imagePaneInitialized=true;
			}
			imagePane.setVisibility(View.VISIBLE);
		}

		public void showWebPanel() {
			notesPane.setVisibility(View.INVISIBLE);
			epubReader.setVisibility(View.INVISIBLE);
			pdfReader.setVisibility(View.INVISIBLE);
			imagePane.setVisibility(View.INVISIBLE);

			webPane.setVisibility(View.VISIBLE);
		}

		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			MenuInflater inflater = getActivity().getMenuInflater();
			switch(v.getId()) {
			case R.id.epub_textPane: 
				inflater.inflate(R.menu.optional_reader_pane_context_menu, menu);
				menu.setHeaderTitle("Book Navigation");
				if(epubReader.book == null) {
					menu.removeItem(R.id.optional_toc);
					menu.removeItem(R.id.optional_view_bookmarks);
					menu.removeItem(R.id.optional_new_bookmark);
				}
				break;
			case R.id.pdfPane:
				inflater.inflate(R.menu.optional_pdf_pane_context_menu, menu);
				menu.setHeaderTitle("Book Navigation");
				break;
			case R.id.notes_pane:
				inflater.inflate(R.menu.optional_notes_pane_context_menu, menu);
				menu.setHeaderTitle("Notes");
				break;
			default:
				break;
			}
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
			Intent i;
			String path;
			switch (item.getItemId()) {
			case R.id.optional_select_new_book:
				i = new Intent(getActivity(), BookSelectorActivity.class);
				i.putExtra("launcher", "ReaderPanel");
				i.putExtra("theme", theme);
				startActivityForResult(i, item.getItemId());
				return true;
			case R.id.optional_toc:
				i = new Intent(getActivity(), TOCExplorer.class);
				i.putExtra("launcher", "ReaderPanel");
				path = epubReader.bookPath;
				if(path == null) {
					Log.d(BookSelectorActivity.APP_NAME, "book is null????");
					return true;
				}
				i.putExtra("bookPath", path);
				i.putExtra("theme", theme);
				Log.d(BookSelectorActivity.APP_NAME, "start activity TOCExplorer");
				startActivityForResult(i, item.getItemId());
				return true;
			case R.id.optional_new_bookmark:
				epubReader.createBookmark();
				return true;
			case R.id.optional_view_bookmarks:
				i = new Intent(getActivity(), BookmarkExplorer.class);
				i.putExtra("launcher", "ReaderPanel");
				path = epubReader.bookPath;
				if(path == null) {
					Log.d(BookSelectorActivity.APP_NAME, "view bookmark, book is null????");
					return true;
				}
				i.putExtra("bookPath", path);
				i.putExtra("theme", theme);
				Log.d(BookSelectorActivity.APP_NAME, "start activity BookmarkExplorer");
				startActivityForResult(i, item.getItemId());
				return true;
			case R.id.optional_load_txt:
				i = new Intent(getActivity(), FileExplorer.class);
				i.putExtra("launcher", ""+R.id.optional_load_txt);
				i.putExtra("theme", theme);
				startActivityForResult(i, R.id.optional_load_txt);
				return true;
			case R.id.open_my_notes:
				initNotes();
				return true;
			case R.id.optional_goto_page:
				activity.gotoPdfPage(pdfReader);
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if(resultCode==BookSelectorActivity.SUCCESS){
				if(requestCode == R.id.optional_select_new_book) {
					String path = data.getStringExtra("bookPath");
					if(DualBooksUtils.isPdf(path)) {
						initPdf(path);
					} else {
						initBook(path);
					}
				} else if(requestCode == R.id.optional_toc) {
					String href = data.getStringExtra("href");
					epubReader.readHref(href);
				} else if(requestCode == R.id.optional_view_bookmarks){
					epubReader.gotoBookmark(data);
				} else if(requestCode == R.id.optional_load_txt) {
					String path = data.getStringExtra("imagePath");
					initNotes(path);
				}
			}
		}

	} //OptionalFragment

	@Override
	public void onPause() {
		super.onPause();
		if(readerPane.epubReader.book != null) {
			readerPane.epubReader.saveCurrentBookPosition();
		} 
		if(secondPane.epubReader.book != null) {
			secondPane.epubReader.saveCurrentBookPosition();
		}
	}

}
