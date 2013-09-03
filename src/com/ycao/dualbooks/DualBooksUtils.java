package com.ycao.dualbooks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class DualBooksUtils {
	//SOH
	public static final String SEP = new String(new char[]{(char)1});
	
	public static enum Direction {
		BACK,
		NEXT
	};
	
	private static ConcurrentMap<String, Book> bookCache = new ConcurrentHashMap<String, Book>();
	
	public static Book getBookFromCache(String path) {
		if(bookCache.containsKey(path)) {
			return bookCache.get(path);
		} else {
			Book book = DualBooksUtils.getBook(path);
			bookCache.put(path, book);
			return book;
		}
	}
	

	public static boolean isPdf(String path) {
		return path.toLowerCase().endsWith("pdf");
	}
	
	public static Book getBook(String path) {
		InputStream is;
		Book book = null;
		try {
			is = new FileInputStream(path);
			book = (new EpubReader()).readEpub(is);
		} catch (FileNotFoundException e) {
			Log.d(BookSelectorActivity.APP_NAME, "Failed to initialized book: "+e);
		} catch (IOException e) {
			Log.d(BookSelectorActivity.APP_NAME, "Failed to initialized book: "+e);
		}
		Log.d(BookSelectorActivity.APP_NAME, "initialized book: "+path);
		return book;
	}

	public static String generateCoverPageFileName(String book) {
		String path = book.substring(0, book.lastIndexOf("/"));
		String filename = book.substring(book.lastIndexOf("/"));
		String coverFile = (path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/"
				+filename+".png");
		Log.d(BookSelectorActivity.APP_NAME, "Cover for "+book+" is: "+coverFile);
		return coverFile;
	}

	public static void copyFile(String from, String to, Context context) {
		File toDir = new File(to.substring(0, to.lastIndexOf("/")));
		if(!toDir.exists()) {
			toDir.mkdirs();
		}
		try{
			InputStream in = new FileInputStream(from);
			OutputStream out = new FileOutputStream(to);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			System.out.println("File copied.");
		}
		catch(FileNotFoundException ex){
			Log.e(BookSelectorActivity.APP_NAME, "Failed to copy cover image: "+ex);
			Toast.makeText(context, "Failed to copy cover image", Toast.LENGTH_LONG);
		}
		catch(IOException e){
			Log.e(BookSelectorActivity.APP_NAME, "Failed to copy cover image: "+e);
			Toast.makeText(context, "Failed to copy cover image", Toast.LENGTH_LONG);
		}
	}

	public static String getResourceContent(Reader reader, boolean raw) {
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[1024];
		int read;
		try {
			while((read = reader.read(buf)) != -1) {
				sb.append(buf, 0, read);
			}
		} catch (IOException e) {
			Log.e(BookSelectorActivity.APP_NAME, "Failed to get content: "+e);
		}

		if(raw) {
			return sb.toString();
		}

		String upperCase = sb.toString().toUpperCase();
		int start = upperCase.indexOf("<HEAD>");
		int end = upperCase.indexOf("</HEAD>");
		if(start > -1 && end > -1) {
			return sb.toString().substring(0, start)+sb.toString().substring(end+7);
		} 
		return sb.toString();
	}


	public static Resource getSpineResource(Book book, int spineResIdx) throws IOException {
		return book.getSpine().getResource(spineResIdx);
	}

	public static String getSpineContent(Resource resource) throws IOException {
		String pageContent="";
		pageContent = getResourceContent(resource.getReader(), false);
		pageContent = removeControlTags(pageContent);
		return pageContent;
	}

	public static boolean resourceIsImage(Resource resource) {
		String name = resource.getMediaType().getName().toLowerCase();
		return name.startsWith("image");
	}

	/**
	 * COPIED FROM epublib HTMLDocumentFactory
	 * 
	 * Quick and dirty stripper of all &lt;?...&gt; and &lt;!...&gt; tags as
	 * these confuse the html viewer.
	 * 
	 * @param input
	 * @return
	 */
	public static String removeControlTags(String input) {
		StringBuilder result = new StringBuilder();
		boolean inControlTag = false;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (inControlTag) {
				if (c == '>') {
					inControlTag = false;
				}
			} else if (c == '<' // look for &lt;! or &lt;?
				&& i < input.length() - 1
				&& (input.charAt(i + 1) == '!' || input.charAt(i + 1) == '?')) {
				inControlTag = true;
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	public static File getBookmarkFile(String book) {
		String path = book.substring(0, book.lastIndexOf("/"));
		String filename = book.substring(book.lastIndexOf("/"));
		String bookmark = (path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/bookmarks");

		return new File(bookmark);
	}

	public static BookmarkExplorer.Bookmark getBookmark(String line) {
		String[] token = line.split(SEP);
		if(token.length != 4) {
			return null;
		}
		Date date = new Date(Long.parseLong(token[0]));
		return new BookmarkExplorer.Bookmark(date, token[1], 
				Integer.parseInt(token[2]), Integer.parseInt(token[3]));
	}

	public static String process(String rawText) {
		Log.d(BookSelectorActivity.APP_NAME, "rawText: "+rawText);
		//first strip all html tags
		String s = rawText.replaceAll("\\\\n", " ");
		s = s.replaceAll("\\s+", " ");
		s = removeControlTags(s).replaceAll("\\<.*?\\>", " ");
		int idx = s.lastIndexOf(">");
		if(idx > -1) {
			s = s.substring(idx);
		}
		idx = s.lastIndexOf("<");
		if(idx > -1) {
			s = s.substring(0, idx);
		}
		if(s.length() > 150) {
			s=s.substring(0, 150); 
		}
		String ret="";
		idx = s.indexOf(" ");
		if(idx>-1 && s.length() > (idx+1)) {
			ret = "..."+s.substring(idx);
		}
		idx = ret.lastIndexOf(" ");
		if(idx>-1) {
			ret = ret.substring(0, idx)+"...";
		}
		Log.d(BookSelectorActivity.APP_NAME, "processed text: "+ret);
		return ret;
	}

	public static void saveBookmark(String bookPath, int spineIdx, 
			int textOffset, String description) throws IOException {
		File bookmark = getBookmarkFile(bookPath);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(bookmark, true)));
		long time = new Date().getTime();
		pw.print(time);
		pw.print(SEP);
		pw.print(description);
		pw.print(SEP);
		pw.print(spineIdx);
		pw.print(SEP);
		pw.print(textOffset);
		pw.println();
		pw.close();
	}
	
	public static void saveChangedBookMarks(String path, List<BookmarkExplorer.Bookmark> item) throws IOException {
		File bookmark = getBookmarkFile(path);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(bookmark, false)));
		for(BookmarkExplorer.Bookmark entry : item) {
			pw.print(entry.date.getTime());
			pw.print(SEP);
			pw.print(entry.content);
			pw.print(SEP);
			pw.print(entry.spineIdx);
			pw.print(SEP);
			pw.print(entry.textOffset);
			pw.println();
		}
		pw.close();
	}

	public static String generateImageFileName(String book, String identifier) throws IOException {
		String imageFolder = getImageNotesFolderName(book, identifier);
		File folder = new File(imageFolder);
		if(!folder.exists()) {
			folder.mkdirs();
		}
		String[] files = folder.list();
		int idx = 0;
		//get max idx
		for(int i=0; i<files.length; ++i) {
			int idx2 = getFileIdx(files[i]);
			if(idx2 > idx) {
				idx = idx2;
			}
		}
		String name = imageFolder+(++idx)+".jpg";
		Log.d(BookSelectorActivity.APP_NAME, "generated file name is: "+name);
		return name;
	}

	public static String getImageNotesFolderName(String book, String identifier) {
		String path = book.substring(0, book.lastIndexOf("/"));
		String filename = book.substring(book.lastIndexOf("/"));
		String imageFolder = (path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/imagenotes"
				+"/"+identifier+"/");

		return imageFolder;
	}
	
	public static String getFirstImageNotes(String book, String identifier) {
		String imageFolder = getImageNotesFolderName(book, identifier);
		File folder = new File(imageFolder);
		if(folder.exists()) {
			String[] files = folder.list();
			if(files.length > 0) {
				int idx = 0;
				while(!new File(imageFolder+idx+".jpg").exists()) {
					++idx;
				}
				Log.d(BookSelectorActivity.APP_NAME, "fist image idx is: "+idx);
				return imageFolder+idx+".jpg";
			}
		} 
		return null;
	}
	
	public static String getNextPicInDirection(String currFile, Direction direction) {
		Log.d(BookSelectorActivity.APP_NAME, "get next file idx for: "+currFile+" next? "+(direction==Direction.NEXT));
		String imageFolder = currFile.substring(0, currFile.lastIndexOf("/"));
		int idx = getFileIdx(currFile);
		String[] images = new File(imageFolder).list();
		int candidate = 0;
		if(direction == Direction.NEXT) {
			candidate = Integer.MAX_VALUE;
			for(int i=0; i<images.length; ++i) {
				int idx2 = getFileIdx(images[i]);
				if(idx2 > idx && (idx2-idx) < (candidate - idx)) {
					candidate=idx2;
				} 
			}
			Log.d(BookSelectorActivity.APP_NAME, "next file idx for: "+currFile+" is "+candidate);
			return (candidate==Integer.MAX_VALUE)?null:imageFolder+"/"+candidate+".jpg";
		} else {
			candidate = -1;
			for(int i=0; i<images.length; ++i) {
				int idx2 = getFileIdx(images[i]);
				if(idx2 < idx && (idx-idx2) < (idx - candidate)) {
					candidate=idx2;
				} 
			}
			Log.d(BookSelectorActivity.APP_NAME, "next file idx for: "+currFile+" is "+candidate);
			return (candidate==-1)?null:imageFolder+"/"+candidate+".jpg";
		}
	}
	
	private static int getFileIdx(String file) {
		Log.d(BookSelectorActivity.APP_NAME, "get File idx: "+file);
		String filename = file.substring(file.lastIndexOf("/")+1);
		int idx = Integer.parseInt(filename.substring(0, filename.lastIndexOf(".")));
		Log.d(BookSelectorActivity.APP_NAME, "File idx: "+idx);
		return idx;
	}
	
	public static String getNotePath(String book, String identifier) {
		String path = book.substring(0, book.lastIndexOf("/"));
		String filename = book.substring(book.lastIndexOf("/"));
		File notesFolder = new File(path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/notes");
		if(!notesFolder.exists()) {
			notesFolder.mkdirs();
		}
		
		return notesFolder.getAbsolutePath()+"/"+identifier;
	}

	public static void saveNotes(String file, String text) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter( new FileWriter(file)));
		pw.print(text);
		pw.close();
	}
	
	public static String read(String filename) throws FileNotFoundException {
		File file = new File(filename);
		if(!file.exists()) {
			return "";
		} 
		FileReader reader = new FileReader(file);
		return getResourceContent(reader, true);
	}


	public static void saveCurrentBookPosition(String book, int spineIdx, int textOffset) throws IOException {
		String path = book.substring(0, book.lastIndexOf("/"));
		String filename = book.substring(book.lastIndexOf("/"));
		String lastPosition = (path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/LastPosition");
		
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(lastPosition)));
		pw.print(spineIdx);
		pw.print(SEP);
		pw.print(textOffset);
		pw.println();
		pw.close();
	}
	
	public static int getSpineIndex(String book) throws IOException {
		File lastPos = getLastPosFile(book);
		if(lastPos.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(lastPos));
			String line = br.readLine();
			return Integer.parseInt(line.split(SEP)[0]);
		} else {
			return 0;
		}
	}

	public static boolean readBefore(String book) {
		return getLastPosFile(book).exists();
	}
	
	public static int getLastReadPosition(String book) throws IOException {
		File lastPos = getLastPosFile(book);
		if(lastPos.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(lastPos));
			String line = br.readLine();
			return Integer.parseInt(line.split(SEP)[1]);
		} else {
			return 0;
		}
	}
	
	private static File getLastPosFile(String book) {
		String path = book.substring(0, book.lastIndexOf("/"));
		String filename = book.substring(book.lastIndexOf("/"));
		return new File(path+"/"+BookSelectorActivity.APP_NAME+"/"+filename+"/LastPosition");
	}


	
}
