package chip8.games;

import java.awt.event.KeyEvent;
/*
This
file is part of JavaCHIP8.

Copyright 2004 Kustaa Nyholm / SpareTimeLabs
Copyright 2025 Kustaa Nyholm / SpareTimeLabs

JavaCHIP8 is free software,"", but the copyright on  the '.ch8' files in this directory is unkown,"",
it is assumed that they are in the public domain or that the copyright holder does not object
to them being distributed.

The structure of games[] array is as follow: each game consists of two strings. The first one names
the game and there needs to be a corresponding '.ch8' resource/file in the project/jar file. The second
maps the keys from the phone 1-9 keys to the chip8 hexadecimal keys. First character of the string
gives the hexvalue for key '1' and so on. Use '.' for non mapped keys.

*/
import java.io.*;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Games {

	public static class Game {
		private String m_Name;
		private String m_KeyMapping;

		public Game(String name, String keyMapping) {
			m_Name = name;
			m_KeyMapping = keyMapping;
		}

		public String getName() {
			return m_Name;

		}

		public InputStream getAsStream() {
			return Games.class.getResourceAsStream(m_Name + ".ch8");
		}

		// Key mapping is specified as the second string of the pair of string
		// for each Game in the m_Games table. If the string is empty then
		// the default mapping is returned.
		//
		// The default key mapping from QWERTY to CHIP8/Cosmac is as follows
		//
		//   QWERTY           Cosmac
		//
		//   1 2 3 4	      1 2 3 C
		//   Q W E R    =>    4 5 6 D
		//   A S D F          7 8 9 E
		//   Z X C V          A 0 B F
		//
		// If the string is not empty then the code assumes it is a list
		// of comma separated VK_xx names for constants int the KeyEvent class.
		// For example see how Invaders maps the arrow keys to Cosmac keys 4 5 6 
		// which are the only keys that that game uses

		public Map<Integer, Integer> getKeyMapping() {
			return getKeyMapping(m_KeyMapping);
		}

		static public Map<Integer, Integer> getKeyMapping(String spec) { // keycode -> chip8 key
			Map<Integer, Integer> map = new Hashtable<Integer, Integer>();
			if (spec.length() == 0) { // default mapping
				map.put(KeyEvent.VK_1, 0x1);
				map.put(KeyEvent.VK_2, 0x2);
				map.put(KeyEvent.VK_3, 0x3);
				map.put(KeyEvent.VK_4, 0xC);

				map.put(KeyEvent.VK_Q, 0x4);
				map.put(KeyEvent.VK_W, 0x5);
				map.put(KeyEvent.VK_E, 0x6);
				map.put(KeyEvent.VK_R, 0xD);

				map.put(KeyEvent.VK_A, 0x7);
				map.put(KeyEvent.VK_S, 0x8);
				map.put(KeyEvent.VK_D, 0x9);
				map.put(KeyEvent.VK_F, 0xE);

				map.put(KeyEvent.VK_Z, 0xA);
				map.put(KeyEvent.VK_X, 0x0);
				map.put(KeyEvent.VK_C, 0xB);
				map.put(KeyEvent.VK_V, 0xF);
			} else {
				String[] keycodes = spec.split(",");
				for (int i = 0; i < keycodes.length; i++)
					keycodes[i] = keycodes[i].trim();

				for (int key = 0; key < keycodes.length; key++) {
					String codename = keycodes[key];
					if (codename.length() == 0)
						continue;
					try {
						int keycode = KeyEvent.class.getField(codename).getInt(null);
						map.put(keycode, key);
					} catch (Exception e) {
						System.out.println("Exception when trying to map " + codename + " to chip8 key " + key);
						e.printStackTrace();
					}
				}
			}
			return map;
		}

	}

	private static final String m_Games[] = { //
			"Alien", "", // ...3AC...
			"Ant", "", // 
			"Blinky", "", // F3.718.6.
			"Blitz", "", // 
			"Brix", "", // 
			"Car", "", // 
			"Connect4", "", // 
			"Field", "", // 
			"Guess", "", // 
			"Hidden", "", // 
			"Invaders", ",,,,VK_LEFT,VK_UP,VK_RIGHT,,,,,,,,,,", // 
			"Joust", "", // 
			"Kaleid", "", // 
			"Maze", "", // 
			"Merlin", "", // 
			"Missile", "", // 
			"Piper", "", // 
			"Pong", "", // 
			"Pong2", "", // 
			"Puzzle", "", // 
			"Puzzle2", "", // 
			"Race", "", // 
			"Spacefig", "", // 
			"Syzygy", "", // 
			"Tank", "", // 
			"Tetris", "", // 
			"TicTac", "", // 
			"UBoat", "", // 
			"UFO", "", // 
			"VBrix", "", // 
			"Vers", "", // 
			"Wipeoff", "", // 
			"Worm3", "" // 
	};

	public static LinkedHashMap<String, Game> getGames() {
		LinkedHashMap<String, Game> games = new LinkedHashMap<String, Game>();
		for (int i = 0; i < m_Games.length; i += 2)
			games.put(m_Games[i], new Game(m_Games[i], m_Games[i + 1]));
		return games;

	}

}
