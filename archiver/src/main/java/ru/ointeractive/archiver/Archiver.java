	package ru.ointeractive.archiver;
  /*
   Created by Acuna on 17.07.2017
  */
	
	import java.io.BufferedWriter;
	import java.io.ByteArrayOutputStream;
	import java.io.File;
	import java.io.FileOutputStream;
	import java.io.IOException;
	import java.io.InputStream;
	import java.io.OutputStream;
	import java.io.OutputStreamWriter;
	import java.net.URL;
	import java.util.ArrayList;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	
	import ru.ointeractive.archiver.providers.Gzip;
	import ru.ointeractive.archiver.providers.Tar;
	import ru.ointeractive.archiver.providers.Zip;
	import ru.ointeractive.jabadaba.Arrays;
	import ru.ointeractive.jabadaba.Console;
	import ru.ointeractive.jabadaba.Crypto;
	import ru.ointeractive.jabadaba.Files;
	import ru.ointeractive.jabadaba.HttpRequest;
	import ru.ointeractive.jabadaba.Int;
	import ru.ointeractive.jabadaba.Net;
	import ru.ointeractive.jabadaba.Objects;
	import ru.ointeractive.jabadaba.Streams;
	import ru.ointeractive.jabadaba.Strings;
	import ru.ointeractive.jabadaba.System;
	import ru.ointeractive.jabadaba.exceptions.HttpRequestException;
	import ru.ointeractive.jabadaba.exceptions.OutOfMemoryException;
	
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
			addProvider (new Tar ());
			addProvider (new Gzip ());
			
			crypto = new Crypto ();
			
		}
		
		public static class CompressException extends Exception {
			
			private String file;
			
			public CompressException (Exception e, String... file) {
				
				super (e);
				
				if (Int.size (file) > 0)
					this.file = Arrays.implode (file);
				
			}
			
			@Override
			public Exception getCause () {
				return (Exception) super.getCause ();
			}
			
			public String getFile () {
				return file;
			}
			
		}
		
		public static class DecompressException extends Exception {
			
			private String file;
			
			public DecompressException (Exception e, String... file) {
				
				super (e);
				
				if (Int.size (file) > 0)
					this.file = Arrays.implode (Files.DS, file);
				
			}
			
			public DecompressException (String msg) {
				super (msg);
			}
			
			@Override
			public Exception getCause () {
				return (Exception) super.getCause ();
			}
			
			public String getEntry () {
				return file;
			}
			
		}
		
		public Archiver addProvider (Provider provider) {
			
			providers.add (provider);
			return this;
			
		}
		
		private Provider getProvider (String type) {
			
			for (Provider provider : providers)
				if (Arrays.contains (type, provider.setFormats ()))
					return provider;
			
			return providers.get (0);
			
		}
		
		public Archiver setShell (String shell) {
			
			this.shell = shell;
			return this;
			
		}
		
		public static void pack (File input, String output) throws CompressException {
			pack (input, new File (output));
		}
		
		public static void pack (File input, File output) throws CompressException {
			pack (input, output, "");
		}
		
		public static void pack (File input, File output, String password) throws CompressException {
			
			try {
				pack (Streams.toInputStream (input), output, password);
			} catch (IOException e) {
				throw new CompressException (e);
			}
			
		}
		
		public static void pack (Object input, String output) throws CompressException {
			pack (input, new File (output));
		}
		
		public static void pack (Object input, File output) throws CompressException {
			pack (Streams.toInputStream (input), output);
		}
		
		public static void pack (InputStream input, File output) throws CompressException {
			pack (input, output, "");
		}
		
		public static void pack (InputStream input, File output, String password) throws CompressException {
			
			Archiver archieve = new Archiver ();
			
			archieve.setPassword (password);
			archieve.add (input);
			
			archieve.create (output);
			
			archieve.pack ();
			archieve.close ();
			
		}
		
		public void create (String folder) throws CompressException {
			create (new File (folder));
		}
		
		public Archiver open (String file) throws DecompressException {
			return open (new File (file));
		}
		
		public Archiver open (File file) throws DecompressException {
			
			init (file);
			
			try {
				
				provider.open (Streams.toInputStream (file));
				return this;
				
			} catch (IOException e) {
				throw new DecompressException (e);
			}
			
		}
		
		public final String getName () {
			return Objects.getClassName (provider);
		}
		
		private void init () throws DecompressException {
			
			try {
				
				crypto.setPassword (prefPassword);
				
				if (provider == null) provider = getProvider (type);
				
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
		
		public Archiver create (File file) throws CompressException {
			
			pathFolder = "";
			
			try {
				
				init (file);
				provider.create (file);
				
				prefDestPath = Files.getPath (this.file);
				Files.makeDir (prefDestPath);
				
			} catch (DecompressException | IOException e) {
				throw new CompressException (e);
			}
			
			return this;
			
		}
		
		public Archiver add (String input) {
			return add (new File (input));
		}
		
		public Archiver add (File input) {
			
			if (input.isDirectory ()) {
				
				if (!Arrays.contains (input, prefSrcFolders)) {
					
					long num = Int.size (Files.list (input, true));
					
					//if (num > 0) {
					
					total += num;
					prefSrcFolders.put (input, pathFolder);
					
					//}
					
				}
				
			} else {
				
				prefSrcFiles.add (input);
				++total;
				
			}
			
			return this;
			
		}
		
		public Archiver add (InputStream input) {
			
			prefSrcStreams.add (input);
			return this;
			
		}
		
		public Archiver setFolderPath (String folder) {
			
			pathFolder = folder;
			return this;
			
		}
		
		public Archiver setPassword (String password) {
			
			prefPassword = password;
			return this;
			
		}
		
		public Archiver setCryptoAlgoritm (String algo) {
			
			cryptoAlgoritm = algo;
			return this;
			
		}
		
		public void denyFolder (String input) { // TODO void
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
		
		public File pack () throws CompressException {
			
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
								
							} else
								throw new IllegalArgumentException (folder + " is not a folder. Use addFile () method instead if you want to add a file");
							
						} else throw new IOException (folder + " is not exists");
						
						//} else throw new IllegalArgumentException ("Path folder is empty. You must called addPath () before addFolder ().");
						
					}
					
					for (File file : prefSrcFiles) {
						
						if (file.exists ()) {
							
							if (!file.isDirectory ()) {
								
								String entryFile = Strings.slice (prefDestPath + "/", file.getAbsolutePath ());
								addFile (file, entryFile, new ArrayList<String> ());
								
							} else
								throw new IllegalArgumentException (file + " is a folder. Use addFolder () method instead if you want to add a folder");
							
						} else throw new IOException (file + " is not exists");
						
					}
					
					for (InputStream stream : prefSrcStreams)
						addStream (stream, null, Strings.slice (file.getAbsolutePath (), prefDestPath + "/", true), new ArrayList<String> ()); // TODO
					
				} else
					throw new IllegalArgumentException ("Please specify folder, file or stream to add to archive");
				
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
		
		private InputStream getInputStream (String entryFile) throws DecompressException {
			
			InputStream stream;
			
			if (encryptedFile (entryFile))
				stream = provider.getInputStream (entryFile + ".aes");
			else
				stream = provider.getInputStream (entryFile);
			
			return stream;
			
		}
		
		public InputStream getEntryStream (String... file) throws DecompressException {
			
			try {
				
				String entryFile = ((Int.size (file) > 1 && file[0].equals ("")) ? file[1] : Arrays.implode ("/", file));
				InputStream inputStream = getInputStream (entryFile);
				
				if (encryptedFile (entryFile)) {
					
					OutputStream outputStream = new ByteArrayOutputStream ();
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
				throw new DecompressException (e, file);
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
							
						} else if (!entryFile.equals (""))
							addEntry (folder, entryFile + "/"); // Если папка пустая
						
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
		
		private void setPermissions (File file, String entryFile) {
			
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
							public void onExecute (String line, int i) {
							}
							
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
					
				} else
					throw new IllegalArgumentException (file.getAbsolutePath () + " not a file, use addFolder () method instead if you want to add a folder");
				
			} catch (IOException | Crypto.EncryptException | IllegalArgumentException e) {
				throw new CompressException (e);
			}
			
			return output;
			
		}
		
		private boolean encryptedFile (String entryFile) {
			return (prefPassword != null && !prefPassword.equals ("") && !entryFile.equals (cmdsFileName));
		}
		
		public static String decompress (File input) throws DecompressException, OutOfMemoryException {
			return decompress (input.getAbsolutePath ());
		}
		
		public static String decompress (String input) throws DecompressException, OutOfMemoryException {
			return new Archiver ().open (input).getEntry ();
		}
		
		public static void unpack (String input, String output) throws DecompressException {
			unpack (new File (input), new File (output));
		}
		
		public static void unpack (File input, File output) throws DecompressException {
			unpack (input, output, "");
		}
		
		public static void unpack (String input, String output, String password) throws DecompressException {
			unpack (new File (input), new File (output), password);
		}
		
		public static void unpack (File input, File output, String password) throws DecompressException {
			
			Archiver archieve = new Archiver ();
			
			archieve.open (input);
			archieve.setPassword (password);
			archieve.unpack (output);
			
		}
		
		public Archiver unpack (String input) throws DecompressException {
			return unpack (new File (input));
		}
		
		public Archiver unpack (File destFolder) throws DecompressException {
			
			prefDestFolder = destFolder;
			
			try {
				
				if (prefDestFolder.isDirectory ()) {
					
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
					
				} else
					throw new DecompressException ("Destination folder must be a folder");
				
			} catch (IOException | Crypto.DecryptException e) {
				throw new DecompressException (e);
			}
			
			return this;
			
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
		
		public static InputStream unpack (URL url) throws DecompressException {
			return unpack (url, Net.getUserAgent ());
		}
		
		public static InputStream unpack (URL url, String userAgent) throws DecompressException {
			return unpack (url, userAgent, HttpRequest.defTimeout);
		}
		
		public static InputStream unpack (URL url, String userAgent, int timeout) throws DecompressException {
			
			try {
				
				HttpRequest request = Net.request (url, userAgent, timeout);
				
				InputStream is = request.getInputStream ();
				if (!request.isOK ()) throw new IOException (request.getStatus ());
				
				return is;
				
			} catch (IOException | HttpRequestException | OutOfMemoryException e) {
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
			
			setFolderPath (srcDir);
			add (srcDir);
			
			pack ();
			close ();
			
			open (file);
			
			String string = getEntry ("111") + "\n\n";
			
			unpack (folder);
			
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