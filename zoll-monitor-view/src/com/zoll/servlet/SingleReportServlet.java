package com.zoll.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;

import com.zoll.db.LoadData;

@SuppressWarnings("serial")
public class SingleReportServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		
		String times = (String) req.getParameter("times");
		String dataType = (String) req.getParameter("type");
		
		System.out.println(dataType + " " + times);
		PrintWriter writer = resp.getWriter();
		
		List<Integer> datas = new ArrayList<Integer>();
		for (int i = 0; i < 60; i++) {
			String[] dateRange = RefreshDashboardServlet.dateRange(Long.parseLong(times) - RefreshDashboardServlet.TIME_SPACE * (60 - i));
			int count = LoadData.getInstance().getTimeUnitDataCount(dateRange[0], dateRange[1], dataType);
			datas.add(count);
		}
		
		JSONArray ja = JSONArray.fromObject(datas);
		
		writer.print(ja.toString());
		
		writer.close();
	}
	
}
