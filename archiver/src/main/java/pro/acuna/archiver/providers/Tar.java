	package pro.acuna.archiver.providers;
	/*
	 Created by Acuna on 19.07.2018
	*/
	
	import org.kamranzafar.jtar.TarEntry;
	import org.kamranzafar.jtar.TarInputStream;
	import org.kamranzafar.jtar.TarOutputStream;
	
	import java.io.File;
	import java.io.IOException;
	import java.io.InputStream;
	
	import pro.acuna.archiver.Archiver;
	import pro.acuna.archiver.Provider;
	import pro.acuna.jabadaba.Streams;
	
	public class Tar extends Provider {
		
		private TarEntry entry;
		
		public Tar () {}
		
		private Tar (Archiver archiver) throws Archiver.CompressException {
			super (archiver);
		}
		
		@Override
		public Provider getInstance (Archiver archiver) throws Archiver.CompressException {
			return new Tar (archiver);
		}
		
		@Override
		public String[] setFormats () {
			return new String[] { "tar" };
		}
		
		@Override
		public boolean setPermissions () {
			return true;
		}
		
		@Override
		public void create (File file) throws Archiver.CompressException {
			
			try {
				outputStream = new TarOutputStream (Streams.toOutputStream (file));
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
		@Override
		public void open (InputStream stream) throws Archiver.DecompressException {
			inputStream = new TarInputStream (stream);
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
		
		@Override
		public void addEntry (File file, String entryFile) throws Archiver.CompressException {
			
			try {
				((TarOutputStream) outputStream).putNextEntry (new TarEntry (file, entryFile));
			} catch (IOException e) {
				throw new Archiver.CompressException (e);
			}
			
		}
		
		@Override
		public boolean getNextEntry () throws Archiver.DecompressException {
			
			try {
				return ((entry = ((TarInputStream) inputStream).getNextEntry ()) != null);
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