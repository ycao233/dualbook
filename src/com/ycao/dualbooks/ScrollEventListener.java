package com.ycao.dualbooks;

import com.ycao.dualbooks.panel.SynchronizableScrollView;

public interface ScrollEventListener {
	
	void onScrollChanged(SynchronizableScrollView scrollView, int x, int y, int oldx, int oldy);
	
}
