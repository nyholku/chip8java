javac -d bin -sourcepath src src\chip8\*.java src\chip8\games\*.java
copy src\chip8\games\*.ch8 bin\chip8\games\
java -cp bin chip8.ChipEmuMain
