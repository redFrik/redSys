RedKCS {
	var <sampleRate, <baudRate, <zeroFreq, <oneFreq,
	<addNewlineGap, <addCarriageReturn,
	<leaderDur, <trailerDur;
	var arrayClass, mul, add;
	var oneBits, zeroBits, gapBits, leaderBits, trailerBits;

	*new {|sampleRate, baudRate= 300, zeroFreq= 1200, oneFreq= 2400,
		addNewlineGap= true, addCarriageReturn= true,
		leaderDur= 5, trailerDur= 5|
		^super.newCopyArgs(
			sampleRate, baudRate, zeroFreq, oneFreq,
			addNewlineGap, addCarriageReturn,
			leaderDur, trailerDur
		).initRedKCS.initRedKCSadditional;
	}

	initRedKCS {
		var len;
		mul= 1;
		add= -0.5;
		arrayClass= Int8Array;
		sampleRate= sampleRate?9600;
		len= sampleRate.div(baudRate);
		oneBits= arrayClass.fill(len, {|i| i.div((sampleRate/oneFreq).div(2))%2});
		zeroBits= arrayClass.fill(len, {|i| i.div((sampleRate/zeroFreq).div(2))%2});
	}

	initRedKCSadditional {
		if(addNewlineGap, {
			gapBits= arrayClass.with(*[zeroBits.dup(9), oneBits.dup(2)].dup(10).flat);
		});
		leaderBits= arrayClass.with(*oneBits.dup(baudRate*leaderDur).flat);
		trailerBits= arrayClass.with(*oneBits.dup(baudRate*trailerDur).flat);
	}

	encodeByte {|byte|
		var i= 0;
		var bits= arrayClass.new(sampleRate.div(baudRate)*11);
		bits.addAll(zeroBits);
		while({i<8}, {
			bits.addAll(if(byte.rightShift(i)&1==1, {oneBits}, {zeroBits}));
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
		bits= arrayClass.new(sampleRate.div(baudRate)*11*str.size+extra);
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
				action.value(this.bytesToString(res));
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
				^this.bytesToString(res);
			});
		}, {
			"% - failed to open %".format(this.class.name, path).warn;
			^nil;
		});
	}

	bytesToString {|arr|
		^arr.reject{|x| x==0}.collect{|x| x.asAscii}.join;
	}
}

RedKCSsine : RedKCS {
	initRedKCS {
		var len;
		mul= 0.5;
		add= 0;
		arrayClass= FloatArray;
		sampleRate= sampleRate?Server.default.sampleRate?44100;
		len= sampleRate.div(baudRate);
		oneBits= arrayClass.fill(len, {|i| sin(i/len*(oneFreq/baudRate)*2pi)});
		zeroBits= arrayClass.fill(len, {|i| sin(i/len*(zeroFreq/baudRate)*2pi)});
	}
}
