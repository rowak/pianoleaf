package io.github.rowak.pianoleaf;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

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
	
	private static final int MAX_PITCH = 127;
	
	private static final String HELP_ARG = "--help";
	private static final String IP_ARG_LONG = "--ip";
	private static final String IP_ARG_SHORT = "-i";
	private static final String PORT_ARG_LONG = "--port";
	private static final String PORT_ARG_SHORT = "-p";
	private static final String API_KEY_ARG_LONG = "--api-key";
	private static final String API_KEY_ARG_SHORT = "-k";
	private static final String KEY_START_ARG_LONG = "--key-start";
	private static final String KEY_START_ARG_SHORT = "-s";
	private static final String KEY_END_ARG_LONG = "--key-end";
	private static final String KEY_END_ARG_SHORT = "-e";
	
	public static void main(String[] args) throws Exception {
		String ip = getStringArg(IP_ARG_LONG, IP_ARG_SHORT, null, args);
		int port = getIntArg(PORT_ARG_LONG, PORT_ARG_SHORT, -1, args);
		String accessToken = getStringArg(API_KEY_ARG_LONG, API_KEY_ARG_SHORT, null, args);
		String keyStart = getStringArg(KEY_START_ARG_LONG, KEY_START_ARG_SHORT, null, args);
		String keyEnd = getStringArg(KEY_END_ARG_LONG, KEY_END_ARG_SHORT, null, args);
		boolean help = hasArg(HELP_ARG, null, false, args);
		if (help) {
			printHelp();
		}
		setup(ip, port, accessToken, keyStart, keyEnd);
	}
	
	private static boolean hasArg(String arg, String shortArg,
			boolean defaultArg, String[] args) {
		for (String a : args) {
			if ((arg != null && a.equals(arg)) || a.equals(shortArg)) {
				return true;
			}
		}
		return defaultArg;
	}
	
	private static String getStringArg(String arg, String shortArg,
			String defaultArg, String[] args) {
		for (int i = 0; i < args.length; i++) {
			if ((arg != null && args[i].equals(arg)) || args[i].equals(shortArg)) {
				if (i < args.length-1) {
					return args[i+1];
				}
			}
		}
		return defaultArg;
	}
	
	private static int getIntArg(String arg, String shortArg,
			int defaultArg, String[] args) {
		for (int i = 0; i < args.length; i++) {
			if ((arg != null && args[i].equals(arg)) || args[i].equals(shortArg)) {
				if (i < args.length-1) {
					try {
						int argInt = Integer.parseInt(args[i+1]);
						return argInt;
					} catch (NumberFormatException nfe) {
						return defaultArg;
					}
				}
			}
		}
		return defaultArg;
	}
	
	static void setup(String ip, int port, String accessToken, String keyStart, String keyEnd) {
		Scanner in = new Scanner(System.in);
		AuroraMetadata metadata = new AuroraMetadata(ip, port, null, null);
		if (ip == null || port == -1) {
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
				AuroraMetadata data = auroras.get(i);
				String name = data.getDeviceName() != null ? data.getDeviceName() : null;
				System.out.println((i+1) + ") " + name + " (" + data.getHostName() + ")");
			}
			int auroraIndex = in.nextInt() - 1;
			metadata = auroras.get(auroraIndex);
		}
		
		if (accessToken == null) {
			System.out.println("To authenticate, hold down the power button on your Nanoleaf " +
					"device for 5-7 seconds until the lights start blinking.");
			accessToken = waitForAccessToken(metadata);
			System.out.println("Your API key is \"" + accessToken +
					"\". Write it down for easier setup next time using the --api-key option.");
		}
		
		Aurora aurora = null;
		try {
			aurora = new Aurora(metadata, "v1", accessToken);
		} catch (HttpRequestException | StatusCodeException e) {
			System.out.println("Failed to connect to Nanoleaf device.");
			System.exit(3);
		}
		
		if (keyStart == null) {
			System.out.print("Enter the leftmost key on your keyboard (for example, A0): ");
			keyStart = in.next();
		}
		
		if (keyEnd == null) {
			System.out.print("Enter the rightmost key on your keyboard (for example, C8): ");
			keyEnd = in.next();
		}
		
		System.out.println("Select a MIDI device:");
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < infos.length; i++) {
			System.out.println((i+1) + ") " + infos[i].getName());
		}
		int midiIndex = in.nextInt() - 1;
		
		MidiDevice device = null;
		try {
			device = openDevice(midiIndex, new OutputReceiver(aurora,
					noteToPitch(keyStart), noteToPitch(keyEnd)));
		} catch (StatusCodeException | IOException | MidiUnavailableException e) {
			System.out.println("Failed to open MIDI device.");
			System.exit(4);
		}
		
		System.out.println("\nPianoleaf is now active.\nPress any key + enter to exit.");
		
		while (in.next().equals(""));
		
		device.close();
	}
	
	// Converts an octave+note pair to its MIDI pitch equivalent.
	// For example, C4 = 60. Sharps and flats are not recognized.
	static int noteToPitch(String note) {
		note = note.toUpperCase().replace(" ", "");
		int key = letterToKeyValue(note.charAt(0));
		int octave = note.charAt(1) - '0' + 1;
		return key + (octave * 12);
	}
	
	static int letterToKeyValue(char letter) {
		switch (letter) {
			case 'C': return 0;
			case 'D': return 2;
			case 'E': return 4;
			case 'F': return 5;
			case 'G': return 7;
			case 'A': return 9;
			case 'B': return 11;
			default: return 1;
		}
	}
	
	static String waitForAccessToken(AuroraMetadata metadata) {
		String accessToken = null;
		while (accessToken == null) {
			try {
				accessToken = Setup.createAccessToken(metadata.getHostName(),
						metadata.getPort(), "v1");
			} catch (StatusCodeException e) {
				// will be thrown until user puts device into auth mode
			}
		}
		return accessToken;
	}
	
	static MidiDevice openDevice(int index, Receiver receiver) throws MidiUnavailableException {
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		MidiDevice device = MidiSystem.getMidiDevice(infos[index]);
		Transmitter trans = device.getTransmitter();
		trans.setReceiver(receiver);
		device.open();
		return device;
	}
	
	static void printHelp() {
		System.out.println("Usage: pianoleaf [-i ip] [-p port] [-a api_key] [-s start_key] [-e end_key]");
		System.out.println("Tool for syncing Nanoleaf devices with a MIDI keyboard.");
		System.out.println("\nOptions:");
		System.out.println("  -i, --ip              specifies the IP address for the Nanoleaf device");
		System.out.println("  -p, --port            specifies the port for the Nanoleaf device");
		System.out.println("  -a, --api-key         Set the api key for the Nanoleaf device (returned after manual setup)");
		System.out.println("  -s, --key-start       Set the leftmost key on your MIDI keyboard in the format NOTEOCTAVE (usually set \"A0\")");
		System.out.println("  -e, --key-end         Set the rightmost key on your MIDI keyboard in the format NOTEOCTAVE (usually set \"C8\")");
		System.out.println("      --help            display this help and exit");
		System.out.println("\nExit status:");
		System.out.println("1  mDNS error when searching for Nanoleaf devices");
		System.out.println("2  no Nanoleaf devices found");
		System.out.println("3  failed to connect to the selected Nanoleaf device");
		System.out.println("4  failed to open the selected MIDI device");
		System.exit(0);
	}
}
