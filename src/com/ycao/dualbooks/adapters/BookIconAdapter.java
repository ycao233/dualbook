package com.ycao.dualbooks.adapters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;

import org.vudroid.pdfdroid.codec.PdfContext;
import org.vudroid.pdfdroid.codec.PdfDocument;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ycao.dualbooks.BookSelectorActivity;
import com.ycao.dualbooks.DualBooksUtils;
import com.ycao.dualbooks.R;

public class BookIconAdapter extends BaseAdapter {

	private LayoutInflater inflater;
	private BookAndPath[] books;
	private boolean[] changed;
	private PdfContext pdfContext;

	public BookIconAdapter(Context context, String libraryPath) {
		this.inflater = LayoutInflater.from(context);
		books = populateBooks(libraryPath);
		changed = new boolean[books.length];
		for(int i=0; i<changed.length; ++i) {
			changed[i] = false;
		}
		Log.i(BookSelectorActivity.APP_NAME, "Books length: "+books.length);
	}

	@Override
	public int getCount() {
		return books.length;
	}

	@Override
	public Object getItem(int position) {
		return books[position].path;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View bookIcon;
		if(convertView == null || changed[position] == true) {
			bookIcon = this.inflater.inflate(R.layout.icon, null);
			TextView tv = (TextView) bookIcon.findViewById(R.id.icon_text);
			LinearLayout lv = (LinearLayout) bookIcon.findViewById(R.id.background);
			lv.setPadding(25, 25, 25, 25);

			String title = books[position].bookTitle;

			tv.setText(title);
			File bookCover = new File(DualBooksUtils.generateCoverPageFileName(books[position].path));
			Log.d(BookSelectorActivity.APP_NAME, "Cover image path: "+bookCover.getAbsolutePath());
			boolean noCoverImage = true;
			if(bookCover.exists() && bookCover.canRead()) {
				try {
					Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(bookCover));
					lv.setBackgroundDrawable(new BitmapDrawable(bmp));
					noCoverImage = false;
				} catch(IOException ioe) {
					Log.e("imageAdapter", "Failed to get book cover");
				}
			} 
			if(noCoverImage){
				TextView cover = (TextView) bookIcon.findViewById(R.id.mock_cover);
				String authors = books[position].bookAuthors;
				String mockCover = "<font size=\"8\">"+title+"</font><br><br><br><br><br><br><br><br>";
				mockCover += authors;
				cover.setText(Html.fromHtml(mockCover));
			}
			changed[position] = false;
		} else {
			bookIcon = convertView;
		}
		return bookIcon;
	}

	private BookAndPath[] populateBooks(String path) {
		ArrayList<BookAndPath> books = new ArrayList<BookAndPath>();
		File library = new File(path);
		if(library.exists() && library.isDirectory()) {
			File[] files = library.listFiles();
			Log.d(BookSelectorActivity.APP_NAME, "number of files: "+files.length);
			for(File file : files) {
				String extension = file.getName().substring(file.getName().toLowerCase().lastIndexOf(".")+1);
				if(file.isFile() && file.canRead() && 
						(extension.equals("epub") || extension.equals("pdf"))) {
					File bookInfoFolder = new File(path+"/"+BookSelectorActivity.APP_NAME+"/"+file.getName());
					if(!bookInfoFolder.exists()) {
						boolean mkdir = bookInfoFolder.mkdirs();
						Log.d(BookSelectorActivity.APP_NAME, "make "+bookInfoFolder.getAbsolutePath()+": "+mkdir);
					}
					Log.i(BookSelectorActivity.APP_NAME, "adding "+file.getName() + " to book list"); 
					String parent = file.getParent()==null?".":file.getParent();
					String filename=file.getName();
					BookAndPath quickBook = loadFromSavedFile(parent, filename);
					if(quickBook == null) {
						if(extension.equals("epub")) {
							try {
								Book book = DualBooksUtils.getBook(file.getAbsolutePath());
								// Log the book's authors
								String authors="";
								for(Author author : book.getMetadata().getAuthors()) {
									authors+=author.getFirstname()+" "+author.getLastname()+"<br>";
								}
								authors = authors.trim();
								if(authors.endsWith(",")) {
									authors = authors.substring(0, authors.length()-1);
								}
								Log.d("epublib", "author(s): " + authors);
								books.add(new BookAndPath(book.getTitle(), authors, file.getAbsolutePath()));

								//save book info
								PrintWriter saveBookInfo = new PrintWriter(new FileOutputStream(parent+"/"
										+BookSelectorActivity.APP_NAME+"/"
										+filename+"/"+filename+".info"));
								saveBookInfo.println(book.getTitle());
								saveBookInfo.println(authors);
								saveBookInfo.close();

								//save cover image
								if(book.getCoverImage() != null) {
									File bookCover = new File(DualBooksUtils.generateCoverPageFileName(file.getAbsolutePath()));
									Log.d(BookSelectorActivity.APP_NAME, "Saving book cover image: "+bookCover.getAbsolutePath());
									FileOutputStream out = new FileOutputStream(bookCover);
									Bitmap bmp = BitmapFactory.decodeStream((book.getCoverImage().getInputStream()));
									bmp.compress(Bitmap.CompressFormat.PNG, 40, out);
									Log.d(BookSelectorActivity.APP_NAME, "book cover image saved. ");
								}
							} catch (FileNotFoundException e) {
								Log.e(BookSelectorActivity.APP_NAME, "Failed to add book: "+e);
							} catch (IOException e) {
								Log.e(BookSelectorActivity.APP_NAME, "Failed to add book: "+e);
							}
						} else if(extension.equals("pdf")) {
							if(pdfContext == null) {
								pdfContext = new PdfContext();
							}
							PdfDocument doc = pdfContext.openDocument(file.getAbsolutePath());
							Log.d(BookSelectorActivity.APP_NAME, "Create pdf doc: "+file.getName());
							try{
								//save book info
								PrintWriter saveBookInfo = new PrintWriter(new FileOutputStream(parent+"/"
										+BookSelectorActivity.APP_NAME+"/"
										+file.getName()+"/"+file.getName()+".info"));
								saveBookInfo.println(file.getName());
								saveBookInfo.println("");
								saveBookInfo.close();
								books.add(new BookAndPath(filename, "", file.getAbsolutePath()));

								//generate cover image
								if(doc.getPageCount() > 0) {
									Bitmap bmp = doc.getPage(0).renderBitmap(180, 270);
									File bookCover = new File(DualBooksUtils.generateCoverPageFileName(file.getAbsolutePath()));
									Log.d(BookSelectorActivity.APP_NAME, "Saving book cover image: "+bookCover.getAbsolutePath());
									FileOutputStream out;
									out = new FileOutputStream(bookCover);
									bmp.compress(Bitmap.CompressFormat.PNG, 40, out);
								}
							} catch (FileNotFoundException e) {
								Log.e(BookSelectorActivity.APP_NAME, "Failed to add pdf book: "+e);
							}
						}
					} else {
						books.add(quickBook);
					}
				} 
				/* Non-recursive
				if(file.isDirectory() && !file.getName().equals(BookSelector.APP_NAME)) {
					BookAndPath[] moreBooks = populateBooks(file.getAbsolutePath());
					for(BookAndPath book : moreBooks) {
						books.add(book);
					}
				}
				 */
			}
		}

		return books.toArray(new BookAndPath[books.size()]);
	}

	//To improve loading speed, save title and authors in separate files
	private BookAndPath loadFromSavedFile(String path, String filename) {
		File dir = new File(path+"/"+BookSelectorActivity.APP_NAME+"/"+filename);
		if(!dir.exists()) {
			return null;
		}
		File bookinfo = new File(path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/"+filename+".info");
		if(!bookinfo.exists()) {
			return null;
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(bookinfo));
			String title = br.readLine();
			String authors = br.readLine();
			return new BookAndPath(title, authors, path+"/"+filename);
		} catch (IOException ioe) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to load book information file");
		}
		return null;
	}

	public void setChanged(int position) {
		changed[position] = true;
	}

	private static class BookAndPath {
		String bookTitle;
		String bookAuthors;
		String path;
		BookAndPath(String bookTitle, String bookAuthors, String path) {
			this.bookTitle = bookTitle;
			this.bookAuthors = bookAuthors;
			this.path = path;
		}
	}
}
