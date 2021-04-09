package com.wangerry.mailcleaner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class Cleaner {

  public Map<String, Folder> printAllFolder(Folder[] folders) throws MessagingException {
    Map<String, Folder> folderMap = new HashMap<>();
    for (Folder folder : folders) {
      if (folder.list().length == 0) {
        log.info(" - folder {}", folder);
        folderMap.put(folder.getFullName(), folder);
      } else {
        folderMap.putAll(printAllFolder(folder.list()));
      }
    }
    return folderMap;
  }

  public void clean(Folder folder, int days) throws MessagingException {
    Date daysBeforeNow = getDaysBeforeNow(days);

    log.info(" * Clean {} before {}", folder, dateTimeToString(daysBeforeNow));
    folder.open(Folder.READ_WRITE);
    int messageCount = folder.getMessageCount();
    log.info("   % total count {}", messageCount);
    if (messageCount == 0) {
      return;
    }

    while (folder.getMessage(1).getSentDate().before(daysBeforeNow)) {
      messageCount = folder.getMessageCount();
      if (messageCount == 0) {
        return;
      }
      int left = 1;
      int right = messageCount;
      int point = (right - left) / 2 + 1;

      log.info(" |- finding point:");
      while (left < point && right > point) {
        Message message = folder.getMessage(point);
        log.info("   |- {}, {} / {}", point, dateTimeToString(message.getSentDate()), message.getSubject());
        if (message.getSentDate().before(daysBeforeNow)) {
          left = point;
        } else {
          right = point;
        }
        point = (right - left) / 2 + left;
      }

      log.info(" |- delete before point {}", point);
      int step = 1000;
      int currentPoint = 1;
      while (currentPoint <= point) {
        int end = step;
        if (point - currentPoint < step) {
          end = point - currentPoint + 1;
        }
        if (end < step) {
          log.info("   |- delete {}", end);
        } else {
          log.info("   |- delete {} - {}", currentPoint, currentPoint + end);
        }
        folder.setFlags(1, end, new Flags(Flags.Flag.DELETED), true);
        folder.expunge();
        currentPoint += end;
      }
    }

    folder.close();
  }

  public void printMailDateTime(Folder folder) throws MessagingException {
    folder.open(Folder.READ_ONLY);
    for (Message message : folder.getMessages()) {
      log.info(" - {}, {}", dateTimeToString(message.getReceivedDate()), message.getSubject());
    }
  }

  private Date getDaysBeforeNow(int days) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -1 * days);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    return calendar.getTime();
  }

  private String dateTimeToString(Date date) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return dateFormat.format(date);
  }
}
