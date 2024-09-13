import com.google.gson.JsonArray;

public class response_message {
    boolean success;
    String message;
    int error_code;

    public response_message(boolean success, String message, int error_code) {
        this.success = success;
        this.message = message;
        this.error_code = error_code;
    }
}