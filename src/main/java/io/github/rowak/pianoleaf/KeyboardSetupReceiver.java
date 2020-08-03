package io.github.rowak.pianoleaf;

import java.io.OutputStream;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class KeyboardSetupReceiver implements Receiver {
	
	private final int NOTE_ON = -112;
	private final int NOTE_OFF = -128;
	
	private int lastKey = -1;
	
	public int waitForKey() {
		while (lastKey == -1) {
			
		}
		int key = lastKey;
		lastKey = -1;
		return key;
	}

	@Override
	public void send(MidiMessage msg, long timeStamp) {
		byte[] data = msg.getMessage();
		if (data[0] == NOTE_ON) {
			lastKey = data[1];
		}
	}
	
	@Override
	public void close() {}
}
