package com.ycao.dualbooks.panel;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.ycao.dualbooks.ScrollEventListener;

public class SynchronizableScrollView extends ScrollView {
	
	  private ScrollEventListener listener = null;

	    public SynchronizableScrollView(Context context) {
	        super(context);
	    }

	    public SynchronizableScrollView(Context context, AttributeSet attrs, int defStyle) {
	        super(context, attrs, defStyle);
	    }

	    public SynchronizableScrollView(Context context, AttributeSet attrs) {
	        super(context, attrs);
	    }

	    public void setScrollEventListener(ScrollEventListener listener) {
	        this.listener = listener;
	    }

	    @Override 
	    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
	        super.onScrollChanged(x, y, oldx, oldy);
	        if(listener != null) {
	            listener.onScrollChanged(this, x, y, oldx, oldy);
	        }
	    }
}
