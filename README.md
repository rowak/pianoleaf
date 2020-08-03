# Pianoleaf
This is a command line tool for syncing your Nanoleaf lights with a MIDI keyboard.

## Usage
Download the latest jar file from the [releases](https://github.com/rowak/pianoleaf/releases) page. **Plug your MIDI keyboard into your computer using USB.** Open up a terminal and run the jar file using <code>java -jar PATH\_TO\_PIANOLEAF\_JAR</code>. You will be prompted to select your Nanoleaf device and MIDI keyboard, then Pianoleaf will be active.

You can also use the following command line arguments to skip their corresponding steps in the setup process:
- -i (--ip)  --  Set the IP address for the Nanoleaf device
- -p (--port)  --  Set the port for the Nanoleaf device
- -k (--api-key)  --  Set the api key for the Nanoleaf device (returned after manual setup)
- -s (--key-start)  --  Set the leftmost key on your MIDI keyboard in the format NOTEOCTAVE (this is usually set to "A0")
- -e (--key-end)  -- Set the rightmost key on your MIDI keyboard in the format NOTEOCTAVE (this is usually set to "C8")

## Effects
The following is a list of the effects/motions that Pianoleaf has:
- Map: Horizontally maps each key from the keyboard to the Nanoleaf device. This effect works best on very wide Nanoleaf design layouts.
