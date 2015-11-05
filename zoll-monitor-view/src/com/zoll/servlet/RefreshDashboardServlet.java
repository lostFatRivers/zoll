package com.zoll.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateFormatUtils;

import com.zoll.db.LoadData;

@SuppressWarnings("serial")
public class RefreshDashboardServlet extends HttpServlet {
	/** 刷新时间频率 */
	public static final int TIME_SPACE = 1000 * 60;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		String time = (String) req.getParameter("times");
		
		String[] dateRange = dateRange(Long.parseLong(time));
		
		String dataType = (String) req.getParameter("type");
		
		System.out.println("start:" + dateRange[0] + "end:" + dateRange[1] + "; dataType:" + dataType);
		
		int count = LoadData.getInstance().getTimeUnitDataCount(dateRange[0], dateRange[1], dataType);
		
		PrintWriter writer = resp.getWriter();
		writer.write(String.valueOf(count));
		writer.close();
	}

	public static String[] dateRange(long time) {
		String end = DateFormatUtils.format(new Date(time), "yyyy-MM-dd HH:mm:ss");
		String start = DateFormatUtils.format(new Date(time - TIME_SPACE), "yyyy-MM-dd HH:mm:ss");
		return new String[] {start, end};
	}
}
