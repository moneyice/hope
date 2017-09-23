package hope.stock.controller;

import com.google.common.io.Files;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@RestController
public class GreetingController {
    @RequestMapping("/greeting")
    public String greeting(
            @RequestParam(value = "name", defaultValue = "World") String name) {
        return "hello " + name;


    }
    @RequestMapping("/testCreateFiles")
    @Async
    public void testCreateFiles(
            @RequestParam(value = "number", defaultValue = "1000") String number) {

        try {
            File src = new File("/home/working/test", "base");
            String  content = Files.readFirstLine(src, Charset.forName("UTF-8"));

            int size=Integer.parseInt(number);
            for (int i=1;i<=size;i++){
                String fileName="F"+i;
                File to =new File("/home/working/test", fileName);
                try{
                    Files.write(content, to, Charset.forName("UTF-8"));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }




}
