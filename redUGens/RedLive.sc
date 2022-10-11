//redFrik 041207

//trigger increases counter.  if counter= 0 then record, else playback

RedLive {

	*ar {|buffer, in, trigger, repeats= 3, interpol= 1|	//1=no, 2=linear, 4=cubic
		var stepper, phasor;
		stepper= Stepper.ar(trigger, 0, 0, repeats, 1, repeats);
		phasor= Sweep.ar(trigger, SampleRate.ir);	//BufSampleRate.kr(buffer)
		BufWr.ar(in, buffer, Gate.ar(phasor, stepper<1), 1);
		^BufRd.ar(buffer.numChannels, buffer, phasor, 1, interpol);
	}

	*kr {|buffer, in, trigger, repeats= 3, interpol= 1|	//1=no, 2=linear, 4=cubic
		var stepper, phasor;
		stepper= Stepper.kr(trigger, 0, 0, repeats, 1, repeats);
		//phasor= Sweep.kr(trigger, BufSampleRate.kr(buffer));
		phasor= Sweep.kr(trigger, ControlRate.ir);
		BufWr.kr(in, buffer, Gate.kr(phasor, stepper<1), 1);
		^BufRd.kr(buffer.numChannels, buffer, phasor, 1, interpol);
	}
}
