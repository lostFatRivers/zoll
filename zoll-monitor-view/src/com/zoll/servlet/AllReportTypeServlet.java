package com.zoll.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;

import com.zoll.db.LoadData;

@SuppressWarnings("serial")
public class AllReportTypeServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		List<String> types = LoadData.getInstance().getAllReportType();
		System.out.println(types);
		JSONArray ja = JSONArray.fromObject(types);
		responseMsg(ja.toString(), resp);
	}
	
	private void responseMsg(String msg, HttpServletResponse resp) throws IOException {
		PrintWriter writer = resp.getWriter();
		if (msg == null) {
			writer.print("");
		} else {
			writer.print(msg);
		}
		writer.close();
	}
}
