# Archiver
Archiver is a powerful and elegant archiving library. Supported formats:

- Zip
- Tar
- Gzip

**Usage**<br>
<br>

     Archiver archiver = new Archiver ();
     
Initialization<br>
<br>

    archiver.create (File file);
    
Create an archive<br>
<br>
    
    archiver.add (String input, File output);
    archiver.add (String input, File output, String password);
    
Create new archive<br>
<br/>
    
    archiver.open (File|InputStream stream);
    
Open an archive<br>
<br>

