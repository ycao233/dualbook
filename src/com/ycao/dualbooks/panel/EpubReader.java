package com.ycao.dualbooks.panel;

import java.io.IOException;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ycao.dualbooks.BookSelectorActivity;
import com.ycao.dualbooks.DualBooksUtils;
import com.ycao.dualbooks.EpubReaderCallback;
import com.ycao.dualbooks.R;
import com.ycao.dualbooks.ScrollEventListener;

public class EpubReader extends LinearLayout {

	public boolean primary = false;
	
	public SynchronizableScrollView scroller;
	public TextView readPane;
	TextView footer;

	ImageButton next;
	ImageButton back;
	ImageButton nextChapter;
	ImageButton previousChapter;

	public Book book;
	public String bookPath;
	public String resourceHref;
	int spineIdx;

	int textOffset;
	public boolean explicitScroll;

	EpubReaderCallback control;

	Context context;

	public EpubReader(Context context) {
		super(context);
		initializeView(context);
	}

	public EpubReader(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView(context);
	}

	public EpubReader(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView(context);
	}


	private void initializeView(Context ctx) {

		LayoutInflater.from(ctx).inflate(R.layout.epub_reader, this, true);
		context = ctx;

		scroller = (SynchronizableScrollView) findViewById(R.id.epub_scrollview);
		readPane = (TextView) findViewById(R.id.epub_textPane);
		footer = (TextView) findViewById(R.id.footer);
		next = (ImageButton) findViewById(R.id.nextButton);
		back = (ImageButton) findViewById(R.id.backButton);
		nextChapter = (ImageButton) findViewById(R.id.nextChapterButton);
		previousChapter = (ImageButton) findViewById(R.id.previousChapterButton);

		setupClickListeners();

		readPane.addOnLayoutChangeListener(new OnLayoutChangeListener(){
			@Override
			public void onLayoutChange(View arg0, int arg1, int arg2,
					int arg3, int arg4, int arg5, int arg6, int arg7,
					int arg8) {
				//now scroll back to the offset
				readPane.clearFocus();
				if(explicitScroll) {
					scrollTo(readPane.getLayout(), textOffset, 0);
					explicitScroll = false;
				}
			}
		});
	}

	private void setupClickListeners() {

		back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(scroller.pageScroll(ScrollView.FOCUS_UP)) {
					Log.d(BookSelectorActivity.APP_NAME, "scroll up returned true, ");
					return;
				} 
				gotoPreviousChapter();
			}
		});

		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(scroller.pageScroll(ScrollView.FOCUS_DOWN)) {
					Log.d(BookSelectorActivity.APP_NAME, "scroll down returned true, ");
					return;
				} 
				gotoNextChapter();
			}
		});

		previousChapter.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoPreviousChapter();
			}
		});

		nextChapter.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gotoNextChapter();
			}
		});

	}
	
	public void setupCallbackContoller(EpubReaderCallback control) {
		this.control = control;
		scroller.setScrollEventListener((ScrollEventListener)control);
	}

	private void gotoPreviousChapter() {
		if(book == null) {
			return;
		}

		if(spineIdx > 0) {
			--spineIdx;
			readCurrentSpine();
			scrollFullChapterUp();
			control.chapterChangedCallback(primary);
		}
	}

	private void gotoNextChapter() {
		if(book == null) {
			return;
		}

		//no more room to scroll up, go to next chapter
		int maxIdx = getMaxSpineSize();
		if(spineIdx < maxIdx-1) {
			++spineIdx;
			readCurrentSpine();
			scrollFullChapterUp();
			control.chapterChangedCallback(primary);
		}
	}

	private void scrollFullChapterUp() {
		scroller.post(new Runnable(){
			public void run() {
				scroller.fullScroll(View.FOCUS_UP);
			}
		});
	}


	public void initailizeBook(String path) {
		if(book != null) {
			//save current book position
			saveCurrentBookPosition();
		}
		book = DualBooksUtils.getBookFromCache(path);
		bookPath = path;
		boolean readBefore = DualBooksUtils.readBefore(bookPath);
		if(readBefore) {
			int offset = 0;
			try {
				spineIdx = DualBooksUtils.getSpineIndex(bookPath);
				offset = DualBooksUtils.getLastReadPosition(bookPath);
				Log.d(BookSelectorActivity.APP_NAME, "Last spine idx: "+spineIdx +" text offset: "+offset);
			} catch (IOException e) {
				Log.e(BookSelectorActivity.APP_NAME, "Failed to get spine idx or text offset: "+e);
				spineIdx = 0;
				offset = 0;
			}
			readCurrentSpine();
			textOffset = offset;
			readPane.requestLayout();
			final int offset2 = offset;
			new Thread() { 
				public void run() {
					while(readPane.getLayout() == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							//suppress
						}
					}
					scrollTo(readPane.getLayout(), offset2, 0);
				}
			}.start();
		} else {
			spineIdx = 0;
			readCurrentSpine();
			scrollFullChapterUp();
		}
	}

	public void saveCurrentBookPosition() {
		int offset = getCurrentTextOffset();
		try {
			DualBooksUtils.saveCurrentBookPosition(bookPath, spineIdx, offset);
		} catch (IOException e) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to save book position: "+e);
			Toast.makeText(context, "Failed to save book position", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void readCurrentSpine() {
		try {
			Resource resource = DualBooksUtils.getSpineResource(book, spineIdx);
			resourceHref = resource.getHref();
			if(DualBooksUtils.resourceIsImage(resource)) {
				final Drawable d = imageHandler.getDrawable(resourceHref);
				ImageSpan image = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
				SpannableString ss = new SpannableString("aaa");
				ss.setSpan(image, 0, 3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				readPane.setText(ss);
			} else {
				final String content = DualBooksUtils.getSpineContent(resource);
				readPane.setBackgroundDrawable(null);
				readPane.setText(Html.fromHtml(content, imageHandler, null));
			} 
			
			textOffset = 0;
			footer.setText(String.format("~~ %s ~~", (book==null)?"select a book":book.getTitle()));
		} catch (IOException e) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to load this book.  Book maybe corrupted.");
			Toast.makeText(context, "Failed to load this book.  Book maybe corrupted.", 
					Toast.LENGTH_LONG).show();
		}
	}

	public int getMaxSpineSize() {
		return this.book.getSpine().size();
	}

	public void markCurrentTextOffset() {

		int y = scroller.getScrollY();
		Layout layout = readPane.getLayout();
		if(layout == null) {
			return;
		}

		int topLine = layout.getLineForVertical(y);
		textOffset = layout.getLineStart(topLine);
	}

	public int getCurrentTextOffset() {

		int y = scroller.getScrollY();
		Layout layout = readPane.getLayout();
		if(layout == null) {
			return 0;
		}

		int topLine = layout.getLineForVertical(y);
		int offset = layout.getLineStart(topLine);

		return offset;
	}

	public int getPosYForTextOffset(Layout layout, int offset) {
		int line = layout.getLineForOffset(offset);
		return layout.getLineTop(line);
	}

	private void scrollTo(final Layout layout, final int offset, final int delay) {
		scroller.postDelayed(new Runnable(){
			public void run() {
				if(layout != null) {
					int y = getPosYForTextOffset(layout, offset);
					scroller.scrollTo(0, y);
				} else {
					//for debug purpose
					Toast.makeText(context, "layout is null", Toast.LENGTH_SHORT).show();
				}
			}
		}, delay);
	}

	public void createBookmark() {
		int textOffset = getCurrentTextOffset();
		CharSequence text = readPane.getText();
		if(text.length() < (textOffset+200)) {
			text = text.subSequence(textOffset, text.length());
		} else {
			text = text.subSequence(textOffset, textOffset+200);
		}
		String processed = DualBooksUtils.process(text.toString());
		try {
			DualBooksUtils.saveBookmark(bookPath, spineIdx, textOffset, processed);
			Toast.makeText(context, "Bookmarked saved.", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to save bookmark: "+e);
			Toast.makeText(context, "Failed to save bookmark.", Toast.LENGTH_SHORT).show();
		}
	}

	public void fadeIn(TextView tv) {
		Animation a = AnimationUtils.loadAnimation(context, R.anim.alpha);
		a.reset();
		tv.clearAnimation();
		tv.startAnimation(a);
	}

	public void gotoBookmark(Intent data) {
		int currTextOffset = getCurrentTextOffset();
		int textOffset = data.getIntExtra("offSet", currTextOffset);
		int idx = data.getIntExtra("spineIdx", spineIdx);
		boolean chapterChanged = false;
		if(idx != spineIdx) {
			spineIdx = idx;
			readCurrentSpine();
			chapterChanged = true;
		}
		if(currTextOffset != textOffset) {
			fadeIn(readPane);
			scrollTo(readPane.getLayout(), textOffset, 0);
		}
		if(chapterChanged) {
			control.chapterChangedCallback(primary);
		}
	}

	public void fontChange(int by) {
		if(readPane.getLayout() != null) {
			final int offset = getCurrentTextOffset();
			fadeIn(readPane);
			readPane.setTextSize(readPane.getTextSize()+by);
			new Thread() { 
				public void run() {
					while(readPane.getLayout() == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							//suppress
						}
					}
					scrollTo(readPane.getLayout(), offset, 0);
				}
			}.start();
		}
	}

	public float getTextSize() {
		return readPane.getTextSize();
	}

	public void setTextSize(float size) {
		readPane.setTextSize(size);
	}

	public void readHref(String href) { 
		spineIdx = book.getSpine().getResourceIndex(href);
		readCurrentSpine();
		scrollFullChapterUp();
	}
	
	private Html.ImageGetter imageHandler = new Html.ImageGetter() {
		@Override
		public Drawable getDrawable(String ref) {
			Log.i(BookSelectorActivity.APP_NAME, "getting image source: "+ref);
			Resource res = EpubReader.this.book.getResources().getByHref(ref);
			try {
				Bitmap bmp = BitmapFactory.decodeStream(res.getInputStream());
				final BitmapDrawable d = new BitmapDrawable(getResources(), bmp);
				d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
				return d;
			} catch (IOException e) {
				Log.i(BookSelectorActivity.APP_NAME, "IO exception: "+e);
				return null;
			}
		}
	};
}
