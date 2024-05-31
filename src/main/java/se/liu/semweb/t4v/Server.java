package se.liu.semweb.t4v;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import se.liu.semweb.t4v.owl2shacl.OWL2SHACL;

@SpringBootApplication
@RestController
public class Server {

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @PostMapping("/api/types")
    public String getClasses(@RequestBody Message message) {
        List<String> list = InferenceEngine.getInferredClasses(message.getData(), message.getSchema(),
                message.getTarget());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(list);
    }

    @GetMapping(value = "/api/owl2shacl", produces = {"text/turtle", "application/json"})
    public String getOwl2Shacl(@RequestParam(name = "url", required = true) String url, @RequestHeader Map<String, String> headers) {
        String result = OWL2SHACL.owl2shacl(url);

        String acceptHeader = headers.get("accept");
        if (acceptHeader != null && acceptHeader.equals("application/json")) {
            // JSON
            Map<String, String> map = new HashMap<>();
            map.put("result", result);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(map);
        } else {
            // Plain text
            return result;
        }

    }
}