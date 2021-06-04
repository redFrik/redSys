//redFrik

RedBase64 {
	classvar <>map= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	*encode {|str|
		var out= List.new;
		str.clump(3).do{|x|
			var bin;
			switch(x.size,
				3, {
					bin= x[0].ascii.leftShift(16)+x[1].ascii.leftShift(8)+x[2].ascii;
					4.do{|y| out.add(map[63.leftShift(3-y*6).bitAnd(bin).rightShift(3-y*6)])};
				},
				2, {
					bin= x[0].ascii.leftShift(16)+x[1].ascii.leftShift(8);
					3.do{|y| out.add(map[63.leftShift(3-y*6).bitAnd(bin).rightShift(3-y*6)])};
					out.add($=);
				},
				1, {
					bin= x[0].ascii.leftShift(16);
					2.do{|y| out.add(map[63.leftShift(3-y*6).bitAnd(bin).rightShift(3-y*6)])};
					out.add($=).add($=);
				}
			);
		}
		^out.join;
	}

	*decode {|str|
		var out= List.new, cnt= 3, val= 0;
		str.do{|x, i|
			if(x.ascii!=10, {
				if(x.ascii!=61, {
					val= val+(map.indexOf(x).leftShift(cnt*6));
				});
				cnt= cnt-1;
				if(cnt<0, {
					3.do{|y| out.add(val.rightShift(2-y*8).bitAnd(255))};
					cnt= 3;
					val= 0;
				});
			});
		};
		^out.select{|x| x>0}.collectAs({|x| x.asAscii}, String);
	}
}
