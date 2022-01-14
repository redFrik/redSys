//redFrik

//--todo:
//multichannel, generalise - now RedMixerChannel only stereo, how to do mono/quad channels?

//--related:
//RedMixerChannelGUI RedMixer RedGUICVMixerChannel RedAbstractMix RedAbstractModule

RedMixerChannel {
	var <group, <cvs, /*<isReady= false,*/
	<inserts,
	synth, synthEq,
	osc, controllers;

	*new {|out= 0, group, lag= 0.05|
		^super.new.initRedMixerChannel(out, lag).init(group);
	}

	initRedMixerChannel {|out, lag|
		cvs= ();
		cvs.levels= Ref(0.dup);
		cvs.peaks= Ref(0.dup);
		cvs.out= Ref(out);
		cvs.mute= Ref(0);
		cvs.lag= Ref(lag);
		cvs.amp= Ref(1);
		cvs.bal= Ref(0);
		cvs.eqHi= Ref(0);
		cvs.eqMi= Ref(0);
		cvs.eqLo= Ref(0);
		cvs.hiFreq= Ref(7000);
		cvs.hiBand= Ref(1);
		cvs.hiGain= Ref(0);
		cvs.miFreq= Ref(700);
		cvs.miBand= Ref(1);
		cvs.miGain= Ref(0);
		cvs.loFreq= Ref(70);
		cvs.loBand= Ref(1);
		cvs.loGain= Ref(0);
	}

	init {|argGroup|
		var server;
		synthEq= nil;
		group= argGroup ?? {Server.default.defaultGroup};
		server= group.server;

		forkIfNeeded{
			server.bootSync;

			if(inserts.isNil, {	//when new
				inserts= List.new;
			}, {	//when restoring from archive
				inserts.do{|x| x.initModule(group)};
			});

			//--send definition
			this.def.add;
			this.defEq.add;
			server.sync;

			//--create synth
			synth= Synth(this.def.name, cvs.asKeyValuePairs, group, \addToTail);
			server.sync;

			controllers= List.newFrom([
				SimpleController(cvs.out).put(\value, {|ref|
					synth.set(\out, ref.value);
					synthEq.set(\out, ref.value);
					inserts.do{|x| x.out= ref.value};
				}),
				SimpleController(cvs.lag).put(\value, {|ref|
					synth.set(\lag, ref.value);
					synthEq.set(\lag, ref.value);
				}),
				SimpleController(cvs.mute).put(\value, {|ref| synth.set(\gate, 1-ref.value)}),
				SimpleController(cvs.amp).put(\value, {|ref| synth.set(\amp, ref.value)}),
				SimpleController(cvs.bal).put(\value, {|ref| synth.set(\bal, ref.value)}),
				SimpleController(cvs.eqHi).put(\value, {|ref| this.prEqAction}),
				SimpleController(cvs.eqMi).put(\value, {|ref| this.prEqAction}),
				SimpleController(cvs.eqLo).put(\value, {|ref| this.prEqAction}),
				SimpleController(cvs.hiFreq).put(\value, {|ref| synthEq.set(\hiFreq, ref.value)}),
				SimpleController(cvs.hiBand).put(\value, {|ref| synthEq.set(\hiBand, ref.value)}),
				SimpleController(cvs.hiGain).put(\value, {|ref| synthEq.set(\hiGain, ref.value)}),
				SimpleController(cvs.miFreq).put(\value, {|ref| synthEq.set(\miFreq, ref.value)}),
				SimpleController(cvs.miBand).put(\value, {|ref| synthEq.set(\miBand, ref.value)}),
				SimpleController(cvs.miGain).put(\value, {|ref| synthEq.set(\miGain, ref.value)}),
				SimpleController(cvs.loFreq).put(\value, {|ref| synthEq.set(\loFreq, ref.value)}),
				SimpleController(cvs.loBand).put(\value, {|ref| synthEq.set(\loBand, ref.value)}),
				SimpleController(cvs.loGain).put(\value, {|ref| synthEq.set(\loGain, ref.value)})
			]);
			this.prEqAction;	//needed when restoring from archive

			osc= OSCFunc({|m|
				var arr= m[3..];
				cvs.levels.value= arr;
				cvs.levels.changed(\value);
				if(arr.any{|x, i| x>cvs.peaks.value[i]}, {
					cvs.peaks.value= cvs.peaks.value.collect{|x, i| x.max(arr[i])};
					cvs.peaks.changed(\value);
				});
			}, \levels, server.addr, nil, [synth.nodeID]);

			//isReady= true;
		};
	}

	interp {|other, val= 0|
		var exclude= #[\out, \mute, \peaks, \levels];
		cvs.keysValuesDo{|k, v, i|
			if(exclude.includes(k).not, {
				v.value_(v.value.blend(other.cvs[k].value, val));
				v.changed(\value);
			});
		};
	}

	insertClass {|redEfxClass, addAction= \addToHead|
		forkIfNeeded{
			redEfxClass.asArray.do{|x|
				x= x.new(this.out, group, addAction);
				group.server.sync;
				if(inserts.isEmpty, {
					if(synthEq.isNil, {
						x.synth.moveBefore(synth);
					}, {
						x.synth.moveBefore(synthEq);
					});
					inserts.add(x);
				}, {
					if(addAction==\addToHead, {
						x.synth.moveBefore(inserts[0].synth);
						inserts.addFirst(x);
					}, {
						x.synth.moveAfter(inserts.last.synth);
						inserts.add(x);
					});
				});
			};
		};
	}

	insert {|redEfx, addAction= \addToHead|
		forkIfNeeded{
			group.server.sync;
			redEfx.asArray.do{|x|
				if(x.synth.isNil, {
					(this.class.name++": could not insert. redEfx synth not created yet").warn;
				}, {
					x.out= this.out;
					if(inserts.isEmpty, {
						if(synthEq.isNil, {
							x.synth.moveBefore(synth);
						}, {
							x.synth.moveBefore(synthEq);
						});
						inserts.add(x);
					}, {
						if(addAction==\addToHead, {
							x.synth.moveBefore(inserts[0].synth);
							inserts.addFirst(x);
						}, {
							x.synth.moveAfter(inserts.last.synth);
							inserts.add(x);
						});
					});
				});
			};
		};
	}

	removeClass {|redEfxClass|
		redEfxClass.asArray.do{|x|
			inserts= inserts.reject{|y| if(y.class==x, {y.free; true}, {false})};
		};
	}

	remove {|redEfx|
		redEfx.asArray.do{|x|
			inserts= inserts.reject{|y| if(y==x, {y.free; true}, {false})};
		};
	}

	removeAll {
		inserts.do{|x| x.free};
		inserts= List.new;
	}

	levels {^cvs.levels.value}
	peaks {^cvs.peaks.value}

	lag {^cvs.lag.value}
	lag_ {|val| cvs.lag.value_(val.max(0)).changed(\value)}
	mute {^cvs.mute.value}
	mute_ {|val| cvs.mute.value_(val).changed(\value)}
	out {^cvs.out.value}
	out_ {|val| cvs.out.value_(val.max(0)).changed(\value)}
	vol {^cvs.amp.value.ampdb}
	vol_ {|val| cvs.amp.value_(val.dbamp).changed(\value)}
	bal {^cvs.bal.value}
	bal_ {|val| cvs.bal.value_(val).changed(\value)}
	eqHi {^cvs.eqHi.value}
	eqHi_ {|val| cvs.eqHi.value_(val).changed(\value)}
	eqMi {^cvs.eqMi.value}
	eqMi_ {|val| cvs.eqMi.value_(val).changed(\value)}
	eqLo {^cvs.eqLo.value}
	eqLo_ {|val| cvs.eqLo.value_(val).changed(\value)}
	hiFreq {^cvs.hiFreq.value}
	hiFreq_ {|val| cvs.hiFreq.value_(val).changed(\value)}
	hiBand {^cvs.hiBand.value}
	hiBand_ {|val| cvs.hiBand.value_(val).changed(\value)}
	hiGain {^cvs.hiGain.value}
	hiGain_ {|val| cvs.hiGain.value_(val).changed(\value)}
	miFreq {^cvs.miFreq.value}
	miFreq_ {|val| cvs.miFreq.value_(val).changed(\value)}
	miBand {^cvs.miBand.value}
	miBand_ {|val| cvs.miBand.value_(val).changed(\value)}
	miGain {^cvs.miGain.value}
	miGain_ {|val| cvs.miGain.value_(val).changed(\value)}
	loFreq {^cvs.loFreq.value}
	loFreq_ {|val| cvs.loFreq.value_(val).changed(\value)}
	loBand {^cvs.loBand.value}
	loBand_ {|val| cvs.loBand.value_(val).changed(\value)}
	loGain {^cvs.loGain.value}
	loGain_ {|val| cvs.loGain.value_(val).changed(\value)}

	gui {|parent, position|
		^RedMixerChannelGUI(this, parent, position);
	}

	free {
		this.removeAll;
		controllers.do{|x| x.remove};
		synthEq.free;
		synth.free;
		osc.free;
	}

	resetPeaks {
		cvs.peaks.value= cvs.peaks.value*0;
		cvs.peaks.changed(\value);
	}

	def {|channels= 2|
		^switch(channels,
			2, {
				SynthDef(\redMixerChannel, {|out= 0, lag= 0.05,
					levelsRate= 15, bal= 0, amp= 1, gate= 1|
					var z= In.ar(out, 2);
					var o= Balance2.ar(z[0], z[1], bal.lag(lag), amp.lag(lag));
					var p= PeakFollower.kr(o, 0.9975).round(1e-6);
					var t= Changed.kr(p);
					SendReply.kr(Impulse.kr(levelsRate)*t, '/levels', p);
					ReplaceOut.ar(out, o*gate.lag(0.05));
				}, metadata: (
					specs: (
						\out: \audiobus.asSpec,
						\lag: ControlSpec(0, 99, 'lin', 0, 0.05),
						\levelsRate: ControlSpec(0, 60, \lin, 0, 15),
						\bal: \bipolar.asSpec,
						\amp: ControlSpec(0, 1, \lin, 0, 1)
					)
				));
			},
			{(this.class.name++": different num channels todo"+channels).warn}
		);
	}

	defEq {|channels= 2|
		^SynthDef(\redMixerChannelEq, {|out= 0, lag= 0.05,
			eqHi= 0, eqMi= 0, eqLo= 0,
			hiFreq= 7000, miFreq= 700, loFreq= 70,
			hiBand= 1, miBand= 1, loBand= 1,
			hiGain= 0, miGain= 0, loGain= 0|
			var z= In.ar(out, channels);
			z= z*(1-eqHi)+BHiShelf.ar(z*eqHi, hiFreq.lag(lag), hiBand.lag(lag), hiGain.lag(lag));
			z= z*(1-eqMi)+BPeakEQ.ar(z*eqMi, miFreq.lag(lag), miBand.lag(lag), miGain.lag(lag));
			z= z*(1-eqLo)+BLowShelf.ar(z*eqLo, loFreq.lag(lag), loBand.lag(lag), loGain.lag(lag));
			ReplaceOut.ar(out, z);
		}, #[0, 0, 0.1, 0.1, 0.1], metadata: (
			specs: (
				\out: \audiobus.asSpec,
				\lag: ControlSpec(0, 99, 'lin', 0, 0.05),
				\eqHi: ControlSpec(0, 1, 'lin', 1, 0),
				\eqMi: ControlSpec(0, 1, 'lin', 1, 0),
				\eqLo: ControlSpec(0, 1, 'lin', 1, 0),
				\hiFreq: ControlSpec(20, 20000, \exp, 0, 7000),
				\miFreq: ControlSpec(20, 20000, \exp, 0, 700),
				\loFreq: ControlSpec(20, 20000, \exp, 0, 70),
				\hiBand: ControlSpec(0.01, 10, \exp, 0, 1),
				\miBand: ControlSpec(0.01, 10, \exp, 0, 1),
				\loBand: ControlSpec(0.01, 10, \exp, 0, 1),
				\hiGain: ControlSpec(-40, 20, \lin, 0, 0),
				\miGain: ControlSpec(-40, 20, \lin, 0, 0),
				\loGain: ControlSpec(-40, 20, \lin, 0, 0)
			)
		));
	}

	archive {
		^(
			cvs: cvs,
			inserts: inserts
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
		cvs= archive.cvs;
		inserts= archive.inserts;
	}

	//--private
	prEqAction {
		if([this.eqHi, this.eqMi, this.eqLo].any{|x| x==1}, {
			if(synthEq.isNil, {
				synthEq= Synth(\redMixerChannelEq, [
					\out, this.out,
					\lag, this.lag,
					\eqHi, this.eqHi,
					\eqMi, this.eqMi,
					\eqLo, this.eqLo,
					\hiFreq, this.hiFreq,
					\miFreq, this.miFreq,
					\loFreq, this.loFreq,
					\hiBand, this.hiBand,
					\miBand, this.miBand,
					\loBand, this.loBand,
					\hiGain, this.hiGain,
					\miGain, this.miGain,
					\loGain, this.loGain
				], synth, \addBefore);
			}, {
				synthEq.set(\eqHi, this.eqHi, \eqMi, this.eqMi, \eqLo, this.eqLo);
			});
		}, {
			if(synthEq.notNil, {
				synthEq.free;
				synthEq= nil;
			});
		});
	}
}
