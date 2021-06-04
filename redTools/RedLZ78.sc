//redFrik

//todo:
//implement table as tree instead of array for speed?

RedLZ78 {
	*compress {|input|
		var tab= [], out= List[], i= 0, j, match, sub, last;
		while({i<input.size}, {
			j= 0;
			last= 0;
			while({
				sub= input.copyRange(i, i+j);
				match= tab.indexOfEqual(sub);
				match.notNil and:{i+j<(input.size-1)};
			}, {
				last= match;
				j= j+1;
			});
			tab= tab.add(sub);
			if(j==0, {
				out.add(0);
				out.add(input[i+j]);
			}, {
				out.add(last+1);
				out.add(input[i+j]);
			});
			i= i+1+j;
		});
		^out.array;
	}
	*decompress {|input|
		var tab= List[], out= List[], i= 0, match, sub, val;
		while({i<input.size}, {
			match= input[i];
			val= input[i+1];
			if(match==0, {
				sub= [val];
			}, {
				sub= tab[match-1]++val;
			});
			tab.add(sub);
			out.addAll(sub);
			i= i+2;
		});
		^out.array;
	}
}
