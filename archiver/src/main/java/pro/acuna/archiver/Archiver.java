	package pro.acuna.archiver;
	/*
	 Created by Acuna on 17.07.2017
	*/
	
	import java.io.BufferedWriter;
	import java.io.ByteArrayOutputStream;
	import java.io.File;
	import java.io.FileOutputStream;
	import java.io.IOException;
	import java.io.InputStream;
	import java.io.OutputStreamWriter;
	import java.net.URL;
	import java.util.ArrayList;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	
	import pro.acuna.archiver.providers.Gzip;
	import pro.acuna.archiver.providers.Zip;
	import pro.acuna.jabadaba.Arrays;
	import pro.acuna.jabadaba.Console;
	import pro.acuna.jabadaba.Crypto;
	import pro.acuna.jabadaba.Files;
	import pro.acuna.jabadaba.HttpRequest;
	import pro.acuna.jabadaba.Int;
	import pro.acuna.jabadaba.Net;
	import pro.acuna.jabadaba.Objects;
	import pro.acuna.jabadaba.Streams;
	import pro.acuna.jabadaba.Strings;
	import pro.acuna.jabadaba.System;
	import pro.acuna.jabadaba.exceptions.HttpRequestException;
	import pro.acuna.jabadaba.exceptions.OutOfMemoryException;
	
	public class Archiver {
		
		public File file;
		private File prefDestFolder, cmdsFile;
		private String prefDestPath, cmdsFileName = "commands.sh", srcFolder = "", pathFolder = "", type;
		private List<File> prefSrcFiles = new ArrayList<> (), denyFolders = new ArrayList<> ();
		private List<InputStream> prefSrcStreams = new ArrayList<> ();
		private List<String> entries = new ArrayList<> (), cmds2 = new ArrayList<> ();
		private Map<File, String> prefSrcFolders = new HashMap<> ();
		private List<Provider> providers = new ArrayList<> ();
		private Provider provider;
		private String prefPassword, cryptoAlgoritm;
		private long fileId = 0, total = 0;
		
		private Console exec;
		private Crypto crypto;
		
		private BufferedWriter cmd;
		
		public String shell = "";
		
		public Archiver () {
			
			addProvider (new Zip ());
			//addProvider (new Tar ());
			addProvider (new Gzip ());
			
			crypto = new Crypto ();
			
		}
		
		public static class CompressException extends Exception {
			
			public CompressException (Exception e) {
				super (e);
			}
			
			@Override
			public Exception getCause () {
				return (Exception) super.getCause ();
			}
			
		}
		
		public static class DecompressException extends Exception {
			
			public DecompressException (Exception e) {
				super (e);
			}
			
			public DecompressException (String msg) {
				super (msg);
			}
			
			@Override
			public Exception getCause () {
				return (Exception) super.getCause ();
			}
			
		}
		
		public Archiver addProvider (Provider provider) {
			
			providers.add (provider);
			return this;
			
		}
		
		public Archiver setPlugin (Provider provider) {
			
			this.provider = provider;
			return this;
			
		}
		
		private Provider getPlugin (String type) {
			
			for (Provider provider : providers)
				if (Arrays.contains (type, provider.setFormats ()))
					return provider;
			
			return providers.get (0);
			
		}
		
		public Archiver setShell (String shell) {
			
			this.shell = shell;
			return this;
			
		}
		
		public static void compress (String input, String output) throws CompressException {
			compress (new File (input), output);
		}
		
		public static void compress (File input, String output) throws CompressException {
			compress (input, new File (output));
		}
		
		public static void compress (String input, File output) throws CompressException {
			compress (new File (input), output);
		}
		
		public static void compress (File input, File output) throws CompressException {
			compress (input, output, "");
		}
		
		public static void compress (File input, File output, String password) throws CompressException {
			
			try {
				compress (Streams.toInputStream (input), output, password);
			} catch (IOException e) {
				throw new CompressException (e);
			}
			
		}
		
		public static void compress (Object input, File output) throws CompressException {
			compress (Streams.toInputStream (input), output);
		}
		
		public static void compress (InputStream input, File output) throws CompressException {
			compress (input, output, "");
		}
		
		public static void compress (InputStream input, File output, String password) throws CompressException {
			
			Archiver archieve = new Archiver ();
			
			archieve.setPassword (password);
			archieve.addStream (input);
			
			archieve.create (output);
			
			archieve.compress ();
			archieve.close ();
			
		}
		
		public void create (String folder) throws CompressException {
			create (new File (folder));
		}
		
		public Archiver open (String file) throws DecompressException {
			return open (new File (file));
		}
		
		public Archiver open (File file) throws DecompressException {
			
			try {
				
				init (file);
				return open (Streams.toInputStream (file), type);
				
			} catch (IOException e) {
				throw new DecompressException (e);
			}
			
		}
		
		public Archiver open (InputStream stream) throws DecompressException {
			return open (stream, "");
		}
		
		public Archiver open (InputStream stream, String type) throws DecompressException {
			
			this.type = type;
			init ();
			
			provider.open (stream);
			return this;
			
		}
		
		public final String getName () {
			return Objects.getClassName (provider);
		}
		
		private void init () throws DecompressException {
			
			try {
				
				crypto.setPassword (prefPassword);
				
				if (provider == null) provider = getPlugin (type);
				
				if (provider.setPermissions () && !shell.equals ("") && prefDestPath != null) {
					
					cmdsFile = new File (prefDestPath, cmdsFileName);
					
					FileOutputStream stream = new FileOutputStream (cmdsFile);
					cmd = new BufferedWriter (new OutputStreamWriter (stream));
					
					exec = new Console ();
					exec.shell (shell);
					
				}
				
				provider = provider.getInstance (this);
				
			} catch (CompressException | IOException | Console.ConsoleException e) {
				throw new DecompressException (e);
			}
			
		}
		
		private void init (File file) throws DecompressException {
			
			this.file = file;
			
			try {
				
				prefDestPath = Files.getPath (file);
				Files.makeDir (prefDestPath);
				
				type = Files.getExtension (file);
				init ();
				
			} catch (IOException e) {
				throw new DecompressException (e);
			}
			
		}
		
		public void create (File file) throws CompressException {
			
			pathFolder = "";
			
			try {
				
				init (file);
				provider.create (file);
				
				prefDestPath = Files.getPath (this.file);
				Files.makeDir (prefDestPath);
				
			} catch (DecompressException | IOException e) {
				throw new CompressException (e);
			}
			
		}
		
		public void destFolder (String folder) {
			destFolder (new File (folder));
		}
		
		public void destFolder (File folder) {
			prefDestFolder = folder;
		}
		
		public void addFile (String input) {
			addFile (new File (input));
		}
		
		public void addFile (File input) {
			
			prefSrcFiles.add (input);
			++total;
			
		}
		
		public void addStream (InputStream input) {
			prefSrcStreams.add (input);
		}
		
		public Archiver addPath (String folder) {
			
			pathFolder = folder;
			return this;
			
		}
		
		public void addFolder (String input) throws CompressException {
			addFolder (new File (input));
		}
		
		/*public void addFolder (String... input) throws CompressException {
			addFolder (new File (Arrays.implode ("/", input)));
		}*/
		
		public void addFolder (File folder) {
			
			if (!Arrays.contains (folder, prefSrcFolders)) {
				
				long num = Int.size (Files.list (folder, true));
				
				//if (num > 0) {
				
				total += num;
				prefSrcFolders.put (folder, pathFolder);
				
				//}
				
			}
			
		}
		
		public void setPassword (String password) {
			prefPassword = password;
		}
		
		public void setCryptoAlgoritm (String algo) {
			cryptoAlgoritm = algo;
		}
		
		public void denyFolder (String input) {
			denyFolder (new String[] {input});
		}
		
		public void denyFolder (String... input) {
			denyFolder (new File (Arrays.implode ("/", input)));
		}
		
		public void denyFolder (File input) {
			denyFolders.add (input);
		}
		
		public interface ArchievsListener {
			
			void onProgress (String file, long i, long total);
			void onError (String mess);
			
		}
		
		private ArchievsListener listener;
		
		public Archiver addListener (ArchievsListener listener) {
			
			this.listener = listener;
			return this;
			
		}
		
		private String path = "";
		
		public Archiver setPath (String path) {
			
			this.path = path;
			return this;
			
		}
		
		public File compress () throws CompressException {
			
			try {
				
				if (Int.size (prefSrcFolders) > 0 || Int.size (prefSrcFiles) > 0 || Int.size (prefSrcStreams) > 0) {
					
					for (File folder : prefSrcFolders.keySet ()) {
						
						folderId = 0;
						
						srcFolder = folder.getAbsolutePath ();
						pathFolder = prefSrcFolders.get (folder);
						
						//if (!pathFolder.equals ("")) {
						
						if (folder.exists ()) {
							
							if (folder.isDirectory ()) {
								
								String entryFile = Strings.slice (Strings.addEnd ("/", (!path.equals ("") ? path : prefDestPath)), srcFolder);
								
								if (!entryFile.equals (""))
									addEntry (folder, entryFile + "/");
								
								addFolder (folder, entryFile, new ArrayList<String> ());
								
							} else throw new IllegalArgumentException (folder + " is not a folder. Use addFile () method instead if you want to add a file");
							
						} else throw new IOException (folder + " is not exists");
						
						//} else throw new IllegalArgumentException ("Path folder is empty. You must called addPath () before addFolder ().");
						
					}
					
					for (File file : prefSrcFiles) {
						
						if (file.exists ()) {
							
							if (!file.isDirectory ()) {
								
								String entryFile = Strings.slice (prefDestPath + "/", file.getAbsolutePath ());
								addFile (file, entryFile, new ArrayList<String> ());
								
							} else throw new IllegalArgumentException (file + " is a folder. Use addFolder () method instead if you want to add a folder");
							
						} else throw new IOException (file + " is not exists");
						
					}
					
					for (InputStream stream : prefSrcStreams)
						addStream (stream, null, Strings.slice (file.getAbsolutePath (), prefDestPath + "/", true), new ArrayList<String> ());
					
				} else throw new IllegalArgumentException ("Please specify folder, file or stream to add to archive");
				
			} catch (IOException | IllegalArgumentException e) {
				throw new CompressException (e);
			}
			
			// Очищаем все для нового архива
			
			fileId = 0;
			total = 0;
			denyFolders.clear ();
			
			prefSrcFolders.clear ();
			prefSrcFiles.clear ();
			prefSrcStreams.clear ();
			
			return file;
			
		}
		
		private void addEntry (File file, String entryFile) throws CompressException {
			
			if (!Arrays.contains (entryFile, entries)) {
				
				provider.addEntry (file, entryFile);
				setPermissions (file, entryFile);
				
			}
			
		}
		
		public InputStream getInputStream (String entryFile) throws DecompressException {
			
			InputStream stream;
			
			if (encryptedFile (entryFile))
				stream = provider.getInputStream (entryFile + ".aes");
			else
				stream = provider.getInputStream (entryFile);
			
			return stream;
			
		}
		
		public InputStream getEntryStream (String... file) throws DecompressException, OutOfMemoryException {
			
			try {
				
				String entryFile = ((Int.size (file) > 1 && file[0].equals ("")) ? file[1] : Arrays.implode ("/", file));
				InputStream inputStream = getInputStream (entryFile);
				
				if (encryptedFile (entryFile)) {
					
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream ();
					crypto.decrypt (inputStream, outputStream);
					
					return Streams.toInputStream (outputStream);
					
				} else return inputStream;
				
			} catch (Crypto.DecryptException e) {
				throw new DecompressException (e);
			}
			
		}
		
		public String getEntry (String... file) throws DecompressException, OutOfMemoryException {
			
			try {
				return Strings.toString (getEntryStream (file));
			} catch (IOException e) {
				throw new DecompressException (e);
			}
			
		}
		
		private int folderId = 0;
		
		private List<String> addFolder (File folder, String entryFile, List<String> output) throws CompressException {
			
			if (!folder.isDirectory () || !denyFolders.contains (folder)) {
				
				try {
					
					if (folder.isDirectory ()) {
						
						File[] files = folder.listFiles ();
						
						if (files != null && Int.size (files) > 0) {
							
							setPermissions (folder, entryFile + "/"); // Самая верхняя папка
							
							++folderId;
							
							for (File file : files)
								output = addFolder (file, (!entryFile.equals ("") ? entryFile + "/" : "") + file.getName (), output);
							
						} else if (!entryFile.equals ("")) addEntry (folder, entryFile + "/"); // Если папка пустая
						
					} else if (folder.isFile ())
						output = addFile (folder, entryFile, output);
					else if (!folder.exists ())
						throw new IOException ("Folder not exists: " + folder);
					else
						throw new IllegalArgumentException (folder.getAbsolutePath () + " not a folder, use addFile () method instead if you want to add a file");
					
				} catch (IOException | IllegalArgumentException e) {
					throw new CompressException (e);
				}
				
			}
			
			return output;
			
		}
		
		private List<String> newEntries = new ArrayList<> ();
		
		public Archiver pathDenyEntry (String entry) {
			
			newEntries.add (entry);
			return this;
			
		}
		
		private void setPermissions (File file, String entryFile) throws CompressException {
			
			if (shell.equals (Console.su)) {
				
				entries.add (entryFile);
				++fileId;
				
				if (!pathFolder.equals ("") && !srcFolder.equals ("")) {
					
					entryFile = Strings.trimStart (srcFolder + "/", file);
					
					final System.DirData dirData = System.getDirData ();
					
					if (listener != null)
						listener.onProgress (pathFolder + (!entryFile.equals ("/") ? "/" + entryFile : ""), fileId, total);
					
					if (exec != null) {
						
						exec.addListener (new Console.Listener () {
							
							@Override
							public void onExecute (String line, int i) {}
							
							@Override
							public void onSuccess (String line, int i) {
								
								System.DirData data = dirData.matcher (line);
								
								if (!Arrays.contains (data.path, newEntries) && data.chmod != null) {
									
									try {
										
										cmd.write ("chmod " + data.chmod + " " + "\"" + data.path + "\"");
										cmd.newLine ();
										
										cmd.write ("chown " + data.uid + ":" + data.gid + " " + "\"" + data.path + "\"");
										cmd.newLine ();
										
									} catch (IOException e) {
										if (listener != null) listener.onError (e.getMessage ());
									}
									
									newEntries.add (data.path);
									
								}
								
							}
							
							@Override
							public void onError (String line, int i) {
								if (listener != null && !line.contains ("No such file")) listener.onError (line);
							}
							
						});
						
					}
					
				}
				
				file = new File (pathFolder, entryFile);
				cmds2 = System.getDirData ().shell (file, cmds2);
				
			}
			
		}
		
		private List<String> addFile (File file, String entryFile, List<String> output) throws CompressException, IOException {
			return addStream (Streams.toInputStream (file), file/* Костыль для библиотек, которые не поддерживают запись в потоки напрямую */, entryFile, output);
		}
		
		private List<String> addStream (InputStream stream, File file, String entryFile, List<String> output) throws CompressException {
			
			try {
				
				if (file == null || file.isFile ()) {
					
					++folderId;
					
					if (!encryptedFile (entryFile)) {
						
						addEntry (file, entryFile); // Создаем файл
						
						Streams.copy (stream, provider.outputStream);
						
					} else {
						
						Crypto crypto = new Crypto ();
						
						crypto.setPassword (prefPassword);
						crypto.setAlgoritm (cryptoAlgoritm);
						
						addEntry (file, entryFile + ".aes"); // Создаем файл
						
						crypto.encrypt (stream, provider.outputStream);
						
					}
					
					stream.close ();
					
				} else throw new IllegalArgumentException (file.getAbsolutePath () + " not a file, use addFolder () method instead if you want to add a folder");
				
			} catch (IOException | Crypto.EncryptException | IllegalArgumentException e) {
				throw new CompressException (e);
			}
			
			return output;
			
		}
		
		private boolean encryptedFile (String entryFile) {
			return (prefPassword != null && !prefPassword.equals ("") && !entryFile.equals (cmdsFileName));
		}
		
		public static void decompress (String input, String output) throws DecompressException {
			decompress (new File (input), new File (output));
		}
		
		public static void decompress (File input, File output) throws DecompressException {
			decompress (input, output, "");
		}
		
		public static void decompress (String input, String output, String password) throws DecompressException {
			decompress (new File (input), new File (output), password);
		}
		
		public static void decompress (File input, File output, String password) throws DecompressException {
			
			Archiver archieve = new Archiver ();
			
			archieve.open (input);
			
			archieve.setPassword (password);
			archieve.destFolder (output);
			
			archieve.decompress ();
			
		}
		
		public String decompress () throws DecompressException {
			
			String output = "";
			
			try {
				
				if (prefDestFolder != null) {
					
					String dest = prefDestFolder.getAbsolutePath ();
					
					while (provider.getNextEntry ()) {
						
						File file = new File (dest, provider.getEntryName ());
						
						if (!provider.isDirectory ()) {
							
							File parentDir = file.getParentFile ();
							
							if (parentDir != null && (parentDir.isDirectory () || parentDir.mkdirs ())) {
								
								InputStream stream = provider.getInputStream (provider.getEntryName ());
								
								if (!encryptedFile (provider.getEntryName ()))
									Files.copy (stream, file);
								else if (stream != null)
									crypto.decrypt (stream, new File (dest, Strings.trimEnd (".aes", provider.getEntryName ())));
								
								provider.closeEntry ();
								
							} else throw new IOException ("Unable to create folder " + parentDir);
							
						}
						
					}
					
					provider.closeStream ();
					
				} else throw new DecompressException ("Destination folder not found. Use destFolder () method to add destination folder.");
				
			} catch (IOException | Crypto.DecryptException e) {
				throw new DecompressException (e);
			}
			
			return output;
			
		}
		
		public List<String> getCommands () throws DecompressException {
			
			List<String> cmds = new ArrayList<> ();
			
			try {
				
				cmdsFile = new File (prefDestFolder, cmdsFileName);
				cmds = Files.read (cmdsFile, cmds);
				
			} catch (IOException e) {
				throw new DecompressException (e);
			}
			
			return cmds;
			
		}
		
		public static String decompress (String input) throws DecompressException, OutOfMemoryException {
			return decompress (new File (input));
		}
		
		public static String decompress (File input) throws DecompressException, OutOfMemoryException {
			
			try {
				
				Archiver archieve = new Archiver ();
				
				archieve.open (input);
				String output = Strings.toString (archieve.getInputStream (null));
				archieve.clear ();
				
				return output;
				
			} catch (IOException e) {
				throw new DecompressException (e);
			}
			
		}
		
		public static InputStream decompress (URL url) throws DecompressException {
			return decompress (url, Net.getUserAgent ());
		}
		
		public static InputStream decompress (URL url, String userAgent) throws DecompressException {
			return decompress (url, userAgent, Net.defTimeout);
		}
		
		public static InputStream decompress (URL url, String userAgent, int timeout) throws DecompressException {
			
			try {
				
				HttpRequest request = Net.request (url, userAgent, timeout);
				
				InputStream is = request.getInputStream ();
				if (!request.isOK ()) throw new IOException (request.getStatus ());
				
				return is;
				
			} catch (IOException | HttpRequestException e) {
				throw new DecompressException (e);
			}
			
		}
		
		public void clear () throws DecompressException {
			
			try {
				
				if (cmdsFile != null) Files.delete (cmdsFile);
				entries.clear ();
				
			} catch (Exception e) {
				throw new DecompressException (e);
			}
			
		}
		
		public void close () throws CompressException {
			
			try {
				
				if (cmd != null) {
					
					if (exec != null) exec.query (cmds2);
					
					cmd.flush ();
					
					if (cmdsFile != null)
						addFile (cmdsFile, cmdsFileName, new ArrayList<String> ());
					
				}
				
				provider.close ();
				
				clear ();
				
			} catch (DecompressException | Console.ConsoleException | IOException e) {
				throw new CompressException (e);
			}
			
		}
		
		private String test (File file, String folder, String srcDir) throws CompressException, DecompressException, OutOfMemoryException {
			
			create (file);
			
			addPath (srcDir);
			addFolder (srcDir);
			
			compress ();
			close ();
			
			open (file);
			
			String string = getEntry ("111") + "\n\n";
			
			destFolder (folder);
			
			decompress ();
			
			return string;
			
		}
		
		public String test (String srcDir, String destDir, String password) throws CompressException, DecompressException, OutOfMemoryException {
			
			String string = "";
			setShell (Console.su);
			
			for (Provider provider : providers) {
				
				for (String type : provider.setFormats ()) {
					
					string += type + ":\n\n";
					
					setPassword ("");
					
					File file = new File (destDir, "compress." + type);
					string += test (file, destDir + "/decompress/" + type, srcDir);
					
					setPassword (password);
					
					file = new File (destDir, "compress-encrypted." + type);
					string += test (file, destDir + "/decompress-encrypted/" + type, srcDir);
					
				}
				
			}
			
			clear ();
			
			return string;
			
		}
		
	}