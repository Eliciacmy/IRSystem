package team05;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
@WebServlet("/Search")
public class Servlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get the query string from the request parameter
        String queryString = req.getQueryString();

        // Initialize a JSON array to store the search results
        JSONArray result = null;

        try {
            // Perform the search query using LuceneSearch class and get the results in a JSON array
            result = LuceneSearch.searchQuery(queryString);

        } catch (Exception e) {
            // If an error occurs during the search, create a JSON object with the error message
            JSONObject errorObject = new JSONObject();
            errorObject.put("Error", e.toString());

            // Create a JSON array with the error object
            result = new JSONArray();
            result.put(errorObject);
        }

        // Convert the JSON array to a string
        String jsonResult = result.toString();

        // Set the response content type to indicate that the response is in JSON format
        resp.setContentType("application/json");

        // Write the JSON result to the response
        resp.getWriter().write(jsonResult);
    }
}
