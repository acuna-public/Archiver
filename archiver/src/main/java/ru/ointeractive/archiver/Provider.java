	package ru.ointeractive.archiver;
	/*
	 Created by Acuna on 19.07.2018
	*/
	
	import java.io.File;
	import java.io.InputStream;
	import java.io.OutputStream;
	
	public abstract class Provider {
		
		public Archiver archiver;
		
		protected InputStream inputStream;
		protected OutputStream outputStream;
    
		public Provider () {}
		
		public Provider (Archiver archiver) {
			this.archiver = archiver;
		}
		
		public abstract Provider getInstance (Archiver archiver) throws Archiver.CompressException;
		public abstract String[] setFormats ();
		public abstract boolean setPermissions ();
		public abstract void create (File file) throws Archiver.CompressException;
		public abstract void open (InputStream stream) throws Archiver.DecompressException;
		public abstract void addEntry (File file, String entryFile) throws Archiver.CompressException;
		public abstract InputStream getInputStream (String entryFile) throws Archiver.DecompressException;
		
		public abstract boolean getNextEntry () throws Archiver.DecompressException;
		public abstract String getEntryName ();
		public abstract boolean isDirectory ();
		public abstract void closeEntry () throws Archiver.DecompressException;
		public abstract void closeStream () throws Archiver.DecompressException;
		public abstract void close () throws Archiver.CompressException;
		
	}