package com.wangerry.mailcleaner;

import com.wangerry.mailcleaner.config.MailCleanerConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@SpringBootApplication
public class MailCleanerApplication implements CommandLineRunner {
  private final Cleaner cleaner;
  private final MailCleanerConfig mailCleanerConfig;

  public static void main(String[] args) {
    SpringApplication.run(MailCleanerApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    Properties props = new Properties();
    props.setProperty("mail.imap.ssl.enable", "true");
    Session session = Session.getDefaultInstance(props);
    Store store = session.getStore("imap");
    store.connect(mailCleanerConfig.getHost(), mailCleanerConfig.getUser(), mailCleanerConfig.getPass());

    while (true) {
      Folder[] list = store.getDefaultFolder().list();
      if (list.length == 0) {
        log.warn("query folders failed, maybe is connecting, retry after 5 secs");
        Thread.sleep(5000);
      } else {
        break;
      }
    }

    log.info("All folders");
    Map<String, Folder> folderMap = cleaner.printAllFolder(store.getDefaultFolder().list());
    Map<Folder, MailCleanerConfig.Rule> toClean = new HashMap<>();
    Map<String, MailCleanerConfig.Rule> ruleMap = mailCleanerConfig.getRules().stream().collect(Collectors.toMap(MailCleanerConfig.Rule::getName, Function.identity()));
    MailCleanerConfig.Rule defaultRule = new MailCleanerConfig.Rule("default", mailCleanerConfig.getDays());

    // 获取排除或包括的文件夹，优先includes，再excludes
    List<String> includes;
    if (!mailCleanerConfig.getFolders().getIncludes().isEmpty()) {
      includes = mailCleanerConfig.getFolders().getIncludes();
    } else {
      List<String> excludes = mailCleanerConfig.getFolders().getExcludes();
      includes = folderMap.keySet().stream().filter(s -> !excludes.contains(s)).collect(Collectors.toList());
    }

    for (String includeFolder : includes) {
      Folder folder = folderMap.get(includeFolder);
      if (folder == null) {
        log.warn("include folder {} not exists, ignored", includeFolder);
      } else {
        MailCleanerConfig.Rule rule = ruleMap.get(includeFolder);
        if (rule == null) {
          rule = defaultRule;
        }
        toClean.put(folder, rule);
      }
    }

    log.info("clean rules:");
    toClean.forEach((k, v) -> log.info(" - folder {} @ {} days", k.getFullName(), v.getDays()));

    log.info("cleaning:");
    toClean.forEach((k, v) -> {
      try {
        cleaner.clean(k, v.getDays());
      } catch (Exception e) {
        log.error("clean folder {} failed", k, e);
      }
    });

    store.close();
    log.info("finished!");
  }
}
