import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.Instant;

@WebServlet("/customers/*")
public class customers extends HttpServlet {

    Database db = new Database();

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        db.init();
        res.setContentType("application/json");

        String json = db.request_reader(req);
        PrintWriter out = res.getWriter();

        Gson gson = new Gson();
        Customer customer = gson.fromJson(json, Customer.class);

        Instant inst = Instant.now();
        System.out.println(inst);

        try {
            PreparedStatement query = db.con.prepareStatement("Insert into customers(customer_id,customer_name,created_time,customer_balance) values(?,?,?,null)");
            query.setInt(1, customer.customer_id);
            query.setString(2, customer.customer_name);
            query.setTimestamp(3, Timestamp.from(inst) );

            int rs = query.executeUpdate();
            if(rs==0) {
                out.println("Update Unsuccesful");
            }
            else {
                out.print("Successfully Updated");
            }
        } catch(Exception e){
            System.out.println(e);
        }
    }

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            // TODO Auto-generated method stub
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();

            db.init();
            try {
                String id = request.getPathInfo();
                if(id==null){
                    PreparedStatement query = db.con.prepareStatement("Select * from customers");
                    ResultSet result = query.executeQuery();
                    JsonArray customers = new JsonArray();
                    Customer customer = new Customer();
                    JsonElement gson;
                    while (result.next()){
                        customer.customer_id = result.getInt("customer_id");
                        customer.customer_name = result.getString("customer_name");
                        gson = new Gson().toJsonTree(customer);
                        customers.add(gson);
                    }
                    out.println(customers);
                }
                else{
                    id=id.split("/")[1];
                    PreparedStatement query = db.con.prepareStatement("Select * from customers where customer_id=?");
                    query.setString(1,id);
                    ResultSet result = query.executeQuery();
                    result.next();
                    Customer customer = new Customer();
                    customer.customer_id = result.getInt("customer_id");
                    customer.customer_name = result.getString("customer_name");
                    String gson = new Gson().toJson(customer);
                    out.println(gson);
                }

    //			out.print("End");
                db.con.close();
            } catch(Exception e){
                System.out.println(e);
            }

        }



    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        db.init();
        String json = db.request_reader(req);
        PrintWriter out = res.getWriter();

        Gson gson = new Gson();
        Customer customer = gson.fromJson(json, Customer.class);

        try {

            PreparedStatement query = db.con.prepareStatement("Update customers set customer_id=?,customer_name=? where customer_id=?");
            query.setInt(1, customer.customer_id);
            query.setString(2, customer.customer_name);
            query.setInt(3, customer.customer_id);

            int rs =  query.executeUpdate();
            if(rs==0) {
                out.println("Update Unsuccesful");
            }
            else {
                out.print("Successfully Updated");
            }

            db.con.close();
        } catch(Exception e){
            System.out.println(e);
        }
    }

    public class Customer{
        int customer_id;
        String customer_name;
    }

}
