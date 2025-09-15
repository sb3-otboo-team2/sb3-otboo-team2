package org.ikuzo.otboo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "org.ikuzo.otboo")
public class OtbooApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtbooApplication.class, args);
    }

}
