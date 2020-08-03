package io.github.rowak.pianoleaf;

import java.io.IOException;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.Panel;
import io.github.rowak.nanoleafapi.StatusCodeException;

public class OutputReceiver implements Receiver {
	
	private final int NOTE_ON = -112;
	private final int NOTE_OFF = -128;
	private final int MAX_VELOCITY = 127;
	
	private int keyStart;
	private int keyEnd;
	
	private boolean[] activeColumns;
	
	private Aurora aurora;
	private Panel[][] panelColumns;
	
	public OutputReceiver(Aurora aurora, int keyStart, int keyEnd)
			throws StatusCodeException, IOException {
		this.aurora = aurora;
		this.keyStart = keyStart;
		this.keyEnd = keyEnd;
		aurora.externalStreaming().enable();
		Panel[] panels = aurora.panelLayout().getPanels();
		panelColumns = PanelTableSort.getColumns(panels);
		activeColumns = new boolean[panelColumns.length];
	}

	@Override
	public void send(MidiMessage msg, long timeStamp) {
		byte[] data = msg.getMessage();
		for (int i = 0; i < msg.getMessage().length; i++) {
			System.out.print(data[i] + " ");
		}
		System.out.println();
		
		final int noteState = data[0];
		final int notePitch = data[1];
		final int noteVelocity = data[2];

		if (noteState == NOTE_ON) {
			final int columnIndex = getNotePanelColumnIndex(notePitch);
			if (!activeColumns[columnIndex]) {
				activeColumns[columnIndex] = true;
				new Thread(new Runnable() {
					@Override
					public void run() {
						Panel[] column = panelColumns[columnIndex];
						for (int i = 0; i < column.length; i++) {
							float hue = 360 * (columnIndex / (float)panelColumns.length);
							float brightness = (noteVelocity + 30 > MAX_VELOCITY ? MAX_VELOCITY : noteVelocity + 30) / (float)MAX_VELOCITY;
							int[] rgb = hsbToRgb(hue, 1, brightness);
							setPanel(column[i], rgb, 4);
						}
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						for (int i = 0; i < column.length; i++) {
							setPanel(column[i], new int[]{0, 0, 0}, 10);
						}
						activeColumns[columnIndex] = false;
					}
				}).start();
			}
		}
	}
	
	@Override
	public void close() {}

	private int getNotePanelColumnIndex(int note) {
//		return (int)((note / (float)(keyEnd - keyStart)) * panelColumns.length);
		return (int)((note / (float)128) * panelColumns.length);
	}

	private int[] hsbToRgb(float h, float s, float b) {
		return new int[] {(int)(f(5, h, s, b)*255), (int)(f(3, h, s, b)*255), (int)(f(1, h, s, b)*255)};
	}

	private float f(int n, float h, float s, float b) {
		float k = (n + h / 60) % 6;
		return b - b*s*Math.max(0, min((int)k, (int)(4-k), 1));
	}

	private int min(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	private void setPanel(Panel p, int[] c, int transTime) {
		try {
			aurora.externalStreaming().setPanel(p, c[0],
				c[1], c[2], transTime);
		} catch (StatusCodeException sce) {
			sce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
