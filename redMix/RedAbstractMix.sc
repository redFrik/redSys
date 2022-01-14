//redFrik 090116

//--related:
//RedMixGUI RedAbstractMod RedMatrixMixer RedTapTempo

RedAbstractMix {	//abstract class
	var <group, <cvs,
	<isReady= false, synth, controllers;

	*new {|inA= 0, inB= 2, out= 0, group, lag= 0.05|
		^super.new.initRedAbstractMix(inA, inB, out, group, lag);
	}

	initRedAbstractMix {|argInA, argInB, argOut, argGroup, argLag|
		var server;
		group= argGroup ?? {Server.default.defaultGroup};
		server= group.server;

		cvs= ();
		cvs.inA= Ref(argInA);
		cvs.inB= Ref(argInB);
		cvs.out= Ref(argOut);
		cvs.mix= Ref(0);
		cvs.amp= Ref(1);
		cvs.lag= Ref(argLag);
		controllers= List.newFrom([
			SimpleController(cvs.inA).put(\value, {|ref| synth.set(\inA, ref.value)}),
			SimpleController(cvs.inB).put(\value, {|ref| synth.set(\inB, ref.value)}),
			SimpleController(cvs.out).put(\value, {|ref| synth.set(\out, ref.value)}),
			SimpleController(cvs.mix).put(\value, {|ref| synth.set(\mix, ref.value)}),
			SimpleController(cvs.amp).put(\value, {|ref| synth.set(\amp, ref.value)}),
			SimpleController(cvs.lag).put(\value, {|ref| synth.set(\lag, ref.value)})
		]);

		forkIfNeeded{
			server.bootSync;

			//--send definition
			this.def.add;
			server.sync;

			//--create synth
			synth= Synth(this.def.name, cvs.asKeyValuePairs, group, \addToTail);
			server.sync;
			isReady= true;
		};
	}

	def {^this.class.def}

	inA {^cvs.inA.value}
	inA_ {|val| cvs.inA.value_(val).changed(\value)}
	inB {^cvs.inB.value}
	inB_ {|val| cvs.inB.value_(val).changed(\value)}
	out {^cvs.out.value}
	out_ {|val| cvs.out.value_(val).changed(\value)}
	mix {^cvs.mix.value}
	mix_ {|val| cvs.mix.value_(val).changed(\value)}
	amp {^cvs.amp.value}
	amp_ {|val| cvs.amp.value_(val).changed(\value)}
	lag {^cvs.lag.value}
	lag_ {|val| cvs.lag.value_(val).changed(\value)}

	free {
		synth.free;
		controllers.do{|x| x.remove};
	}

	gui {|parent, position|
		^RedMixGUI(this, parent, position);
	}

	//--for subclasses
	*def {
		^this.subclassResponsibility(thisMethod);
	}
}
