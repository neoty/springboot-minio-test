package io.melong.miniodemo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
public class FileController {

    @GetMapping("/")
    public String form() {
        return "uploadForm";
    }

    @PostMapping("/")
    public String upload(@RequestParam("file") MultipartFile multipartFile) {
        log.info(multipartFile.getOriginalFilename());
        log.info("minio upload success");
        return "redirect:/";
    }
}
