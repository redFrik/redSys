//redFrik

RedLZSS {
	classvar <>window= 4096, <>length= 32, <>pad;

	*compress {|input|
		var out= "", i= 0, len, off, sub, j;
		var bitsWin= (window-1).numBits;
		var bitsLen= (length-1).numBits;
		while({i<input.size}, {
			len= length;
			while({
				sub= input.copyRange(i, (i+len-1).min(input.size-1));
				len= len.min(sub.size);
				off= input.copyRange((i-window+1).max(0), i-1).find(sub);
				off.isNil and:{len>1};
			}, {
				len= len-1;
			});
			if(off.isNil, {
				out= out++0++sub[0].asStringToBase(2, 8);
				i= i+1;
			}, {
				if(off+len==i, {
					j= 0;
					while({
						input[i+len+j]==input[i+j] and:{len+j<length};
					}, {
						j= j+1;
					});
					len= len+j;
				});
				if(len>2, {
					out= out++1++(len-2).asStringToBase(2, bitsLen)
						++(i.min(window-1)-off).asStringToBase(2, bitsWin);
					i= i+len;
					if(i<input.size, {
						out= out++input[i].asStringToBase(2, 8);
						i= i+1;
					});
				}, {
					len.do{|x|
						if(i+x<input.size, {
							out= out++0++input[i+x].asStringToBase(2, 8);
						});
					};
					i= i+len;
				});
			});
		});
		^out;
	}
	*decompress {|input|
		var out= List.new, i= 0, len, off;
		var bitsWin= (window-1).numBits;
		var bitsLen= (length-1).numBits;
		while({i<input.size}, {
			if(input[i].digit==0, {
				i= i+1;
				out.add(this.prBitStrToInt(input.copyRange(i, i+7)));
				i= i+8;
			}, {
				i= i+1;
				len= this.prBitStrToInt(input.copyRange(i, i+bitsLen-1))+2;
				i= i+bitsLen;
				off= this.prBitStrToInt(input.copyRange(i, i+bitsWin-1));
				i= i+bitsWin;
				while({len>0}, {
					out.add(out[out.size-off]);
					len= len-1;
				});
				if(i<input.size, {
					out.add(this.prBitStrToInt(input.copyRange(i, i+7)));
					i= i+8;
				});
			});
		});
		^out.array;
	}
	*binaryStringToBytes {|str|
		pad= 0;
		^str.clump(8).collect{|x|
			while({x.size<8}, {
				x= x++0;
				pad= pad+1;
			});
			this.prBitStrToInt(x);
		};
	}
	*bytesToBinaryString {|arr|
		var str= arr.collect{|x|
			x.asStringToBase(2, 8);
		}.join;
		^str.copyRange(0, str.size-1-pad);
	}

	//--private
	*prBitStrToInt {|str|
		^str.sum{|bit, i| 1<<(str.size-1-i)*bit.digit};
	}
}
