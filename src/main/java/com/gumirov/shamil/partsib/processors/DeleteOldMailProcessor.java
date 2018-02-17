package com.gumirov.shamil.partsib.processors;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import com.gumirov.shamil.partsib.util.SkipMessageException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static com.gumirov.shamil.partsib.MainRouteBuilder.DAY_MILLIS;

public class DeleteOldMailProcessor implements Processor {
  Logger log = LoggerFactory.getLogger(this.getClass());
  private int daysStore;
  private int passedBy;

  public DeleteOldMailProcessor(int daysStore) {
    this.daysStore = daysStore;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    String subj = exchange.getIn().getHeader("Subject")+"";
    String date = exchange.getIn().getHeader("Date")+"";
    String from = exchange.getIn().getHeader("From")+"";
    //double-check:
    Date oldestStore = new Date(System.currentTimeMillis() - DAY_MILLIS * daysStore);
    Object dateHeader = exchange.getIn().getHeader("Date");
    if (dateHeader == null) throw new SkipMessageException("No Date header for mail. Skipping: subj="+subj);
    Date mailDate = MainRouteBuilder.mailDateFormat.parse((String)dateHeader);
    if (mailDate.before(oldestStore)) {
      log.info(String.format("Delete old: date=%s, subj=%s, from=%s", date, subj, from));
      passedBy++;
    } else {
      log.info(String.format("Skipping this: subj=%s, from=%s, date=%s", subj, from, date));
      throw new SkipMessageException("Skipping this: subj="+subj);
    }
  }

  public int getPassedBy() {
    return passedBy;
  }
}