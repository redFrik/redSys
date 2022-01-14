//redFrik

//related:
//RedEffectModule RedMixGUI RedMixerChannelGUI

RedEffectModuleGUI {
	var <redEffectModule, <parent, position,
	win, <views, <view, <params;

	*new {|redEffectModule, parent, position|
		^super.newCopyArgs(redEffectModule, parent, position).initRedEffectModuleGUI;
	}

	initRedEffectModuleGUI {
		var bw= 16, lh= 14;
		params= redEffectModule.def.metadata[\order].reject{|x| x.key==\out};
		view= this.prContainer(params.size-1);
		views= List.new;

		view.layout= HLayout(

			*params.collect{|assoc|
				var k= assoc.key;
				var v= assoc.value;
				if(k==\mix, {

					views.add(
						RedGUICVSlider(
							nil,
							nil,
							redEffectModule.cvs[v],
							{|x| redEffectModule.specs[k].map(x)},
							{|x| redEffectModule.specs[k].unmap(x)}
						)
					);
					views.last.view.mouseUpAction_{|view, x, y, mod|
						if(mod.isCtrl, {	//ctrl to center dry/wet mix
							redEffectModule.cvs[v].value_(0).changed(\value);
						});
					};
					views.last.view.maxWidth_(bw);
					views.last

				}, {
					VLayout(

						views.add(
							RedGUICVKnob(
								nil,
								nil,
								redEffectModule.cvs[v],
								{|x| redEffectModule.specs[k].map(x)},
								{|x| redEffectModule.specs[k].unmap(x)}
							)
						);
						views.last.view.minWidth_(RedGUICVKnob.defaultWidth);
						views.last.view.minHeight_(RedGUICVKnob.defaultHeight);
						views.last,

						views.add(
							RedGUICVNumberBox(
								nil,
								nil,
								redEffectModule.cvs[v],
								{|x| redEffectModule.specs[k].constrain(x)},
								{|x| redEffectModule.specs[k].constrain(x)}
							)
						);
						views.last.view.maxHeight_(lh);
						views.last,

						RedStaticText(nil, nil, v).maxHeight_(lh)
					).spacing_(0)
				});
			};
		).margins_(4).spacing_(4);

		view.maxWidth_(bw+4+(RedGUICVKnob.defaultWidth+4*params.size));
		view.maxHeight_(8+RedGUICVSlider.defaultHeight);
	}

	close {
		if(win.notNil and:{win.isClosed.not}, {win.close});
	}

	onClose_ {|func|
		view.onClose_(func);
	}

	cmp {
		this.deprecated(thisMethod, this.class.findMethod(\view));
		^view
	}

	//--private
	prContainer {|num|
		var width, height;
		position= position ?? {Point(400, 400)};
		width= 8+RedGUICVSlider.defaultWidth+(RedGUICVKnob.defaultWidth+4*num);
		height= 8+RedGUICVSlider.defaultHeight;
		if(parent.isNil, {
			win= Window(
				redEffectModule.class.name,
				Rect(position.x, position.y, width, height),
				false
			);
			parent= win;
			win.front;
			CmdPeriod.doOnce({this.close});
		});
		^View(parent, Point(width, height))
		.background_(GUI.skins.redFrik.background)
		.onClose_({this.close});
	}
}
