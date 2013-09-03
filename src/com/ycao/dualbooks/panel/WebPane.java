package com.ycao.dualbooks.panel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ycao.dualbooks.EpubReaderCallback;
import com.ycao.dualbooks.R;

public class WebPane extends LinearLayout {

	Context context;

	WebView webView;
	EditText urlBar;
	Button go;
	ImageButton back;

	public WebPane(Context context) {
		super(context);
		initializeView(context);
	}

	public WebPane(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView(context);
	}

	public WebPane(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView(context);
	}

	private void initializeView(Context ctx) {

		LayoutInflater.from(ctx).inflate(R.layout.web_pane, this, true);
		context = ctx;

		webView = (WebView) findViewById(R.id.opt_webPane);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setJavaScriptEnabled(true);	
		webView.loadUrl("http://www.google.com");
		urlBar = (EditText) findViewById(R.id.urlbar);
		go = (Button) findViewById(R.id.loadurl);
		back = (ImageButton) findViewById(R.id.opt_webBack);


		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				urlBar.setText(url);
				view.loadUrl(url);
				return true;
			}
		});

		setupClickLisenters();
	}

	private void setupClickLisenters() {

		go.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = urlBar.getText().toString().trim();
				Toast.makeText(context, "Loading "+url, Toast.LENGTH_SHORT).show();
				if(!url.startsWith("http://") && !url.startsWith("https://")) {
					url="http://"+url;
				}
				webView.loadUrl(url);
				InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
			}
		});

		back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(webView.canGoBack()) {
					webView.goBack();
				}
			}
		});
	}
}
