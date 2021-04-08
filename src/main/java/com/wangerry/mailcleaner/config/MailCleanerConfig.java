package com.wangerry.mailcleaner.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "cleaner")
public class MailCleanerConfig {
  @NotEmpty(message = "必须指定邮箱imap地址 [cleaner.host]")
  private String host;

  @NotEmpty(message = "必须指定用户名 [cleaner.user]")
  private String user;

  @NotEmpty(message = "必须指定密码 [cleaner.pass]")
  private String pass;

  private Folders folders = new Folders();

  @NotNull(message = "必须指定过期天数 [cleaner.days]")
  private Integer days;

  private List<Rule> rules = new ArrayList<>();

  @Data
  public static class Folders {
    private List<String> excludes = new ArrayList<>();
    private List<String> includes = new ArrayList<>();
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Rule {
    private String name;
    private Integer days;
  }
}
