import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.gson.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/products/*")
public class products extends HttpServlet{

	Database db = new Database();
	
	public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("application/json");

		String json = db.request_reader(req);
		PrintWriter out = res.getWriter();
		
		Gson gson = new Gson();
		Product product = gson.fromJson(json, Product.class);

		
//		out.println("Product Details - " + product.product_id);product_id

		try {
			db.init();
			PreparedStatement query = db.con.prepareStatement("Update products set product_id=?,product_name=?,selling_price=?,created_time=? where product_id=?");
			query.setInt(1, product.product_id);
			query.setString(2, product.product_name);
			query.setInt(3, product.product_price);
			query.setTimestamp(4,null);
			query.setInt(5, product.product_id);

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
	
public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("application/json");

		String json = db.request_reader(req);
		PrintWriter out = res.getWriter();

		Gson gson = new Gson();
		Product product = gson.fromJson(json, Product.class);
		
		try {
			db.init();
//
			PreparedStatement query = db.con.prepareStatement("Insert into products(product_id,product_name,selling_price,created_time) values(?,?,?,?)");
			query.setInt(1, product.product_id);
			query.setString(2, product.product_name);
			query.setInt(3, product.product_price);
			query.setTimestamp(4,null);
			
			query.executeUpdate();
			
			out.print("Successfully Inserted");
			db.con.close();
		} catch(Exception e){
			System.out.println(e);
//			res.sendError(409);
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		try {
			db.init();

			String id = request.getPathInfo();
//			System.out.println(id);
			if(id==null){
				PreparedStatement query = db.con.prepareStatement("Select * from products");
				ResultSet result = query.executeQuery();
				JsonArray products = new JsonArray();
				Product product = new Product();
				JsonElement gson;
				while (result.next()){
					product.product_id = result.getInt("product_id");
					product.product_name = result.getString("product_name");
					product.product_price = result.getInt("selling_price");
					gson = new Gson().toJsonTree(product);
					products.add(gson);
				}
				out.println(products);
				out.flush();
			}
			else{
				id=id.split("/")[1];
//				System.out.println(id);
				PreparedStatement query = db.con.prepareStatement("Select * from products where product_id=?");
				query.setString(1,id);
				ResultSet result = query.executeQuery();
				result.next();
				Product product = new Product();
				product.product_id = result.getInt("product_id");
				product.product_name = result.getString("product_name");
				product.product_price = result.getInt("selling_price");
				String gson = new Gson().toJson(product);
				out.println(gson);
			}
			
//			out.print("End");
			db.con.close();
		} catch(Exception e){
			System.out.println(e);
		}
		
	}

	public class Product{
		int product_id;
		String product_name;
		int product_price;
	}
}


//	public static void main(String args[]) throws SQLException, ClassNotFoundException {
////		Class.forName("com.mysql.jdbc.Driver");
//		Connection con=DriverManager.getConnection("jdbc:mysql://localhost:3306/Invoice_App","root","");
//
//
//		String query = "Select * from products where product_id=1";
//		Statement s = con.createStatement();
//		ResultSet r = s.executeQuery(query);
//
//		r.next();
//		System.out.println(r.getString("selling_price"));
//		s.close();con.close();
//	}