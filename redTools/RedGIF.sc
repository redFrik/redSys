//redFrik

//related: RedBMP

RedGIF {
	var	<>type,						//string

	<>width, <>height,		//integers
	<>background,					//color
	<>aspectRatio,				//integer
	<>depth,							//integer
	<>globalColorMap,			//array of colors

	<>controls,						//array of RedGIFControl
	<>comments,						//array of strings

	<>appId, <>appCode,		//strings
	<>appData,						//array

	<>images,							//array of redgifimages

	dict,									//array used for lzw decompression
	codeSize,							//integer used for lzw decompression
	clearCode,						//integer used for lzw decompression
	endCode;							//integer used for lzw decompression

	*read {|path, action|
		^super.new.read(path, action);
	}
	read {|path, action|
		path= path.standardizePath;
		if(File.exists(path), {
			this.prRead(path);
			{action.value(this)}.fork;
		}, {(this.class.name++": file"+path+"not found").error});
	}

	makeWindow {|bounds|
		var b= bounds ?? {Rect(300, 300, width, height)};
		var win= Window(this.class.name, b);
		var image= Image(width, height).interpolation_(\fast);
		var index= 0, img, row;
		var interlace= {|passes, step, offset|
			passes.do{|j|
				img.data.copyRange(row*width, row+1*width-1).do{|col, i|
					image.setColor(col, i, j*step+offset);//TODO test and check transparent
				};
				row= row+1;
			};
		};
		win.view.background= background??{Color.clear};
		win.drawFunc= {
			var i, j, size, x, y, col;
			var left, right, top, bottom;
			img= images.wrapAt(index);
			if(img.interlaced, {
				row= 0;
				interlace.value((height+7).div(8), 8, 0);
				interlace.value((height+3).div(8), 8, 4);
				interlace.value((height+1).div(4), 4, 2);
				interlace.value(height.div(2), 2, 1);
			}, {
				i= 0;
				j= 0;
				left= img.bounds.left;
				right= img.bounds.width+left;
				top= img.bounds.top;
				bottom= img.bounds.height+top;
				size= width*height;
				while({i<size}, {
					x= i.mod(width);
					y= i.div(width);
					if(x>=left and:{x<right and:{y>=top and:{y<bottom}}}, {
						col= img.data[j];
						if(img.control.transparentFlag and:{col==img.control.transparent}, {
							col= background;
						});
						image.setColor(col, x, y);
						j= j+1;
					});
					i= i+1;
				});
				Pen.drawImage(Rect(0, 0, win.bounds.width, win.bounds.height), image);
				if(img.control.disposalMethod==2, {
					image.fill(background);
				});
				//TODO ==3 restore previous
			});
		};
		if(images.size>1, {						//if animated gif
			Routine({
				while({win.isClosed.not}, {
					win.refresh;
					//check userinputflag here
					if(img.notNil, {
						(img.control.duration*0.01).max(0.01).wait;
						index= index+1;
					}, {
						0.01.wait;
					});
				});
			}).play(AppClock);
		});
		win.onClose= {image.free};
		win.front;
		^win;
	}

	//--private
	prRead {|path|
		var file= File(path, "rb");
		var separator;
		this.prReadHeader(file);
		if(type=="GIF87a" or:{type=="GIF89a"}, {
			this.prReadLogicalScreenDescriptor(file);
			images= [];
			"reading".post;
			while({separator= file.getInt8.bitAnd(0xff); separator!=0x3b}, {
				switch(separator,
					0x21, {this.prReadExtensionBlock(file)},
					0x2c, {this.prReadImageDescriptor(file); ".".post},
					0x00, {},
					{(this.class.name++": unknown separator"+separator).warn}
				);
			});
			file.close;
			"\nuncompressing".post;
			images.do{|image, i|						//copy over control objects to image objects
				image.data= this.prDecode(image.data).collect{|x| image.colorMap[x]};
				".".post;
				if(i<controls.size, {				//set corresponding control object
					image.control= controls[i];
				}, {
					if(controls.size>0, {			//use last found control object
						image.control= controls.last;
					}, {							//use default control
						image.control= RedGIFControl.new;
						image.control.duration= 0;
						image.control.transparent= Color.clear;
						image.control.disposalMethod= 0;
						image.control.userInputFlag= false;
						image.control.transparentFlag= true;
					});
				});
			};
			"".postln;
		}, {
			(this.class.name++": type"+type+"not recognized").error;
			file.close;
		});
	}
	prReadHeader {|file|							//header with signature and version (6bytes)
		type= {file.getChar}.dup(6).join;
	}
	prReadLogicalScreenDescriptor {|file|	//logical screen descriptor (7bytes)
		var flags;
		width= file.getInt16LE;						//logical screen width
		height= file.getInt16LE;					//logical screen height
		flags= file.getInt8.bitAnd(0xff);			//packed fields
		background= file.getInt8.bitAnd(0xff);		//background colour index
		aspectRatio= file.getInt8.bitAnd(0xff);		//pixel aspect ratio
		if(aspectRatio!=0, {"aspectRatio not zero".warn});//debug
		depth= flags.bitAnd(0x07)+1;				//size of global colour table
		//resolution= flags.rightShift(4).bitAnd(0x07);//colour resolution (unused)

		globalColorMap= [];
		if(flags.bitTest(7), {						//global colour map (3bytes*numColors)
			globalColorMap= {
				Color(*file.read(Int8Array[0, 0, 0]).bitAnd(0xff)/255);
			}.dup(2.leftShift(depth-1));
			background= globalColorMap[background];
		}, {
			background= nil;
			//(this.class.name++": no global color map").postln;//debug
		});
	}

	prReadExtensionBlock {|file|					//extension block (?bytes)
		var flags, blockSize, ctrl;
		switch(file.getInt8.bitAnd(0xff),
			0x01, {
				(this.class.name++": plain text not yet supported").warn;
				blockSize= file.getInt8.bitAnd(0xff);
				if(blockSize!=12, {"blockSize in plaintext not 12".warn});//debug
				("\ttextgrid left pos:"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid top pos :"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid width   :"+file.getInt16.bitAnd(0xff)).postln;
				("\ttextgrid height  :"+file.getInt16.bitAnd(0xff)).postln;
				("\tchar cell width  :"+file.getInt8.bitAnd(0xff)).postln;
				("\tchar cell height :"+file.getInt8.bitAnd(0xff)).postln;
				("\tforecolor index  :"+file.getInt8.bitAnd(0xff)).postln;
				("\tbackcolor index  :"+file.getInt8.bitAnd(0xff)).postln;
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					"plaintext block...".postln;	//debug
					{file.getChar}.dup(blockSize).join.postln;
				});
			},
			0xf9, {								//graphic control extension block
				ctrl= RedGIFControl.new;
				blockSize= file.getInt8.bitAnd(0xff);
				flags= file.getInt8.bitAnd(0xff);	//packed field
				ctrl.duration= file.getInt16LE;		//delay time (1/100ths of a second)
				ctrl.transparent= globalColorMap[file.getInt8.bitAnd(0xff)];//transparent colour
				ctrl.disposalMethod= flags.bitAnd(2r00011100).rightShift(2);//undraw
				ctrl.userInputFlag= flags.bitTest(1);	//wait for user input flag
				ctrl.transparentFlag= flags.bitTest(0);//transparency flag
				controls= controls.add(ctrl);
			},
			0xfe, {								//comment extension block
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					(this.class.name++": found comment block").postln;//debug
					comments= comments.add({file.getChar}.dup(blockSize).join);
				});
			},
			0xff, {								//application extension block
				blockSize= file.getInt8.bitAnd(0xff);
				//if(appData.notNil, {"already have application data!!!".postln});//debug
				appId= {file.getChar}.dup(8).join;
				appCode= {file.getChar}.dup(3).join;
				while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
					appData= file.read(Int8Array.newClear(blockSize)).bitAnd(0xff);
				});
			},
			{(this.class.name++": extensionblock - code not recognised").warn}
		);
	}
	prReadImageDescriptor {|file|					//local descriptor (10bytes)
		var flags, bounds, image;
		bounds= {file.getInt16LE}.dup(4).asRect;
		flags= file.getInt8.bitAnd(0xff);
		image= RedGIFImage(bounds, flags);

		if(image.hasColorMap, {
			image.colorMap= {
				Color(*file.read(Int8Array[0, 0, 0]).bitAnd(0xff)/255);
			}.dup(2.leftShift(image.depth-1));
		}, {
			//(this.class.name++": no local color map - using global").postln;//debug
			image.colorMap= globalColorMap;
		});

		//--read table based image data
		codeSize= file.getInt8.bitAnd(0xff);
		image.data= this.prReadData(file);
		images= images.add(image);
	}
	prReadData {|file|
		var blockSize, data= List.new;
		while({blockSize= file.getInt8.bitAnd(0xff); blockSize!=0}, {
			data.addAll(file.read(Int8Array.newClear(blockSize)).bitAnd(0xff));
		});
		^data
	}

	prInitDict {
		var size= 2.leftShift(codeSize-1);
		dict= Array.newClear(4096);
		size.do{|i| dict.put(i, [i])};
		dict.put(clearCode, [clearCode]);
		dict.put(endCode, [endCode]);
	}
	prDecode {|arr|								//lzw decompression
		var str= Int8Array.fill(arr.size*8, {|i| arr[i.div(8)].rightShift(i.mod(8)).bitAnd(1)});
		var old, val, out= List.new, sub, more= true, cnt;
		var k, tempCodeSize= codeSize+1, index= 0;
		clearCode= 2.leftShift(codeSize-1);
		endCode= clearCode+1;
		while({more}, {
			k= 0;
			tempCodeSize.do{|i|
				k= k+(str[index]*2.leftShift(i-1));
				index= index+1;
			};
			if(k==clearCode, {
				this.prInitDict;
				cnt= endCode+1;
				tempCodeSize= codeSize+1;
				if(index>tempCodeSize, {			//first time do not shift position
					index= index+3;
				});
			}, {
				if(k==endCode, {
					more= false;
				}, {
					if(dict[k].notNil, {
						sub= dict[k];
					}, {
						sub= dict[old]++val;
					});
					out.addAll(sub);
					val= sub[0];
					if(old.notNil, {
						dict.put(cnt, dict[old]++val);
						cnt= cnt+1;
					});
					old= k;
					if(cnt==2.leftShift(tempCodeSize-1), {
						tempCodeSize= tempCodeSize+1;
						if(tempCodeSize==13, {
							tempCodeSize= codeSize+1;
							old= nil;
							val= nil;
						});
					});
				});
			});
		});
		^out;
	}
}

RedGIFImage {
	var <bounds, <flags, <>colorMap, <>data, <>control;
	*new {|bounds, flags| ^super.newCopyArgs(bounds, flags)}
	depth {^flags.bitAnd(0x07)+1}
	hasColorMap {^flags.bitTest(7)}
	interlaced {^flags.bitTest(6)}
	sorted {^flags.bitTest(5)}
}

RedGIFControl {
	var	<>duration,							//integer
	<>transparent,							//color
	<>disposalMethod,						//integer
	<>userInputFlag,						//boolean
	<>transparentFlag;					//boolean
}
