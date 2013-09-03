package com.ycao.dualbooks;

import java.util.ArrayList;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TOCExplorer extends ListActivity {

	private List<TOCItem> item = null;
	private TextView myPath;
	private String title;
	private String path;
	private TableOfContents toc; 
	//fake root TOC as parent of level one toc references
	private final TOCItem rootTOC = new TOCItem(null, null);
	private TOCItem parentTOC;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getIntent().getIntExtra("theme", BookSelectorActivity.THEME_LIGHT) == BookSelectorActivity.THEME_DARK) {
			setTheme(android.R.style.Theme_Holo);
		}
		setContentView(R.layout.fileexplorer);
		myPath = (TextView)findViewById(R.id.path);
		path = getIntent().getStringExtra("bookPath");
		Book book = DualBooksUtils.getBookFromCache(path);
		title = book.getTitle();
		toc = book.getTableOfContents();
		firstLevelTOC(toc);
	}

	private void firstLevelTOC(TableOfContents toc) {
		myPath.setText(title);
		parentTOC=rootTOC;
		
		item = new ArrayList<TOCItem>();
		for(TOCReference ref : toc.getTocReferences()) {
			item.add(new TOCItem(ref, rootTOC));
		}
		
		ArrayAdapter<TOCItem> tocList =
			new ArrayAdapter<TOCItem>(this, R.layout.row, item);
		
		setListAdapter(tocList);
	}
	
	private void getTOC(TOCItem tocItem) {
		myPath.setText(tocItem.toString());
		parentTOC = tocItem;
		
		item = new ArrayList<TOCItem>();
		for(TOCReference ref : tocItem.tocRef.getChildren()) {
			item.add(new TOCItem(ref, tocItem));
		}
		
		ArrayAdapter<TOCItem> tocList =
			new ArrayAdapter<TOCItem>(this, R.layout.row, item);
		
		setListAdapter(tocList);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		TOCItem tocItem = item.get(position);
		List<TOCReference> children = tocItem.tocRef.getChildren();
		if (children != null && children.size() > 0) {
			getTOC(tocItem);
		} else {
			String href = tocItem.tocRef.getResource().getHref();
			Intent in = new Intent();
			in.putExtra("href", href);
			setResult(BookSelectorActivity.SUCCESS, in);
			finish();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	if(parentTOC == rootTOC) {
	    		return super.onKeyDown(keyCode, event);
	    	} else { //act like move up one directory
	    		Log.d(BookSelectorActivity.APP_NAME, "back to parent toc: "+parentTOC);
	    		if(parentTOC.parent == rootTOC) {
	    			firstLevelTOC(toc);
	    		} else {
	    			getTOC(parentTOC.parent);
	    		}
	    		return true;
	    	}
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		TextView tv;
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in Action Bar clicked; go home
			Intent intent = new Intent(TOCExplorer.this, BookSelectorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return true;
		}
	}
	
	private static class TOCItem {
		TOCReference tocRef;
		TOCItem parent;
		
		public TOCItem(TOCReference tocRef, TOCItem parent) {
			this.tocRef = tocRef;
			this.parent = parent;
		}
		
		public String toString() {
			if(tocRef.getChildren() != null && tocRef.getChildren().size() > 0) {
				return tocRef.getTitle() + " >";
			} 
			return tocRef.getTitle();
		}
	}
	
}