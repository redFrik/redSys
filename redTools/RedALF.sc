//redFrik

RedALF {
	*atolf {|str|
		var res= List[1/(1<<5)];
		var tre= [1<<12, 1<<20, 1<<28];
		str.do{|chr, i|
			var j= i.div(3);
			if(i.mod(3)==2, {
				res.add(1/(1<<5));
			});
			res.put(j, res[j]+(chr.ascii/tre[i.mod(3)]));
		};
		^res.array;
	}
	*lftoa {|arr|
		var res= List[];
		arr.do{|val|
			var a, b, c;
			val= val-(1/(1<<5))*4096;
			a= val.asInteger;
			val= val-a*256;
			b= val.asInteger;
			val= val-b*256;
			c= val.asInteger;
			res.add(a).add(b).add(c);
		};
		^res.select{|x| x>0}.collectAs({|x| x.asAscii}, String);
	}
}

/*
a= RedALF.atolf("aber");
RedALF.lftoa(a);
*/
