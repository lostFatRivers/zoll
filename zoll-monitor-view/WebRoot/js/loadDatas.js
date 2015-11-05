var oTimer = null; 														// oTimer用来保存一个interval对象
$(function() {
	if ($("#demo-list").size() > 1) {
		alert("1");
	} else {
		$.ajax({														// 使用 ajax 加载初始 left_list 列表;
			url : "AllReportTypeServlet",
			type : "POST",
			dataType : "json",
			data : "platform_name=" + $('.player_id').val(),			// 没用其实;
			success : function(msg) {
				$.each(msg, function(commentIndex, comment) {			// 返回的 msg 是一个List, 使用$.each()来遍历它;
					if (comment != null && $('#hide-dashboard-type').text() == "default") {
						$('#hide-dashboard-type').text(comment);		// 一个隐藏span元素用来保存列表第一个的值
					}
					var newRow = "<li> <a id=list_" + comment + " href=\"javascript:void(0);\"  onclick=\"onChangeType('"+ comment + "')\"> <i class=\"fa fa-tag\"></i>"
							+ comment + "</li>";
					$("#demo-list li:last").after(newRow);				// 在<ul>的尾部再添加<li>元素
				});
				loadInitData($('#hide-dashboard-type').text());			// 左侧菜单部分加载完成后, 调用loadInitData加载图表的初始数据
			}
		});
	}
});

function loadInitData(dashboardType) {
	$.ajax({															// 还是使用 ajax 加载初始化图表用的数据 data;
		url : "SingleReportServlet",
		type : "POST",
		dataType : "json",
		data : "times=" + (new Date()).getTime() + "&"
				+ "type=" + dashboardType,								// 传入当前时间和 dataType;
		success : function(msg) {
			var data = new Array();									
			$.each(msg, function(commentIndex, comment) {
				data.push(comment);
			});
			painChart($('#hide-dashboard-type').text(), data);			// 调用painChart方法, 将数据 data 传入, 绘制图表;
		}
	});
}

function painChart(dashboardType, initDatas) {							// 大部分是直接拷来用的, 改了小部分;
	Highcharts.setOptions({
		global : {
			useUTC : false
		}
	});
	$('#right-view').highcharts({
		chart : {
			type : 'spline',
			animation : Highcharts.svg,
			marginRight : 10,
			events : {
				load : function() {
					var series = this.series[0];
					oTimer = setInterval(								// 在这里添加一个 windows 元素 interval, 开始还以为是这个工具自带的, 查文档半天没查到;
						// 配置刷新时间;
						function() {
							$.ajax({
								url : "RefreshDashboardServlet",
								type : "POST",
								dataType : "json",
								data : "times=" + (new Date()).getTime() + "&"
									+ "type=" + dashboardType,
								success : function(msg) {
									var x = (new Date()).getTime(); 
									if(msg != ""){
										y = msg;
									} else {
										y = Math.random();
									}
									series.addPoint([x,y], true, true);
								}
							});
							
						}, 60000);										// 每一分钟刷新一次
				}
			}
		},
		title : {
			text : dashboardType + ' / min'
		},
		xAxis : {
			title : {
				text : 'time'
			},
			type : 'datetime',
			tickPixelInterval : 100
		},
		yAxis : {
			title : {
				text : dashboardType + ' / min'
			},
			plotLines : [ {
				value : 0,
				width : 1,
				color : '#808080'
			} ]
		},
		tooltip : {
			formatter : function() {
				return '<b>'
						+ this.series.name + '=' + Highcharts
						.numberFormat(
								this.y,
								2)
						+ '</b><br/>'
						+ Highcharts
								.dateFormat(
										'%Y-%m-%d %H:%M:%S',
										this.x);
			}
		},
		credits: {
            enabled : false
		},
		legend : {
			enabled : false
		},
		exporting : {
			enabled : false
		},
		series : [ {													// 显示在图表上的线, 将来要添加多个线条应该也是在这里;
			name : dashboardType + ' data',
			data : (function() {
				// generate an array of
				// random data
				var data = [], 
				time = (new Date()).getTime(), 
				i;
				for (i = -59; i <= 0; i++) {
					yPoint = Math.random();
					if(initDatas[i + 59] != "undefined" && initDatas[i + 59] != null) {
						yPoint = initDatas[i + 59];
					}
					data.push({
						x : time + i * 60000,
						y : yPoint
					});
				}
				return data;
			})()
		}]
	});
}

function onChangeType(dashboardType) {
	var chart = $("#right-view").highcharts();
	chart.series[0].remove;												// 移除现有的线条;
	window.clearInterval(oTimer);										// 清除所有的 interval 任务;
	$('#hide-dashboard-type').text(dashboardType);
	loadInitData(dashboardType);
}