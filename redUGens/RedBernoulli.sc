//safest to follow with a LeakDC

RedBernoulli {
	*ar {|thresh= 0.975, mul= 1, add= 0|
		^Latch.ar(WhiteNoise.ar(mul, add), WhiteNoise.ar>thresh)
	}
	*kr {|thresh= 0.975, mul= 1, add= 0|
		^Latch.kr(WhiteNoise.kr(mul, add), WhiteNoise.kr>thresh)
	}
}
