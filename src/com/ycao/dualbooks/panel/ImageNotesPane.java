package com.ycao.dualbooks.panel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import Robert.Foss.TouchImageView;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ycao.dualbooks.BookSelectorActivity;
import com.ycao.dualbooks.DualBooksUtils;
import com.ycao.dualbooks.EpubReaderCallback;
import com.ycao.dualbooks.R;
import com.ycao.dualbooks.ReaderPanelActivity;
import com.ycao.dualbooks.DualBooksUtils.Direction;
import com.ycao.dualbooks.R.drawable;
import com.ycao.dualbooks.R.id;
import com.ycao.dualbooks.R.layout;


public class ImageNotesPane extends LinearLayout {

	Context context;

	TouchImageView imagePane;

	ImageButton lastPic;
	ImageButton nextPic;
	ImageButton takePic;
	ImageButton rotatePic;

	EpubReaderCallback control;

	volatile String currentPic;

	public ImageNotesPane(Context context) {
		super(context);
		initializeView(context);
	}

	public ImageNotesPane(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView(context);
	}

	public ImageNotesPane(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView(context);
	}

	public void setupCallbackContoller(EpubReaderCallback control) {
		this.control = control;
	}

	private void initializeView(Context ctx) {

		LayoutInflater.from(ctx).inflate(R.layout.image_notes_panel, this, true);
		context = ctx;

		imagePane = (TouchImageView) findViewById(R.id.opt_notesImagePane);
		lastPic = (ImageButton) findViewById(R.id.opt_lastPic);
		nextPic = (ImageButton) findViewById(R.id.opt_nextPic);
		takePic = (ImageButton) findViewById(R.id.opt_takepic);
		rotatePic = (ImageButton) findViewById(R.id.opt_rotate);

		setupClickListeners();
	}

	private void setupClickListeners() {

		takePic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startCamera();
			}
		});

		lastPic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				picBottonClicked(Direction.BACK);
			}
		});

		nextPic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				picBottonClicked(Direction.NEXT);
			}
		});
		rotatePic.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view) {
				Bitmap bmp = imagePane.getImage();
				if(bmp == null) {
					return;
				}
				Matrix matrix = new Matrix();
				matrix.postRotate(90);
				Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, 
						bmp.getWidth(), bmp.getHeight(), matrix, true);
				imagePane.setImage(rotated, rotated.getWidth(), rotated.getHeight());
			}
		});

	}

	private void picBottonClicked(Direction direction) {
		if(currentPic == null) {
			return;
		}
		String nextPic = DualBooksUtils.getNextPicInDirection(currentPic, direction);
		if(nextPic == null) {
			return;
		}
		try {
			Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(nextPic));
			imagePane.setImage(bmp, bmp.getWidth(), bmp.getHeight());
			currentPic = nextPic;
		} catch (FileNotFoundException e) {
			Log.d(BookSelectorActivity.APP_NAME, "Failed to initialize image pane with: "+nextPic+" error: "+e);
		}
	}

	private void startCamera() {

		Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		String pic_name;
		try {
			pic_name = DualBooksUtils.generateImageFileName(
					control.getCurrentPrimaryBookPath(), control.getCurrentPrimaryBookHref());
		} catch (IOException e1) {
			Toast.makeText(context, "Failed to initialize image file, aborting...", Toast.LENGTH_LONG).show();
			return;
		}
		i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(pic_name)));
		final String myPath = pic_name;

		//create own file monitor 
		new Thread(new Runnable(){
			public void run() {
				File myPic = new File(myPath);
				while(!myPic.exists() || !myPic.canRead()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//suppress
					}
				}
				//file exists now, wait for another 1 second 
				imagePane.postDelayed(new Runnable(){
					public void run() {
						try {
							final Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(myPath));
							imagePane.setImage(bmp, bmp.getWidth(), bmp.getHeight());
							imagePane.setBackgroundDrawable(null);
							currentPic = myPath;
						} catch (FileNotFoundException e) {
							Log.d(BookSelectorActivity.APP_NAME, "Failed to decode the picture");
							e.printStackTrace();
						}
					}
				}, 1000);

			}
		}).start();
		Log.d(BookSelectorActivity.APP_NAME, "start watching: "+pic_name);
		((ReaderPanelActivity) control).startActivityForResult(i, R.id.opt_takepic);
	}


	public void initImagePane() {
		String firstPic = DualBooksUtils.getFirstImageNotes(control.getCurrentPrimaryBookPath(), 
				control.getCurrentPrimaryBookHref());
		if(firstPic == null) {
			currentPic = null;
			imagePane.setImage(null, 0, 0);
			imagePane.setBackgroundResource(R.drawable.image_hint);
			return;
		}

		Bitmap bmp;
		try {
			bmp = BitmapFactory.decodeStream(new FileInputStream(firstPic));
			imagePane.setBackgroundDrawable(null);
			imagePane.setImage(bmp, bmp.getWidth(), bmp.getHeight());
			currentPic = firstPic;
		} catch (FileNotFoundException e) {
			Log.d(BookSelectorActivity.APP_NAME, "Failed to initialize image pane with: "+firstPic+" error: "+e);
		}
	}
}
