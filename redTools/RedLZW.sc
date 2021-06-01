//redFrik

//double-check exception
//rewrite as bitstring?

RedLZW {
	*compress {|input|
		var tab= List.fill(256, {|i| [i]});
		var out= List.new;
		var sub= [];
		input.do{|val|
			var tmp= sub++val;
			if(tab.includesEqual(tmp), {
				sub= tmp;
			}, {
				tab.add(tmp);
				out.add(tab.indexOfEqual(sub));
				sub= [val];
			});
		};
		^out.add(tab.indexOfEqual(sub)).array;
	}
	*decompress {|input|
		var tab= List.fill(256, {|i| [i]});
		var old= input[0];
		var out= List[old];
		var val= old;
		input.drop(1).do{|k|
			var sub;
			if(tab[k].notNil, {
				sub= tab[k];
			}, {
				sub= tab[old]++val;
			});
			out.addAll(sub);
			val= sub[0];
			tab.addAll([tab[old]++val]);
			old= k;
		};
		^out.array;
	}
}


/*
//--old string only implementation

RedLZW {
	*compress {|input|								//string
		var tab= {|i| i.asAscii.asSymbol}.dup(256);
		var out= [];
		var str= '';
		input.do{|chr|
			var tmp= (str++chr).asSymbol;
			if(tab.includes(tmp), {
				str= tmp;
			}, {
				tab= tab.add(tmp);
				out= out.add(tab.indexOf(str));
				str= chr.asSymbol;
			});
		};
		^out.add(tab.indexOf(str));					//array of 9bit integers
	}
	*decompress {|input|							//array of 9bit integers
		var tab= {|i| i.asAscii}.dup(256);
		var old= input[0];
		var out= ""++old.asAscii;
		var chr= old.asAscii;
		input.drop(1).do{|k|
			var str;
			if(tab[k].notNil, {
				str= tab[k].asString;
			}, {
				str= tab[old]++chr;
			});
			out= out++str;
			chr= str[0];
			tab= tab.add((tab[old]++chr));
			old= k;
		};
		^out										//string
	}

	//--variant.  no difference in speed though
	*compress2 {|input|							//string
		var dict= {|i| i.asAscii.asSymbol}.dup(256);
		var old= input[0].asAscii.asSymbol;
		var res= [];
		input.drop(1).do{|chr|
			var oldchr= (old++chr).asSymbol;
			if(dict.includes(oldchr), {
				old= oldchr;
			}, {
				res= res.add(dict.indexOf(old));
				dict= dict.add(oldchr);
				old= chr.asSymbol;
			});
		};
		^res.add(dict.indexOf(old));				//array of 9bit integers
	}
}
*/