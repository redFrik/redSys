//redFrik

RedKCS {
	var <sampleRate, <baudRate, <zeroFreq, <oneFreq,
	<addNewlineGap, <addCarriageReturn,
	<leaderDur, <trailerDur;
	var <synth, <buffer, <listenSynth, listenResponder;
	var arrayClass, mul, add, frameLen;
	var oneBits, zeroBits, gapBits, leaderBits, trailerBits;

	//squarewaves
	*new {|sampleRate, baudRate= 300, zeroFreq= 1200, oneFreq= 2400,
		addNewlineGap= true, addCarriageReturn= true,
		leaderDur= 5, trailerDur= 5|
		^super.newCopyArgs(
			sampleRate, baudRate, zeroFreq, oneFreq,
			addNewlineGap, addCarriageReturn,
			leaderDur, trailerDur
		).initSquare.initArrays;
	}

	//sinewaves
	*newSine {|sampleRate, baudRate= 300, zeroFreq= 1200, oneFreq= 2400,
		addNewlineGap= true, addCarriageReturn= true,
		leaderDur= 5, trailerDur= 5|
		^super.newCopyArgs(
			sampleRate, baudRate, zeroFreq, oneFreq,
			addNewlineGap, addCarriageReturn,
			leaderDur, trailerDur
		).initSine.initArrays;
	}

	initSquare {
		arrayClass= Int8Array;
		sampleRate= sampleRate?9600;
		frameLen= sampleRate.div(baudRate);
		mul= 1;
		add= -0.5;
		zeroBits= arrayClass.fill(frameLen, {|i| i.div((sampleRate/zeroFreq).div(2))%2});
		oneBits= arrayClass.fill(frameLen, {|i| i.div((sampleRate/oneFreq).div(2))%2});
	}
	initSine {
		arrayClass= FloatArray;
		sampleRate= sampleRate?Server.default.sampleRate?44100;
		frameLen= sampleRate.div(baudRate);
		mul= 0.5;
		add= 0;
		zeroBits= arrayClass.fill(frameLen, {|i| sin(i/frameLen*(zeroFreq/baudRate)*2pi)});
		oneBits= arrayClass.fill(frameLen, {|i| sin(i/frameLen*(oneFreq/baudRate)*2pi)});
	}
	initArrays {
		if(addNewlineGap, {
			gapBits= arrayClass.newFrom([zeroBits.dup(9), oneBits.dup(2)].dup(10).flat);
		});
		leaderBits= arrayClass.newFrom(oneBits.dup(baudRate*leaderDur).flat);
		trailerBits= arrayClass.newFrom(oneBits.dup(baudRate*trailerDur).flat);
	}

	//--sclang encode

	encodeByte {|byte|
		var i= 0;
		var bits= arrayClass.new(frameLen*11);
		bits.addAll(zeroBits);
		while({i<8}, {
			bits.addAll(if(byte.rightShift(i)&1==0, {zeroBits}, {oneBits}));
			i= i+1;
		});
		bits.addAll(oneBits);
		bits.addAll(oneBits);
		^bits;
	}

	encodeString {|str|
		var bits, extra= 0, res;
		if(addCarriageReturn, {
			str= str.replace(Char.nl, "\r\n");
		});
		if(addNewlineGap, {
			extra= str.occurrencesOf(Char.nl)*gapBits.size;
		});
		bits= arrayClass.new(frameLen*11*str.size+extra);
		str.do{|char|
			bits.addAll(this.encodeByte(char.ascii));
			if(addNewlineGap and:{char==Char.nl}, {bits.addAll(gapBits)});
		};
		res= arrayClass.new(bits.size+leaderBits.size+trailerBits.size)
		.addAll(leaderBits)
		.addAll(bits)
		.addAll(trailerBits);
		^FloatArray.fill(res.size, {|i| res[i]*mul+add});
	}

	encodeStringAsBuffer {|server, str, action|
		var res;
		server= server?Server.default;
		if(server.serverRunning, {
			res= this.encodeString(str);
			if(sampleRate!=server.sampleRate, {
				res= res.resamp0(res.size*(server.sampleRate/sampleRate));
			});
			^Buffer.loadCollection(server, res, 1, action);
		}, {
			"% - boot server first".format(this.class.name).warn;
		});
	}

	encodeStringAsSoundFile {|str, path|
		var type= if(arrayClass==Int8Array, {"uint8"}, {"float"});
		var file= SoundFile.openWrite(path.standardizePath, "WAV", type, 1, sampleRate);
		if(file.notNil, {
			file.writeData(this.encodeString(str));
			file.close;
		}, {
			"% - failed to create %".format(this.class.name, path).warn;
		});
		^file.notNil;
	}

	//--sclang decode

	decodeFloatArray {|arr, rate, channels= 1|
		var len= (rate?sampleRate/oneFreq*8).round.asInteger;
		var byte, i= 0, j, k, sum;
		var numFrames= arr.size.div(channels);
		var bits= Int8Array(numFrames);
		var prev= false;
		var res= Int8Array();

		numFrames.do{|i|
			var samp= arr[i*channels]>=0;
			bits.add((samp!=prev).binaryValue);  //zero crossings
			prev= samp;
		};

		while({i+(len*11)<bits.size}, {
			sum= 0;
			j= 0;
			while({j<len}, {
				sum= sum+bits[i+j];
				j= j+1;
			});
			if(sum<=9, {
				i= i+len;
				byte= 0;
				k= 0;
				while({k<8}, {
					sum= 0;
					j= 0;
					while({j<len}, {
						sum= sum+bits[i+j];
						j= j+1;
					});
					if(sum>=12, {
						byte= byte+2.pow(k);
					});
					i= i+len;
					k= k+1;
				});
				res= res.add(byte);
				i= i+(len*2);
			}, {
				i= i+1;
			});
		});
		^res;
	}

	decodeBuffer {|buf, raw= false, action|
		buf.loadToFloatArray(action: {|arr|
			var res= this.decodeFloatArray(arr, buf.sampleRate, buf.numChannels);
			if(raw, {
				action.value(res);
			}, {
				action.value(this.prBytesToString(res));
			});
		});
	}

	decodeSoundFile {|path, raw= false|
		var arr, res;
		var file= SoundFile.openRead(path.standardizePath);
		if(file.notNil, {
			arr= FloatArray.newClear(file.numFrames*file.numChannels);
			file.readData(arr);
			file.close;
			res= this.decodeFloatArray(arr, file.sampleRate, file.numChannels);
			if(raw, {
				^res;
			}, {
				^this.prBytesToString(res);
			});
		}, {
			"% - failed to open %".format(this.class.name, path).warn;
			^nil;
		});
	}

	prBytesToString {|arr|
		^arr.reject{|x| x==0}.collect{|x| if(x<0, {x= 256-x}); x.asAscii}.join;
	}

	//--scserver encode

	play {|target, out= 0, amp= 0.5, addAction= \addToHead|
		var group= if(target.isKindOf(Server), {
			target.defaultGroup;
		}, {
			target?Server.default.defaultGroup;
		});
		if(group.server.serverRunning, {
			fork{
				if(SynthDescLib.at(\redKCSencode).isNil, {

					SynthDef(\redKCSencode, {
						|out= 0, buf, t_trig= 0, amp= 0.5, baudRate= 300, zeroFreq= 1200, oneFreq= 2400|
						var trig= Latch.ar(Trig1.ar(t_trig, 2/baudRate), Impulse.ar(baudRate, 2/baudRate));
						var gate= SetResetFF.ar(trig);
						var phasor= Sweep.ar(trig, baudRate);
						var data= BufRd.ar(1, buf, phasor*gate, 0, 1)+(1-gate);
						var f0= baudRate/zeroFreq;
						var f1= baudRate/oneFreq;
						var snd= (phasor%f0>(f0*0.5)*(1-data))+(phasor%f1>(f1*0.5)*data);
						Out.ar(out, snd*2-1*amp);
					}).add;

					group.server.sync;
				});

				synth.free;
				buffer.free;
				buffer= Buffer.alloc(group.server, 1);
				group.server.sync;
				synth= Synth(\redKCSencode,
					[
						\out, out, \amp, amp, \buf, buffer,
						\baudRate, baudRate, \zeroFreq, zeroFreq, \oneFreq, oneFreq
					],
					group.server,
					addAction
				);
			};
		}, {
			"% - boot server first".format(this.class.name).warn;
		});
	}

	playSine {|target, out= 0, amp= 0.5, addAction= \addToHead|
		var group= if(target.isKindOf(Server), {
			target.defaultGroup;
		}, {
			target?Server.default.defaultGroup;
		});
		if(group.server.serverRunning, {
			fork{
				if(SynthDescLib.at(\redKCSencodeSine).isNil, {

					SynthDef(\redKCSencodeSine, {
						|out= 0, buf, t_trig= 0, amp= 0.5, baudRate= 300, zeroFreq= 1200, oneFreq= 2400|
						var trig= Latch.ar(Trig1.ar(t_trig, 2/baudRate), Impulse.ar(baudRate, 2/baudRate));
						var gate= SetResetFF.ar(trig);
						var phasor= Sweep.ar(trig, baudRate);
						var data= BufRd.ar(1, buf, phasor*gate, 0, 1)+(1-gate);
						var snd= (SinOsc.ar(zeroFreq)*(1-data))+(SinOsc.ar(oneFreq)*data);
						Out.ar(out, snd*amp);
					}).add;

					group.server.sync;
				});

				synth.free;
				buffer.free;
				buffer= Buffer.alloc(group.server, 1);
				group.server.sync;
				synth= Synth(\redKCSencodeSine,
					[
						\out, out, \amp, amp, \buf, buffer,
						\baudRate, baudRate, \zeroFreq, zeroFreq, \oneFreq, oneFreq
					],
					group.server,
					addAction
				);
			};
		}, {
			"% - boot server first".format(this.class.name).warn;
		});
	}

	send {|str= ""|
		if(synth.notNil, {
			fork{
				buffer.free;
				buffer= Buffer.loadCollection(buffer.server, this.prEncodeBytes(str.ascii));
				buffer.server.sync;
				synth.set(\buf, buffer, \t_trig, 1);
			};
		}, {
			"% - call play or playSine first".format(this.class.name).warn;
		});
	}

	stop {
		synth.free;
		synth= nil;
		buffer.free;
	}

	prEncodeBytes {|bytes|
		^bytes.collect{|byte| [0]++byte.asBinaryDigits.reverse++#[1, 1]}.flat;
	}

	//--scserver decode

	listen {|target, action, in= 0, thresh= 0.01, rq= 0.2, addAction= \addToTail|
		var group= if(target.isKindOf(Server), {
			target.defaultGroup;
		}, {
			target?Server.default.defaultGroup;
		});
		if(group.server.serverRunning, {
			action= action?{|byte| byte.asAscii.post};
			fork{
				if(SynthDescLib.at(\redKCSdecode).isNil, {

					SynthDef(\redKCSdecode, {
						|in= 0, baudRate= 300, zeroFreq= 1200, oneFreq= 2400, thresh= 0.01, rq= 0.2|
						var startTrig, trig, byte;
						var arr= 0!11;
						var zeroCycles= zeroFreq/baudRate;
						var oneCycles= oneFreq/baudRate;
						var snd= In.ar(in);
						snd= BPF.ar(snd, zeroFreq, rq)+BPF.ar(snd, oneFreq, rq);
						snd= Schmidt.ar(snd, 0-thresh, thresh)-0.5;
						snd= (ZeroCrossing.ar(snd)/baudRate).round(1);
						startTrig= SetResetFF.ar(BinaryOpUGen('==', snd, zeroCycles));
						10.do{|i|
							var d= 10-i/baudRate;
							arr[i]= DelayN.ar(snd, d, d);
						};
						arr[10]= snd;
						arr= Latch.ar(arr, Phasor.ar(startTrig, baudRate*SampleDur.ir)>0.1);
						trig= Trig1.ar(
							BinaryOpUGen('==', arr[0], zeroCycles)&
							BinaryOpUGen('==', arr[9], oneCycles)&
							BinaryOpUGen('==', arr[10], oneCycles),
							10/baudRate
						);
						byte= arr[1..8].sum{|x, i| BinaryOpUGen('==', x, oneCycles)*2.pow(i)};
						SendReply.ar(trig, '/redKCSdecode', byte);
					}).add;

					group.server.sync;
				});

				listenSynth.free;
				group.server.sync;
				listenResponder.free;
				listenResponder= OSCFunc({|msg|
					action.value(msg[3].asInteger);
				}, '/redKCSdecode', group.server.addr);
				listenSynth= Synth(\redKCSdecode,
					[
						\in, in, \thresh, thresh, \rq, rq,
						\baudRate, baudRate, \zeroFreq, zeroFreq, \oneFreq, oneFreq
					],
					group.server,
					addAction
				);
			};
		}, {
			"% - boot server first".format(this.class.name).warn;
		});
	}

	stopListening {
		listenSynth.free;
		listenResponder.free;
	}
}
