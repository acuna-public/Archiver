	package ru.ointeractive.archiver.providers;
	/*
	 Created by Acuna on 19.07.2018
	*/
	
	import java.io.File;
	import java.io.IOException;
	import java.io.InputStream;
	import java.util.zip.ZipEntry;
	import java.util.zip.ZipFile;
	import java.util.zip.ZipInputStream;
	import java.util.zip.ZipOutputStream;
	
	import ru.ointeractive.archiver.Archiver;
	import ru.ointeractive.archiver.Provider;
	import ru.ointeractive.jabadaba.Log;
	import ru.ointeractive.jabadaba.Streams;
	
	public class Zip extends Provider {
		
		private ZipEntry entry;
		private ZipFile file;
		
		public Zip () {
			super ();
		}
		
		private Zip (Archiver archiver) throws Archiver.CompressException {
			
			super (archiver);
			
			try {
				file = new ZipFile (archiver.file);
			} catch (IOException e) {
				// empty
			}
			
		}
		
		@Override
		public Provider getInstance (Archiver archiver) throws Archiver.CompressException {
			return new Zip (archiver);
		}
		
		@Override
		public String[] setFormats () {
			return new String[0]; // По умолчанию работаем с zip
		}
		
		@Override
		public boolean setPermissions () {
			return true;
		}
		
		@Override
		public void create (File file) throws Archiver.CompressException {
			
			try {
				outputStream = new ZipOutputStream (Streams.toOutputStream (file));
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
		@Override
		public void open (InputStream stream) throws Archiver.DecompressException {
			inputStream = new ZipInputStream (stream);
		}
		
		@Override
		public InputStream getInputStream (String entryFile) throws Archiver.DecompressException {
			
			try {
				return file.getInputStream (new ZipEntry (entryFile));
			} catch (IOException e) {
				throw new Archiver.DecompressException (e);
			}
			
		}
		
		@Override
		public void addEntry (File file, String entryFile) throws Archiver.CompressException {
			
			try {
				((ZipOutputStream) outputStream).putNextEntry (new ZipEntry (entryFile));
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
		@Override
		public boolean getNextEntry () throws Archiver.DecompressException {
			
			try {
				return ((entry = ((ZipInputStream) inputStream).getNextEntry ()) != null);
			} catch (IOException e) {
				throw new Archiver.DecompressException (e);
			}
			
		}
		
		@Override
		public String getEntryName () {
			return entry.getName ();
		}
		
		@Override
		public boolean isDirectory () {
			return entry.isDirectory ();
		}
		
		@Override
		public void closeEntry () throws Archiver.DecompressException {
			
			try {
				((ZipInputStream) inputStream).closeEntry ();
			} catch (IOException e) {
				throw new Archiver.DecompressException (e);
			}
			
		}
		
		@Override
		public void closeStream () throws Archiver.DecompressException {
			
			try {
				inputStream.close ();
			} catch (IOException e) {
				throw new Archiver.DecompressException (e);
			}
			
		}
		
		@Override
		public void close () throws Archiver.CompressException {
			
			try {
				
				outputStream.flush ();
				outputStream.close ();
				
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
	}