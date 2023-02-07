# FEnc.jar
A simple, cross-platform CLI program to encrypt/decrypt files. The program takes as input a file (or a list of files/directories). Each file is overwritten with its encrypted counterpart. For example, a file containing part of a section from the hobbit:

![image](https://user-images.githubusercontent.com/117754232/217385037-54a635da-fd2a-46e8-bea0-c95830854d55.png)

The program is called like so...
```batch
java -jar fenc.jar -k=secret hobbit.txt
```

...which encrypts `hobbit.txt` using AES, with the key/password `secret`. `hobbit.txt` now contains encrypted data. Part of the result is displayed in Windows notepad as follows:

![image](https://user-images.githubusercontent.com/117754232/217385109-acbf02a5-0e2d-4efc-95da-1075667d17d8.png)

> **Note**
> This is *example* output by the program on this file with `secret` as the key. The result of the encryption is random upon each execution, so encrypting two copies of the same file with the same key is almost *guaranteed* to result in different, random outputs. Despite this, each of these encrypted files will be validly decrypted back to the same, original file. (This is normal.)

## Invocation
Invoking the program is simple. Call it as a Java program, specify a `--key`, and specify which files (or directories) you want encrypted: 
```batch
java -jar fenc.jar --key="some key" file1.txt /file2.txt C:/some/directory/
```
The program encrypts each of the files and overwrites them with their encrypted counterparts. For each directory specified, every file in the directory is encrypted as if specified directly as an argument.

To decrypt, you may run the same but with the `-d` flag:
```batch
java -jar fenc.jar --key="some key" -d file1.txt /file2.txt C:/some/directory/
```

If a flag or file path has a space in it, you can wrap it in quotes:
```batch
java -jar fenc.jar --key=shmassword "C:/Some File.txt"
```

## Features/CLI Flags
FEnc allows for encryption and decryption using AES and also supports a few command line options and additional features. Here's a list of all the command-line parameters it accepts:

### Key (`-k`, `--key`)
The encryption/decryption **Key** can be specified with `-k` or `--key`, e.g.:
```
java -jar fenc.jar -k="Some key" file.txt
```
The key is used when encrypting or decrypting. A file can only be decrypted with the key that it was encrypted with.

### Decryption (`-d`, `--dec`, `--decrypt`)
**Decryption** can be enabled using `-d`, `--dec`, or `--decrypt`, e.g.:
```
java -jar fenc.jar -k="Some key" --dec file.txt
```
This will cause the program to decrypt `file.txt` instead of encrypt it.

### Buffer Size (`-bs`, `--buffer-size`)
The **Buffer Size**, in bytes, is used when reading in files. It can be specified using `-bs` or `--buffer-size`, e.g.:
```
java -jar fenc.jar -k="Some key" -bs=1048576 file.txt
```
The default buffer size is `65536` bytes, or 64KB.

### Quiet Mode (`-q`, `-s`, `--quiet`, `--suppress-success-messages`)
**Quiet Mode** can be enabled using `-q`, `-s`, `--quiet`, or `--suppress-success-messages`, e.g.:
```
java -jar fenc.jar -k="Some key" -q file.txt
```
When this option is enabled, success messages are suppressed. This is useful for large batches of files or directories containing many small files to prevent repeated successes from flooding the console. Errors are still printed out.

### Hash Mode (`-h`, `--hash`)
**Hash Mode** can be enabled using `-h` or `--hash`, e.g.:
```
java -jar fenc.jar -h file.txt
```
This causes the program to hash each of the files it traverses then print the result, instead of it encrypting/decrypting them. When this mode is enabled, the program does not write to the file system at all; it only reads from it. If this flag is enabled, a **key** may not be specified and **decryption** may not be enabled, and all other command line options, except for the **buffer size**, are ignored.


## Algorithm
FEnc uses AES with the following options:
* 256-bit keys
* CBC mode
* PKCS5Padding

Where applicable, SHA-256 hashes are used. The program hashes the string provided as the value of the `--key` (or `-k`) command line option to get the 256-byte string used as the AES key. The program also attaches a hashed header to each encrypted file to be able to determine if the file has already been encrypted. This also utilizes SHA-256.
