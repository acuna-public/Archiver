	package pro.acuna.archiver.providers;
	/*
	 Created by Acuna on 19.07.2018
	*/
	
	import java.io.File;
	import java.io.IOException;
	import java.io.InputStream;
	import java.util.zip.GZIPInputStream;
	import java.util.zip.GZIPOutputStream;
	
	import pro.acuna.archiver.Archiver;
	import pro.acuna.archiver.Provider;
	import pro.acuna.jabadaba.Files;
	import pro.acuna.jabadaba.Streams;
	
	public class Gzip extends Provider {
		
		public Gzip () {}
		
		private Gzip (Archiver archiver) throws Archiver.CompressException {
			super (archiver);
		}
		
		@Override
		public Provider getInstance (Archiver archiver) throws Archiver.CompressException {
			return new Gzip (archiver);
		}
		
		@Override
		public String[] setFormats () {
			return new String[] { "gz" };
		}
		
		@Override
		public boolean setPermissions () {
			return false;
		}
		
		@Override
		public void create (File file) throws Archiver.CompressException {
			
			try {
				outputStream = new GZIPOutputStream (Streams.toOutputStream (file));
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
		@Override
		public void open (InputStream stream) throws Archiver.DecompressException {
			
			try {
				inputStream = new GZIPInputStream (stream);
			} catch (IOException e) {
				throw new Archiver.DecompressException (e);
			}
			
		}
		
		@Override
		public void addEntry (File file, String entryFile) throws Archiver.CompressException {
			// Не нужно
		}
		
		@Override
		public InputStream getInputStream (String entryFile) throws Archiver.DecompressException {
			
			try {
				open (Streams.toInputStream (new File (entryFile)));
			} catch (IOException e) {
				throw new Archiver.DecompressException (e);
			}
			
			return inputStream;
			
		}
		
		private int i = 0;
		
		@Override
		public boolean getNextEntry () throws Archiver.DecompressException {
			++i;
			
			return (i == 1);
			
		}
		
		@Override
		public void closeEntry () throws Archiver.DecompressException {}
		
		@Override
		public String getEntryName () {
			return Files.getName (archiver.file);
		}
		
		@Override
		public boolean isDirectory () {
			return false;
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
				
				//outputStream.finish ();
				outputStream.close ();
				
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
	}