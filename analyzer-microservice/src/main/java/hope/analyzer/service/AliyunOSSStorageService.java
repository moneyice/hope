package hope.analyzer.service;

import java.io.*;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.OSSObject;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This sample demonstrates how to upload/download an object to/from
 * Aliyun OSS using the OSS SDK for Java.
 */
@Service
public class AliyunOSSStorageService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private static String endpoint = "oss-cn-zhangjiakou.aliyuncs.com";
    private static String accessKeyId = "youraccessKeyId";
    private static String accessKeySecret = "youraccessKeySecret";
    private static String bucketName = "hhope";
    private static String key = "samplekey";
    private  OSSClient ossClient;
    private InputStream inputStream;

    public AliyunOSSStorageService() {

    }

    public void put(String fileName, String content){
        try {
            ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucketName, fileName, new ByteArrayInputStream(content.getBytes()));
        } catch (ClientException ce) {
            logger.error("store to aliyun OSS error " + fileName,ce);
        } finally {
            ossClient.shutdown();
        }
    }

    public String get(String fileName){
        InputStream inputStream=null;
        try {
            ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
            OSSObject object = ossClient.getObject(bucketName, fileName);
            inputStream = object.getObjectContent();

            String result = toString(inputStream);
            return result;
        } catch (Exception ce) {
            logger.error("store to OSS error",ce);
        } finally {
            IOUtils.safeClose(inputStream);
            ossClient.shutdown();
        }
        return null;
    }



    private String toString(InputStream inputStream)  {
        ByteArrayOutputStream result=null;
        try {
            result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String s = result.toString("UTF-8");
            return s;
        }catch (Exception e){
            logger.error("toString error",e);
        }finally {
            IOUtils.safeClose(inputStream);
            IOUtils.safeClose(result);
        }
        return null;
    }

//    public static void estsdf(String[] args) throws IOException {
//        /*
//         * Constructs a client instance with your account for accessing OSS
//         */
//
//
//        try {
//
//            /**
//             * Note that there are two ways of uploading an object to your bucket, the one
//             * by specifying an input stream as content source, the other by specifying a file.
//             */
//
//            /*
//             * Upload an object to your bucket from an input stream
//             */
//            System.out.println("Uploading a new object to OSS from an input stream\n");
//            String content = "Thank you for using Aliyun Object Storage Service";
//            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
//
//            /*
//             * Upload an object to your bucket from a file
//             */
//            System.out.println("Uploading a new object to OSS from a file\n");
//            ossClient.putObject(new PutObjectRequest(bucketName, key, createSampleFile()));
//
//            /*
//             * Download an object from your bucket
//             */
//            System.out.println("Downloading an object");
//            OSSObject object = ossClient.getObject(new GetObjectRequest(bucketName, key));
//            System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
//            displayTextInputStream(object.getObjectContent());
//
//        } catch (OSSException oe) {
//            System.out.println("Caught an OSSException, which means your request made it to OSS, "
//                    + "but was rejected with an error response for some reason.");
//            System.out.println("Error Message: " + oe.getErrorCode());
//            System.out.println("Error Code:       " + oe.getErrorCode());
//            System.out.println("Request ID:      " + oe.getRequestId());
//            System.out.println("Host ID:           " + oe.getHostId());
//        } catch (ClientException ce) {
//            System.out.println("Caught an ClientException, which means the client encountered "
//                    + "a serious internal problem while trying to communicate with OSS, "
//                    + "such as not being able to access the network.");
//            System.out.println("Error Message: " + ce.getMessage());
//        } finally {
//            /*
//             * Do not forget to shut down the client finally to release all allocated resources.
//             */
//            ossClient.shutdown();
//        }
//    }



}
