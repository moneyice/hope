package hope.analyzer.dao;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;


@Repository
@RefreshScope
public class ReportDAO4Redis {

    public static final int LITE_LEAST_DAY_NUMBER=12*22;// about one year

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public ReportDAO4Redis() {
    }

    public void storeReport(String filename,String report) {
        stringRedisTemplate.opsForValue().set(filename,report);
    }

    public String getReport(String filename) {
        String rs=  stringRedisTemplate.opsForValue().get(filename);
        return rs;
    }

    public void clearReport(String filename) {
       stringRedisTemplate.delete(filename);
    }
}
