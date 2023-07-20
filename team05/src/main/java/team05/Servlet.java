package team05;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

@WebServlet("/Search")
public class Servlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		File indexDir = new File("C:\\Users\\ngowe\\eclipse-workspace\\React\\src\\indexDir");

		LuceneSearch search = new LuceneSearch();
		String queryString = req.getQueryString();

		try {
//			JSONArray result = search.searchIndex(indexDir, queryString);
//			String jsonResult = result.toString();
//			resp.setContentType("application/json");
//			resp.getWriter().write(jsonResult);

		} catch (Exception e) {
			resp.getWriter().write(e.toString());
		}
	}
}
