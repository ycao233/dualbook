package com.ycao.dualbooks.panel;

import java.io.ByteArrayOutputStream;

import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfDocument;

import com.ycao.dualbooks.R;
import com.ycao.dualbooks.R.id;
import com.ycao.dualbooks.R.layout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PDFReader extends LinearLayout {
	
	private static final String HTML_FORMAT = "<img src=\"data:image/jpeg;base64,%1$s\" />";
	private static final int WIDTH=500;
	private static final int HEIGHT=688;

	Context context;

	public WebView pdfPane;
	private TextView pageNum;
	ImageButton nextPage;
	ImageButton previousPage;

	public PdfDocument pdfDoc;
	public String pdfPath;
	int currPage;

	public PDFReader(Context context) {
		super(context);
		initializeView(context);
	}

	public PDFReader(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView(context);
	}

	public PDFReader(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView(context);
	}

	private void initializeView(Context ctx) {

		LayoutInflater.from(ctx).inflate(R.layout.pdf_reader, this, true);
		context = ctx;

		pdfPane = (WebView) findViewById(R.id.pdfPane);
		pdfPane.getSettings().setBuiltInZoomControls(true);
		
		pageNum = (TextView) findViewById(R.id.page_num);
		nextPage = (ImageButton) findViewById(R.id.nextButton);
		previousPage = (ImageButton) findViewById(R.id.backButton);

		setupClickListeners();
	}

	public void initializePdf(String pdfPath) {
		this.pdfPath = pdfPath;
		pdfDoc = new PdfContext().openDocument(this.pdfPath);
		
		currPage = 0;
		loadPage(currPage);
	}
	
	public void gotoPage(int pageNum) {
		if(pdfPane == null) {
			return;
		}
		
		if(pageNum >=0 && pageNum <= pdfDoc.getPageCount()) {
			currPage = pageNum;
			loadPage(currPage);
		}
	}
	
	private void loadPage(int page) {
		Bitmap bmp = pdfDoc.getPage(page).renderBitmap(WIDTH, HEIGHT);
	
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 0, bos);
		byte[] image = bos.toByteArray();
		String b64Image = Base64.encodeToString(image, Base64.DEFAULT);
		String html = String.format(HTML_FORMAT, b64Image);
		pdfPane.loadData(html, "text/html", "utf-8");
		String displayPage = page+1+" / "+pdfDoc.getPageCount();
		pageNum.setText(" ~~ Page "+displayPage+" ~~");
	}
	
	private void setupClickListeners() {
		nextPage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(currPage >= (pdfDoc.getPageCount()-1)) {
					return;
				}
				loadPage(++currPage);
			}
		});

		previousPage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(currPage <= 0) {
					return;
				}
				loadPage(--currPage);
			}
		});
	}

	public int getDocPageCount() {
		return pdfDoc.getPageCount();
	}
}
