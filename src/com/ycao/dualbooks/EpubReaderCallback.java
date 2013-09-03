package com.ycao.dualbooks;

public interface EpubReaderCallback {

	//To change notes / image panel content of the second panel
	public void chapterChangedCallback(boolean primary); 

	//get the book information for the notes / image change
	public String getCurrentPrimaryBookPath();
	public String getCurrentPrimaryBookHref();
}
