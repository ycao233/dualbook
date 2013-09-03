package org.vudroid.pdfdroid.codec;

public class PDFLibraryLoader {
	private static boolean alreadyLoaded = false;

	public static void load()
	{
		if (alreadyLoaded)
		{
			return;
		}
		System.loadLibrary("vudroid");
		alreadyLoaded = true;
	}
}
