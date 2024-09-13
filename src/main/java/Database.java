
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    Connection con;
    public void init() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/Invoice_App", "root", "");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public String request_reader(HttpServletRequest request) throws IOException, ServletException {
        StringBuilder reqString = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while((line=reader.readLine())!=null) {
            reqString.append(line);
        }
        String json = reqString.toString();
        return json;
    }

}
