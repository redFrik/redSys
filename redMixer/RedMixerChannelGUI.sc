//redFrik

//--related:
//RedMixerChannel RedMixerGUI RedEffectModuleGUI

//todo: numberbox for out bus?
//todo: how to deal with inserts and archiving?

RedMixerChannelGUI {

	classvar <>width= 110, <>height= 280, <>numInserts= 2;

	var <redMixerChannel, <parent, position,
	win, <views, <view, textView,
	insertEfxs, insertWins;

	*new {|redMixerChannel, parent, position, name|
		^super.newCopyArgs(redMixerChannel, parent, position).initRedMixerChannelGUI(name);
	}

	initRedMixerChannelGUI {|name|
		var peakButtons, peakLevels;
		var volSpec= #[-90, 6, \db].asSpec;
		var volWarp= DbFaderWarp(volSpec);
		var freqSpec= \freq.asSpec;
		var bandSpec= ControlSpec(0.25, 2, 'exp', 0, 1);
		var gainSpec= #[-40, 20, \db, 0, 0].asSpec;
		var controllers= List.new;
		var bw= 20, lh= 14;
		view= this.prContainer;
		views= List.new;
		insertEfxs= Array.newClear(numInserts);
		insertWins= Array.newClear(numInserts);

		view.layout= VLayout(*[

			//--effect inserts
			numInserts.collect{|i|
				var popup, button;
				HLayout(
					popup= RedPopUpMenu()
					.items_(["_inserts_"]++RedEffectModule.subclasses.collect{|x| x.name}),
					button= RedButton(nil, nil, "o", "o").maxWidth_(bw)
					.action_({|v|
						var efx, win;
						if(v.value==1, {
							if(popup.value>0, {
								Routine({
									var pos;
									efx= RedEffectModule.subclasses[popup.value-1].new;
									redMixerChannel.insert(efx, #[\addToHead, \addToTail].clipAt(i));
									pos= Point(parent.bounds.right, parent.bounds.bottom-80-(i*110));
									win= efx.gui(nil, pos);
									insertEfxs[i]= efx;
									insertWins[i]= win;
									win.onClose_({
										redMixerChannel.remove(insertEfxs[i]);
										insertEfxs[i]= nil;
										v.value= 0;
									});
								}).play(AppClock);
							});
						}, {
							if(insertWins[i].notNil, {
								insertWins[i].close;
								insertWins[i]= nil;
							});
						});
					})
				)
			},

			//--equaliser
			HLayout(
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.hiFreq,
						{|x| freqSpec.map(x)}, {|x| freqSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.hiBand,
						{|x| bandSpec.map(x)}, {|x| bandSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.hiGain,
						{|x| gainSpec.map(x)}, {|x| gainSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVButton(nil, nil, redMixerChannel.cvs.eqHi, nil, nil, (str: "hi"))
				).last
			),
			HLayout(
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.miFreq,
						{|x| freqSpec.map(x)}, {|x| freqSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.miBand,
						{|x| bandSpec.map(x)}, {|x| bandSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.miGain,
						{|x| gainSpec.map(x)}, {|x| gainSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVButton(nil, nil, redMixerChannel.cvs.eqMi, nil, nil, (str: "mi"))
				).last
			),
			HLayout(
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.loFreq,
						{|x| freqSpec.map(x)}, {|x| freqSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.loBand,
						{|x| bandSpec.map(x)}, {|x| bandSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVKnob(nil, nil, redMixerChannel.cvs.loGain,
						{|x| gainSpec.map(x)}, {|x| gainSpec.unmap(x)})
				).last,
				views.add(
					RedGUICVButton(nil, nil, redMixerChannel.cvs.eqLo, nil, nil, (str: "lo"))
				).last
			),

			//--balance
			HLayout(
				views.add(
					RedGUICVSlider(
						nil,
						nil,
						redMixerChannel.cvs.bal,
						{|x| x*2-1},
						{|x| x*0.5+0.5}
					)
				);
				views.last.view.orientation_(\horizontal);
				views.last.view.maxHeight_(lh);
				views.last.view.mouseUpAction_{|v, x, y, mod|
					if(mod.isCtrl, {	//ctrl to center balance
						{redMixerChannel.bal= 0}.defer(0.1);
					});
				};
				views.last,

				//--mute
				views.add(
					RedGUICVButton(
						nil,
						nil,
						redMixerChannel.cvs.mute,
						nil,
						nil,
						(str: "m")
					)
				);
				views.last.view.maxWidth_(bw);
				views.last
			),

			//--volume number
			HLayout(*[
				views.add(
					RedGUICVNumberBox(
						nil,
						nil,
						redMixerChannel.cvs.amp,
						{|x| x.dbamp},
						{|x| x.ampdb}
					)
				);
				views.last,

				//--peaks
				View().fixedSize_(Size(12, lh)),  //spacer
				peakButtons= [
					RedButton(nil, nil, "", ""),
					RedButton(nil, nil, "", "")
				];
				peakButtons.do{|b|
					b.canFocus_(false);
					b.maxWidth_(bw);
				};
				controllers.add(
					SimpleController(redMixerChannel.cvs.peaks).put(\value, {|ref|
						defer{
							ref.value.do{|x, i|
								if(x>1 and:{peakButtons[i].value==0}, {
									peakButtons[i].value= 1;
								}, {
									if(x<=1 and:{peakButtons[i].value==1}, {
										peakButtons[i].value= 0;
									});
								});
							};
						};
					})
				);
				peakButtons
			].flat),

			//--volume slider
			HLayout(*[
				views.add(
					RedGUICVSlider(
						nil,
						nil,
						redMixerChannel.cvs.amp,
						{|x| volWarp.map(x).dbamp},
						{|x| volWarp.unmap(x.ampdb)}
					)
				);
				views.last.view.mouseUpAction_{|v, x, y, mod|
					if(mod.isCtrl, {	//ctrl to reset volume
						{redMixerChannel.vol= 0}.defer(0.1);
					});
				},

				//--unity gain marking
				UserView().fixedWidth_(12).drawFunc_({|usr|
					var y= 1-volSpec.unmap(0)*usr.bounds.height;
					var fnt= RedFont.new;
					var col= GUI.skins.redFrik.foreground;
					Pen.stringAtPoint("-u", Point(0, y), fnt, col);
				}),

				//--levels
				peakLevels= [
					RedLevelIndicator(),
					RedLevelIndicator()
				];
				peakLevels.do{|l|
					l.canFocus_(false);
					l.maxWidth_(bw);
					l.warning_(volSpec.unmap(-3));
					l.critical_(volSpec.unmap(0));
				};
				controllers.add(
					SimpleController(redMixerChannel.cvs.levels).put(\value, {|ref|
						defer{
							ref.value.do{|x, i| peakLevels[i].value= volSpec.unmap(x.ampdb)};
						};
					})
				);
				peakLevels
			].flat),

			//--name
			if(name.notNil, {
				textView= RedStaticText(nil, nil, name);
				textView.maxHeight_(lh).align_(\center);
			})

		].flat).margins_(4).spacing_(4);

		view.allChildren.do{|v|
			if(v.isKindOf(Knob), {
				v.maxHeight_(20);
			}, {
				if([PopUpMenu, Button, NumberBox].any{|c| v.isKindOf(c)}, {
					v.maxHeight_(lh);
				});
			});
		};

		view.onClose_({controllers.do{|x| x.remove}});
	}

	text_ {|str|
		var tmp, tmp2;
		if(textView.notNil, {
			tmp= textView.string.bounds(RedFont.new).width;
			tmp2= str.bounds(RedFont.new).width+2;
			textView.bounds= Rect(
				textView.bounds.left-(tmp2-tmp/4),
				textView.bounds.top,
				tmp2,
				textView.bounds.height
			);
			textView.string= str;
		});
	}

	close {
		insertWins.do{|x| if(x.notNil, {x.close})};
		if(win.notNil and:{win.isClosed.not}, {win.close});
	}

	cmp {
		this.deprecated(thisMethod, this.class.findMethod(\view));
		^view
	}

	//--private
	prContainer {
		var width, height;
		position= position ?? {Point(500, 500)};
		width= RedMixerChannelGUI.width;
		height= RedMixerChannelGUI.height;
		if(parent.isNil, {
			win= Window(
				redMixerChannel.class.name,
				Rect(position.x, position.y, width, height),
				false
			);
			parent= win;
			win.front;
			CmdPeriod.doOnce({if(win.isClosed.not, {win.close})});
		});
		^View(parent, Point(width, height))
		.background_(GUI.skins.redFrik.background)
		.onClose_({this.close});
	}
}
