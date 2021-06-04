//redFrik

RedBencode {
	*encode {|array|
		var res= List.new;
		array.do{|val, i|
			case
			{val.isInteger} {  //integer
				res.add($i).add(val).add($e);
			}
			{val.isString} {  //string
				res.add(val.size).add($:).add(val);
			}
			{val.isKindOf(Set)} {  //dictionary
				res.add($d);
				val.asSortedArray.do{|a| res.add(this.encode([a[0].asString, a[1]]))};
				res.add($e);
			}
			{val.isSequenceableCollection} {  //list
				res.add($l).add(this.encode(val)).add($e);
			}
			{("%: error encoding % at index %").format(this.class.name, val, i).error}
		};
		^res.join;
	}
	*encodeBytes {|array|
		var str= this.encode(array);
		^str.collectAs({|x| x.ascii}, Int8Array);
	}
	*decodeBytes {|array|
		^this.decodeString(String.fill(array.size, {|i| array[i].asAscii}));
	}
	*decodeString {|string|
		^this.decode(CollStream(string));
	}
	*decode {|stream|
		var running= true, state= 0, res= List.new;
		var str, chr, tmp, dict;
		while({running}, {
			chr= stream.next;
			running= chr.notNil;
			switch(state,
				0, {  //new item
					case
					{chr.isNil} {
						running= false;
					}
					{chr==$i} {  //integer
						state= 1;
						str= "";
					}
					{chr.isDecDigit} {  //string length
						state= 2;
						str= ""++chr;
					}
					{chr==$l} {  //list recursion call
						res.add(this.decode(stream));
					}
					{chr==$d} {  //dictionary recursion call
						tmp= this.decode(stream);
						dict= Dictionary(tmp.size);
						tmp.pairsDo{|k, v| dict.put(k.asSymbol, v)};
						res.add(dict);
					}
					{chr==$e} {  //end of list or dictionary
						running= false;
					}
					{("%: error decoding % at index %").format(this.class.name, chr, stream.pos).error}
				},
				1, {  //integer
					case
					{chr.isDecDigit or:{chr==$-}} {  //integer digits or sign
						str= str++chr;
					}
					{chr==$e} {  //end of integer
						res.add(str.asInteger);
						state= 0;
					}
					{("%: error decoding integer % at index %").format(this.class.name, chr, stream.pos).error}
				},
				2, {  //string
					case
					{chr.isDecDigit} {  //string length
						str= str++chr;
					}
					{chr==$:} {  //string delimiter and content
						res.add(stream.nextN(str.asInteger));
						state= 0;
					}
					{("%: error decoding string % at index %").format(this.class.name, chr, stream.pos).error}
				}
			);
		});
		^res.array;
	}
}
