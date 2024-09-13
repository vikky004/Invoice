import com.google.gson.*;
import com.sun.org.apache.xpath.internal.objects.XString;
import sun.font.CStrike;
import sun.util.calendar.BaseCalendar;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.parser.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;

@WebServlet("/payments/*")
public class payments extends HttpServlet {

    Database db = new Database();
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        db.init();
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        //reading the payment fields from request
        String json = db.request_reader(req);
        Gson gson = new Gson();
        Payment payment = gson.fromJson(json, Payment.class);

        //read invoices related to payments
        JsonObject data = gson.fromJson(json, JsonObject.class);
        JsonArray invoices = data.getAsJsonArray("invoices");
        JsonObject object;
        JsonElement object_id;

        try {
            db.con.setAutoCommit(false);
            PreparedStatement query,query2,batchQuery;

            query = db.con.prepareStatement("Insert into payments(payment_date,payment_value,payment_type) values(?,?,?)",Statement.RETURN_GENERATED_KEYS);
            query.setDate(1,Date.valueOf(payment.payment_date));
            query.setInt(2,payment.payment_value);
            query.setString(3, payment.payment_type);
            query.executeUpdate();
            ResultSet keyset = query.getGeneratedKeys();
            keyset.next();
            int key = keyset.getInt(1);

            int payment_value = 0;
            batchQuery = db.con.prepareStatement("Insert into invoice_payment(invoice_id,payment_value,payment_id) values(?,?,?)");

            for(int i = 0; i<invoices.size(); i++){
                object = invoices.get(i).getAsJsonObject();
                object_id = object.get("invoice_id");
                query = db.con.prepareStatement("Select invoice_id, invoice_total from invoices where invoice_id=?");
                query.setInt(1,object_id.getAsInt());
                query2 = db.con.prepareStatement("Select payment_value from invoice_payment where invoice_id=?");
                query2.setInt(1,object_id.getAsInt());
                ResultSet rs_p_value = query2.executeQuery();
                ResultSet rs = query.executeQuery();
                if(!rs.next()){
                    response_message rm = new response_message(false, "Invoice id "+ object_id.getAsInt() + " is not valid",400);
                    String r_obj = new Gson().toJson(rm,response_message.class);
                    JsonObject out_object = new Gson().fromJson(r_obj,JsonObject.class);
                    out_object.add("data",new JsonObject());
                    out.println(out_object);
                    res.setStatus(400);
                    return;
                }
                int total_payments=0;
                while(rs_p_value.next()){
                    total_payments += rs_p_value.getInt("payment_value");
                }
                if(!(rs.getInt("invoice_total")>=object.get("payment_value").getAsInt()+total_payments)){
                    response_message rm = new response_message(false, "Payment value error in invoice id "+ object_id.getAsInt(),400);
                    String r_obj = new Gson().toJson(rm,response_message.class);
                    JsonObject out_object = new Gson().fromJson(r_obj,JsonObject.class);
                    out_object.add("data",new JsonObject());
                    out.println(out_object);
                    res.setStatus(400);
                    return;
                }
                if(!(rs.getInt("invoice_total")>=object.get("payment_value").getAsInt())){
                    res.sendError(400);
                    return;
                }
                payment_value+=object.get("payment_value").getAsInt();
                System.out.println(object.get("payment_value").getAsInt());
                batchQuery.setInt(1,rs.getInt("invoice_id"));
                batchQuery.setInt(2,object.get("payment_value").getAsInt());
                batchQuery.setInt(2,object.get("payment_value").getAsInt());
                batchQuery.setInt(3,key);
                batchQuery.addBatch();
            }
            batchQuery.executeBatch();
            query = db.con.prepareStatement("Update payments set payment_value=? where payment_id=?");
            query.setInt(1,payment_value);
            query.setInt(2,key);
            query.executeUpdate();

            db.con.commit();

        } catch(Exception e){
            System.out.println(e);
        }
    }

    public class Payment{
        int payment_id;
        String payment_date;
        int payment_value;
        String payment_type;

        public Payment(int payment_id, String payment_date, int payment_value, String payment_type) {
            this.payment_id = payment_id;
            this.payment_date = payment_date;
            this.payment_value = payment_value;
            this.payment_type = payment_type;
        }
    }

    public class PaymentInvoice{
        int invoice_id;
        int payment_value;
    }
}
