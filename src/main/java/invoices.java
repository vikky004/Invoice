import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.RequestDispatcher;
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
import java.util.Date;

@WebServlet("/invoices/*")
public class invoices extends HttpServlet {

    Database db = new Database();

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        db.init();
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        //reading the payment fields from request
        String json = db.request_reader(req);
        Gson gson = new Gson();
        Invoice invoices = gson.fromJson(json, Invoice.class);
        ProductInvoice productinvoice;

        Instant inst = Instant.now();

        try {
            PreparedStatement query;
            JsonObject data = gson.fromJson(json, JsonObject.class);
            JsonArray products = data.getAsJsonArray("products");
            JsonObject object;
            JsonElement object_id;

            //Check for products in the product-table
            for(int i = 0; i<products.size(); i++){
                object = products.get(i).getAsJsonObject();
                object_id = object.get("product_id");
                query = db.con.prepareStatement("Select product_id from products where product_id=?");
                query.setInt(1,object_id.getAsInt());
                ResultSet rs = query.executeQuery();
                if(!rs.next()){
                    res.sendError(400);
                    return;
                }
            }

            //Add values in product table
            query = db.con.prepareStatement("Insert into invoices(invoice_subtotal,invoice_date,discount_percentage,discount_value,adjustments_value,tax_type,tax_percentage,invoice_total,customer_id) values(0,?,?,0,?,?,?,0,?)",Statement.RETURN_GENERATED_KEYS);
            query.setTimestamp(1, Timestamp.from(inst) );
            query.setInt(2,invoices.discount_percentage);
            query.setFloat(3,invoices.adjustment_value);
            query.setString(4,invoices.tax_type);
            query.setInt(5,invoices.tax_percentage);
            query.setInt(6,invoices.customer_id);

            query.executeUpdate();
            ResultSet keyset = query.getGeneratedKeys();
            keyset.next();
            int key = keyset.getInt(1);
//            out.println(key);

            //add values in product-invoice table
            float invoice_subtotal=0;
            ResultSet resultSet;
            JsonElement product_element; JsonArray product_array = new JsonArray();
            for(int i = 0; i<products.size(); i++) {
                query = db.con.prepareStatement("Insert into product_invoice(product_id,invoice_id,quantity,subtotal,tax_name,tax_percentage,tax_amount,total) values(?,?,?,?,?,?,?,?)");

                object = products.get(i).getAsJsonObject();
                productinvoice = gson.fromJson(object,ProductInvoice.class);

                //fetch selling price and
                PreparedStatement query2 = db.con.prepareStatement("Select selling_price from products where product_id=?");
                query2.setInt(1,productinvoice.product_id);
                resultSet = query2.executeQuery();
                resultSet.next();
                productinvoice.subtotal = productinvoice.product_quantity* (resultSet.getInt("selling_price"));
                productinvoice.tax_amount = productinvoice.subtotal*((float) productinvoice.tax_percentage /100);
                productinvoice.total = productinvoice.subtotal + productinvoice.tax_amount;
                invoice_subtotal += productinvoice.total;
                query.setInt(1, productinvoice.product_id);
                query.setInt(2, key);
                query.setInt(3, productinvoice.product_quantity);
                query.setFloat(4,productinvoice.subtotal);
                query.setString(5, productinvoice.tax_name);
                query.setInt(6, productinvoice.tax_percentage);
                query.setFloat(7, productinvoice.tax_amount);
                query.setFloat(8,productinvoice.total);

                product_element = new Gson().toJsonTree(productinvoice);
                product_array.add(product_element);
                query.executeUpdate();
            }

            //Update fields in Invoice Table
            float discount_value = invoice_subtotal*((float) invoices.discount_percentage /100);
            float tax_value = invoice_subtotal*((float) invoices.tax_percentage /100);
            PreparedStatement query3 = db.con.prepareStatement("Update invoices set invoice_subtotal=?,discount_value=?,invoice_total=?, tax_amount=? where invoice_id=?");
            query3.setFloat(1,invoice_subtotal);
            query3.setFloat(2,discount_value);
            query3.setFloat(3,invoice_subtotal-discount_value+tax_value);
            query3.setFloat(4,tax_value);
            query3.setInt(5,key);
            query3.executeUpdate();

            //Query the db to send response with updated fields
            query3 = db.con.prepareStatement("Select * from invoices where invoice_id=?");
            query3.setInt(1,key);
            ResultSet result = query3.executeQuery();
            result.next();
            Invoice out_invoices = new Invoice(result.getInt("invoice_id"),result.getTimestamp("invoice_date"),result.getInt("invoice_subtotal"),result.getInt("discount_percentage"),result.getInt("discount_value"),result.getInt("adjustments_value"),result.getString("tax_type"),result.getInt("tax_percentage"),result.getInt("tax_amount"), result.getInt("invoice_total"), result.getInt("customer_id") );
            String gson_invoices = new Gson().toJson(out_invoices);

            //Add the gson_invoices and product_array(Json array of associated products) to send response
            JsonObject out_object = new Gson().fromJson(gson_invoices,JsonObject.class);
            out_object.add("products",product_array);
            out.println(out_object);

        } catch(Exception e){
            System.out.println(e);
        }
        res.setStatus(201);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");

        db.init();
        try {

            String id = request.getPathInfo();
            if(id==null){
                PreparedStatement query = db.con.prepareStatement("Select * from invoices");
                ResultSet result = query.executeQuery();
                JsonArray invoice_array = new JsonArray();
                Invoice invoices;
                JsonElement gson;
                while (result.next()){
                    invoices = new Invoice(result.getInt("invoice_id"),result.getTimestamp("invoice_date"),result.getInt("invoice_subtotal"),result.getInt("discount_percentage"),result.getInt("discount_value"),result.getInt("adjustments_value"),result.getString("tax_type"),result.getInt("tax_percentage"),result.getInt("tax_amount"), result.getInt("invoice_total"), result.getInt("customer_id") );
                    gson = new Gson().toJsonTree(invoices);
                    invoice_array.add(gson);
                }
                out.println(invoice_array);
            }
            else{
                id=id.split("/")[1];
                PreparedStatement query = db.con.prepareStatement("Select * from invoices where invoice_id=?");
                query.setString(1,id);
                ResultSet result = query.executeQuery();
                if(!result.next()){
                    response.sendError(404);
                    return;
                }
                Invoice invoices = new Invoice(result.getInt("invoice_id"),result.getTimestamp("invoice_date"),result.getInt("invoice_subtotal"),result.getInt("discount_percentage"),result.getInt("discount_value"),result.getInt("adjustments_value"),result.getString("tax_type"),result.getInt("tax_percentage"),result.getInt("tax_amount"), result.getInt("invoice_total"), result.getInt("customer_id") );
                String gson = new Gson().toJson(invoices);

                query = db.con.prepareStatement("Select * from product_invoice where invoice_id=?");
                query.setString(1,id);
                result = query.executeQuery();
                ProductInvoice productInvoice; JsonElement gson_object;
                JsonArray products_array = new JsonArray();
                while(result.next()){
                    productInvoice = new ProductInvoice(result.getInt("product_id"),result.getInt("quantity"),result.getInt("subtotal"),result.getString("tax_name"),result.getInt("tax_percentage"),result.getInt("tax_amount"),result.getInt("total"));
                    gson_object = new Gson().toJsonTree(productInvoice);
                    products_array.add(gson_object);
                }
                JsonObject out_object = new Gson().fromJson(gson,JsonObject.class);
                out_object.add("products",products_array);
                out.println(out_object);
            }
        }catch (Exception e){
            System.out.println(e);
            }
        }

    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        db.init();
        String json = db.request_reader(req);
        PrintWriter out = res.getWriter();

        Gson gson = new Gson();
        ProductInvoice productInvoice = gson.fromJson(json, ProductInvoice.class);

        try {
            PreparedStatement query = db.con.prepareStatement("Update customers set customer_id=?,customer_name=? where customer_id=?");

        }
        catch (Exception e){
            System.out.println(e);
        }
    }


            public class Invoice{
        int invoice_id;
        Timestamp invoice_date;
        int invoice_sub_total;
        int discount_percentage;
        int discount_value;
        int adjustment_value;
        String tax_type;
        int tax_percentage;
        int tax_amount;
        int invoice_total;
        int customer_id;

            public Invoice(int invoice_id, Timestamp invoice_date, int invoice_sub_total, int discount_percentage, int discount_value, int adjustment_value, String tax_type, int tax_percentage, int tax_amount, int invoice_total, int customer_id) {
                this.invoice_id = invoice_id;
                this.invoice_date = invoice_date;
                this.invoice_sub_total = invoice_sub_total;
                this.discount_percentage = discount_percentage;
                this.discount_value = discount_value;
                this.adjustment_value = adjustment_value;
                this.tax_type = tax_type;
                this.tax_percentage = tax_percentage;
                this.tax_amount = tax_amount;
                this.invoice_total = invoice_total;
                this.customer_id = customer_id;
            }
        }

    public class ProductInvoice{
        int product_id;
        int product_quantity;
        int subtotal;
        String tax_name;
        int tax_percentage;
        float tax_amount;
        float total;

        public ProductInvoice(int product_id, int product_quantity, int subtotal, String tax_name, int tax_percentage, int tax_amount, int total) {
            this.product_id = product_id;
            this.product_quantity = product_quantity;
            this.subtotal = subtotal;
            this.tax_name = tax_name;
            this.tax_percentage = tax_percentage;
            this.tax_amount = tax_amount;
            this.total = total;
        }
    }

}
