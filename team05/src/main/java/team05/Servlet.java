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
import org.json.JSONObject;

@WebServlet("/Search")
public class Servlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    LuceneSearch search = new LuceneSearch();
    String queryString = req.getQueryString();
    JSONArray result = null;
     
    try {
      //JSONObject responseObject = new JSONObject();
      result = LuceneSearch.searchQuery(queryString);
      
      //System.out.println(result);
      for (int i = 0; i < result.length(); i++) {
        JSONObject resultObject = result.getJSONObject(i);
        String title = resultObject.getString("SuggestedWord");
        //System.out.println(title);
    }

    } catch (Exception e) {
       JSONObject jsonObject = new JSONObject();    
      result.put(jsonObject.put("Error", e.toString()));
    }
    String jsonResult = result.toString();
    resp.setContentType("application/json");
    resp.getWriter().write(jsonResult);
  }
}