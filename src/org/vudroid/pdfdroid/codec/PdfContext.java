package org.vudroid.pdfdroid.codec;


import android.content.ContentResolver;


public class PdfContext 
{
    static
    {
        PDFLibraryLoader.load();
    }

    public PdfDocument openDocument(String fileName)
    {
        return PdfDocument.openDocument(fileName, "");
    }

    public void setContentResolver(ContentResolver contentResolver)
    {
        //TODO
    }

    public void recycle() {
    }
}
