//redFrik

RedGUICVSlider : RedGUICV {
	classvar <defaultWidth= 16, <defaultHeight= 74;

	//--private
	prMake {|parent, bounds|
		bounds= (bounds ?? {Rect(0, 0, defaultWidth, defaultHeight)}).asRect;
		^RedSlider(parent, bounds);
	}
}
