//redFrik

//--related:
//RedMixerGUI RedMixerChannel RedMatrixMixer RedEffectsRack

//--todo:
//solution to redefx/redmixer order
//mono?, swap RedMixerChannels on the fly possible? or stereo/mono switch for all channels? quad?
//multichannel, generalise - now RedMixerChannel only stereo, remake as mono-quad channels

RedMixer {
	var <group, <cvs, <isReady, groupPassedIn,
	<channels, <mixers, controllers,
	internalSynths, internalInputChannels, internalOutputChannels;

	*new {|inputChannels= #[2, 4, 6, 8], outputChannels= #[0], group, lag= 0.05|
		^super.new
		.initRedMixer(inputChannels.asArray, outputChannels.asArray, lag)
		.init(group);
	}

	initRedMixer {|argInputChannels, argOutputChannels, lag|
		cvs= ();
		cvs.lag= Ref(lag);

		//--temp storage to avoid channel arguments for init
		internalInputChannels= argInputChannels;
		internalOutputChannels= argOutputChannels;
	}

	init {|argGroup|
		var server;
		isReady= false;
		if(argGroup.isNil, {
			server= Server.default;
			groupPassedIn= false;
		}, {
			server= argGroup.server;
			groupPassedIn= true;
		});

		Routine.run{
			if(groupPassedIn.not, {
				server.bootSync;
				group= Group.after(server.defaultGroup);
				server.sync;
			}, {
				group= argGroup;
			});

			//--create channels
			if(channels.isNil, {	//when new
				channels= internalInputChannels.collect{|x|
					RedMixerChannel(x, group, this.lag);
				};
			}, {	//when restoring from archive
				channels.do{|x| x.init(group)};
			});
			controllers= List.new;
			channels.do{|x, i|
				controllers.add(
					SimpleController(x.cvs.out).put(\value, {|ref|
						internalSynths.do{|x| x.set(\inputs, this.inputChannels)};
					})
				);
			};

			//--internal synth for routing from channels to mixers
			this.def(internalInputChannels).add;
			server.sync;
			internalSynths= internalOutputChannels.collect{|x|
				Synth(\redMixerInternalRouting, [\out, x], group, \addToTail);
			};

			//--create mixers
			if(mixers.isNil, {	//when new
				mixers= internalOutputChannels.collect{|x|
					RedMixerChannel(x, group, this.lag);
				};
			}, {	//when restoring from archive
				mixers.do{|x| x.init(group)};
			});

			controllers.add(
				SimpleController(cvs.lag).put(\value, {|ref|
					var val= ref.value.max(0);
					mixers.do{|x| x.lag= val};
					channels.do{|x| x.lag= val};
				})
			);

			//while({channels.any{|x| x.isReady.not}}, {0.02.wait});
			isReady= true;
		};
	}

	mixer {
		^mixers[0];
	}

	inputChannels {
		^channels.collect{|x| x.cvs.out.value};
	}
	inputChannels_ {|arr|
		arr= arr.asArray;
		if(channels.size!=arr.size, {
			(this.class.name++": array must match number of channels").error;
		}, {
			channels.do{|x, i|
				x.cvs.out.value_(arr[i]).changed(\value);
			};
			internalSynths.do{|x|
				x.set(\inputs, arr);
			};
		});
	}

	outputChannels {
		^mixers.collect{|x| x.cvs.out.value};
	}
	outputChannels_ {|arr|
		arr= arr.asArray;
		if(internalSynths.size!=arr.size, {
			(this.class.name++": array must match outputChannels argument").error;
		}, {
			arr.do{|x, i|
				mixers[i].cvs.out.value_(x).changed(\value);
				internalSynths[i].set(\out, x);
			};
		});
	}

	free {
		if(groupPassedIn.not, {
			group.free;
		}, {
			mixers.do{|x| x.free};
			channels.do{|x| x.free};
			internalSynths.do{|x| x.free};
		});
		controllers.do{|x| x.remove};
	}

	gui {|position|
		^RedMixerGUI(this, position);
	}

	lag {^cvs.lag.value}
	lag_ {|val| cvs.lag.value_(val.max(0)).changed(\value)}

	mute_ {|val|
		channels.do{|x| x.mute= val.binaryValue};
	}
	solo_ {|channel|
		channels.do{|x, i| x.mute= channel.asArray.includes(i).not.binaryValue};
	}

	def {|inputChannels= #[2, 4, 6, 8]|
		^SynthDef(\redMixerInternalRouting, {|out= 0|
			var c= Control.names(\inputs).kr(inputChannels);
			var z= inputChannels.collect{|x, i| In.ar(c.asArray[i], 2)};
			Out.ar(out, Mix(z));
		});
	}

	archive {
		^(
			internalInputChannels: this.inputChannels,
			internalOutputChannels: this.outputChannels,
			mixers: mixers.collect{|x| x.archive},
			channels: channels.collect{|x| x.archive},
			cvs: cvs
		);
	}
	store {|path|
		this.archive.writeArchive(path);
	}
	*restoreFile {|path|	//should call .init afterwards to initialize server side
		^super.new.restore(Object.readArchive(path));
	}
	*restoreArchive {|archive|	//should call .init afterwards to initialize server side
		^super.new.restore(archive);
	}
	restore {|archive|
		internalInputChannels= archive[\internalInputChannels];
		internalOutputChannels= archive[\internalOutputChannels];
		cvs= archive[\cvs];
		channels= archive[\channels].collect{|x| RedMixerChannel.restoreArchive(x)};
		mixers= archive[\mixers].collect{|x| RedMixerChannel.restoreArchive(x)};
	}
}
