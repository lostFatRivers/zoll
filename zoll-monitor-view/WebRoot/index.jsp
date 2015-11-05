<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<title>the report of games</title>
		<link rel="stylesheet"  type="text/css"  href="css/jquery-accordion-menu.css"/>
		<link rel="stylesheet"  type="text/css"  href="css/font-awesome.css"/>
		<link rel="stylesheet"  type="text/css"  href="css/own-style.css"/>
		<script type="text/javascript" src="js/jquery-1.11.2.min.js"></script>
		<script type="text/javascript" src="js/jquery-accordion-menu.js"></script>
		<script type="text/javascript" src="js/highcharts.js"></script>
		
		<!-- 自己写的页面加载用的js -->
		<script type="text/javascript" src="js/loadDatas.js"></script>
	</head>
	
	<body>
		<div id="all-dashboard">
			<div id="left-menus">
			
				<!-- "hide-dashboard-type"用来保存将要显示的图表的dataType -->
				<span id="hide-dashboard-type" style="display: none;">default</span>
				
				<!-- 使用jquery-accordion-menu.css生成的一个漂亮的菜单, 样式都是自带的 -->
				<div id="jquery-accordion-menu" class="jquery-accordion-menu blue">
					<div id="form" class="jquery-accordion-menu-header">
						<form class="filterform" action="#">
							<input class="filterinput" type="text">
						</form>
					</div>
					<ul id="demo-list">
						<li>
							<a href="#">
								
								<!-- 能显示各种图标, 用的是font-awesome.css -->
								<i class="fa fa-home"></i>
								Home
							</a>
						</li>
					</ul>
				</div>
			</div>
			<div id="right-view">
				
			</div>
		</div>
	</body>
</html>