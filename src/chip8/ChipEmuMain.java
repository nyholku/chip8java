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

import javax.swing.*;

import chip8.games.Games;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Demo to run Chip8Emu emulator (running Invaders game) in an AWT/Swing window.
 * <p>
 * Creates a window, instantiates the emulator, loads the game and builds the <br>
 * supporting infra to interface the emulator with the window/graphics and keyboard,<bR
 * and loads the game and runs the emulator.
 * <p>
 * Use cursor up key to start the game and shoot, left/right to move the gun.
 * <p>
 * Assuming you are in the <code>src</code> folder you can compile and run <br>
 * this code with the following two commands:
 * <p>
 * <pre><code>
 * javac chip8/*.java
 * java chip8.ChipEmuMain
 * </code></pre>
 * 
 * @author Kustaa Nyholm
 */
public class ChipEmuMain {
	public ChipEmuMain() {
		boolean[] keyPressed = new boolean[16];

		// Instantiate the emulator and use an anonymous class to implement the interface
		Chip8Emu.Chip8IO chip8io = new Chip8Emu.Chip8IO() {
			@Override
			public boolean testKey(int key) {
				return keyPressed[key];
			}

			@Override
			public void playBeep() {
				// Would be nice to play on actual sound here but this'll do for now
				java.awt.Toolkit.getDefaultToolkit().beep();
			}
		};

		Chip8Emu emulator = new Chip8Emu(chip8io);
		
		// Load a game with the help of the Games class, all games are in the package chip8.games as ch8 binary files
		Games.Game game = Games.getGames().get("Invaders");
		emulator.loadGame(game.getAsStream());
		
		// Invaders uses arrow keys, star/shoot with the cursor up key, left/right to move the gun
		// See getKeyMapping for how QWERTY keys are mapped Cosmac keys
		Map<Integer, Integer> mapping = game.getKeyMapping(); 
		
		// The emulator executes CHIP8 instructions one by one in backgroudn thread, create it here
		Thread emuThread = new Thread(() -> {
			while (true) {
				try {
					emulator.executeOneInstruction();
					Thread.currentThread().sleep(1); // This seems to produce a usable speed
				} catch (InterruptedException e) {
				}
			}
		});

		// The emulator provides the pixels as int[], we need a panel and a BufferedImage to display them
		BufferedImage bufferedImage = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
		JPanel panel = new JPanel() {
			@Override
			public void paint(Graphics g) {
				super.paint(g);
				bufferedImage.setRGB(0, 0, 128, 64, emulator.getPixels(), 0, 128);
				g.drawImage(bufferedImage, 0, 0, getWidth(), getHeight(), null);
			}
		};

		// Create a key listener to catch and map the key presses to CHIP8 keys
		panel.addKeyListener(new KeyAdapter() {
			public void setPressed(KeyEvent e, boolean pressed) {
				int keyCode = e.getKeyCode();
				if (mapping.containsKey(keyCode))
					keyPressed[mapping.get(keyCode)] = pressed;
			}

			@Override
			public void keyPressed(KeyEvent e) {
				setPressed(e, true);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				setPressed(e, false);
			}
		});
		
		// Create at timer to request repaint of the panel/pixels at about 60 frames per second
		Timer timer = new Timer(0, (x) -> {
			panel.repaint();
		});
		timer.setDelay(16);
		timer.setRepeats(true);

		// Finally create a window to hold the panel 
		JFrame frame = new JFrame("Chip8 - Games");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(100,100);
		frame.setSize(8 * 64, 8 * 32);
		frame.add(panel);
		frame.setVisible(true);

		panel.setFocusable(true);
		panel.requestFocusInWindow();
		
		// That is need now is to start the emulator and the timer
		emuThread.start();
		timer.start();

	}

	public static void main(String[] args) {
		// The UI needs to be created in the EDT so we instantiate the whole thing there with this
		SwingUtilities.invokeLater(() -> {
			new ChipEmuMain();
		});

	}

}
