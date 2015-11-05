document.onmouseup = addYeebox;
function addYeebox(ev){
	var selectText = getSelectionText();
	if(selectText.replace(/(^\s*)|(\s*$)/g, "").length>0){
		/*var ev=ev||window.event,left=ev.clientX,top=ev.clientY;
		var htmlText = '<div id="yeebox" name="yeebox" style="border: 1px solid red; z-index:2147483647; width:200px;'+
		' height:150px; position:absolute; left:'+left+'px; top:'+top+'px;">12345</div>';
		var divElement = document.createElement(htmlText);
		document.body.appendChild(divElement);*/
		var yeebox = document.getElementById('yeebox');
		if(yeebox!=null){
			yeebox.parentNode.removeChild(yeebox);
		}
		var divElement = document.createElement('div');
		divElement.charset = 'utf-8';
		
		var ev=ev||window.event;
		var x=0;
		var y=0;
		if(ev.pageX || ev.pageY){
			x = ev.pageX;
			y = ev.pageY;
		} else{
			x = ev.clientX + document.body.scrollLeft - document.body.clientLeft;
			y = ev.clientY + document.body.scrollTop - document.body.clientTop;
		}

		left=ev.clientX,top=ev.clientY;
		document.body.appendChild(divElement);
		divElement.id = 'yeebox';
		divElement.name = 'yeebox';
		divElement.style.position = "absolute";
		divElement.style.left = x + 'px';
		divElement.style.top = y + 'px';
		divElement.style.height = '200px';
		divElement.style.width = '350px';
		divElement.style.zIndex = 2147483647;
		divElement.style.border = 'solid 1px #0093D5';
		divElement.style.background='#FFFFFF';
		divElement.style.fontSize='12px';
		divElement.style.borderRadius='5px';
		divElement.style.boxShadow='0px 0px 5px #0093D5';
		divElement.innerHTML='<div style="margin-top:5px;padding-left:5px;">选择语言：<select id="targetLanguage">'+
			'<option value="en">英文</option>'+
			'<option value="zh">中文</option>'+
		'</select></div><div id="transRes" style="margin:0 auto;margin-top:10px;padding-left:5px;padding-top:5px;font-size:14px;width:93%;height:150px;overflow:auto;border:1px solid lightgrey;border-radius:5px;cursor:text;"><div style="color:gray;margin-top:60px;text-align:center;font-size:12px;">翻译中 . . . </div></div>';
		divElement.onmousemove = function(){
			document.onclick="";
			document.onmouseup="";
		};
		divElement.onmouseout = function(){
			document.onmouseup = addYeebox;
		};
		
		//根据浏览器设置的语言设置目标语言
		var target = window.navigator.userLanguage || window.navigator.language;
		if(target == "zh-CN" || target == "zh")
			document.getElementById("targetLanguage")[1].selected=true;
		else
			document.getElementById("targetLanguage")[0].selected=true;
		
		//翻译
		transSelectText(selectText);
	}else{
		//监听到鼠标在yeebox里时，不关闭box
		var is_close = true;
		var yeebox = document.getElementById('yeebox');
		if(yeebox!=null){
			yeebox.addEventListener("mousemove", function(){
				is_close = false;
			}, false);
		}
		//不在yeebox里，点击其他地方时，关闭box
		if(is_close){
			removeYeebox();
		}
	}
}

//翻译选中内容
function transSelectText(selectText){
	$.ajax({
		type : "POST",
		url : 'http://54.64.40.131:9006/detection',
		contentType : "application/json",
		crossDomain: true,
		data : {
			text : selectText
		},
		dataType : "jsonp",
		jsonp : "jsoncallback",
		success : function(json) {
			var _source = json.result;
			var _target = document.getElementById("targetLanguage").value;
			trans(_source,_target,selectText);
		},
		error : function(XMLHttpRequest, textStatus, errorThrown){
			document.getElementById('yeebox').innerHTML='<div style="font-size:16px; text-align:center;">远程接口调用失败，获取源语言失败!</div>';
		}
	});
}
//翻译
function trans(sour,tar,text){
	$.ajax({
		type : "POST",
		url : 'http://translateport.yeekit.com/translate',
		contentType : "application/json",
		crossDomain : true,
		data : {
			"srcl" : sour,
			"tgtl" : tar,
			"text" : text
		},
		dataType : "jsonp",
		jsonp : "jsoncallback",
		success : function(json) {
			var translation = "";
			if (json) {
				var resTranslation = json.translation[0];
				for (var i = 0; i < resTranslation.translated.length; i++) {
					translation += resTranslation.translated[i].text;
				}
			}
			document.getElementById('transRes').innerHTML = translation;
			
			//监听选择其他语言
			document.getElementById("targetLanguage").addEventListener("change", function(){
					changeLanguage(sour,text);
			}, false);
		},
		error : function(XMLHttpRequest, textStatus, errorThrown){
			document.getElementById('yeebox').innerHTML='<div style="font-size:16px; text-align:center;">远程接口调用失败，翻译失败!</div>';
		}
	});
}

//选择其他语言
function changeLanguage(sour,text){
	var newTarget = document.getElementById("targetLanguage").value;
	if(sour==newTarget){
		document.getElementById('transRes').innerHTML = text;
	}else{
		trans(sour,newTarget,text);
	}
}

//移除插件
function removeYeebox(){
	var createYeeboxJs2 = document.getElementById('createYeeboxJs');
	if (createYeeboxJs2 != null) {
		createYeeboxJs2.parentNode.removeChild(createYeeboxJs2);
	}

	var yeebox = document.getElementById('yeebox');
	if (yeebox != null) {
		yeebox.parentNode.removeChild(yeebox);
	}
}

//获取选中文本
function getSelectionText() {
	if (window.getSelection) {
		return window.getSelection().toString();
	} else if (document.selection && document.selection.createRange) { //if is IE
		return copytext_keleyi_com = document.selection.createRange().text;
	}
	return '';
}