package team05;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.json.JSONArray;

@WebServlet("/Search")
public class Servlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		LuceneSearch search = new LuceneSearch();
		String queryString = req.getQueryString();
		IndexSearcher searcher = search.createSearcher();
		
		TopDocs topDocs = null;
		try {
			topDocs = search.searchInContent(queryString, searcher);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			JSONArray result = search.searchIndex(topDocs, searcher);
			String jsonResult = result.toString();
			resp.setContentType("application/json");
			resp.getWriter().write(jsonResult);

		} catch (Exception e) {
			resp.getWriter().write(e.toString());
		}
	}
}
