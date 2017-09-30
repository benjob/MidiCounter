package org.benjob.midicounter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

public class Main {
	//default bank
	public static final int DEFAULT_BANK = 0;

	//default instrument
	public static final int DEFAULT_INSTRUMENT = 0;

	//we count up to 1023, that's the max on 10 bits
	public static final int MAX_COUNT = 1023;

	//Notes length in ticks
	public static final long NOTE_LENGTH = 2;

	//Default velocity. Velocity is how "fast" the note is hit
	//Very low velocity will not be heard at all
	public static final int DEFAULT_VELOCITY = 50;

	public static final float TEMPO_FACTOR = 1.1f;

	//Notes: http://www.electronics.dit.ie/staff/tscarff/Music_technology/midi/midi_note_numbers_for_octaves.htm
	//10 bits = 10 notes
	//D E F G A A G F E D. Second half is an octave down, starts at D5.
	public static final int[] ALL_NOTES = {62, 64, 65, 67, 69, 81, 79, 77, 76, 74};

	public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, IOException {
		List<String> arguments = Collections.<String>emptyList();
		if (args != null && args.length != 0) {
			arguments = Arrays.asList(args);
		}

		if (arguments.contains("--help")) {
			showInstruments();
		} else {

			//default bank;
			int bank = DEFAULT_BANK;

			//set instrument
			if (arguments.contains("-b")) {
				int index = arguments.indexOf("-b");
				bank = new Integer(arguments.get(index + 1));;
			}

			//default instrument;
			int instrument = DEFAULT_INSTRUMENT;

			//set instrument
			if (arguments.contains("-i")) {
				int index = arguments.indexOf("-i");
				instrument = new Integer(arguments.get(index + 1));
			}

			play(bank, instrument);
		}
	}

	public static void play(int bank, int instrument) throws MidiUnavailableException, InvalidMidiDataException, IOException {
		// 5 pulses per quarter note.
		Sequence sequence = new Sequence(Sequence.PPQ, 5);

		Track track = sequence.createTrack();

		//set bank number
		ShortMessage controlChange = new ShortMessage(ShortMessage.CONTROL_CHANGE, 1, 0, bank >> 7);
		MidiEvent controlChangeEvent = new MidiEvent(controlChange, 0);
		track.add(controlChangeEvent);

		ShortMessage controlChange2 = new ShortMessage(ShortMessage.CONTROL_CHANGE, 1, 32, bank & 0x7f);
		MidiEvent controlChangeEvent2 = new MidiEvent(controlChange2, 0);
		track.add(controlChangeEvent2);

		//set instrument
		ShortMessage programChange = new ShortMessage(ShortMessage.PROGRAM_CHANGE, 1, instrument, 0);
		MidiEvent programChangeEvent = new MidiEvent(programChange, 0);
		track.add(programChangeEvent);

		int note_count = 0;
		for (int i = 1; i < MAX_COUNT; i++) {
			int last = i - 1;
			//Check all 10 bits
			for (int j = 0; j < 10; j++) {
				//if the current bit is set and was not set on last number, press key
				if (((i >> j) & 1) == 1 && ((last >> j) & 1) == 0) {
					//the state changed and the bit is on
					ShortMessage on = new ShortMessage();
					on.setMessage(ShortMessage.NOTE_ON, 1, ALL_NOTES[ALL_NOTES.length - 1 - j], DEFAULT_VELOCITY);
					MidiEvent noteOn = new MidiEvent(on, 1 + (note_count * NOTE_LENGTH));
					track.add(noteOn);

					ShortMessage off = new ShortMessage();
					off.setMessage(ShortMessage.NOTE_OFF, 1, ALL_NOTES[ALL_NOTES.length - 1 - j], DEFAULT_VELOCITY);
					MidiEvent noteOff = new MidiEvent(off, 1 + ((note_count + 1) * NOTE_LENGTH));
					track.add(noteOff);

					note_count++;
				}
			}
		}

		Sequencer sequencer = MidiSystem.getSequencer();
		sequencer.open();

		sequencer.setTempoFactor(TEMPO_FACTOR);

		// Let us know when it is done playing
		sequencer.addMetaEventListener(new MetaEventListener() {
			public void meta(MetaMessage m) {
				// A message of this type is automatically sent
				// when we reach the end of the track
				if (m.getType() == 47) {
					System.exit(0);
				}
			}
		});

		sequencer.setSequence(sequence);
		sequencer.start();
	}

	public static void showInstruments() throws MidiUnavailableException {
		Synthesizer synthesizer = MidiSystem.getSynthesizer();
		synthesizer.open();
		Instrument[] orchestra = synthesizer.getAvailableInstruments();

		final StringBuilder sb = new StringBuilder();
		String eol = System.getProperty("line.separator");
		sb.append("The orchestra has ");
		sb.append(orchestra.length);
		sb.append(" instruments.");
		sb.append(eol);
		for (Instrument instrument : orchestra) {
			sb.append(instrument.toString());
			sb.append(eol);
		}
		synthesizer.close();

		System.out.println(sb);

		System.out.println("List all instruments: ");
		System.out.println("java -classpath . org.benjob.midicounter.Main --help");
		System.out.println("Specific Instrument: ");
		System.out.println("java -classpath . org.benjob.midicounter.Main -b <Bank> -i <InstrumentNumber>");
		System.out.println("Default: ");
		System.out.println("java -classpath . org.benjob.midicounter.Main");
	}
}
