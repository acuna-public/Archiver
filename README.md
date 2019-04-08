# Archiver
Archiver is a powerful and elegant archiving library. Supports AES-encruption by password and gzip compression.

Supported formats:

- Zip
- Tar<br>
<br>

**Usage**

     Archiver archiver = new Archiver ();
     
Initialization<br>
<br>

    archiver.create (File file);
    
Create an archive<br>
<br>
    
    archiver.setPassword (String password);
    
Set password to archive<br>
<br>
    
    archiver.add (String|File|InputStream input);
    
Adds file, folder or stream to archive<br>
<br>

    archiver.pack ();
    
Pack current items to archive<br>
<br>
    
    archiver.open (File|InputStream stream);
    
Opens the archive<br>
<br>

    archiver.getEntry (String entry);
    
Get entry from archive<br>
<br>

    archiver.unpack (String|File folder);
    
Unpacks archieve to folder
