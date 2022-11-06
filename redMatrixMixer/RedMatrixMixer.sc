//redFrik

//--related:
//RedMatrixMixerGUI, RedEffectsRack, RedMixer

RedMatrixMixer {
	var <group,
	<isReady= false, groupPassedIn,
	<nIn, <nOut, <synth, <cvs, <defString, controllers;

	*new {|nIn= 8, nOut= 8, in= 0, out= 0, group, lag= 0.05|
		^super.new.initRedMatrixMixer(nIn, nOut, in, out, group, lag);
	}

	initRedMatrixMixer {|argNIn, argNOut, argIn, argOut, argGroup, argLag|
		var server;
		nIn= argNIn;
		nOut= argNOut;

		if(argGroup.notNil, {
			server= argGroup.server;
			groupPassedIn= true;
		}, {
			server= Server.default;
			groupPassedIn= false;
		});

		cvs= (
			in: Ref(argIn),
			out: Ref(argOut),
			lag: Ref(argLag)
		);
		controllers= List.newFrom([
			SimpleController(cvs.in).put(\value, {|ref| synth.set(\in, ref.value)}),
			SimpleController(cvs.out).put(\value, {|ref| synth.set(\out, ref.value)}),
			SimpleController(cvs.lag).put(\value, {|ref| synth.set(\lag, ref.value)})
		]);
		forkIfNeeded{
			if(groupPassedIn.not, {
				server.bootSync;
				group= Group.after(server.defaultGroup);
				server.sync;
			}, {
				group= argGroup;
			});

			//--send definitions
			this.def.add;
			server.sync;

			//--create synth
			synth= Synth(\redMatrixMixer, cvs.asKeyValuePairs, group, \addToTail);
			nOut.do{|i|
				var arr;
				var name= ("o"++i).asSymbol;
				this.addUniqueMethod(name.asSetter, {|mixer, arr|
					cvs[name].value_(arr).changed(\value);
					this;
				});
				this.addUniqueMethod(name, {|mixer|
					cvs[name].value;
				});
				arr= 0.dup(nIn);
				if(i<nIn, {arr= arr.put(i, 1)});
				cvs[name]= Ref(arr);
				controllers.add(
					SimpleController(cvs[name]).put(\value, {|ref|
						synth.set(name, ref.value);
						ref.changed;
					})
				);
			};
			server.sync;
			isReady= true;
		};
	}

	def {
		defString= "SynthDef('redMatrixMixer', {|in= 0, out= 0, lag= 0.05";
		nOut.do{|i|
			var arr= 0.dup(nIn);
			if(i<nIn, {arr= arr.put(i, 1)});
			defString= defString++", o"++i++"= #"++arr;
		};
		defString= defString++"|\n\tvar z= In.ar(in, "++nIn++");";
		nOut.do{|i| defString= defString++"\n\tvar m"++i++"= Mix(z*Ramp.kr(o"++i++", lag));"};
		nOut.do{|i| defString= defString++"\n\tReplaceOut.ar(out+"++i++", m"++i++");"};
		defString= defString++"\n});";
		^defString.interpret;
	}

	in {^cvs.in.value}
	in_ {|val| cvs.in.value_(val.max(0)).changed(\value)}
	out {^cvs.out.value}
	out_ {|val| cvs.out.value_(val.max(0)).changed(\value)}
	lag {^cvs.lag.value}
	lag_ {|val| cvs.lag.value_(val.max(0)).changed(\value)}

	free {
		if(groupPassedIn.not, {
			group.free;
		}, {
			synth.free;
		});
		controllers.do{|x| x.remove};
	}

	gui {|position|
		^RedMatrixMixerGUI(this, position);
	}
}
