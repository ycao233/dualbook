package com.ycao.dualbooks.adapters;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ycao.dualbooks.BookmarkExplorer;
import com.ycao.dualbooks.R;

public class BookmarkAdapter extends BaseAdapter {

	private LayoutInflater inflater;
	private List<BookmarkExplorer.Bookmark> item;
	//private DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
	private SimpleDateFormat formatter = new SimpleDateFormat("E MMMM dd, yyyy HH:mm:ss a", Locale.US);

	public BookmarkAdapter(Context context, List<BookmarkExplorer.Bookmark> item) {
		this.inflater = LayoutInflater.from(context);
		this.item = item;
	}

	@Override
	public int getCount() {
		return item.size();
	}

	@Override
	public Object getItem(int pos) {
		return item.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		return 0;
	}

	@Override
	public View getView(int pos, View convertedView, ViewGroup parent) {
		View entry = this.inflater.inflate(R.layout.bookmark_entry, null);
		TextView dateField = (TextView) entry.findViewById(R.id.date_time);
		TextView text = (TextView) entry.findViewById(R.id.bookmark_content);
		dateField.setText(formatter.format(item.get(pos).getDate()));
		text.setText(item.get(pos).getContent().toString());
		return entry;
	}

}
