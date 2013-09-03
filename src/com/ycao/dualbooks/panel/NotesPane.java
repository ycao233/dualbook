package com.ycao.dualbooks.panel;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.ycao.dualbooks.BookSelectorActivity;
import com.ycao.dualbooks.DualBooksUtils;
import com.ycao.dualbooks.R;

public class NotesPane extends LinearLayout {

	Context context;

	public CheckBox readOnlyBox;
	public boolean readOnly;
	public EditText notes;

	String notePath;
	
	public NotesPane(Context context) {
		super(context);
		initializeView(context);
	}

	public NotesPane(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView(context);
	}

	public NotesPane(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView(context);
	}


	private void initializeView(Context ctx) {

		LayoutInflater.from(ctx).inflate(R.layout.notes_panel, this, true);
		context = ctx;

		readOnlyBox = (CheckBox) findViewById(R.id.read_only);
		notes = (EditText) findViewById(R.id.notes_pane);
		
		readOnlyBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton button, boolean checked) {
				if(checked) {
					readOnly = true;
					notes.setFocusable(false);
					notes.setFocusableInTouchMode(false);
					hideKeyboard();
				} else {
					readOnly = false;
					notes.setFocusable(true);
					notes.setFocusableInTouchMode(true);
				}
			}
		});
		
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(notes.getWindowToken(), 0);
	}

	public void initNotes(String filename) {
		Editable text = notes.getText();
		String raw;
		if(notePath != null) { //save current note
			if(notePath.toLowerCase().endsWith("html") || notePath.toLowerCase().endsWith("htm")) {
				raw = Html.toHtml(text);
			} else {
				raw = text.toString();
			}
			
			try {
				DualBooksUtils.saveNotes(notePath, raw);
			} catch (IOException e) {
				Toast.makeText(context, "Failed to save current notes", Toast.LENGTH_LONG).show();
				Log.e(BookSelectorActivity.APP_NAME, "Failed to save notes: "+notePath);
			}
		}
		try {
			raw = DualBooksUtils.read(filename);
		} catch (FileNotFoundException e) {
			Toast.makeText(context, "Failed to load notes", Toast.LENGTH_LONG).show();
			Log.e(BookSelectorActivity.APP_NAME, "Failed to load notes: "+filename);
			notePath = null;
			return;
		}
		
		//need to clear "read only" status temporarily otherwise content never 
		//displays on screen
		if(readOnly) {
			notes.setFocusable(true);
			notes.setFocusableInTouchMode(true);
		}
		notes.setText(Html.fromHtml(raw));
		notePath = filename;
		
		if(readOnly) {
			notes.setFocusable(false);
			notes.setFocusableInTouchMode(false);
		}
	}


}
