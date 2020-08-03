package io.github.rowak.pianoleaf;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.AuroraMetadata;
import io.github.rowak.nanoleafapi.StatusCodeException;
import io.github.rowak.nanoleafapi.tools.Setup;

public class App {
	
	private static final int KEY_START = 21;
	private static final int KEY_END = 108;
	
	private static String ip;
	private static String accessToken;
	private static int port;
	private static int startKey = -1, endKey = -1;
	
	public static void main(String[] args) throws Exception {
		setup();
	}
	
	static void setup() {
		Scanner in = new Scanner(System.in);
		System.out.println("Searching for Nanoleaf devices...");
		List<AuroraMetadata> auroras = null;
		try {
			auroras = Setup.findAuroras();
		} catch (IOException e) {
			System.out.println("mDNS error.");
			System.exit(1);
		}
		if (auroras.isEmpty()) {
			System.out.println("No devices found.");
			System.exit(2);
		}
		System.out.println("Select a Nanoleaf device:");
		for (int i = 0; i < auroras.size(); i++) {
			AuroraMetadata metadata = auroras.get(i);
			String name = metadata.getDeviceName() != null ? metadata.getDeviceName() : null;
			System.out.println((i+1) + ") " + name + " (" + metadata.getHostName() + ")");
		}
		int auroraIndex = in.nextInt() - 1;
		AuroraMetadata data = auroras.get(auroraIndex);
		
		System.out.println("Hold down the power button on your Nanoleaf device for 5-7 seconds " +
				" until the lights start blinking. This will grant Pianoleaf access" +
				" to your Nanoleaf device.");
//		accessToken = null;
//		while (accessToken == null) {
//			try {
//				accessToken = Setup.createAccessToken(data.getHostName(), data.getPort(), "v1");
//			} catch (StatusCodeException e) {
//				
//			}
//		}
		System.out.println("Your API key is \"" + accessToken + "\". Write it down for easier setup next time using the --key option.");
		Aurora aurora = null;
		try {
			aurora = new Aurora(data, "v1", accessToken);
		} catch (HttpRequestException | StatusCodeException e) {
			System.out.println("Failed to connect to Nanoleaf device.");
			System.exit(3);
		}
		
		System.out.println("Select a MIDI device:");
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < infos.length; i++) {
			System.out.println((i+1) + ") " + infos[i].getName());
		}
		int midiIndex = in.nextInt() - 1;
		
		try {
			openDevice(midiIndex, new OutputReceiver(aurora, KEY_START, KEY_END));
		} catch (StatusCodeException | IOException e) {
			System.out.println("Failed to open MIDI device.");
			System.exit(4);
		}
		
		while (true);
	}
	
	static MidiDevice openDevice(int index, Receiver receiver) {
		MidiDevice device = null;
		try {
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			device = MidiSystem.getMidiDevice(infos[index]);
			Transmitter trans = device.getTransmitter();
			trans.setReceiver(receiver);
			device.open();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
		return device;
	}
}
