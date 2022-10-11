//redFrik 041207

//gate= 1 record, gate= 0 looped playback

RedTapeRecorder {

	*ar {|buffer, in, gate, interpol= 1, muteWhileRecording= 0, lag= 0|	//1=no, 2=linear, 4=cubic
		var playTrigger= 1-gate;
		var mute= Latch.ar(DC.ar(1), gate)*(1-muteWhileRecording|playTrigger).lag(lag);
		var recPhasor= Gate.ar(
			EnvGen.ar(
				Env(#[0, 1, 0], #[1, 0], 'lin', 1),
				gate,
				BufFrames.kr(buffer),
				0,
				BufDur.kr(buffer)
			),
			gate
		);
		var playPhasor= Phasor.ar(playTrigger, 1, 0, Latch.ar(recPhasor, playTrigger));
		BufWr.ar(in, buffer, recPhasor, 0);
		^BufRd.ar(buffer.numChannels, buffer, playPhasor, 1, interpol)*mute;
	}

	*kr {|buffer, in, gate, interpol= 1, muteWhileRecording= 0, lag= 0|	//1=no, 2=linear, 4=cubic
		var playTrigger= 1-gate;
		var mute= Latch.kr(DC.kr(1), gate)*(1-muteWhileRecording|playTrigger).lag(lag);
		var recPhasor= Gate.kr(
			EnvGen.kr(
				Env(#[0, 1, 0], #[1, 0], 'lin', 1),
				gate,
				BufFrames.kr(buffer),
				0,
				BufDur.kr(buffer)*buffer.server.options.blockSize
			),
			gate
		);
		var playPhasor= Phasor.kr(playTrigger, 1, 0, Latch.kr(recPhasor, playTrigger));
		BufWr.kr(in, buffer, recPhasor, 0);
		^BufRd.kr(buffer.numChannels, buffer, playPhasor, 1, interpol)*mute;
	}
}
