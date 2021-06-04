//redFrik

RedRLE {
	*encode {|input|
		var out= List[], cnt, val, i= 0;
		while({i<input.size}, {
			cnt= 1;
			val= input[i];
			while({i+1<input.size and:{input[i+1]==val}}, {
				cnt= cnt+1;
				i= i+1;
			});
			out.add(cnt);
			out.add(val);
			i= i+1;
		});
		^out.array;
	}
	*decode {|input|
		var out= List[], cnt, val, i= 0;
		while({i<input.size}, {
			cnt= input[i];
			val= input[i+1];
			out.addAll(val.dup(cnt));
			i= i+2;
		});
		^out.array;
	}
}
