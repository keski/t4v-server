package se.liu.semweb.t4v;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        List<Object[]> map = InferenceEngine.getInferredClasses(message.getData(), message.getSchema(),message.getTarget());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonArray = gson.toJson(map);
        return jsonArray;
    }

    @GetMapping("/api/owl2shacl")
    public String getOwl2Shacl(@RequestParam(name = "data", required = true) String uri) {
        String s = OWL2SHACL.owl2shacl(uri);
        try {
            FileWriter fw = new FileWriter(new File("test.ttl"));
            fw.write(s);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }
}