package chip8;
/*
 This file is part of JavaCHIP8.

 Copyright 2004 Kustaa Nyholm / SpareTimeLabs
 Copyright 2025 Kustaa Nyholm / SpareTimeLabs

 JavaCHIP8 is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 JavaCHIP8 is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with JavaCHIP8; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.io.*;
import java.util.*;

import chip8.games.Games;

/*
 Code  Assembler  Description  Notes
 00Cx  scdown x  Scroll the screen down x lines  Super only, not implemented
 00E0  cls  Clear the screen
 00EE  rts  return from subroutine call
 00FE  low  disable extended screen mode  Super only
 00FF  high  enable extended screen mode (128 x 64)  Super only
 1xxx  jmp xxx  jump to address xxx
 2xxx  jsr xxx  jump to subroutine at address xxx  16 levels maximum
 3rxx  skeq vr,xx  skip if register r = constant
 4rxx  skne vr,xx  skip if register r <> constant
 5ry0  skeq vr,vy  skip f register r = register y
 6rxx  mov vr,xx  move constant to register r
 7rxx  add vr,vx  add constant to register r  No carry generated
 8ry0  mov vr,vy  move register vy into vr
 8ry1  or rx,ry  or register vy into register vx
 8ry2  and rx,ry  and register vy into register vx
 8ry3  xor rx,ry  exclusive or register ry into register rx
 8ry4  add vr,vy  add register vy to vr,carry in vf
 8ry5  sub vr,vy  subtract register vy from vr,borrow in vf  vf set to 1 if borroesws
 8r06  shr vr  shift register vy right, bit 0 goes into register vf
 8ry7  rsb vr,vy  subtract register vr from register vy, result in vr  vf set to 1 if borrows
 8r0e  shl vr  shift register vr left,bit 7 goes into register vf
 9ry0  skne rx,ry  skip if register rx <> register ry
 axxx  mvi xxx  Load index register with constant xxx
 bxxx  jmi xxx  Jump to address xxx+register v0
 crxx  rand vr,xxx     vr = random number less than or equal to xxx
 dxys  sprite rx,ry,s  Draw sprite at screen location register x,register y height s  Sprites stored in memory at location in index register, maximum 8 bits wide. Wraps around the screen. If when drawn, clears a pixel, vf is set to 1 otherwise it is zero. All drawing is xor drawing (e.g. it toggles the screen pixels
 ek9e  skpr k  skip if key (register rk) pressed  The key is a key number, see the chip-8 documentation
 eka1  skup k  skip if key (register rk) not pressed
 fr07  gdelay vr  get delay timer into vr
 fr0a  key vr  wait for for keypress,put key in register vr
 fr15  sdelay vr  set the delay timer to vr
 fr18  ssound vr  set the sound timer to vr
 fr1e  adi vr  add register vr to the index register
 fr29  font vr  point I to the sprite for hexadecimal character in vr  Sprite is 5 bytes high
 fr33  bcd vr  store the bcd representation of register vr at location I,I+1,I+2  Doesn't change I
 fr55  str v0-vr  store registers v0-vr at location I onwards  I is incremented to point to the next location on. e.g. I = I + r + 1
 fx65  ldr v0-vr  load registers v0-vr from location I onwards  as above.




 --------------------------------------------------------------------------------







 */
public class Chip8Emu {

	public interface Chip8IO {
		boolean testKey(int key);

		void playBeep();
	}

	private int opcode;
	private int argument;
	private int result;

	private byte[] memory = new byte[4096];
	private int[] registers = new int[16];
	private int[] stack = new int[16];
	private int programCounter = 0x200;
	private int stackPointer = 0;
	private int indexRegister = 0;
	private int delayTimer = 0;
	private long timerSetTime = 0;
	private int foreColor = 0xffffffff;
	private int backColor = 0xff000000;

	private int fontSprites[] = { //
			0xf, 0x9, 0x9, 0x9, 0xf, 0x2, 0x6, 0x2, //
			0x2, 0x7, 0xf, 0x1, 0xf, 0x8, 0xf, 0xf, //
			0x1, 0xf, 0x1, 0xf, 0x9, 0x9, 0xf, 0x1, //
			0x1, 0xf, 0x8, 0xf, 0x1, 0xf, 0xf, 0x8, //
			0xf, 0x9, 0xf, 0xf, 0x1, 0x2, 0x4, 0x4, //
			0xf, 0x9, 0xf, 0x9, 0xf, 0xf, 0x9, 0xf, //
			0x1, 0xf, 0xf, 0x9, 0xf, 0x9, 0x9, 0xe, //
			0x9, 0xe, 0x9, 0xe, 0xf, 0x8, 0x8, 0x8, //
			0xf, 0xe, 0x9, 0x9, 0x9, 0xe, 0xf, 0x8, //
			0xf, 0x8, 0xf, 0xf, 0x8, 0xf, 0x8, 0x8 //
	};

	private Chip8IO chip8IO;
	private Random random = new Random();
	private int[] pixels = new int[128 * 64];

	private boolean hiResMode;

	public int[] getPixels() {
		return pixels;
	}

	static class Error extends RuntimeException {
		public Error(String x) {
			super(x);
		}
	}

	public Chip8Emu(Chip8IO io) {
		chip8IO = io;
		reset();
	}

	private void illegalOpcode() {
		throw new Error("Illegal opcode " + hex(opcode, 2) + hex(argument, 2) + " at " + hex(programCounter - 2, 4));
	}

	private void clearScreen() {
		for (int i = 0; i < 128 * 64; ++i) {
			pixels[i] = backColor;
		}
	}

	private void scrollDown(int n) {

		int N = (64 - n) * 128;
		int d = 64 * 128;
		int s = d - n * 128;
		for (int i = N; i > 0; --i) {
			pixels[--d] = pixels[--s];
		}
		for (int i = n * 128; i > 0; --i) {
			pixels[i] = backColor;

		}
	}

	private void drawSprite(int x0, int y0, int h) {
		boolean f = false;
		if (hiResMode) {
			for (int iy = 0; iy < h; ++iy) {
				int m = memory[indexRegister + iy];
				int by = 128 * ((y0 + iy) & 0x3f);
				for (int ix = 0; ix < 8; ix++) {
					if ((m & 0x80) != 0) {
						int t = ((x0 + ix) & 0x7f) + by;
						if (pixels[t] != backColor) {
							pixels[t] = backColor;
							f = true;
						} else {
							pixels[t] = foreColor;
						}
					}
					m <<= 1;
				}
			}
		} else {
			for (int iy = 0; iy < h; ++iy) {
				int m = memory[indexRegister + iy];
				int by = 2 * 128 * ((y0 + iy) & 0x1f);
				for (int ix = 0; ix < 8; ix++) {
					if ((m & 0x80) != 0) {
						int t = ((x0 + ix) & 0x3f) * 2 + by;
						if (pixels[t] != backColor) {
							pixels[t] = backColor;
							pixels[t + 1] = backColor;
							pixels[t + 128] = backColor;
							pixels[t + 128 + 1] = backColor;
							f = true;
						} else {
							pixels[t] = foreColor;
							pixels[t + 1] = foreColor;
							pixels[t + 128] = foreColor;
							pixels[t + 128 + 1] = foreColor;
						}
					}
					m <<= 1;
				}
			}

		}
		registers[0xf] = f ? 1 : 0;
	}

	public void unimplementedOpcode() {
		System.out.println("Unimplemented opcode");
	}

	private void trace(String s) {
		System.out.println("PC 0x" + Integer.toHexString(programCounter) + ": " + s);
	}

	public void executeOneInstruction() {
		opcode = memory[programCounter++] & 0xff;
		argument = memory[programCounter++] & 0xff;
		//System.out.println();
		//System.out.println(hex(programCounter - 2, 4) + "> " + hex(opcode, 2) +hex(argument, 2));
		switch (opcode & 0xf0) {
		case 0x00:
			if (opcode != 0x00) {
				illegalOpcode();
			}
			switch (argument) {
			case 0xc1:
				scrollDown(1);
				break;
			case 0xe0:

				//00E0  cls  Clear the screen
				clearScreen();
				break;
			case 0xee:

				//00EE  rts  return from subroutine call
				programCounter = stack[--stackPointer];

				break;
			case 0xfe:
				hiResMode = false;
				break;
			case 0xff:
				hiResMode = true;
				break;
			default:

				illegalOpcode();
				break;
			}
			break;
		case 0x10:

			//1xxx  jmp xxx  jump to address xxx
			programCounter = ((opcode & 0xf) << 8) + argument;
			break;
		case 0x20:

			//2xxx  jsr xxx  jump to subroutine at address xxx  16 levels maximum
			stack[stackPointer++] = programCounter;
			programCounter = ((opcode & 0xf) << 8) + argument;

			break;
		case 0x30:

			//3rxx  skeq vr,xx  skip if register r = constant
			if (registers[opcode & 0xf] == argument) {
				programCounter += 2;
			}
			break;
		case 0x40:

			//4rxx  skne vr,xx  skip if register r <> constant
			if (registers[opcode & 0xf] != argument) {
				programCounter += 2;
			}
			break;
		case 0x50:

			//5ry0  skeq vr,vy  skip f register r = register y
			if (registers[opcode & 0xf] == registers[argument >> 4]) {
				programCounter += 2;
			}
			break;
		case 0x60:

			//6rxx  mov vr,xx  move constant to register r
			registers[opcode & 0xf] = argument;
			break;
		case 0x70:

			//7rxx  add vr,vx  add constant to register r  No carry generated
			registers[opcode & 0xf] = 0xff & (registers[opcode & 0xf] + argument);
			break;
		case 0x80:
			switch (argument & 0xf) {

			case 0x0:

				//8ry0  mov vr,vy  move register vy into vr
				registers[opcode & 0xf] = registers[argument >> 4];
				break;
			case 0x1:

				//8ry1  or rx,ry  or register vy into register vx
				registers[opcode & 0xf] = registers[opcode & 0xf] | registers[argument >> 4];
				break;
			case 0x2:

				//8ry2  and rx,ry  and register vy into register vx
				registers[opcode & 0xf] = registers[opcode & 0xf] & registers[argument >> 4];
				break;
			case 0x3:

				//8ry3  xor rx,ry  exclusive or register ry into register rx
				registers[opcode & 0xf] = registers[opcode & 0xf] ^ registers[argument >> 4];
				break;
			case 0x4:

				//8ry4  add vr,vy  add register vy to vr,carry in vf
				result = registers[opcode & 0xf] + registers[argument >> 4];
				registers[0xf] = (result & 0xFFFFFF00) != 0 ? 1 : 0;
				registers[opcode & 0xf] = result & 0xff;
				break;
			case 0x5:

				//8ry5  sub vr,vy  subtract register vy from vr,borrow in vf  vf set to 1 if borroesws
				result = registers[opcode & 0xf] - registers[argument >> 4];
				registers[0xf] = (result & 0xFFFFFF00) != 0 ? 1 : 0;
				registers[opcode & 0xf] = result & 0xff;

				break;
			case 0x6:

				//8r06  shr vr  shift register vy right, bit 0 goes into register vf
				result = registers[opcode & 0xf];
				registers[0xf] = result & 0x1;
				registers[opcode & 0xf] = result >> 1;
				if ((argument & 0xf0) != 0) {
					//illegalOpcode();
				}
				break;
			case 0x7:

				//8ry7  rsb vr,vy  subtract register vr from register vy, result in vr  vf set to 1 if borrows
				result = registers[argument >> 4] - registers[opcode & 0xf];
				registers[0xf] = (result & 0xFFFFFF00) != 0 ? 1 : 0;
				registers[opcode & 0xf] = result & 0xff;
				break;
			case 0xe:

				//8r0e  shl vr  shift register vr left,bit 7 goes into register vf
				result = registers[opcode & 0xf];
				registers[0xf] = (result & 0x80) != 0 ? 1 : 0;
				registers[opcode & 0xf] = (result << 1) & 0xff;
				if ((argument & 0xf0) != 0) {
					illegalOpcode();
				}
				break;
			default:
				illegalOpcode();
				break;
			}
			break;
		case 0x90:

			//9ry0  skne rx,ry  skip if register rx <> register ry
			if (registers[opcode & 0xf] != registers[argument >> 4]) {
				programCounter += 2;
			}
			break;
		case 0xa0:

			//axxx  mvi xxx  Load index register with constant xxx
			indexRegister = 0xfff & ((opcode << 8) + argument);
			break;
		case 0xb0:

			//bxxx  jmi xxx  Jump to address xxx+register v0
			programCounter = 0xfff & ((opcode << 8) + argument + registers[0]);
			break;
		case 0xc0:

			//crxx  rand vr,xxx     vr = random number less than or equal to xxx
			registers[opcode & 0xf] = random.nextInt() & argument;
			break;
		case 0xd0: {

			//drys  sprite rx,ry,s  Draw sprite at screen location rx,ry height s  Sprites stored in memory at location in index register, maximum 8 bits wide. Wraps around the screen. If when drawn, clears a pixel, vf is set to 1 otherwise it is zero. All drawing is xor drawing (e.g. it toggles the screen pixels
			int x = registers[opcode & 0xf];
			int y = registers[argument >> 4];
			int h = argument & 0xf;
			drawSprite(x, y, h);

			break;
		}
		case 0xe0:
			switch (argument) {
			case 0x9e: {
				//ek9e  skpr k  skip if key (register rk) pressed  The key is a key number, see the chip-8 documentation
				//System.out.println("KEY=" + registers[0xf & opcode]);
				if (chip8IO.testKey(registers[0xf & opcode])) {
					programCounter += 2;
				}
				break;
			}
			case 0xa1: {
				//eka1  skup k  skip if key (register rk) not pressed
				//System.out.println("KEY=" + registers[0xf & opcode]);
				if (!chip8IO.testKey(registers[0xf & opcode])) {
					programCounter += 2;
				}
				break;
			}
			default:
				illegalOpcode();
			}

			break;
		case 0xf0:
			switch (argument) {
			case 0x07: {

				//fr07  gdelay vr  get delay timer into vr
				int value = delayTimer - ((int) (System.currentTimeMillis() - timerSetTime)) / 60;
				if (value < 0) {
					value = 0;
				}
				registers[opcode & 0xf] = value;
				break;
			}
			case 0x0a: {
				//fr0a  key vr  wait for for keypress, put key in register vr
				// FIXME not nice to block here waiting for a key ...
				for (int key = 0; key < 16; key++) {
					if (chip8IO.testKey(key)) {
						registers[opcode & 0xf] = key;
						break;
					}
				}
			}
				break;
			case 0x15:

				//fr15  sdelay vr  set the delay timer to vr
				delayTimer = registers[opcode & 0xf];
				timerSetTime = System.currentTimeMillis();
				break;
			case 0x18:

				chip8IO.playBeep();
				;
				break;
			case 0x1e:

				//fr1e  adi vr  add register vr to the index register
				indexRegister = 0xfff & (indexRegister + registers[opcode & 0xf]);
				//trace(Integer.toHexString(indexRegister)+" "+memory[indexRegister]);
				break;
			case 0x29:

				//fr29  font vr  point I to the sprite for hexadecimal character in vr  Sprite is 5 bytes high
				indexRegister = registers[opcode & 0xf] * 5;
				break;
			case 0x33:

				//fr33  bcd vr  store the bcd representation of register vr at location I,I+1,I+2  Doesn't change I
				int val = registers[opcode & 0xf];
				memory[indexRegister + 0] = (byte) (val / 100);
				memory[indexRegister + 1] = (byte) (val % 100 / 10);
				memory[indexRegister + 2] = (byte) (val % 10);

				break;
			case 0x55: {

				//fr55  str v0-vr  store registers v0-vr at location I onwards  I is incremented to point to the next location on. e.g. I = I + r + 1
				int r = opcode & 0xf;
				for (int i = 0; i <= r; ++i) {
					memory[indexRegister++] = (byte) registers[i];
				}
				break;

			}
			case 0x65: {

				//fx65  ldr v0-vr  load registers v0-vr from location I onwards  as above.
				int r = opcode & 0xf;
				for (int i = 0; i <= r; ++i) {
					registers[i] = memory[indexRegister++] & 0xff;
				}
				break;
			}

			}
			break;
		default:
			illegalOpcode();
		}
	}

	private int toInt(byte b1, byte b2) {
		int i1 = b1 >= 0 ? b1 : 256 + b1;
		int i2 = b2 >= 0 ? b2 : 256 + b2;
		return i1 * 256 + i2;
	}

	private String hex(int x, int n) {
		String t = Integer.toHexString(x);
		while (t.length() < n) {
			t = "0" + t;
		}
		return t;
	}

	public synchronized void loadGame(InputStream is) {
		try {
			int t, i = 0x200;
			while ((t = is.read()) >= 0) {
				memory[i++] = (byte) t;
			}

			int n = i;
			for (i = 0; i < fontSprites.length; ++i) {
				memory[i] = (byte) (fontSprites[i] << 4);
			}
			if (false) {
				for (int j = 0x200; j < n; j += 2) {
					//System.out.println(memory[j]);
					if ((j & 0xf) == 0) {
						System.out.println();
						System.out.print(hex(j, 4) + ": ");
					}
					System.out.print(hex(toInt(memory[j], memory[j + 1]), 4) + " ");

				}
				System.out.println();
			}
		} catch (IOException e) {
			System.out.println(e);

		}
	}

	public void reset() {
		for (int i = 0; i < registers.length; i++) {
			registers[i] = 0;
		}
		programCounter = 0x200;
		indexRegister = 0;
		clearScreen();
	}

}
