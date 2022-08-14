RedSmooth {
	var <>factor, <>val= 0;
	*new {|factor= 0.1, initVal= 0|
		^super.newCopyArgs(factor, initVal);
	}
	filter {|v|
		^val= val+((v-val)*factor);
		//^val= v*factor+(1-factor*val);  //same
	}
}

RedSmoothUD {
	var <>factorUp, <>factorDown, <>val= 0;
	*new {|factorUp= 0.1, factorDown= 0.1, initVal= 0|
		^super.newCopyArgs(factorUp, factorDown, initVal);
	}
	filter {|v|
		^val= val+((v-val)*if(v>val, factorUp, factorDown));
	}
}

RedSmooth2 {
	var <>factor, <>factor2, <>val= 0;
	var <>trend= 0;
	*new {|factor= 0.1, factor2= 0.2, initVal= 0|
		^super.newCopyArgs(factor, factor2, initVal);
	}
	filter {|v|
		var prev= val;
		val= v*factor+(1-factor*(val+trend));
		trend= (val-prev)*factor2+(1-factor2*trend);
		^val;
	}
}
