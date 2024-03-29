package io.melong.miniodemo;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                .build());

        ArrayList<String> objectNames = new ArrayList<>();
        for (Result<Item> result : results) {
            objectNames.add(result.get().objectName());
        }
        model.addAttribute("objectNames", objectNames);

        return "uploadForm";
    }

    @PostMapping("/")
    public String upload(@RequestParam("file") MultipartFile multipartFile,
                         RedirectAttributes redirectAttributes
    ) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        if (! minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            log.info("bucket created");
        }

        if (multipartFile.getSize() < 1) {
            redirectAttributes.addFlashAttribute("message", "is correct file?");
            return "redirect:/";
        }

        Map<String, String> metas = new HashMap<>();
        metas.put("name", multipartFile.getOriginalFilename());

        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(UUID.randomUUID().toString())
                        .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                        .contentType(multipartFile.getContentType())
                        .userMetadata(metas)
                .build());

        redirectAttributes.addFlashAttribute("message", "upload success");
        return "redirect:/";
    }

    @GetMapping("/{objectId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable("objectId") String objectId,
                                                        RedirectAttributes redirectAttributes) {
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectId)
                    .build());
            StatObjectResponse statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(objectId)
                    .build());
            Map<String, String> metas = statObjectResponse.userMetadata();

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, statObjectResponse.contentType());
            headers.add("Content-Disposition",
                    "attachment; filename=\""+ URLEncoder.encode(metas.get("name"), StandardCharsets.UTF_8).replaceAll("\\+", "%20")+"\"");
            return ResponseEntity.ok().headers(headers).body(new InputStreamResource(stream));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostConstruct
    void init() {
        minioClient = MinioClient.builder()
                .endpoint("http://localhost", 9000, false)
                .credentials("minio", "minio123")
                .build();
    }
}
