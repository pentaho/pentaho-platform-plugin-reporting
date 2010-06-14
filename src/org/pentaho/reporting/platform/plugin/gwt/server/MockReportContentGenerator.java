package org.pentaho.reporting.platform.plugin.gwt.server;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class MockReportContentGenerator extends HttpServlet {

	public MockReportContentGenerator() {
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String queryString = req.getQueryString();
			URL url = new URL("http://localhost:8080/pentaho/content/reporting?userid=joe&password=password&" + queryString);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			conn.connect();
			resp.setContentType(req.getParameter("output-type"));
			IOUtils.copy(req.getInputStream(), conn.getOutputStream());
			IOUtils.copy(conn.getInputStream(), resp.getOutputStream());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
