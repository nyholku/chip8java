# A simple CHIP8 simulator in Java

This code was originally written as a  MIDlet for Nokia 6230 phone.

Since MIDlets and Java applets are now all but gone I converted this
just for fun to run on desktop.

This is not intended for any actual purpose, feel free to rip of
any code and create something useful from this.

No source code or instructions are available for the games,
after all I think they were created on ruled paper back in the 1970s.

The emulator seems pretty complete and bug free AFAIK, some of
the games seem to have small bugs IMO.

The emulator is full emulation of the original CHIP8, except
that the graphics does not live in the same emulated memory
as the code and sprites so any code that expects to be
able to grab something from the graphics and use that data
in the CHIP8 code is not going to work.  

Also attempting to call CDP1802 code is not possible as
this does not emulate that CPU.

## Running the code

To run the code execute from command line following in the root directory
of the project in macOS or Linux:

```
./run.sh
```

If the script does not have execute rights do

```
chmod +x run.sh
```

For Windows use

```
run.bat
```
