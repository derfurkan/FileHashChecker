# 🗂️ FileHashChecker

FileHashChecker is a program that will verify contents of a folder by hash values given in a HashFile.
It uses thread technology to get the maximum speed out of any system to verify contents as quickly as possible.

## How to run
FileHashChecker must be run with a configuration file as first argument.<br/>
It would look like this<br/><br/>
`java -jar FileHashChecker.jar ConfigurationFile.json -v`<br/>
The `-v` argument is optinal and prints out verbose messages in the console.<br/><br/>

`ConfigurationFile` example:
``` Json
{
  "checkFile": "FileHashes.json",
  "algorithm": "MD5",
  "multiThreading": true,
  "threads": 30
}
```

</p>

#### Parameters ####

* `checkFile` This is the file that contains all hashes to verify the files. Can be generated with [FileHashGenerator](https://github.com/nichtfurkan/FileHashGenerator "FileHashGenerator")
* `algorithm` The algorithm used to generate the file hashes. You can use `MD5, SHA1, SHA256, SHA512, ADLER32, CRC32`</p>
> Where [`ADLER32`](https://en.wikipedia.org/wiki/Adler-32#Calculation "ADLER32") and [`MD5`](https://en.wikipedia.org/wiki/MD5#Algorithm "MD5") are checksum algorithms and therefor the fastest option.

* `multiThreading` Boolean if multiple threads should be used for faster verification.
* `threads` Number of threads that should be generated at once for faster verification. `multiThreading` must be enabled.<br/><br/>


`checkFile` example generated with [FileHashGenerator](https://github.com/nichtfurkan/FileHashGenerator "FileHashGenerator") using the MD5 algorithm:
```Json
{
  "\\Folder1\\File1.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\File2.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\Folder1\\Folder2\\File1.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\File3.txt": "d41d8cd98f00b204e9800998ecf8427e",
  "\\File5.txt": "d41d8cd98f00b204e9800998ecf8427e"
}
```

## How it works

FileHashChecker will verify files in the folder where the jar is being located.<br/>
It will first locate all files in the `checkFile` put them in a list and sort them by file size for a higher quantity of verified files in a short time.<br/>
After it will split the list into the `threads` number and put the rest in an extra thread.<br/>
Now the MainThread will post a status every second in the command prompt.









