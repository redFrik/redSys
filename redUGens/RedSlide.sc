RedSlide {
	var <>up, <>down, prev= 0;
	*new {|up= 10, down|
		^super.newCopyArgs(up, down?up);
	}
	slide {|in|
		var res;
		if(in>prev, {
			res= prev+((in-prev)/up);
		}, {
			res= prev+((in-prev)/down);
		});
		prev= res;
		^res;
	}

	//--ugen
	*ar {|in, up= 10, down|
		var buf, prev, res;
		down= down?up;
		buf= LocalBuf(1).clear;
		prev= Dbufrd(buf);
		res= prev+((in-prev)/if(in>prev, up, down));
		^Duty.ar(SampleDur.ir, 0, Dbufwr(res, buf));
	}
	*kr {|in, up= 10, down|
		var buf, prev, res;
		down= down?up;
		buf= LocalBuf(1).clear;
		prev= Dbufrd(buf);
		res= prev+((in-prev)/if(in>prev, up, down));
		^Duty.kr(ControlDur.ir, 0, Dbufwr(res, buf));
	}
}
