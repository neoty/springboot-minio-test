package io.melong.miniodemo;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Controller
public class FileController {



    private MinioClient minioClient;
    private static final String BUCKET_NAME = "test";
    @GetMapping("/")
    public String form(Model model) throws Exception {
        return "uploadForm";
    }

    @PostMapping("/")
    public String upload(@RequestParam("file") MultipartFile multipartFile) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        if (! minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            log.info("bucket created");
        }

        Map<String, String> tags = new HashMap<>();
        tags.put("fileName", multipartFile.getOriginalFilename());

        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(UUID.randomUUID().toString())
                        .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                        .contentType(multipartFile.getContentType())
                        .tags(tags)
                .build());
        log.info("minio upload success");

        return "redirect:/";
    }

    private void getMinioClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint("http://localhost", 9000, false)
                    .credentials("minio", "minio123")
                    .build();
        }
    }

    @PostConstruct
    void init() {
        getMinioClient();
    }
}
